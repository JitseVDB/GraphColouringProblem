import java.util.*;

public class IteratedLocalSearch {

    private final Graph graph;
    private final long timeLimitMillis;

    // Algorithm Parameters
    private static final int A = 10;
    private static final double DELTA = 0.6;
    private static final int MAX_ITERATIONS_WITHOUT_IMPROVEMENT = 1000; // Lowered for responsiveness
    private static final int PERTURBATION_STRENGTH = 2;

    // NEW: Stop trying a specific K if we have perturbed this many times with no success
    private static final int MAX_PERTURBATIONS_PER_K = 100;

    private Random random;

    public IteratedLocalSearch(Graph graph, long timeLimitMillis) {
        this.graph = graph;
        this.timeLimitMillis = timeLimitMillis;
        this.random = new Random(12345);
    }

    public void solve() {
        long startTime = System.currentTimeMillis();

        int initialK = graph.getNumberOfUsedColors();
        int[] currentColors = Arrays.copyOf(graph.getColorArray(), graph.getTotalVertices());

        int[] bestGlobalColors = Arrays.copyOf(currentColors, currentColors.length);
        int bestK = initialK;

        // System.out.println("ILS Start: Initial K = " + bestK);

        while (System.currentTimeMillis() - startTime < timeLimitMillis) {

            int targetK = bestK - 1;

            // 1. Hard Stop: Impossible to color with less than 1 color
            if (targetK < 1) break;

            // 2. Hard Stop: Impossible to color with 1 color if edges exist
            if (targetK == 1 && graph.getNumberOfEdges() > 0) break;

            int[] workingColors = squashColors(bestGlobalColors, bestK, targetK);
            SolutionState state = new SolutionState(graph, workingColors, targetK);

            // 3. Run Tabu Search with strict limits
            boolean solved = runTabuSearch(state, startTime);

            if (solved) {
                bestK = targetK;
                bestGlobalColors = Arrays.copyOf(state.colors, state.colors.length);
                applySolutionToGraph(bestGlobalColors);
                // System.out.println("Found valid coloring for k=" + bestK);
            } else {
                // If we failed to color for targetK after trying hard (max perturbations),
                // we assume optimal K is reached.
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

    private int[] squashColors(int[] validColors, int oldK, int newK) {
        int[] newColors = Arrays.copyOf(validColors, validColors.length);
        for (int i = 0; i < newColors.length; i++) {
            if (!graph.isActive(i)) continue;
            if (newColors[i] >= newK) {
                newColors[i] = random.nextInt(newK);
            }
        }
        return newColors;
    }

    private boolean runTabuSearch(SolutionState state, long startTime) {
        int[][] tabuMatrix = new int[graph.getTotalVertices()][state.k];
        long iterations = 0;
        long lastImprovementIter = 0;
        int bestConflicts = state.totalConflicts;

        // Count how many times we've tried to "kick" the solution
        int perturbationCount = 0;

        // Scale max iterations based on graph size (small graphs finish fast, large graphs get more time)
        // For your test cases (size < 100), this prevents spinning for 10 seconds.
        long maxTotalIterations = 10000L * Math.max(graph.getNumberOfNodes(), 10);

        while (System.currentTimeMillis() - startTime < timeLimitMillis) {

            if (state.totalConflicts == 0) return true;

            // NEW: Safety Break for impossible K
            if (perturbationCount >= MAX_PERTURBATIONS_PER_K) return false;
            if (iterations > maxTotalIterations) return false;

            // Perturbation Condition
            if (iterations - lastImprovementIter > MAX_ITERATIONS_WITHOUT_IMPROVEMENT) {
                perturb(state);
                tabuMatrix = new int[graph.getTotalVertices()][state.k];
                lastImprovementIter = iterations;
                perturbationCount++; // Increment "give up" counter
            }

            int bestNode = -1;
            int bestColor = -1;
            int bestDelta = Integer.MAX_VALUE;

            int conflictCount = state.getConflictingNodesCount();
            int tabuTenure = random.nextInt(A) + (int)(DELTA * conflictCount);

            List<Integer> conflictList = state.getConflictingNodes();
            List<Move> equalMoves = new ArrayList<>();

            for (int u : conflictList) {
                int oldColor = state.colors[u];
                for (int c = 0; c < state.k; c++) {
                    if (c == oldColor) continue;

                    int delta = state.adjCounts[u][c] - state.adjCounts[u][oldColor];
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
                Move chosen = equalMoves.get(random.nextInt(equalMoves.size()));
                int oldC = state.colors[chosen.u];
                tabuMatrix[chosen.u][oldC] = (int)(iterations + tabuTenure);
                state.updateColor(chosen.u, chosen.c);
                if (state.totalConflicts < bestConflicts) {
                    bestConflicts = state.totalConflicts;
                    lastImprovementIter = iterations;
                    // Reset perturbation count on improvement
                    // because we are making progress!
                    perturbationCount = 0;
                }
            } else {
                perturb(state);
                tabuMatrix = new int[graph.getTotalVertices()][state.k];
                perturbationCount++;
            }
            iterations++;
        }
        return false;
    }

    private void perturb(SolutionState state) {
        Set<Integer> targetClasses = new HashSet<>();
        while(targetClasses.size() < Math.min(PERTURBATION_STRENGTH, state.k)) {
            targetClasses.add(random.nextInt(state.k));
        }

        List<Integer> nodesToMove = new ArrayList<>();
        for (int v : graph.getNodes()) {
            if (targetClasses.contains(state.colors[v])) {
                nodesToMove.add(v);
            }
        }

        for (int v : nodesToMove) {
            int oldColor = state.colors[v];
            int bestC = -1;
            int minConf = Integer.MAX_VALUE;
            int startC = random.nextInt(state.k);

            for (int i = 0; i < state.k; i++) {
                int c = (startC + i) % state.k;
                if (c == oldColor) continue;
                int conf = state.adjCounts[v][c];
                if (conf < minConf) {
                    minConf = conf;
                    bestC = c;
                }
            }
            if (bestC != -1) state.updateColor(v, bestC);
        }
    }

    private static class SolutionState {
        int[] colors;
        int[][] adjCounts;
        int totalConflicts;
        int k;
        Graph graph;

        SolutionState(Graph g, int[] initColors, int k) {
            this.graph = g;
            this.k = k;
            this.colors = Arrays.copyOf(initColors, initColors.length);
            this.adjCounts = new int[g.getTotalVertices()][k];
            this.totalConflicts = 0;

            for (int u : g.getNodes()) {
                for (int v : g.getNeighborsOf(u)) {
                    adjCounts[u][colors[v]]++;
                }
            }
            for (int u : g.getNodes()) {
                totalConflicts += adjCounts[u][colors[u]];
            }
            totalConflicts /= 2;
        }

        void updateColor(int u, int newColor) {
            int oldColor = colors[u];
            totalConflicts = totalConflicts - adjCounts[u][oldColor] + adjCounts[u][newColor];
            colors[u] = newColor;
            for (int v : graph.getNeighborsOf(u)) {
                adjCounts[v][oldColor]--;
                adjCounts[v][newColor]++;
            }
        }

        List<Integer> getConflictingNodes() {
            List<Integer> list = new ArrayList<>();
            for (int u : graph.getNodes()) {
                if (adjCounts[u][colors[u]] > 0) list.add(u);
            }
            return list;
        }

        int getConflictingNodesCount() {
            int count = 0;
            for (int u : graph.getNodes()) {
                if (adjCounts[u][colors[u]] > 0) count++;
            }
            return count;
        }
    }

    private static class Move {
        int u, c;
        Move(int u, int c) { this.u = u; this.c = c; }
    }
}