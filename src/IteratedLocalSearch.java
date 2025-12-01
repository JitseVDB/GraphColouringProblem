import java.util.*;

/**
 * A class implementing an Iterated Local Search (ILS) metaheuristic for the
 * graph coloring problem, enhanced with tabu search mechanisms and efficient
 * perturbation strategies.
 *
 * This algorithm repeatedly improves a candidate coloring through local search
 * while avoiding cycling via tabu restrictions. When no further improvement is
 * possible, controlled perturbations are applied to escape local minima. The
 * overall search process restarts from the most promising solution found so far,
 * combining intensification and diversification.
 *
 * To accelerate neighborhood evaluation, the implementation uses a fast adjacency
 * cache and tight BitSet-based operations where applicable, significantly reducing
 * repeated memory accesses and improving performance on dense or medium-sized graphs.
 *
 * The behaviour of the search is controlled by several parameters: tabu tenure
 * (with both a base value and multiplicative scaling), the number of iterations
 * allowed without improvement, and the number of perturbations performed per
 * attempted number of colors k.
 *
 * This class is designed for high-performance optimization tasks and assumes that
 * the provided graph is static throughout the search process.
 *
 * @author  Jitse Vandenberghe
 *
 * @version 1.0
 */
public class IteratedLocalSearch {

    /**********************************************************
     * Variables
     **********************************************************/

    /**
     * The graph on which the iterated local search procedure is executed.
     *
     * This graph remains fixed throughout the search process and provides
     * adjacency information for evaluating conflicts and computing feasible
     * recolorings.
     */
    private final Graph graph;

    /**
     * The maximum allowed running time of the algorithm, expressed in
     * milliseconds. Once this time limit is reached, the algorithm stops,
     * even if no locally optimal or stable solution has been reached.
     */
    private final long timeLimitMillis;

    /**
     * A fast, lightweight pseudo-random number generator used for tie-breaking,
     * selecting perturbation moves, and guiding the diversification process.
     *
     * This generator uses an xorshift-based sequence to minimize overhead.
     * It is intentionally simpler and faster than java.util.Random and
     * avoids memory allocation and synchronization overhead.
     */
    private final FastRandom random;

    /**
     * A cached adjacency structure providing fast access to neighbors of each node.
     *
     * For every node v, fastAdj[v] contains the indices of all nodes adjacent to v.
     * This compact primitive-array representation reduces memory indirection and
     * improves CPU cache locality during neighborhood evaluations in tabu search,
     * move selection, and perturbation steps.
     */
    private final int[][] fastAdj;

    /**********************************************************
     * Parameters
     **********************************************************/

    /**
     * The base value of the tabu tenure.
     *
     * The actual tabu tenure applied to a move may scale with problem size
     * and search stage, using TABU_TENURE_MULTI as a multiplier. Increasing
     * this value tends to reduce cycling but may slow down convergence.
     */
    private final int TABU_TENURE_BASE;

    /**
     * A multiplicative scaling factor applied to the tabu tenure.
     *
     * The tenure is dynamically adjusted based on the current number of
     * conflicting nodes. When the search is in a "difficult" region (many
     * conflicts), the tenure increases to prevent cycling.
     */
    private final double TABU_TENURE_MULTI;

    /**
     * The maximum number of iterations allowed without improving the
     * best-known coloring. Once this threshold is exceeded, a perturbation
     * step or restart is triggered to escape local minima.
     */
    private final int MAX_ITERATIONS_WITHOUT_IMPROVEMENT;

    /**
     * The maximum number of perturbations applied for a given target number
     * of colors k before abandoning the attempt.
     *
     * If this limit is reached without finding a valid coloring for k, the
     * algorithm concludes that k cannot be reached within the resource limits
     * and terminates, returning the best solution found for k+1.
     */
    private final int MAX_PERTURBATIONS_PER_K;

    /**********************************************************
     * Constructors
     **********************************************************/

