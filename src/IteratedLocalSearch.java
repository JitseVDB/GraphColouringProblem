import java.util.*;

public class IteratedLocalSearch {

    private final Graph graph;
    private final long timeLimitMillis;
    private final Random random;

    // Parameters
    private final int TABU_TENURE_BASE;
    private final double TABU_TENURE_MULTI;
    // Lowered slightly: if we don't improve in 1000 iter on a small graph, we are stuck.
    private final int MAX_ITERATIONS_WITHOUT_IMPROVEMENT;

    // Safety
    private final int MAX_PERTURBATIONS_PER_K;

    // Constructor with parameter inputs
    public IteratedLocalSearch(Graph graph, long timeLimitMillis,
                               int tabuTenureBase,
                               double tabuTenureMulti,
                               int maxIterationsWithoutImprovement,
                               int maxPerturbationsPerK) {
        this.graph = graph;
        this.timeLimitMillis = timeLimitMillis;
        this.random = new Random(12345);

        this.TABU_TENURE_BASE = tabuTenureBase;
        this.TABU_TENURE_MULTI = tabuTenureMulti;
        this.MAX_ITERATIONS_WITHOUT_IMPROVEMENT = maxIterationsWithoutImprovement;
        this.MAX_PERTURBATIONS_PER_K = maxPerturbationsPerK;
    }

    /**
     * Auto-Config Constructor:
     * Calculates Density and Size to automatically select the best parameter logic.
     */
    public IteratedLocalSearch(Graph graph, long timeLimitMillis) {
        this.graph = graph;
        this.timeLimitMillis = timeLimitMillis;
        this.random = new Random(12345);

        // 1. Calculate Graph Metrics
        int n = graph.getTotalVertices();
        int e = graph.getNumberOfEdges();

        // Safety check to avoid division by zero
        double density = (n > 1) ? (2.0 * e) / (double) (n * (n - 1)) : 0.0;

        // 2. Logic Cascade to set Parameters directly

        // CASE: PATIENT (High Density)
        // Very dense graphs have tight constraints and short cycles.
        // We need high tenure to prevent cycling and high iterations to dig deep.
        if (density >= 0.75) {
            this.TABU_TENURE_BASE = 20;
            this.TABU_TENURE_MULTI = 0.8;
            this.MAX_ITERATIONS_WITHOUT_IMPROVEMENT = 4000;
            this.MAX_PERTURBATIONS_PER_K = 50;
            System.out.println("Mode: PATIENT (High Density: " + String.format("%.2f", density) + ")");
        }

        // CASE: HIGH NOISE (Large Scale or Medium-Dense Traps)
        // Large graphs (wap) or medium-dense structures (latin square) have deep local optima.
        // We need massive perturbations (300) to kick the solver out of traps.
        else if (n > 1500 || (n > 800 && density > 0.5)) {
            this.TABU_TENURE_BASE = 12;
            this.TABU_TENURE_MULTI = 0.6;
            this.MAX_ITERATIONS_WITHOUT_IMPROVEMENT = 800;
            this.MAX_PERTURBATIONS_PER_K = 300;
            System.out.println("Mode: HIGH NOISE (Large/Complex Graph)");
        }

        // CASE: AGGRESSIVE (Sparse or Small)
        // Sparse or small graphs are easy to traverse. Speed is key.
        // We restart frequently (low iterations) and move fast (low tenure).
        else if (density < 0.2 || n < 300) {
            this.TABU_TENURE_BASE = 5;
            this.TABU_TENURE_MULTI = 0.4;
            this.MAX_ITERATIONS_WITHOUT_IMPROVEMENT = 500;
            this.MAX_PERTURBATIONS_PER_K = 200;
            System.out.println("Mode: AGGRESSIVE (Sparse/Small Graph)");
        }

        // CASE: BALANCED (The Middle Ground)
        // For standard random graphs (DSJC...5) where no extreme strategy dominates.
        else {
            this.TABU_TENURE_BASE = 10;
            this.TABU_TENURE_MULTI = 0.6;
            this.MAX_ITERATIONS_WITHOUT_IMPROVEMENT = 1500;
            this.MAX_PERTURBATIONS_PER_K = 100;
            System.out.println("Mode: BALANCED (Standard Random Graph)");
        }
    }

