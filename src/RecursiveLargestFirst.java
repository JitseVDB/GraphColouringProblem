import java.util.*;

/**
 * A class implementing the Recursive Largest First (RLF) graph coloring algorithm
 * with an M-trial optimization strategy.
 *
 * This algorithm constructs color classes iteratively by repeatedly selecting
 * a large independent set of nodes. The M-trial optimization evaluates a subset
 * of promising candidate starting nodes to approximate the optimal color class
 * expansion at each iteration.
 *
 * The implementation is heavily optimized using BitSet operations to ensure
 * low memory allocation and efficient logical operations over dense graphs.
 *
 * @author  Jitse Vandenberghe
 *
 * @version 1.0
 */
public class RecursiveLargestFirst {

    /**********************************************************
     * Variables
     **********************************************************/

    /**
     * The graph that will be colored.
     */
    private final Graph graph;

    /**
     * Total number of nodes in the graph (including inactive ones).
     */
    private final int n;

    /**
     * The resulting color assigned to each node.
     * A value of -1 means uncolored.
     */
    private final int[] colors;

    /**
     * The number of colors currently used during the coloring process.
     */
    private int colorCount;

    /**
     * The number of trial candidates per color class.
     */
    private final int M;

    /**
     * A reusable scratch BitSet for fast logical operations without reallocating memory.
     */
    private final BitSet scratch;

    /**********************************************************
     * Constants
     **********************************************************/

    /**
     * Upper bound on the number of trial candidates to evaluate.
     * Prevents excessive runtimes on large graphs.
     */
    private static final int MAX_TRIALS = 100;

    /**********************************************************
     * Constructors
     **********************************************************/

    /**
     * Initialize a new Recursive Largest First (RLF) solver instance to color the given graph.
     *
     * @param   graph
     *          The graph that will be colored.
     *
     * @param   P
     *          A fraction determining the trial set size. The solver will evaluate
     *          at most M = max(1, min(P * n, MAX_TRIALS)) candidate starting
     *          vertices when constructing each new color class.
     *
     * @post    The graph instance the RLF solver will color is set to the given graph.
     *          | new.graph == graph
     *
     * @post    The total number of nodes in the graph (including inactive nodes)
     *          is registered as the total number of nodes of the given graph instance.
     *          | new.n == graph.getTotalNodes()
     *
     * @post    All nodes are initialized as uncoloured.
     *          | new.colors.length == new.n
     *          | for each i in 0 .. new.n - 1 :
     *          |     new.colors[i] == -1
     *
     * @post    The color count is initialized as zero, since the graph is originally uncolored.
     *          | new.colorCount == 0
     *
     * @post    The number of trial candidates per color class is calculated according
     *          to the formula:
     *          | new.M == max(1, min((int)(P * new.n), MAX_TRIALS))
     *
     * @post    The scratch BitSet is initialized as an empty BitSet with capacity n.
     *          | new.scratch = new BitSet(n)
     */
    public RecursiveLargestFirst(Graph graph, double P) {
        this.graph = graph;
        this.n = graph.getTotalNodes();
        this.colors = new int[n];
        Arrays.fill(this.colors, -1);
        this.colorCount = 0;

        // Optimize M: 20% is good for small graphs, but for 10k nodes, 2000 trials is too slow.
        // We take the minimum of (P * N) and MAX_TRIALS.
        this.M = Math.max(1, Math.min((int)(P * n), MAX_TRIALS));

        // Initialize scratchpad once
        this.scratch = new BitSet(n);
    }

    /**
     * Returns the resulting color assignments.
     */
    public int[] getColors() {
        return colors;
    }

