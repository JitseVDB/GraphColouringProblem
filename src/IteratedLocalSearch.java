import java.util.*;

/**
 * Optimized Iterated Local Search (ILS) with Tabu Search.
 *
 * Optimizations:
 * 1. Incremental Conflict Tracking: SolutionState now tracks conflicting nodes in a Set.
 * This avoids O(N) scans every iteration, making move evaluation O(|Conflicts| * K).
 * 2. Smart Squash: When reducing colors, nodes are moved to the 'least bad' available color
 * instead of a random one.
 * 3. Adaptive Limits: Iteration counts are tuned to be responsive on small graphs
 * and persistent on large graphs.
 */
public class IteratedLocalSearch {

    private final Graph graph;
    private final long timeLimitMillis;

    // Algorithm Parameters
    private static final int TABU_TENURE_BASE = 15; // Slightly increased for stability
    private static final double TABU_TENURE_MULTI = 0.6;
    private static final int MAX_ITERATIONS_WITHOUT_IMPROVEMENT = 2500;
    private static final int PERTURBATION_STRENGTH = 4; // Kick harder when stuck

    // Safety breaks
    private static final int MAX_PERTURBATIONS_PER_K = 75;

    private final Random random;

    public IteratedLocalSearch(Graph graph, long timeLimitMillis) {
        this.graph = graph;
        this.timeLimitMillis = timeLimitMillis;
        this.random = new Random(12345);
    }

    public void solve() {
        long startTime = System.currentTimeMillis();

        int initialK = graph.getNumberOfUsedColors();
        int[] currentColors = Arrays.copyOf(graph.getColorArray(), graph.getTotalVertices());

        // Ensure the graph has the colors loaded
        if (initialK == 0) {
            // Fallback if graph wasn't colored yet
            initialK = graph.getNumberOfNodes();
            for(int i=0; i<graph.getTotalVertices(); i++) currentColors[i] = i;
        }

        int[] bestGlobalColors = Arrays.copyOf(currentColors, currentColors.length);
        int bestK = initialK;

        while (System.currentTimeMillis() - startTime < timeLimitMillis) {

            int targetK = bestK - 1;

            // Hard Stops
            if (targetK < 1) break;
            if (targetK == 1 && graph.getNumberOfEdges() > 0) break;

            // OPTIMIZATION: Smart Squash instead of Random
            int[] workingColors = smartSquashColors(bestGlobalColors, bestK, targetK);

            SolutionState state = new SolutionState(graph, workingColors, targetK);

            // Run Tabu Search
            boolean solved = runTabuSearch(state, startTime);

            if (solved) {
                bestK = targetK;
                bestGlobalColors = Arrays.copyOf(state.colors, state.colors.length);
                applySolutionToGraph(bestGlobalColors);
            } else {
                // If we couldn't solve for K-1 within the time budget/perturbation limits,
                // we assume the previous K was the optimum found.
                break;
            }
        }
    }

    private void applySolutionToGraph(int[] colors) {
        for (int i = 0; i < colors.length; i++) {
            if (graph.isActive(i)) {
                graph.colorNode(i, colors[i]);
            }
        }
    }

    /**
     * Smart Squash: Moves nodes from removed color class to the best possible
     * existing color (min conflicts), rather than random.
     */
    private int[] smartSquashColors(int[] validColors, int oldK, int newK) {
        int[] newColors = Arrays.copyOf(validColors, validColors.length);

        // Identify nodes that need moving (those with color >= newK)
        List<Integer> nodesToMove = new ArrayList<>();
        for (int i = 0; i < newColors.length; i++) {
            if (graph.isActive(i) && newColors[i] >= newK) {
                nodesToMove.add(i);
            }
        }

        // Shuffle them to prevent bias order
        Collections.shuffle(nodesToMove, random);

        // Access raw adjacency for speed
        Map<Integer, BitSet> adj = graph.getAdjCopy();

        for (int u : nodesToMove) {
            int bestColor = -1;
            int minConflicts = Integer.MAX_VALUE;

            // Try all valid colors 0..newK-1
            for (int c = 0; c < newK; c++) {
                int conflicts = 0;
                BitSet neighbors = adj.get(u);
                for (int v = neighbors.nextSetBit(0); v >= 0; v = neighbors.nextSetBit(v + 1)) {
                    if (newColors[v] == c) {
                        conflicts++;
                    }
                }

                if (conflicts < minConflicts) {
                    minConflicts = conflicts;
                    bestColor = c;
                    // Early exit if 0 conflict found
                    if (conflicts == 0) break;
                }
            }
            newColors[u] = bestColor;
        }
        return newColors;
    }