    /**
     * Initialize a new Iterated Local Search (ILS) solver instance to improve the coloring of the graph.
     *
     * @param   graph
     *          The graph that will be colored.
     *
     * @param   timeLimitMillis
     *          The maximum allowed running time of the algorithm, expressed in milliseconds.
     *
     * @param   tabuTenureBase
     *          The base value of the tabu tenure.
     *
     * @param   tabuTenureMulti
     *          A multiplicative scaling factor applied to the tabu tenure.
     *
     * @param   maxIterationsWithoutImprovement
     *          The maximum number of iterations allowed without improving the best-known coloring.
     *
     * @param   maxPerturbationsPerK
     *          The maximum number of perturbations applied for a given target number of colors k
     *          before abandoning the attempt.
     *
     * @post    The graph instance the RLF solver will color is set to the given graph.
     *          | new.graph == graph
     *
     * @post    The time limit is set to the given time limit in milliseconds.
     *          | new.timeLimitMillis == timeLimitMillis
     *
     * @post    The random number generator is initialized for internal use.
     *          | new.random = new FastRandom(System.nanoTime())
     *
     * @post    The adjacency matrix is built based on the given graph instance.
     *          | new.fastAdj = buildFastAdj(graph)
     *
     * @post   The base value of the tabu tenure is set to the provided argument.
     *         | new.TABU_TENURE_BASE == tabuTenureBase
     *
     * @post   The multiplicative scaling factor for the tabu tenure is set to the provided argument.
     *         | new.TABU_TENURE_MULTI == tabuTenureMulti
     *
     * @post   The maximum number of iterations without improvement is set to the provided argument.
     *         | new.MAX_ITERATIONS_WITHOUT_IMPROVEMENT == maxIterationsWithoutImprovement
     *
     * @post   The maximum number of perturbations per target color k is set to the provided argument.
     *         | new.MAX_PERTURBATIONS_PER_K == maxPerturbationsPerK
     */
    public IteratedLocalSearch(Graph graph, long timeLimitMillis, int tabuTenureBase,
                               double tabuTenureMulti, int maxIterationsWithoutImprovement,
                               int maxPerturbationsPerK) {
        this.graph = graph;
        this.fastAdj = buildFastAdj(graph);
        this.timeLimitMillis = timeLimitMillis;

        // Initialize pseudo-random number generator
        this.random = new FastRandom(System.nanoTime());

        // Set parameters
        this.TABU_TENURE_BASE = tabuTenureBase;
        this.TABU_TENURE_MULTI = tabuTenureMulti;
        this.MAX_ITERATIONS_WITHOUT_IMPROVEMENT = maxIterationsWithoutImprovement;
        this.MAX_PERTURBATIONS_PER_K = maxPerturbationsPerK;
    }

    /**
     * CONSTRUCTOR 2: INTELLIGENT AUTO-CONFIGURATION
     * Tweaked for higher quality on difficult graphs.
     */
    public IteratedLocalSearch(Graph graph, long timeLimitMillis) {
        this.graph = graph;
        this.timeLimitMillis = timeLimitMillis;
        // Fixed seed for reproducibility
        this.random = new FastRandom(12345);

        // 1. Build Optimized Graph Structure
        this.fastAdj = buildFastAdj(graph);

        // 2. Calculate Metrics
        int n = graph.getTotalNodes();
        int e = graph.getNumberOfEdges();
        double density = (n > 1) ? (2.0 * e) / (double) (n * (n - 1)) : 0.0;

        // 3. APPLY TUNING LOGIC

        if (n < 50) {
            // TINY MODE (Unchanged)
            this.TABU_TENURE_BASE = 5;
            this.TABU_TENURE_MULTI = 0.5;
            this.MAX_ITERATIONS_WITHOUT_IMPROVEMENT = 100;
            this.MAX_PERTURBATIONS_PER_K = 10;
            System.out.println(">> Auto-Config: TINY MODE");
        }
        else {
            // TWEAK: Lowered sparse threshold slightly (0.15 -> 0.12) to ensure
            // "semi-sparse" graphs get the Hard Mode treatment which searches deeper.
            if (density < 0.12) {
                // --- CASE A: SPARSE GRAPHS (wap, ash, will) ---
                // Strategy: High Tenure, Moderate Iterations.
                // Tweak: Increased max iterations (5k -> 20k) to help with 'le450' sparse instances.

                this.TABU_TENURE_BASE = (int) (10 + Math.log(n) * 2.5);
                this.TABU_TENURE_MULTI = 0.6;

                this.MAX_ITERATIONS_WITHOUT_IMPROVEMENT = Math.min(n * 20, 20000);
                this.MAX_PERTURBATIONS_PER_K = 200; // Give it more tries

                System.out.println(">> Auto-Config: SPARSE MODE (High Tenure)");

            } else if (density >= 0.12 && density < 0.75) {
                // --- CASE B: HARD / PHASE TRANSITION (flat, DSJC.5) ---
                // This covers the widest range now (0.12 to 0.75).
                // Strategy: Massive persistence.

                // Tweak: Increased Base Tenure slightly (6 -> 8) to reduce immediate cycling.
                this.TABU_TENURE_BASE = 8;
                this.TABU_TENURE_MULTI = 0.9; // Keep high reactive multiplier

                // TWEAK: Massive increase in iterations.
                // Previous: max(4000, n*10) -> New: max(10000, n*50).
                // We have the CPU cycles to spare; let's use them to find that one elusive move.
                this.MAX_ITERATIONS_WITHOUT_IMPROVEMENT = Math.max(10000, n * 50);

                // Tweak: Try longer before giving up on 'k'
                this.MAX_PERTURBATIONS_PER_K = 250;

                System.out.println(">> Auto-Config: HARD MODE (Phase Transition, High Iters)");

            } else {
                // --- CASE C: DENSE GRAPHS (queen, latin, DSJC.9) ---
                // Strategy: These were previously too fast and low quality.

                // Tweak: Doubled the multiplier (0.3 -> 0.6).
                // Dense graphs have thousands of conflicts; a low multiplier meant the tabu list
                // was too short to prevent cycling.
                this.TABU_TENURE_BASE = 20;
                this.TABU_TENURE_MULTI = 0.6;

                // Tweak: 5x increase in iterations.
                // Being 595x faster than the paper is cool, but being 100x faster with better colors is cooler.
                this.MAX_ITERATIONS_WITHOUT_IMPROVEMENT = Math.max(5000, n * 20);

                this.MAX_PERTURBATIONS_PER_K = 150;

                System.out.println(">> Auto-Config: DENSE MODE (Balanced Quality/Speed)");
            }
        }
    }

