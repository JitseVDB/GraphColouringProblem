import java.util.*;

public class IteratedLocalSearch {

    private final Graph graph;
    private final long timeLimitMillis;
    private final FastRandom random;

    // Parameters
    private final int TABU_TENURE_BASE;
    private final double TABU_TENURE_MULTI;
    private final int MAX_ITERATIONS_WITHOUT_IMPROVEMENT;
    private final int MAX_PERTURBATIONS_PER_K;

    // Fast Adjacency Cache (The critical optimization)
    private final int[][] fastAdj;

    /**
     * CONSTRUCTOR 1: MANUAL CONFIGURATION
     * Use this if you want to perform grid searches or manual tuning.
     */
    public IteratedLocalSearch(Graph graph, long timeLimitMillis,
                               int tabuTenureBase,
                               double tabuTenureMulti,
                               int maxIterationsWithoutImprovement,
                               int maxPerturbationsPerK) {
        this.graph = graph;
        this.timeLimitMillis = timeLimitMillis;
        this.random = new FastRandom(System.nanoTime());

        // 1. Build Optimized Graph Structure
        this.fastAdj = buildFastAdj(graph);

        // 2. Set Manual Parameters
        this.TABU_TENURE_BASE = tabuTenureBase;
        this.TABU_TENURE_MULTI = tabuTenureMulti;
        this.MAX_ITERATIONS_WITHOUT_IMPROVEMENT = maxIterationsWithoutImprovement;
        this.MAX_PERTURBATIONS_PER_K = maxPerturbationsPerK;
    }

    /**
     * CONSTRUCTOR 2: INTELLIGENT AUTO-CONFIGURATION
     * Dynamically tunes parameters based on Graph Topology (Density & Size).
     * Formulas derived from DeepParameterTuner results on 53 benchmark graphs.
     */
    public IteratedLocalSearch(Graph graph, long timeLimitMillis) {
        this.graph = graph;
        this.timeLimitMillis = timeLimitMillis;
        // Use a fixed seed for reproducibility during grading/testing
        this.random = new FastRandom(12345);

        // 1. Build Optimized Graph Structure
        this.fastAdj = buildFastAdj(graph);

        // 2. Calculate Metrics
        int n = graph.getTotalVertices();
        int e = graph.getNumberOfEdges();
        // Density formula: 2E / (V * (V-1))
        double density = (n > 1) ? (2.0 * e) / (double) (n * (n - 1)) : 0.0;

        // 3. APPLY TUNING LOGIC BASED ON RESULTS

        // Safety check for tiny graphs (prevent weird behavior on trivial cases)
        if (n < 50) {
            this.TABU_TENURE_BASE = 5;
            this.TABU_TENURE_MULTI = 0.5;
            this.MAX_ITERATIONS_WITHOUT_IMPROVEMENT = 100;
            this.MAX_PERTURBATIONS_PER_K = 10;
            System.out.println(">> Auto-Config: TINY MODE");
        }
        else {
            if (density < 0.15) {
                // --- CASE A: SPARSE GRAPHS (wap, ash, will) ---
                // Result Pattern: High Base Tenure, Moderate Iterations.
                // Reason: Sparse graphs need strong "memory" (Tenure) to escape local optima.

                // Scale base tenure by log of nodes (larger graphs need slightly more tenure)
                this.TABU_TENURE_BASE = (int) (10 + Math.log(n) * 2.5); // Approx 20-30 for large graphs
                this.TABU_TENURE_MULTI = 0.6;

                // Scale iterations by node count, but cap it so huge graphs don't timeout
                this.MAX_ITERATIONS_WITHOUT_IMPROVEMENT = Math.min(n * 5, 5000);
                this.MAX_PERTURBATIONS_PER_K = 150;

                System.out.println(">> Auto-Config: SPARSE MODE (High Tenure)");

            } else if (density >= 0.4 && density <= 0.6) {
                // --- CASE B: PHASE TRANSITION (flat, DSJC.5) ---
                // Result Pattern: Very Low Base Tenure, High Multiplier, VERY High Iterations.
                // Reason: These are the hardest. We need loose restrictions (Low Base) but
                // massive persistence (High Iterations) to find the needle in the haystack.

                this.TABU_TENURE_BASE = 6; // Fixed low base based on 'flat' results
                this.TABU_TENURE_MULTI = 0.9; // Higher reactive multiplier

                // Push iterations high, as these graphs are computationally stubborn
                this.MAX_ITERATIONS_WITHOUT_IMPROVEMENT = Math.max(4000, n * 10);
                this.MAX_PERTURBATIONS_PER_K = 100;

                System.out.println(">> Auto-Config: HARD MODE (Phase Transition, High Iters)");

            } else {
                // --- CASE C: DENSE GRAPHS (queen, latin, DSJC.9) ---
                // Result Pattern: Moderate Base, Low Multiplier.
                // Reason: High density = high conflict count. If Multiplier is high,
                // tenure becomes too large. We suppress Multiplier here.

                this.TABU_TENURE_BASE = 15;
                this.TABU_TENURE_MULTI = 0.3; // Low multiplier to prevent locking the whole graph
                this.MAX_ITERATIONS_WITHOUT_IMPROVEMENT = Math.max(1000, n * 4);
                this.MAX_PERTURBATIONS_PER_K = 50; // Dense graphs stabilize faster

                System.out.println(">> Auto-Config: DENSE MODE (Low Multiplier)");
            }
        }
    }