    /**
     * Executes the main loop of the Recursive Largest First (RLF) algorithm
     * in order to compute a valid coloring of the graph.
     *
     * The algorithm iteratively constructs color classes. In each iteration,
     * it selects up to M trial vertices as candidates to start a new color class,
     * evaluates the resulting color classes, and chooses the trial that leaves
     * the smallest number of vertices uncolored. Vertices in the chosen color
     * class receive the next color index. The process repeats until all active
     * nodes have been assigned a color.
     *
     * @post    All active nodes in the graph receive a valid color index.
     *          | for each v in 0 .. n - 1:
     *          |     if graph.isActive(v):
     *          |         1 <= new.colors[v]
     *          |         new.colors[v] <= new.colorCount
     *
     * @post    The resulting coloring is a proper coloring:
     *          no two adjacent active nodes share the same color.
     *          | for each u in 0 .. n - 1:
     *          |     if graph.isActive(u):
     *          |         for each v in graph.adj[u]:
     *          |             if graph.isActive(v):
     *          |                 new.colors[u] != new.colors[v]
     *
     * @post    The global color count equals the highest color index used.
     *          | new.colorCount == max { new.colors[v] | v in 0 .. n - 1 }
     *
     * @return  The total number of colors used to color the graph.
     *          | result == this.colorCount
     */
    public int colorGraph() {
        // Main working state
        RLFState mainState = new RLFState(n);

        // Load active vertices into U
        for (int v = 0; v < n; v++) {
            if (graph.isActive(v)) {
                mainState.U.set(v);
            }
        }

        // Build color classes until no vertices remain
        while (!mainState.U.isEmpty()) {
            colorCount++;

            // 1. Select top M vertex candidates based on degree inside U
            int[] topM = getTopMnodes(mainState);

            int bestVertex = -1;
            int minResidualEdges = Integer.MAX_VALUE;
            RLFState bestTrialState = null;

            // 2. Evaluate each trial candidate
            for (int candidate : topM) {
                RLFState trial = new RLFState(mainState);
                constructColorClass(trial, candidate);

                int uncoloredCount = n - trial.Cv.cardinality();

                if (uncoloredCount < minResidualEdges) {
                    minResidualEdges = uncoloredCount;
                    bestTrialState = trial;
                    bestVertex = candidate;
                }
            }

            // 3. Apply best color class
            if (bestTrialState != null) {
                for (int v = bestTrialState.Cv.nextSetBit(0); v >= 0; v = bestTrialState.Cv.nextSetBit(v + 1)) {
                    this.colors[v] = colorCount;
                }

                // Continue with remaining uncolored vertices
                mainState = new RLFState(n);
                mainState.U = bestTrialState.W; // W becomes new U
            } else {
                break; // Fail-safe
            }
        }

        return colorCount;
    }

    /**
     * Constructs a single color class within the given RLF state.
     *
     * The method begins by inserting the given firstNode into the
     * color class. It then repeatedly selects the next eligible vertex (a
     * vertex nonadjacent to all already-selected vertices), and moves it to
     * the color class, until no such vertex remains.
     *
     * @param   state
     *          The RLF state in which the color class is being constructed.
     *
     * @param   firstNode
     *          The initial node to be placed into the color class.
     *
     * @effect  The initial node is placed into the color class.
     *          | moveNodeToColorClass(state, firstNode)
     *
     * @effect  The method repeatedly attempts to extend the color class:
     *          | while (!state.U.isEmpty()) {
     *          |     next = findNextNode(state);
     *          |     if (next != -1)
     *          |         moveNodeToColorClass(state, next);
     *          |     else
     *          |         break;
     *          | }
     */
    private void constructColorClass(RLFState state, int firstNode) {
        moveNodeToColorClass(state, firstNode);

        while (!state.U.isEmpty()) {
            int next = findNextNode(state);
            if (next == -1) break;
            moveNodeToColorClass(state, next);
        }
    }

    /**
     * Moves one node from U into the current color class Cv,
     * while updating forbidden set W using fast BitSet logic.
     *
     * @param   state
     *          The active RLF state.
     *
     * @param   node
     *          The node to move.
     *
     * @post    The bit representing the given node is set in the BitSet representing the current color class.
     *          | state.Cv.set(node)
     *
     * @post    The bit representing the given node is cleared in the BitSet representing the uncolored nodes.
     *          | state.U.clear(node)
     *
     * @post    All neighbors of the node that were uncolored before this call
     *          are removed from U and added to W. The scratch BitSet is used
     *          to identify the neighbors of the node that are currently colored.
     *          | scratch.clear()
     *          | scratch.or(graph.adj[node])
     *          | scratch.and(state.U)
     *          | state.U.andNot(scratch)
     *          | state.W.or(scratch)
     */
    private void moveNodeToColorClass(RLFState state, int node) {
        state.Cv.set(node);
        state.U.clear(node);

        // 1. Load neighbors into scratchpad
        scratch.clear();
        scratch.or(graph.adj[node]);

        // 2. Find intersection with U: These are the nodes to move
        scratch.and(state.U);

        // 3. Perform the move
        state.U.andNot(scratch); // Remove from U
        state.W.or(scratch);     // Add to W
    }