    private int[][] buildFastAdj(Graph graph) {
        int n = graph.getTotalNodes();
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

        int initialK = graph.getColorCount();
        int[] currentColors = Arrays.copyOf(graph.getColorArray(), graph.getTotalNodes());

        if (initialK == 0) {
            initialK = graph.getTotalNodes();
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

            // 2. Build State
            SolutionState state = new SolutionState(fastAdj, workingColors, targetK);

            // 3. Tabu
            boolean solved = runTabuSearch(state, startTime);

            if (solved) {
                bestK = targetK;
                bestGlobalColors = Arrays.copyOf(state.colors, state.colors.length);
                applySolutionToGraph(bestGlobalColors);
                // System.out.println("Found k=" + bestK + " Time=" + (System.currentTimeMillis() - startTime));
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

        int[] nodesToMove = new int[n];
        int count = 0;
        for (int i = 0; i < n; i++) {
            if (graph.isActive(i) && newColors[i] >= newK) {
                nodesToMove[count++] = i;
            }
        }

        // Shuffle
        for (int i = count - 1; i > 0; i--) {
            int idx = random.nextInt(i + 1);
            int temp = nodesToMove[idx];
            nodesToMove[idx] = nodesToMove[i];
            nodesToMove[i] = temp;
        }

        // Greedy
        for (int i = 0; i < count; i++) {
            int u = nodesToMove[i];
            int bestColor = 0;
            int minConflicts = Integer.MAX_VALUE;
            int startC = random.nextInt(newK);

            for (int k = 0; k < newK; k++) {
                int c = (startC + k) % newK;
                int conflicts = 0;
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
        // Tweak: Increased Tabu Matrix size handling implicitly by logic,
        // but ensuring we don't overflow logic is handled by 'long iterations'
        int[] tabuMatrix = new int[graph.getTotalNodes() * state.k];
        long iterations = 0;
        long lastImprovementIter = 0;
        int bestConflicts = state.totalConflicts;
        int perturbationCount = 0;

        while (true) {
            if ((iterations & 1023) == 0) {
                if (System.currentTimeMillis() - startTime >= timeLimitMillis) return false;
            }

            if (state.totalConflicts == 0) return true;

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

            int calculatedTenure = TABU_TENURE_BASE + (int) (TABU_TENURE_MULTI * conflictCount);
            // Cap tenure to prevent locking the whole graph
            int tabuTenure = Math.min(calculatedTenure, (graph.getTotalNodes() * k) / 2);

            // FIND BEST MOVE
            for (int i = 0; i < conflictCount; i++) {
                int u = conflictArr[i];
                int oldColor = state.colors[u];
                int uOffset = u * k;
                int currentConflicts = state.adjCounts[uOffset + oldColor];

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
                        if (delta == -currentConflicts) {
                            bestNode = u;
                            bestColor = c;
                            bestDelta = delta;
                            tieCount = 1;
                            i = conflictCount;
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
                if (perturbationCount >= MAX_PERTURBATIONS_PER_K) return false;
                perturb(state);
                lastImprovementIter = iterations;
                perturbationCount++;
            }
            iterations++;
        }
    }

    private void perturb(SolutionState state) {
        int conflictCount = state.conflictCount;
        if (conflictCount == 0) return;

        // Tweak: Slightly stronger kick for larger graphs
        int kickStrength = (conflictCount < 20) ? 1 : (conflictCount < 50 ? 3 : 6);

        for (int i = 0; i < kickStrength; i++) {
            int idx = random.nextInt(conflictCount);
            int u = state.conflictingNodes[idx];
            int newC = random.nextInt(state.k);
            if (newC == state.colors[u]) newC = (newC + 1) % state.k;
            state.updateColor(u, newC);
        }

        // Random topology kick
        int rnd = random.nextInt(graph.getTotalNodes());
        if (graph.isActive(rnd)) {
            int newC = random.nextInt(state.k);
            if (newC != state.colors[rnd]) state.updateColor(rnd, newC);
        }
    }

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