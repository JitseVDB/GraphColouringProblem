import java.util.*;

/**
 * Implements the Recursive Largest First (RLF) graph coloring algorithm
 * with the top-M trial optimization.
 *
 * Each instance maintains the graph, current coloring, and parameters
 * for the top-M trial heuristic.
 *
 * The algorithm incrementally constructs color classes by selecting
 * vertices with the highest connectivity to forbidden sets.
 *
 * @author  Jitse Vandenberghe
 *
 * @version 1.0
 */
public class RecursiveLargestFirst {
    /**
     * The graph to color.
     */
    private Graph graph;

    /**
     * Total number of vertices including inactive/deleted vertices.
     */
    private int n;

    /**
     * Array storing the final coloring. colors[v] = -1 if uncolored, else assigned color.
     */
    private int[] colors;

    /**
     * Number of colors used so far.
     */
    private int colorCount;

    /**
     * Percentage of vertices to consider in top-M trials.
     */
    private double P;

    /**
     * Number of vertices considered in top-M trials.
     */
    private int M;

    private BitSet tmp;

    /**
     * Initialize a RecursiveLargestFirst solver for the given graph and trial percentage.
     *
     * @param   graph
     *          The graph to color.
     *
     * @param   P
     *          The percentage (0.0–1.0) of vertices to consider in top-M trials.
     *
     * @post    n is initialized to the total number of vertices in the graph.
     *          | new.n == graph.getTotalVertices()
     *
     * @post    colors array is initialized to -1 for all vertices.
     *          | new.colors.length == new.n
     *          | for each v in [0, n): new.colors[v] == -1
     *
     * @post    colorCount is initialized to 0.
     *          | new.colorCount == 0
     *
     * @post    P and M are initialized, ensuring at least one vertex is used in trials.
     *          | new.P == P
     *          | new.M == Math.max(1, (int)(P * n))
     */
    public RecursiveLargestFirst(Graph graph, double P) {
        this.graph = graph;
        this.n = graph.getTotalVertices();
        this.colors = new int[n];
        Arrays.fill(this.colors, -1);
        this.colorCount = 0;
        this.P = P;

        // Ensure at least 1 vertex is considered for top-M trials
        this.M = Math.max(1, (int)(P * n));
        this.tmp = new BitSet(n);
    }