    /**
     * Finds the next node to add to the current color class.
     *
     * The node is selected from the uncolored set U as the one that has the
     * fewest uncolored neighbors in W (forbidden set), breaking ties
     * arbitrarily. This heuristic tends to maximize the color class size.
     *
     * @param   state
     *          The current RLF state containing U, W, and Cv.
     *
     * @post    The scratch BitSet is used to compute degW and degU as follows:
     *          | for each v in state.U:
     *          |     scratch.clear()
     *          |     scratch.or(graph.adj[v])
     *          |     scratch.and(state.W)
     *          |     let degW = scratch.cardinality()
     *          |     if degW > maxDegW:
     *          |         scratch.clear()
     *          |         scratch.or(graph.adj[v])
     *          |         scratch.and(state.U)
     *          |         let degU = scratch.cardinality()
     *          |     else if degW == maxDegW:
     *          |         scratch.clear()
     *          |         scratch.or(graph.adj[v])
     *          |         scratch.and(state.U)
     *          |         let degU = scratch.cardinality()
     *
     * @return  The index of the next node to add to the color class,
     *          or -1 if no eligible nodes remain.
     *          | result == -1 || state.U.get(result) == true
     */
    private int findNextNode(RLFState state) {
        int bestNode = -1;
        int maxDegW = -1;
        int minDegU = Integer.MAX_VALUE;

        for (int v = state.U.nextSetBit(0); v >= 0; v = state.U.nextSetBit(v + 1)) {

            BitSet neighbors = graph.adj[v];

            // Calculate neighbors in W
            scratch.clear();
            scratch.or(neighbors);
            scratch.and(state.W);
            int degW = scratch.cardinality();

            // Only calculate DegU if strictly better
            if (degW > maxDegW) {
                scratch.clear();
                scratch.or(neighbors);
                scratch.and(state.U);
                int degU = scratch.cardinality();

                maxDegW = degW;
                minDegU = degU;
                bestNode = v;
            } else if (degW == maxDegW) { // Tie-breaker
                scratch.clear();
                scratch.or(neighbors);
                scratch.and(state.U);
                int degU = scratch.cardinality();

                if (degU < minDegU) {
                    minDegU = degU;
                    bestNode = v;
                }
            }
        }
        return bestNode;
    }

    /**
     * Selects the top M candidate nodes from the uncolored set U
     * based on their degree inside U (number of adjacent uncolored nodes).
     *
     * The selection is deterministic: if multiple nodes have the same degree,
     * the first M encountered in iteration order are chosen.
     *
     * @param   state
     *          The current RLF state containing the set of uncolored nodes.
     *
     * @post    The scratch BitSet is used to compute the degree of each node in U:
     *          | for each v in state.U:
     *          |     scratch.clear()
     *          |     scratch.or(graph.adj[v])
     *          |     scratch.and(state.U)
     *          |     let degreeInU[v] = scratch.cardinality()
     *
     * @return  An array of the top candidate vertices for starting a new color class.
     *          | result.length == min(M, state.U.cardinality())
     *          | for each i in 0 .. result.length - 1:
     *          |     result[i] in state.U
     *          | for each i, j in 0 .. result.length - 1:
     *          |     if i < j then degreeInU[result[i]] >= degreeInU[result[j]]
     */
    private int[] getTopMnodes(RLFState state) {
        int uSize = state.U.cardinality();
        if (uSize == 0) return new int[0];

        int limit = Math.min(M, uSize);

        Integer[] candidates = new Integer[uSize];
        int idx = 0;
        for (int v = state.U.nextSetBit(0); v >= 0; v = state.U.nextSetBit(v + 1)) {
            candidates[idx++] = v;
        }

        // Pre-calculate degrees to avoid recalculating in Comparator
        final Map<Integer, Integer> degrees = new HashMap<>(uSize);
        for(int v : candidates) {
            scratch.clear();
            scratch.or(graph.adj[v]);
            scratch.and(state.U);
            degrees.put(v, scratch.cardinality());
        }

        // Sort largest degrees first
        Arrays.sort(candidates, (a, b) -> degrees.get(b) - degrees.get(a));

        int[] result = new int[limit];
        for (int i = 0; i < limit; i++) {
            result[i] = candidates[i];
        }
        return result;
    }