    private boolean runTabuSearch(SolutionState state, long startTime) {
        int[][] tabuMatrix = new int[graph.getTotalVertices()][state.k];
        long iterations = 0;
        long lastImprovementIter = 0;
        int bestConflicts = state.totalConflicts;
        int perturbationCount = 0;

        // Adaptive Max Iterations:
        // Small graphs (fast iterations) get more loops.
        // Large graphs (slow iterations) get fewer loops, but we rely on the time limit.
        long maxTotalIterations = 20000L * (long)Math.sqrt(graph.getNumberOfNodes());

        while (System.currentTimeMillis() - startTime < timeLimitMillis) {

            if (state.totalConflicts == 0) return true;

            // Check Limits
            if (perturbationCount >= MAX_PERTURBATIONS_PER_K) return false;
            // Only enforce max iterations if we really aren't improving
            if (iterations > maxTotalIterations && perturbationCount > 5) return false;

            // Perturbation Logic
            if (iterations - lastImprovementIter > MAX_ITERATIONS_WITHOUT_IMPROVEMENT) {
                perturb(state);
                // Reset Tabu memory after perturbation to allow freedom
                tabuMatrix = new int[graph.getTotalVertices()][state.k];
                lastImprovementIter = iterations;
                perturbationCount++;
            }

            int bestNode = -1;
            int bestColor = -1;
            int bestDelta = Integer.MAX_VALUE;

            // OPTIMIZATION: Only iterate over known conflicting nodes.
            // This set is maintained incrementally by SolutionState.
            // On a 1000-node graph with 5 conflicts, this is 200x faster than scanning all nodes.
            Set<Integer> conflictingNodes = state.getConflictingNodes();

            // Dynamic Tabu Tenure
            int tabuTenure = random.nextInt(TABU_TENURE_BASE) + (int)(TABU_TENURE_MULTI * conflictingNodes.size());

            // Evaluate Moves
            List<Move> equalMoves = new ArrayList<>(); // To randomize ties

            for (int u : conflictingNodes) {
                int oldColor = state.colors[u];

                // Try moving conflicting node u to every other color
                for (int c = 0; c < state.k; c++) {
                    if (c == oldColor) continue;

                    // Delta = (Conflicts if we move to C) - (Conflicts at current oldColor)
                    // We want the most negative delta (largest reduction)
                    int delta = state.adjCounts[u][c] - state.adjCounts[u][oldColor];

                    // Aspiration Criteria: Ignore tabu if it improves global best
                    boolean isTabu = (tabuMatrix[u][c] > iterations);
                    if (isTabu && (state.totalConflicts + delta < bestConflicts)) {
                        isTabu = false;
                    }

                    if (!isTabu) {
                        if (delta < bestDelta) {
                            bestNode = u;
                            bestColor = c;
                            bestDelta = delta;
                            equalMoves.clear();
                            equalMoves.add(new Move(u, c));
                        } else if (delta == bestDelta) {
                            equalMoves.add(new Move(u, c));
                        }
                    }
                }
            }

            if (!equalMoves.isEmpty()) {
                // Execute best move
                Move chosen = equalMoves.get(random.nextInt(equalMoves.size()));
                int oldC = state.colors[chosen.u];

                // Set Tabu
                tabuMatrix[chosen.u][oldC] = (int)(iterations + tabuTenure);

                // Update State
                state.updateColor(chosen.u, chosen.c);

                if (state.totalConflicts < bestConflicts) {
                    bestConflicts = state.totalConflicts;
                    lastImprovementIter = iterations;
                    perturbationCount = 0; // We are making progress, reset "give up" counter
                }
            } else {
                // No valid non-tabu moves found (rare, but possible in tight constraints)
                perturb(state);
                lastImprovementIter = iterations;
                perturbationCount++;
            }
            iterations++;
        }
        return false;
    }