    /**
     * Executes the Recursive Largest First (RLF) algorithm with M-trial optimization
     * to color the graph.
     *
     * @post    All active vertices in the graph are assigned a color >= 1.
     *          | for each v in graph.getNodes(): colors[v] >= 1
     *
     * @post    All inactive or deleted vertices remain uncolored (-1).
     *          | for each v not in graph.getNodes(): colors[v] == -1
     *
     * @post    Updates the internal colors array to reflect the coloring of the graph.
     *          | for each v in [0, n): colors[v] == best coloring assigned during algorithm
     *
     * @effect  Constructs RLFState objects representing the current state of uncolored,
     *          forbidden, and current color class vertices.
     *          | mainState == new RLFState(graph)
     *
     * @effect  Creates trial copies of RLFState for each top-M vertex to evaluate residual edges.
     *          | trialStates.add(mainState.copy()) for each top vertex
     *
     * @effect  Updates degreesU and degreesW arrays in each RLFState as vertices are assigned colors.
     *          | updateSets(trial, vertex) is called for each vertex added to a color class
     *
     * @effect  Selects the trial with minimal residual edges and propagates its state to the main RLFState.
     *          | mainState == best trial state for next iteration
     *
     * @return  The total number of colors used to color all active vertices of the graph.
     *          | result == colorCount
     */
    public int colorGraph() {
        RLFState mainState = new RLFState(graph);

        while (mainState.hasUncoloredVertices()) {
            colorCount++;

            // 1. Pick top M vertices based on degrees in U
            int[] topM = getTopMVertices(mainState);

            // 2. Try trial classes for each top vertex
            List<RLFState> trialStates = new ArrayList<>();
            int[] residualEdges = new int[topM.length];

            for (int i = 0; i < topM.length; i++) {
                int firstV = topM[i];

                // Copy current state for trial
                RLFState trial = mainState.copy();

                // Construct trial color class
                constructColorClass(trial, colorCount, firstV);

                trialStates.add(trial);

                // Count residual edges (sum of degreesU in trial.U)
                int sumEdges = 0;
                for (int v = trial.U.nextSetBit(0); v >= 0; v = trial.U.nextSetBit(v + 1)) {
                    sumEdges += trial.degreesU[v];
                }
                residualEdges[i] = sumEdges;
            }

            // 3. Pick trial class with minimal residual edges
            int bestIndex = 0;
            int minEdges = residualEdges[0];
            for (int i = 1; i < residualEdges.length; i++) {
                if (residualEdges[i] < minEdges) {
                    minEdges = residualEdges[i];
                    bestIndex = i;
                }
            }

            // 4. Update main state and coloring with best trial
            RLFState best = trialStates.get(bestIndex);

            // copy colored vertices into global colors array
            for (int v = 0; v < n; v++) {
                if (best.colors[v] != -1) {
                    colors[v] = best.colors[v];
                }
            }

            // build next mainState: the remaining uncolored vertices are in best.W
            RLFState next = new RLFState();          // uses private empty ctor in your RLFState
            next.n = this.n;
            next.U = (BitSet) best.W.clone();       // remaining vertices to color next
            next.W = new BitSet(n);                 // empty W for next iteration
            next.Cv = new BitSet(n);                // empty current color class
            next.degreesU = Arrays.copyOf(best.degreesW, n); // degrees w.r.t. the new U
            next.degreesW = new int[n];
            next.colors = Arrays.copyOf(best.colors, n);

            // set mainState to new prepared state and continue
            mainState = next;

        }

        return colorCount;
    }

    /**
     * Constructs a color class starting from the given first vertex
     * and updates the RLFState accordingly.
     *
     * @param   state
     *          The RLFState object to update.
     *
     * @param   color
     *          The color to assign to the class.
     *
     * @param   firstVertex
     *          The vertex to start the color class from.
     *
     * @post    firstVertex is added to Cv and removed from U.
     *          | state.Cv.get(firstVertex) == true
     *          | state.U.get(firstVertex) == false
     *          | state.colors[firstVertex] == color
     *
     * @post    Additional vertices are added to Cv based on RLF rules
     *          until no uncolored vertex in U can be added.
     *          | for each v in state.Cv: v was previously in state.U
     *
     * @effect  Updates the forbidden set W and degree counts in the state
     *          after adding each vertex to the color class.
     *          | updateSets(state, firstVertex)
     *          | updateSets(state, any added vertex)
     */
    private void constructColorClass(RLFState state, int color, int firstVertex) {
        // Add first vertex
        state.Cv.set(firstVertex);
        state.U.clear(firstVertex);
        state.colors[firstVertex] = color;

        updateSets(state, firstVertex);

        // Continue adding vertices to class
        while (!state.U.isEmpty()) {
            int next = findNextVertex(state);
            if (next == -1) break;
            state.Cv.set(next);
            state.U.clear(next);
            state.colors[next] = color;
            updateSets(state, next);
        }
    }

