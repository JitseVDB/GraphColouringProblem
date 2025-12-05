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
     * Initializes a new Iterated Local Search (ILS) solver instance to improve the
     * coloring of the given graph. This constructor is primarily used by parameter
     * tuner classes to evaluate different parameter configurations and determine
     * which combinations yield the best performance.
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
     * @post    The parameter fields are set to the corresponding provided values.
     *          | new.TABU_TENURE_BASE == tabuTenureBase
     *          | new.TABU_TENURE_MULTI == tabuTenureMulti
     *          | new.MAX_ITERATIONS_WITHOUT_IMPROVEMENT == maxIterationsWithoutImprovement
     *          | new.MAX_PERTURBATIONS_PER_K == maxPerturbationsPerK
     */
    public IteratedLocalSearch(Graph graph, long timeLimitMillis, int tabuTenureBase,
                               double tabuTenureMulti, int maxIterationsWithoutImprovement,
                               int maxPerturbationsPerK) {
        this.graph = graph;
        this.fastAdj = buildFastAdj(graph);
        this.timeLimitMillis = timeLimitMillis;

        // Initialize pseudo-random number generator
        // Seed must be fixed so every tuning run is comparable
        this.random = new FastRandom(12345);

        // Set parameters
        this.TABU_TENURE_BASE = tabuTenureBase;
        this.TABU_TENURE_MULTI = tabuTenureMulti;
        this.MAX_ITERATIONS_WITHOUT_IMPROVEMENT = maxIterationsWithoutImprovement;
        this.MAX_PERTURBATIONS_PER_K = maxPerturbationsPerK;
    }

    /**
     * Initializes a new Iterated Local Search (ILS) solver instance to improve the
     * coloring of the given graph. This constructor automatically configures all
     * algorithm parameters based on structural properties of the graph, such as
     * the number of nodes, number of edges, and graph density.
     *
     * The auto-configuration logic categorizes the input graph into different
     * regimes (tiny, sparse, hard/phase-transition, dense) and assigns parameter
     * values tuned for each regime.<
     *
     * @param   graph
     *          The graph that will be colored.
     *
     * @param   timeLimitMillis
     *          The maximum allowed running time of the algorithm, expressed in milliseconds.
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
     * @post    The parameter fields are set based on the density of the graph.
     *          | density = (graph.getNumberOfNodes() > 1) ? (2.0 * graph.getNumberOfEdges()) /
     *                      (graph.getNumberOfNodes() * (graph.getNumberOfNodes() - 1)) : 0.0
     *          | if (graph.getNumberOfNodes() < 50) {
     *          |     new.TABU_TENURE_BASE = 5
     *          |     new.TABU_TENURE_MULTI = 0.5
     *          |     new.MAX_ITERATIONS_WITHOUT_IMPROVEMENT = 100
     *          |     new.MAX_PERTURBATIONS_PER_K = 10
     *          | }
     *          | else if (density < 0.12) {
     *          |     new.TABU_TENURE_BASE = (int) (10 + Math.log(n) * 2.5)
     *          |     new.TABU_TENURE_MULTI = 0.6
     *          |     new.MAX_ITERATIONS_WITHOUT_IMPROVEMENT = min(n * 20, 20000)
     *          |     new.MAX_PERTURBATIONS_PER_K = 200
     *          | }
     *          | else if (density < 0.75) {
     *          |     new.TABU_TENURE_BASE = 8
     *          |     new.TABU_TENURE_MULTI = 0.9
     *          |     new.MAX_ITERATIONS_WITHOUT_IMPROVEMENT = max(10000, n * 50)
     *          |     new.MAX_PERTURBATIONS_PER_K = 250
     *          | }
     *          | else {
     *          |     new.TABU_TENURE_BASE = 20
     *          |     new.TABU_TENURE_MULTI = 0.6
     *          |     new.MAX_ITERATIONS_WITHOUT_IMPROVEMENT = max(5000, n * 20)
     *          |     new.MAX_PERTURBATIONS_PER_K = 150
     *          | }
     */
    public IteratedLocalSearch(Graph graph, long timeLimitMillis) {
        this.graph = graph;
        this.timeLimitMillis = timeLimitMillis;

        // Initialize PRNG
        this.random = new FastRandom(System.nanoTime());

        // 1. Build optimized adjacency structure
        this.fastAdj = buildFastAdj(graph);

        // 2. Compute basic graph metrics
        int n = graph.getNumberOfNodes();
        int e = graph.getNumberOfEdges();
        double density = (n > 1) ? (2.0 * e) / (double) (n * (n - 1)) : 0.0;

        // 3. Apply parameter auto-tuning
        if (n < 50) {
            // TINY MODE: small instances → low tenure, short runs
            this.TABU_TENURE_BASE = 5;
            this.TABU_TENURE_MULTI = 0.5;
            this.MAX_ITERATIONS_WITHOUT_IMPROVEMENT = 100;
            this.MAX_PERTURBATIONS_PER_K = 10;

            System.out.println(">> Auto-Config: TINY MODE");
        }
        else {
            if (density < 0.12) {
                // SPARSE MODE: very sparse graphs → higher tenure, moderate iteration cap
                this.TABU_TENURE_BASE = (int) (10 + Math.log(n) * 2.5);
                this.TABU_TENURE_MULTI = 0.6;

                this.MAX_ITERATIONS_WITHOUT_IMPROVEMENT = Math.min(n * 20, 20000);
                this.MAX_PERTURBATIONS_PER_K = 200;

                System.out.println(">> Auto-Config: SPARSE MODE (High Tenure)");
            }
            else if (density < 0.75) {
                // HARD MODE: mid-density / phase-transition → long runs, high reactive multiplier
                this.TABU_TENURE_BASE = 8;
                this.TABU_TENURE_MULTI = 0.9;

                this.MAX_ITERATIONS_WITHOUT_IMPROVEMENT = Math.max(10000, n * 50);
                this.MAX_PERTURBATIONS_PER_K = 250;

                System.out.println(">> Auto-Config: HARD MODE (Phase Transition)");
            }
            else {
                // DENSE MODE: high-density graphs → balanced tenure, moderate iteration cap
                this.TABU_TENURE_BASE = 20;
                this.TABU_TENURE_MULTI = 0.6;

                this.MAX_ITERATIONS_WITHOUT_IMPROVEMENT = Math.max(5000, n * 20);
                this.MAX_PERTURBATIONS_PER_K = 150;

                System.out.println(">> Auto-Config: DENSE MODE");
            }
        }

    }

    /**
     * Builds a compact adjacency representation for the given graph.
     *
     * @param   graph
     *          The graph for which the adjacency structure is constructed.
     *
     * @return  A 2D array containing, for each node, the list of its active neighbors.
     *          | result.length == graph.getTotalNodes()
     *          | for each i in 0 .. graph.getTotalNodes() - 1 :
     *          |     if graph.isActive(i) then
     *          |         result[i].length == graph.getAdjacencyRules(i).cardinality()
     *          |         for each j in 0 .. result[i].length - 1 :
     *          |             result[i][j] == the j-th neighbor of node i in graph.getAdjacencyRules(i)
     *          |     else
     *          |         result[i].length == 0
     */
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

    /**
     * Solve the graph coloring problem using an iterative reduction approach
     * with Tabu Search.
     *
     * This method repeatedly attempts to reduce the number of colors used in
     * the graph while maintaining a valid coloring. It starts from the initial
     * coloring, and at each iteration, it tries to decrement the number of colors
     * by one. Nodes with colors higher than the target are reassigned using
     * a greedy heuristic in `smartSquashColors`. Then, a `SolutionState` is
     * constructed, and Tabu Search is applied using `runTabuSearch`. If a valid
     * coloring is found with the reduced number of colors, the solution is
     * applied to the graph and the process repeats. The algorithm terminates
     * when the time limit is reached, no further reduction is possible, or
     * Tabu Search fails to find a valid coloring.
     *
     * @post    The graph is colored according to the best solution found within
     *          the time limit.
     *          | for each node u in 0 .. graph.getTotalNodes()-1 :
     *          |     if graph.isActive(u) then
     *          |         graph.getColor(u) == best color found for u
     */
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


    /**
     * Reduce the color range by reassigning nodes with colors >= newK using
     * a greedy heuristic and randomization.
     *
     * Each node whose current color exceeds the target number of colors is
     * considered for reassignment. The nodes are shuffled randomly to
     * reduce bias, and then each node is assigned the color with the
     * fewest conflicts among its neighbors in the adjacency structure.
     *
     * @param   validColors
     *          The current color assignment for all nodes.
     *
     * @param   oldK
     *          The original number of colors.
     *
     * @param   newK
     *          The target number of colors after reduction.
     *
     * @post    All nodes with original color < newK retain their color.
     *          | for each node u in 0 .. validColors.length-1 :
     *          |     if validColors[u] < newK then
     *          |         result[u] == validColors[u]
     *
     * @post    All nodes with original color >= newK are reassigned to a
     *          color in 0 .. newK-1 minimizing adjacency conflicts.
     *          | for each node u where validColors[u] >= newK and graph.isActive(u) :
     *          |     result[u] in 0 .. newK-1
     *
     * @return  A new color array containing the reassigned colors.
     */
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

    /**
     * Performs a Tabu Search to eliminate conflicts in the given solution state.
     *
     * @param   state
     *          The current solution state, which tracks conflicts and color assignments.
     *
     * @param   startTime
     *          The system time in milliseconds when the overall solve process began.
     *
     * @effect  The state object is modified during the search. The colors and conflict counts
     *          are updated iteratively.
     *          | state.colors != old.state.colors
     *
     * @return  True if a valid coloring (0 conflicts) was found within the time limit and
     *          iteration constraints; false otherwise.
     *          | result == (state.totalConflicts == 0)
     *
     * @note    This method employs a Tabu tenure mechanism to prevent cycling and an
     *          aspiration criterion to allow tabu moves that improve the global best solution.
     */
    private boolean runTabuSearch(SolutionState state, long startTime) {
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

            // Find best move
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

    /**
     * Apply random perturbations to the SolutionState to escape local minima.
     *
     * A subset of conflicting nodes and a randomly selected node are
     * reassigned random colors to diversify the search. The perturbation
     * modifies the `state` in-place.
     *
     * @param   state
     *          The current solution state to perturb.
     *
     * @post    Between 1 and kickStrength conflicting nodes are reassigned
     *          a new color different from their current color.
     *          | for each node u chosen in perturbation:
     *          |     state.colors[u] != previous color of u
     *
     * @post    One random active node may be reassigned a new color different
     *          from its current color.
     *          | let rnd be random active node
     *          | state.colors[rnd] != previous color of rnd (if active)
     *
     * @post    The conflictCount is updated according to the new coloring assignments.
     *          | conflictCount = state.conflictCount
     */
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

    /**
     * A class representing the state of a k-coloring solution for a graph.
     *
     * This class maintains a compact representation of the coloring, tracking conflicts efficiently.
     * It stores the adjacency structure, the current color assignment of each node, conflict counts,
     * and the list of nodes that are currently in conflict. It also supports efficient updates
     * when changing the color of a node, automatically adjusting the total conflict count and
     * the list of conflicting nodes.
     *
     * @author  Jitse Vandenberghe
     *
     * @version 1.0
     */
    private static class SolutionState {

        /**********************************************************
         * Variables
         **********************************************************/

        /**
         * The adjacency structure of the graph.
         * Each element adj[u] is an array of node indices that are neighbors of node u.
         */
        final int[][] adj;

        /**
         * The number of available colors.
         */
        final int k;

        /**
         * The current color assignment for each node.
         * colors[u] is the color of node u.
         */
        final int[] colors;

        /**
         * For each node u and color c, adjCounts[u*k + c] stores
         * the number of adjacent nodes to u that are currently colored c.
         */
        final int[] adjCounts;


        /**
         * The total number of conflicts in the current coloring.
         * Each conflict corresponds to an edge where both endpoints have the same color.
         */
        int totalConflicts;

        /**
         * Array of nodes currently involved in at least one conflict.
         * Only nodes in conflict appear in this array up to conflictCount.
         */
        final int[] conflictingNodes;

        /**
         * Maps a node index to its position in conflictingNodes.
         * A value of -1 indicates that the node is not currently in conflict.
         */
        final int[] nodeIndices;

        /**
         * The number of nodes currently in conflict.
         */
        int conflictCount;

        /**********************************************************
         * Constructors
         **********************************************************/

        /**
         * Initialize a new SolutionState from the given adjacency structure, initial coloring, and number of colors.
         *
         * @param   fastAdj
         *          The adjacency structure of the graph.
         * @param   initColors
         *          The initial color assignment for each node.
         * @param   k
         *          The number of available colors.
         *
         * @post    The adjacency structure is stored as given.
         *          | new.adj == fastAdj
         *
         * @post    The value of k is initialized as the given value of k.
         *          | new.k == k
         *
         * @post    The color assignment is copied from initColors.
         *          | new.colors.length == initColors.length
         *          | for each u in 0 .. initColors.length-1 :
         *          |     new.colors[u] == initColors[u]
         *
         * @post    The adjacency counts array is initialized to zero for all nodes and colors, then updated according to neighbors' colors.
         *          | new.adjCounts.length == new.colors.length * k
         *          | for each u in 0 .. new.colors.length-1 :
         *          |     let uOffset = u * k
         *          |     for each neighbor v in new.adj[u] :
         *          |         new.adjCounts[uOffset + new.colors[v]] == count of neighbors w of u with colors[w] == new.colors[v]
         *
         * @post    The conflicting nodes array is initialized, and nodeIndices updated to reflect positions of nodes in conflict.
         *          | new.conflictingNodes.length == new.colors.length
         *          | new.nodeIndices.length == new.colors.length
         *          | for each u in 0 .. new.colors.length-1 :
         *          |     if new.adjCounts[u*k + new.colors[u]] > 0 then
         *          |         new.nodeIndices[u] != -1
         *          |         new.conflictingNodes[new.nodeIndices[u]] == u
         *          |     else
         *          |         new.nodeIndices[u] == -1
         *
         * @post    The number of nodes in conflict is correctly computed.
         *          | new.conflictCount == number of nodes u where new.adjCounts[u*k + new.colors[u]] > 0
         *
         * @post    The total conflict count is computed as half the sum of conflicts for all conflicting nodes.
         *          | new.totalConflicts == sum_{u in new.conflictingNodes[0 .. new.conflictCount-1]} new.adjCounts[u*k + new.colors[u]] / 2
         */
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


        /**********************************************************
         * Conflict Management Methods
         **********************************************************/

        /**
         * Adds the given node to the list of conflicting nodes if it is not already present.
         *
         * @param   u
         *          The node to add to the conflict list.
         *
         * @post    If node u was not already in conflict, it is added:
         *          | if nodeIndices[u] == -1 :
         *          |     conflictingNodes[conflictCount] == u
         *          |     nodeIndices[u] == conflictCount
         *          |     conflictCount == old.conflictCount + 1
         */
        private void addConflict(int u) {
            if (nodeIndices[u] == -1) {
                conflictingNodes[conflictCount] = u;
                nodeIndices[u] = conflictCount;
                conflictCount++;
            }
        }

        /**
         * Removes the given node from the list of conflicting nodes if it is present.
         *
         * @param   u
         *          The node to remove from the conflict list.
         *
         * @post    If node u was in conflict, it is removed and the last conflicting node is swapped into its position:
         *          | if nodeIndices[u] != -1 :
         *          |     let idx = nodeIndices[u]
         *          |     let lastNode = conflictingNodes[conflictCount-1]
         *          |     conflictingNodes[idx] == lastNode
         *          |     nodeIndices[lastNode] == idx
         *          |     nodeIndices[u] == -1
         *          |     conflictCount == old.conflictCount - 1
         */
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

        /**********************************************************
         * Update Methods
         **********************************************************/

        /**
         * Update the color of a node and adjust conflict counts accordingly.
         *
         * @param   u
         *          The node to recolor.
         * @param   newColor
         *          The new color for the node.
         *
         * @post    The color of node u is updated.
         *          | colors[u] == newColor
         *
         * @post    The total number of conflicts is updated according to the color change.
         *          | totalConflicts == old.totalConflicts - adjCounts[u*k + oldColor] + adjCounts[u*k + newColor]
         *
         * @post    Nodes are added to or removed from conflictingNodes as needed:
         *          | if adjCounts[u*k + newColor] > 0 then u in conflictingNodes else u not in conflictingNodes
         *          | for each neighbor v of u :
         *          |     update adjCounts[v*k + oldColor] and adjCounts[v*k + newColor]
         *          |     add or remove v from conflictingNodes as appropriate
         */
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
     * A simple pseudo-random number generator using a xorshift-like algorithm.
     *
     * This generator maintains an internal 64-bit seed, which is updated each time
     * a new random number is generated. It produces integers in a deterministic
     * sequence based on the initial seed, making it suitable for reproducible
     * experiments in graph coloring algorithms.
     *
     * The generator implements a fast xorshift transformation on the seed and returns
     * integers within a specified bound using modular arithmetic.
     *
     * @note    This generator is used instead of java.util.Random because it provides
     *          faster execution due to the lightweight xorshift operations.
     *
     * @author  Jitse Vandenberghe
     *
     * @version 1.0
     */
    private static class FastRandom {
        /**********************************************************
         * Variables
         **********************************************************/

        /**
         * The internal state of the generator.
         */
        private long seed;

        /**********************************************************
         * Constructors
         **********************************************************/

        /**
         * Initialize a new FastRandom instance with the given seed.
         *
         * @param   seed
         *          The initial state of the pseudo-random number generator.
         *
         * @post    The internal seed is initialized to the given value.
         *          | new.seed == seed
         */
        FastRandom(long seed) {
            this.seed = seed;
        }

        /**
         * Return a pseudo-random integer between 0 (inclusive) and the given bound (exclusive).
         *
         * @param   bound
         *          The exclusive upper bound of the random number.
         *
         * @pre     The bound must be positive.
         *          | bound > 0
         *
         * @post    The internal seed is updated according to the xorshift transformation:
         *          | let x = old.seed
         *          | x ^= (x >> 12)
         *          | x ^= (x << 25)
         *          | x ^= (x >> 27)
         *          | new.seed == x
         *
         * @return  A pseudo-random integer r such that 0 <= r < bound. The returned integer
         *          is derived from the high 32 bits of x multiplied by a fixed multiplier
         *          and modulo the bound.
         *          | result == (int) (((new.seed * 0x2545F4914F6CDD1DL) >>> 32) % bound)
         */
        int nextInt(int bound) {
            long x = seed;
            x ^= (x >> 12);
            x ^= (x << 25);
            x ^= (x >> 27);
            seed = x;

            long raw = (x * 0x2545F4914F6CDD1DL) >>> 32;
            return (int) (raw % bound);
        }
    }
}