    private void perturb(SolutionState state) {
        // Targeted Perturbation:
        // Pick a few conflicting nodes and force them to random colors.

        Set<Integer> conflicts = state.getConflictingNodes();
        if (conflicts.isEmpty()) return;

        // FIX: Create a copy of the set to avoid ConcurrentModificationException.
        // We cannot iterate over 'conflicts' while 'updateColor' is modifying it.
        List<Integer> candidates = new ArrayList<>(conflicts);

        // Shuffle to ensure we kick random conflicting nodes, not just the first ones in the Set
        Collections.shuffle(candidates, random);

        int kickSize = Math.min(candidates.size(), PERTURBATION_STRENGTH);

        // 1. Kick conflicting nodes (using the safe copy)
        for (int i = 0; i < kickSize; i++) {
            int u = candidates.get(i);
            int newC = random.nextInt(state.k);

            // Ensure we actually pick a different color
            if (newC == state.colors[u]) {
                newC = (newC + 1) % state.k;
            }

            state.updateColor(u, newC);
        }

        // 2. Kick a random active node to shake structure
        // (This helps escape local optima even if we fixed the specific conflicts above)
        int randomNode = random.nextInt(graph.getTotalVertices());
        // Find a valid active node (simple loop protection)
        for(int i=0; i<100; i++) {
            if (graph.isActive(randomNode)) break;
            randomNode = random.nextInt(graph.getTotalVertices());
        }

        if (graph.isActive(randomNode)) {
            int newC = random.nextInt(state.k);
            if (newC == state.colors[randomNode]) {
                newC = (newC + 1) % state.k;
            }
            state.updateColor(randomNode, newC);
        }
    }

    /**
     * Inner class representing the solution state.
     * Maintains adjacency counts and total conflicts incrementally.
     */
    private static class SolutionState {
        int[] colors;
        // adjCounts[u][c] = number of neighbors of u that have color c
        int[][] adjCounts;
        int totalConflicts;
        int k;
        Graph graph;

        // Optimisation: Incremental Set of conflicting nodes
        private final Set<Integer> conflictingNodes;

        SolutionState(Graph g, int[] initColors, int k) {
            this.graph = g;
            this.k = k;
            this.colors = Arrays.copyOf(initColors, initColors.length);
            this.adjCounts = new int[g.getTotalVertices()][k];
            this.totalConflicts = 0;
            this.conflictingNodes = new HashSet<>();

            // Initial full scan (happens once per K)
            for (int u : g.getNodes()) {
                int uColor = colors[u];
                for (int v : g.getNeighborsOf(u)) {
                    adjCounts[u][colors[v]]++;
                }
                // If u has conflicts (neighbors with same color), add to tracking set
                if (adjCounts[u][uColor] > 0) {
                    conflictingNodes.add(u);
                }
            }

            // Calc total conflicts (sum of all conflict degrees / 2)
            int sum = 0;
            for (int u : conflictingNodes) {
                sum += adjCounts[u][colors[u]];
            }
            totalConflicts = sum / 2;
        }

        /**
         * The critical bottleneck optimization.
         * Updates all data structures incrementally when node u changes to newColor.
         */
        void updateColor(int u, int newColor) {
            int oldColor = colors[u];

            // 1. Update Global Conflict Count
            // Remove old conflicts of u, add new conflicts of u
            totalConflicts = totalConflicts - adjCounts[u][oldColor] + adjCounts[u][newColor];

            // 2. Update Color Array
            colors[u] = newColor;

            // 3. Update 'u' in Conflicting Set
            // If u now has conflicts, add it. If not, remove it.
            if (adjCounts[u][newColor] > 0) {
                conflictingNodes.add(u);
            } else {
                conflictingNodes.remove(u);
            }

            // 4. Update Neighbors
            // We must update the adjCounts for all neighbors, AND check if their conflict status changed.
            for (int v : graph.getNeighborsOf(u)) {

                // v had u as a neighbor of color 'oldColor'. It lost that.
                adjCounts[v][oldColor]--;

                // Check v's status regarding oldColor
                // If v was colored 'oldColor', it just LOST a conflict with u.
                if (colors[v] == oldColor) {
                    if (adjCounts[v][oldColor] == 0) {
                        conflictingNodes.remove(v);
                    }
                }

                // v now has u as a neighbor of color 'newColor'. It gained that.
                adjCounts[v][newColor]++;

                // Check v's status regarding newColor
                // If v is colored 'newColor', it just GAINED a conflict with u.
                if (colors[v] == newColor) {
                    conflictingNodes.add(v);
                }
            }
        }

        Set<Integer> getConflictingNodes() {
            return conflictingNodes;
        }
    }

    private static class Move {
        int u, c;
        Move(int u, int c) { this.u = u; this.c = c; }
    }
}