    /**
     * Updates the sets and degree arrays after adding a vertex to the current color class.
     *
     * @param   state
     *          The RLFState object to update.
     *
     * @param   node
     *          The vertex that was just added to the current color class.
     *
     * @post    All neighbors of node that were uncolored (i.e., in state.U) are moved to state.W.
     *          Formally, for each v in [0, n):
     *          | if graph.getAdjCopy().get(node).get(v) && state.U.get(v) before call
     *          | then state.W.get(v) after call == true
     *          | and state.U.get(v) after call == false
     *
     * @post    The vertex node itself is removed from state.U and remains in state.Cv.
     *          | state.U.get(node) after call == false
     *
     * @post    The degreesU and degreesW arrays are updated for all vertices u that are neighbors
     *          of any vertex v moved to W. For all such u:
     *          | state.degreesU[u] after call == state.degreesU[u] before call - 1
     *          | state.degreesW[u] after call == state.degreesW[u] before call + 1
     *
     * @effect  Accesses the adjacency structure of the graph for node and its neighbors.
     *          | graph.getAdjCopy().get(node)
     */
    private void updateSets(RLFState state, int node) {
        tmp.clear();
        tmp.or(graph.adj[node]);
        tmp.and(state.U);
        state.W.or(tmp);
        state.U.andNot(tmp);

        for (int v = tmp.nextSetBit(0); v >= 0; v = tmp.nextSetBit(v + 1)) {
            tmp.clear();
            tmp.or(graph.adj[v]);
            tmp.and(state.U);
            for (int u = tmp.nextSetBit(0); u >= 0; u = tmp.nextSetBit(u + 1)) {
                state.degreesU[u]--;
                state.degreesW[u]++;
            }
        }
    }

    /**
     * Determines the next vertex to add to the current color class based on RLF rules.
     *
     * The selected vertex is chosen to maximize its number of neighbors in the
     * forbidden set W (degreesW). If multiple vertices tie, the vertex with the
     * minimal number of neighbors in U (degreesU) is chosen.
     *
     * @param   state
     *          The RLFState object representing the current algorithm state.
     *
     * @post    If the result is not -1, it is a vertex v in U such that:
     *          1. v maximizes degreesW among all vertices in U.
     *             | forall u in [0, state.n) : state.U.get(u) ==> state.degreesW[u] <= state.degreesW[result]
     *          2. Among vertices with maximal degreesW, v minimizes degreesU.
     *             | forall u in [0, state.n) : state.U.get(u) && state.degreesW[u] == state.degreesW[result]
     *             |     ==> state.degreesU[u] >= state.degreesU[result]
     *
     * @return  The index of the vertex to add next to the color class, or -1 if
     *          no vertex can be added.
     *          | result == -1 || (0 <= result < state.n && state.U.get(result))
     */
    private int findNextVertex(RLFState state) {
        int maxDegW = -1;
        int chosen = -1;

        for (int v = state.U.nextSetBit(0); v >= 0; v = state.U.nextSetBit(v + 1)) {
            int degW = state.degreesW[v];
            if (degW > maxDegW) {
                maxDegW = degW;
                chosen = v;
            } else if (degW == maxDegW) {
                if (chosen == -1 || state.degreesU[v] < state.degreesU[chosen]) {
                    chosen = v;
                }
            }
        }
        return chosen;
    }


    /**
     * Returns the top M vertices with the highest degree in U for trial color classes.
     *
     * @param   state
     *          The RLFState object to examine. Must not be null.
     *          | state != null
     *
     * @post    Each vertex in the returned array is uncolored in the input state.
     *          | for each v in result: state.U.get(v) == true
     *
     * @post    Vertices are sorted by decreasing degreesU value:
     *          | for all i in [0, result.length-2]: state.degreesU[result[i]] >= state.degreesU[result[i+1]]
     *
     * @return  An array of vertex indices of size at most M, containing vertices
     *          with the highest degreesU in descending order.
     *          | result.length <= M
     */
    private int[] getTopMVertices(RLFState state) {
        int size = Math.min(M, state.getUncoloredCount());
        if (size <= 0) return new int[0]; // no uncolored vertices

        int[] top = new int[size];
        PriorityQueue<Integer> heap = new PriorityQueue<>(size, (a, b) -> Integer.compare(state.degreesU[a], state.degreesU[b]));

        for (int v = state.U.nextSetBit(0); v >= 0; v = state.U.nextSetBit(v + 1)) {
            if (heap.size() < size) heap.offer(v);
            else if (state.degreesU[v] > state.degreesU[heap.peek()]) {
                heap.poll();
                heap.offer(v);
            }
        }

        for (int i = size - 1; i >= 0; i--) top[i] = heap.poll();

        return top;
    }

    /**
     * Get final coloring
     */
    public int[] getColors() {
        return colors;
    }
}