    /**
     * A class storing the state of the Recursive Largest First (RLF) graph coloring
     * algorithm during execution.
     *
     * This state keeps track of three sets: U, containing all nodes that are still uncolored;
     * Cv, containing the nodes that have been chosen for the color class currently being
     * constructed; and W, containing nodes that remain uncolored but are temporarily forbidden
     * from joining the current color class because they are adjacent to at least one vertex
     * already added to that class.
     *
     * The RLF algorithm repeatedly builds one color class at a time. During the
     * construction of a single color class, nodes move from U to either Cv (if they
     * are chosen for the class) or W (if they cannot join the class due to adjacency
     * constraints).
     *
     * Once a color class is completed, Cv and W are cleared, and the next color
     * class is built using the remaining nodes in U.
     *
     * @author  Jitse
     *
     * @version 1.0
     */
    private static class RLFState {

        /**********************************************************
         * Variables
         **********************************************************/

        /**
         * Total number of nodes in the graph (including inactive ones).
         */
        int n;

        /**
         * Bit set indicating which nodes are currently uncolored.
         *
         * Each bit corresponds to a node in the graph.
         * A bit set to 1 means that the node is uncolored.
         * A bit set to 0 means that the node has been assigned a color.
         */
        BitSet U;

        /**
         * Bit set indicating which nodes are part of the current color class.
         *
         * Each bit corresponds to a node in the graph.
         * A bit set to 1 means that the node is part of the color class being constructed.
         * A bit set to 0 means that the node is not part of the color class being constructed.
         */
        BitSet Cv;

        /**
         * Bit set indicating which nodes are forbidden from joining the current color class.
         *
         * A node becomes forbidden when it is adjacent to a node already in the current color
         * class (Cv). This ensures the class remains an independent set, as required by proper
         * graph coloring.
         *
         * These nodes are still uncolored overall but cannot join this particular
         * color class. They remain candidates for later classes.
         *
         * Each bit corresponds to a node in the graph.
         * A bit set to 1 means that the node is uncolored but cannot join the color class being constructed.
         * A bit set to 0 means that the node is not forbidden to join the color class being constructed.
         */
        BitSet W;

        /**********************************************************
         * Constructors
         **********************************************************/

        /**
         * Initialize a new Recursive Largest First (RLF) state.
         *
         * @post    The total number of nodes in the graph (including inactive nodes)
         *          is registered as a copy of n in the RecursiveLargestFirst class.
         *          | new.n == RecursiveLargestFirst.n
         *
         * @post    The U BitSet is initialized as an empty BitSet with capacity n.
         *          This BitSet will be used to indiciate uncolored nodes.
         *          | new.U = new BitSet()
         *
         * @post    The Cv BitSet is initialized as an empty BitSet with capacity n.
         *          This BitSet will be used to indicate nodes part of the current color class.
         *          | new.Cv = new BitSet()
         *
         * @post    The W BitSet is initialized as an empty BitSet with capacity n.
         *          This BitSet will be used to track forbidden nodes for the current color class.
         *          | new.W = new BitSet()
         */
        RLFState(int n) {
            this.n = n;
            this.U = new BitSet(n);
            this.Cv = new BitSet(n);
            this.W = new BitSet(n);
        }

        /**
         * Initialize a new Recursive Largest First (RLF) state as a copy of a given state.
         *
         * @param   other
         *          The state you want to create a copy of.
         *
         * @post    The total number of nodes in the graph (including inactive nodes)
         *          is registered as a copy of n in the RecursiveLargestFirst class.
         *          | new.n == other.n
         *
         * @post    The U BitSet is initialized as a copy of the U BitSet of the given RLFState.
         *          This BitSet will be used to indiciate uncolored nodes.
         *          | new.U = other.U.clone()
         *
         * @post    The Cv BitSet is initialized as a copy of the Cv BitSet of the given RLFState.
         *          This BitSet will be used to indicate nodes part of the current color class.
         *          | new.Cv = other.Cv.clone()
         *
         * @post    The W BitSet is initialized as a copy of the W BitSet of the given RLFState.
         *          This BitSet will be used to ...
         *          | new.W = other.W.clone()
         */
        RLFState(RLFState other) {
            this.n = other.n;
            this.U = (BitSet) other.U.clone();
            this.Cv = (BitSet) other.Cv.clone();
            this.W = (BitSet) other.W.clone();
        }
    }
}