    public void solve() {
        long startTime = System.currentTimeMillis();

        int initialK = graph.getNumberOfUsedColors();
        int[] currentColors = Arrays.copyOf(graph.getColorArray(), graph.getTotalVertices());

        if (initialK == 0) {
            initialK = graph.getNumberOfNodes();
            for(int i=0; i<graph.getTotalVertices(); i++) currentColors[i] = i;
        }

        // Apply a quick cleanup to the input colors before starting
        // (Sometimes RLF leaves valid colors but poorly distributed)
        applySolutionToGraph(currentColors);

        int bestK = initialK;
        int[] bestGlobalColors = Arrays.copyOf(currentColors, currentColors.length);

        while (System.currentTimeMillis() - startTime < timeLimitMillis) {
            int targetK = bestK - 1;
            if (targetK < 1) break;

            // 1. Reduce K: Squash colors
            int[] workingColors = smartSquashColors(bestGlobalColors, bestK, targetK);

            // 2. Build High-Performance State
            SolutionState state = new SolutionState(graph, workingColors, targetK);

            // 3. Run Tabu Search
            boolean solved = runTabuSearch(state, startTime);

            if (solved) {
                bestK = targetK;
                bestGlobalColors = Arrays.copyOf(state.colors, state.colors.length);
                applySolutionToGraph(bestGlobalColors);
                // System.out.println("Improved to k=" + bestK + " Time: " + (System.currentTimeMillis() - startTime) + "ms");
            } else {
                break;
            }
        }
    }

    private void applySolutionToGraph(int[] colors) {
        for (int i = 0; i < colors.length; i++) {
            if (graph.isActive(i)) graph.colorNode(i, colors[i]);
        }
    }

    private int[] smartSquashColors(int[] validColors, int oldK, int newK) {
        int[] newColors = Arrays.copyOf(validColors, validColors.length);
        List<Integer> nodesToMove = new ArrayList<>();

        for (int i = 0; i < newColors.length; i++) {
            if (graph.isActive(i) && newColors[i] >= newK) {
                nodesToMove.add(i);
            }
        }
        Collections.shuffle(nodesToMove, random);

        Map<Integer, BitSet> adj = graph.getAdjCopy();

        // Greedy placement for squashed nodes
        for (int u : nodesToMove) {
            int bestColor = 0;
            int minConflicts = Integer.MAX_VALUE;

            // Random start index to avoid packing color 0
            int startC = random.nextInt(newK);

            for (int i = 0; i < newK; i++) {
                int c = (startC + i) % newK;
                int conflicts = 0;
                BitSet neighbors = adj.get(u);
                for (int v = neighbors.nextSetBit(0); v >= 0; v = neighbors.nextSetBit(v + 1)) {
                    if (newColors[v] == c) conflicts++;
                }
                if (conflicts < minConflicts) {
                    minConflicts = conflicts;
                    bestColor = c;
                    if (conflicts == 0) break;
                }
            }
            newColors[u] = bestColor;
        }
        return newColors;
    }