    /**
     * Helper to convert BitSet/Map graph to Jagged Array int[][]
     * This ensures O(1) adjacency iteration.
     */
    private int[][] buildFastAdj(Graph graph) {
        int n = graph.getTotalVertices();
        int[][] adj = new int[n][];
        for (int i = 0; i < n; i++) {
            if (graph.isActive(i)) {
                BitSet neighbors = graph.getAdjacencyRules(i);
                int deg = neighbors.cardinality();
                adj[i] = new int[deg];
                int idx = 0;
                for (int v = neighbors.nextSetBit(0); v >= 0; v = neighbors.nextSetBit(v + 1)) {
                    adj[i][idx++] = v;
                }
            } else {
                adj[i] = new int[0];
            }
        }
        return adj;
    }

    public void solve() {
        long startTime = System.currentTimeMillis();

        int initialK = graph.getNumberOfUsedColors();
        int[] currentColors = Arrays.copyOf(graph.getColorArray(), graph.getTotalVertices());

        if (initialK == 0) {
            initialK = graph.getTotalVertices();
            for (int i = 0; i < currentColors.length; i++) currentColors[i] = i;
        }

        applySolutionToGraph(currentColors);

        int bestK = initialK;
        int[] bestGlobalColors = Arrays.copyOf(currentColors, currentColors.length);

        while (true) {
            if (System.currentTimeMillis() - startTime >= timeLimitMillis) break;

            int targetK = bestK - 1;
            if (targetK < 1) break;

            // 1. Squash
            int[] workingColors = smartSquashColors(bestGlobalColors, bestK, targetK);

            // 2. Build State (using fastAdj)
            SolutionState state = new SolutionState(fastAdj, workingColors, targetK);

            // 3. Tabu
            boolean solved = runTabuSearch(state, startTime);

            if (solved) {
                bestK = targetK;
                bestGlobalColors = Arrays.copyOf(state.colors, state.colors.length);
                applySolutionToGraph(bestGlobalColors);
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
        int n = newColors.length;

        // Identify nodes to move
        int[] nodesToMove = new int[n];
        int count = 0;
        for (int i = 0; i < n; i++) {
            if (graph.isActive(i) && newColors[i] >= newK) {
                nodesToMove[count++] = i;
            }
        }

        // Shuffle subset (Fisher-Yates)
        for (int i = count - 1; i > 0; i--) {
            int idx = random.nextInt(i + 1);
            int temp = nodesToMove[idx];
            nodesToMove[idx] = nodesToMove[i];
            nodesToMove[i] = temp;
        }

        // Greedy placement
        for (int i = 0; i < count; i++) {
            int u = nodesToMove[i];
            int bestColor = 0;
            int minConflicts = Integer.MAX_VALUE;
            int startC = random.nextInt(newK);

            for (int k = 0; k < newK; k++) {
                int c = (startC + k) % newK;
                int conflicts = 0;
                // Fast Iteration
                for (int v : fastAdj[u]) {
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
        int[] tabuMatrix = new int[graph.getTotalVertices() * state.k];
        long iterations = 0;
        long lastImprovementIter = 0;
        int bestConflicts = state.totalConflicts;
        int perturbationCount = 0;

        while (true) {
            // Optimization: Check time every 1024 iterations
            if ((iterations & 1023) == 0) {
                if (System.currentTimeMillis() - startTime >= timeLimitMillis) return false;
            }

            if (state.totalConflicts == 0) return true;

            // Perturbation Logic
            if (iterations - lastImprovementIter > MAX_ITERATIONS_WITHOUT_IMPROVEMENT) {
                if (perturbationCount >= MAX_PERTURBATIONS_PER_K) return false;
                perturb(state);
                lastImprovementIter = iterations;
                perturbationCount++;
            }

            int bestNode = -1;
            int bestColor = -1;
            int bestDelta = Integer.MAX_VALUE;
            int tieCount = 0;

            int conflictCount = state.conflictCount;
            int[] conflictArr = state.conflictingNodes;
            int k = state.k;

            int tabuTenure = TABU_TENURE_BASE + (int) (TABU_TENURE_MULTI * conflictCount);

            // 1. Iterate Conflicts
            for (int i = 0; i < conflictCount; i++) {
                int u = conflictArr[i];
                int oldColor = state.colors[u];
                int uOffset = u * k;

                int currentConflicts = state.adjCounts[uOffset + oldColor];

                // 2. Iterate Colors
                for (int c = 0; c < k; c++) {
                    if (c == oldColor) continue;

                    int newConflicts = state.adjCounts[uOffset + c];
                    int delta = newConflicts - currentConflicts;

                    boolean isTabu = tabuMatrix[uOffset + c] > iterations;

                    // Aspiration
                    if (isTabu && (state.totalConflicts + delta < bestConflicts)) {
                        isTabu = false;
                    }

                    if (!isTabu) {
                        // Short-circuit: Perfect Move
                        if (delta == -currentConflicts) {
                            bestNode = u;
                            bestColor = c;
                            bestDelta = delta;
                            tieCount = 1;
                            i = conflictCount; // Break outer
                            break;
                        }

                        if (delta < bestDelta) {
                            bestNode = u;
                            bestColor = c;
                            bestDelta = delta;
                            tieCount = 1;
                        } else if (delta == bestDelta) {
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
                tabuMatrix[bestNode * k + oldC] = (int) (iterations + tabuTenure);
                state.updateColor(bestNode, bestColor);

                if (state.totalConflicts < bestConflicts) {
                    bestConflicts = state.totalConflicts;
                    lastImprovementIter = iterations;
                    perturbationCount = 0;
                }
            } else {
                perturb(state);
                lastImprovementIter = iterations;
            }
            iterations++;
        }
    }

    private void perturb(SolutionState state) {
        int conflictCount = state.conflictCount;
        if (conflictCount == 0) return;

        int kickStrength = (conflictCount < 20) ? 1 : (conflictCount < 50 ? 2 : 4);

        for (int i = 0; i < kickStrength; i++) {
            int idx = random.nextInt(conflictCount);
            int u = state.conflictingNodes[idx];
            int newC = random.nextInt(state.k);
            if (newC == state.colors[u]) newC = (newC + 1) % state.k;
            state.updateColor(u, newC);
        }

        // Random topology kick
        int rnd = random.nextInt(graph.getTotalVertices());
        if (graph.isActive(rnd)) {
            int newC = random.nextInt(state.k);
            if (newC != state.colors[rnd]) state.updateColor(rnd, newC);
        }
    }

    // --- INNER CLASSES ---

    /**
     * Optimized State Class using Jagged Arrays (Reference to Global fastAdj)
     */
    private static class SolutionState {
        final int[][] adj;
        final int k;
        final int[] colors;
        final int[] adjCounts;

        int totalConflicts;

        final int[] conflictingNodes;
        final int[] nodeIndices;
        int conflictCount;

        SolutionState(int[][] fastAdj, int[] initColors, int k) {
            this.adj = fastAdj;
            this.k = k;
            this.colors = Arrays.copyOf(initColors, initColors.length);

            int numNodes = colors.length;
            this.adjCounts = new int[numNodes * k];
            this.conflictingNodes = new int[numNodes];
            this.nodeIndices = new int[numNodes];
            Arrays.fill(nodeIndices, -1);
            this.conflictCount = 0;

            for (int u = 0; u < numNodes; u++) {
                if (adj[u].length == 0) continue;

                int uColor = colors[u];
                int uOffset = u * k;

                for (int v : adj[u]) {
                    adjCounts[uOffset + colors[v]]++;
                }

                if (adjCounts[uOffset + uColor] > 0) {
                    addConflict(u);
                }
            }

            long sum = 0;
            for (int i = 0; i < conflictCount; i++) {
                int u = conflictingNodes[i];
                sum += adjCounts[u * k + colors[u]];
            }
            this.totalConflicts = (int) (sum / 2);
        }

        private void addConflict(int u) {
            if (nodeIndices[u] == -1) {
                conflictingNodes[conflictCount] = u;
                nodeIndices[u] = conflictCount;
                conflictCount++;
            }
        }

        private void removeConflict(int u) {
            int idx = nodeIndices[u];
            if (idx != -1) {
                int lastNode = conflictingNodes[conflictCount - 1];
                conflictingNodes[idx] = lastNode;
                nodeIndices[lastNode] = idx;
                nodeIndices[u] = -1;
                conflictCount--;
            }
        }

        void updateColor(int u, int newColor) {
            int oldColor = colors[u];
            int uOffset = u * k;

            int oldConflicts = adjCounts[uOffset + oldColor];
            int newConflicts = adjCounts[uOffset + newColor];
            totalConflicts = totalConflicts - oldConflicts + newConflicts;

            colors[u] = newColor;

            if (newConflicts > 0) addConflict(u);
            else removeConflict(u);

            for (int v : adj[u]) {
                int vOffset = v * k;

                adjCounts[vOffset + oldColor]--;
                if (colors[v] == oldColor) {
                    if (adjCounts[vOffset + oldColor] == 0) removeConflict(v);
                }

                adjCounts[vOffset + newColor]++;
                if (colors[v] == newColor) {
                    addConflict(v);
                }
            }
        }
    }

    /**
     * Fast XorShift Random Number Generator.
     * Not thread-safe, but 5-10x faster than java.util.Random.
     */
    private static class FastRandom {
        private long seed;
        FastRandom(long seed) { this.seed = seed; }

        int nextInt(int bound) {
            long x = seed;
            x ^= (x >> 12);
            x ^= (x << 25);
            x ^= (x >> 27);
            seed = x;
            return (int) (((x * 0x2545F4914F6CDD1DL) >>> 32) % bound);
        }
    }
}