    private boolean runTabuSearch(SolutionState state, long startTime) {
        // Tabu Matrix: Stores the iteration number UNTIL which a move is tabu
        // Flattened for performance [node * k + color]
        int[] tabuMatrix = new int[graph.getTotalVertices() * state.k];

        long iterations = 0;
        long lastImprovementIter = 0;
        int bestConflicts = state.totalConflicts;
        int perturbationCount = 0;

        // More iterations for smaller graphs (they run faster), fewer for massive ones
        long maxTotalIterations = 100000L;

        while (System.currentTimeMillis() - startTime < timeLimitMillis) {
            if (state.totalConflicts == 0) return true;

            // Perturbation check
            if (iterations - lastImprovementIter > MAX_ITERATIONS_WITHOUT_IMPROVEMENT) {
                if (perturbationCount >= MAX_PERTURBATIONS_PER_K) return false;

                perturb(state);
                Arrays.fill(tabuMatrix, 0); // Reset memory
                lastImprovementIter = iterations;
                perturbationCount++;
            }

            // --- CRITICAL PERFORMANCE SECTION ---

            int bestNode = -1;
            int bestColor = -1;
            int bestDelta = Integer.MAX_VALUE;
            int tieCount = 0;

            // Fast iteration over conflict set (No Iterator objects created)
            int conflictCount = state.getConflictCount();
            int[] conflictArr = state.getConflictingNodesArray();
            int k = state.k;
            int totalV = graph.getTotalVertices();

            // Dynamic Tenure based on conflict size
            int tabuTenure = TABU_TENURE_BASE + (int)(TABU_TENURE_MULTI * conflictCount);

            for (int i = 0; i < conflictCount; i++) {
                int u = conflictArr[i];
                int oldColor = state.colors[u];
                int uOffset = u * k; // Precompute offset for flattened array

                // Check all colors
                for (int c = 0; c < k; c++) {
                    if (c == oldColor) continue;

                    // Fast Lookup in flattened array
                    int delta = state.adjCounts[uOffset + c] - state.adjCounts[uOffset + oldColor];

                    // Check Tabu
                    boolean isTabu = tabuMatrix[uOffset + c] > iterations;

                    // Aspiration
                    if (isTabu && (state.totalConflicts + delta < bestConflicts)) {
                        isTabu = false;
                    }

                    if (!isTabu) {
                        if (delta < bestDelta) {
                            bestNode = u;
                            bestColor = c;
                            bestDelta = delta;
                            tieCount = 1;
                        } else if (delta == bestDelta) {
                            // Reservoir Sampling: Avoid creating a List<Move>
                            tieCount++;
                            if (random.nextInt(tieCount) == 0) {
                                bestNode = u;
                                bestColor = c;
                            }
                        }
                    }
                }
            }

            if (bestNode != -1) {
                int oldC = state.colors[bestNode];

                // Update Tabu (flattened index)
                tabuMatrix[bestNode * k + oldC] = (int)(iterations + tabuTenure);

                state.updateColor(bestNode, bestColor);

                if (state.totalConflicts < bestConflicts) {
                    bestConflicts = state.totalConflicts;
                    lastImprovementIter = iterations;
                    perturbationCount = 0;
                }
            } else {
                // Dead end (all moves Tabu)
                perturb(state);
                lastImprovementIter = iterations;
            }

            iterations++;
        }
        return false;
    }

    private void perturb(SolutionState state) {
        int conflictCount = state.getConflictCount();
        if (conflictCount == 0) return;

        int[] conflictArr = state.getConflictingNodesArray();

        // Strength: if 1 conflict, kick hard. If 100, kick soft.
        int kickStrength = Math.max(1, Math.min(conflictCount, 4));

        for (int i = 0; i < kickStrength; i++) {
            // Pick random conflict
            int idx = random.nextInt(conflictCount);
            int u = conflictArr[idx];

            // Force random color
            int newC = random.nextInt(state.k);
            if (newC == state.colors[u]) newC = (newC + 1) % state.k;

            state.updateColor(u, newC);
        }

        // Also kick one random non-conflicting node to change topology
        int rnd = random.nextInt(graph.getTotalVertices());
        if (graph.isActive(rnd)) {
            int newC = random.nextInt(state.k);
            if (newC != state.colors[rnd]) state.updateColor(rnd, newC);
        }
    }

    /**
     * Highly Optimized State Class.
     * Uses flattened arrays and O(1) Set operations.
     */
    private static class SolutionState {
        final Graph graph;
        final int k;
        final int[] colors;

        // Flattened Adjacency Counts: adjCounts[u * k + c]
        // This improves cache locality significantly over int[][]
        final int[] adjCounts;

        int totalConflicts;

        // Custom "Set" implementation using arrays for O(1) random access and iteration
        // WITHOUT Integer object allocation.
        private final int[] conflictingNodes;
        private final int[] nodeIndices; // Maps nodeID -> index in conflictingNodes
        private int conflictCount;

        SolutionState(Graph g, int[] initColors, int k) {
            this.graph = g;
            this.k = k;
            this.colors = Arrays.copyOf(initColors, initColors.length);

            int numNodes = g.getTotalVertices();
            this.adjCounts = new int[numNodes * k];
            this.conflictingNodes = new int[numNodes];
            this.nodeIndices = new int[numNodes];
            Arrays.fill(nodeIndices, -1);
            this.conflictCount = 0;

            // Initial Scan
            for (int u : g.getNodes()) {
                int uColor = colors[u];
                BitSet neighbors = g.getAdjacencyRules(u); // Direct BitSet Access

                int uOffset = u * k;

                for (int v = neighbors.nextSetBit(0); v >= 0; v = neighbors.nextSetBit(v + 1)) {
                    adjCounts[uOffset + colors[v]]++;
                }

                if (adjCounts[uOffset + uColor] > 0) {
                    addConflict(u);
                }
            }

            // Calculate initial global conflict
            long sum = 0;
            for (int i = 0; i < conflictCount; i++) {
                int u = conflictingNodes[i];
                sum += adjCounts[u * k + colors[u]];
            }
            this.totalConflicts = (int)(sum / 2);
        }

        // O(1) Add to Set
        private void addConflict(int u) {
            if (nodeIndices[u] == -1) {
                conflictingNodes[conflictCount] = u;
                nodeIndices[u] = conflictCount;
                conflictCount++;
            }
        }

        // O(1) Remove from Set (Swap with last element)
        private void removeConflict(int u) {
            int idx = nodeIndices[u];
            if (idx != -1) {
                int lastNode = conflictingNodes[conflictCount - 1];

                // Move last node to the empty slot
                conflictingNodes[idx] = lastNode;
                nodeIndices[lastNode] = idx;

                // Clear last slot
                nodeIndices[u] = -1;
                conflictCount--;
            }
        }

        int getConflictCount() { return conflictCount; }
        int[] getConflictingNodesArray() { return conflictingNodes; }

        void updateColor(int u, int newColor) {
            int oldColor = colors[u];
            int uOffset = u * k;

            // 1. Update Global Conflicts
            int oldConflicts = adjCounts[uOffset + oldColor];
            int newConflicts = adjCounts[uOffset + newColor];
            totalConflicts = totalConflicts - oldConflicts + newConflicts;

            colors[u] = newColor;

            // 2. Update Conflict Set for U
            if (newConflicts > 0) addConflict(u);
            else removeConflict(u);

            // 3. Update Neighbors
            // We iterate strictly over neighbors.
            BitSet neighbors = graph.getAdjacencyRules(u);
            for (int v = neighbors.nextSetBit(0); v >= 0; v = neighbors.nextSetBit(v + 1)) {
                int vOffset = v * k;

                // v lost a neighbor of oldColor
                adjCounts[vOffset + oldColor]--;

                // Did v lose a conflict?
                if (colors[v] == oldColor) {
                    if (adjCounts[vOffset + oldColor] == 0) removeConflict(v);
                }

                // v gained a neighbor of newColor
                adjCounts[vOffset + newColor]++;

                // Did v gain a conflict?
                if (colors[v] == newColor) {
                    addConflict(v);
                }
            }
        }
    }
}