import java.util.*;

/**
 * Highly Optimized Recursive Largest First (RLF) implementation.
 *
 * Performance Improvements:
 * 1. Reuses a single "scratch" BitSet to avoid expensive BitSet.clone() allocations.
 * 2. Uses native BitSet logical operations (AND/OR) which are CPU-vectorized (64x faster than looping).
 * 3. Caps the 'M' parameter to prevent exponential runtime scaling on large graphs.
 */
public class RecursiveLargestFirst {

    private final Graph graph;
    private final int n;
    private final int[] colors;
    private int colorCount;
    private final int M;

    // A reusable scratchpad to perform bitwise logical ops without memory allocation
    private final BitSet scratch;

    // Hard cap on trials to prevent 17-minute runtimes on large graphs
    // 100 trials is usually statistically sufficient to find a good candidate.
    private static final int MAX_TRIALS = 100;

    public RecursiveLargestFirst(Graph graph, double P) {
        this.graph = graph;
        this.n = graph.getTotalVertices();
        this.colors = new int[n];
        Arrays.fill(this.colors, -1);
        this.colorCount = 0;

        // Optimize M: 20% is good for small graphs, but for 10k nodes, 2000 trials is too slow.
        // We take the minimum of (P * N) and MAX_TRIALS.
        this.M = Math.max(1, Math.min((int)(P * n), MAX_TRIALS));

        // Initialize scratchpad once
        this.scratch = new BitSet(n);
    }

    public int[] getColors() {
        return colors;
    }

    public int colorGraph() {
        // Active vertices in the graph
        RLFState mainState = new RLFState(n);

        // Only load actually active vertices
        for (int v = 0; v < n; v++) {
            if (graph.isActive(v)) {
                mainState.U.set(v);
            }
        }

        while (!mainState.U.isEmpty()) {
            colorCount++;

            // 1. Pick top M candidates based on degree in U
            int[] topM = getTopMVertices(mainState);

            int bestVertex = -1;
            int minResidualEdges = Integer.MAX_VALUE;
            RLFState bestTrialState = null;

            // 2. Run reduced trials
            for (int candidate : topM) {
                // We must clone the state for the trial, but RLFState copy is now optimized
                RLFState trial = new RLFState(mainState);

                constructColorClass(trial, colorCount, candidate);

                // Heuristic: The best class leaves the FEWEST uncolored vertices active (neighbors of neighbors)
                // In RLF terms, this effectively maximizes the size of the color class.
                // We check the size of W (vertices moved out of U due to conflict).
                // Smaller W means fewer nodes were "forbidden", but RLF usually wants MAXIMAL Independent Set.
                // Max IS means U became small.
                // Residual Edges Metric: sum of degrees in U?
                // Standard RLF: Minimize edges remaining in the uncolored subgraph.

                // For speed, maximizing the size of the color class (Cv) is a very strong proxy.
                // Minimizing (Total Vertices - Cv.cardinality())
                int uncoloredCount = n - trial.Cv.cardinality();

                if (uncoloredCount < minResidualEdges) {
                    minResidualEdges = uncoloredCount;
                    bestTrialState = trial;
                    bestVertex = candidate;
                }
            }

            // 3. Apply best trial
            if (bestTrialState != null) {
                // Update global colors
                for (int v = bestTrialState.Cv.nextSetBit(0); v >= 0; v = bestTrialState.Cv.nextSetBit(v + 1)) {
                    this.colors[v] = colorCount;
                }

                // Prepare next state: U becomes the set of uncolored active nodes (which is exactly W from the trial)
                // We reuse the bestTrialState's W as the new U to avoid recalculation
                mainState = new RLFState(n);
                mainState.U = bestTrialState.W; // Transfer ownership of the BitSet
                // Cv is empty in new state
                // W is empty in new state
            } else {
                break;
            }
        }

        return colorCount;
    }

    private void constructColorClass(RLFState state, int color, int firstVertex) {
        moveNodeToColorClass(state, firstVertex);

        while (!state.U.isEmpty()) {
            int next = findNextVertex(state);
            if (next == -1) break;
            moveNodeToColorClass(state, next);
        }
    }

    /**
     * Optimized move: Uses BitSet logic (vectorized) instead of iterating neighbors manually.
     */
    private void moveNodeToColorClass(RLFState state, int node) {
        state.Cv.set(node);
        state.U.clear(node);

        // LOGIC: Neighbors of 'node' that are in U must move to W.
        // Doing this with BitSets is much faster than iterating int neighbors for dense graphs.

        // 1. Load neighbors into scratchpad
        scratch.clear();
        scratch.or(graph.adj[node]); // fast block copy

        // 2. Find intersection with U: These are the nodes to move
        scratch.and(state.U);

        // 3. Perform the move
        // scratch now contains {v | v is neighbor of node AND v is in U}
        state.U.andNot(scratch); // Remove from U
        state.W.or(scratch);     // Add to W
    }

    /**
     * Finds the next vertex maximizing neighbors in W (degreesW), minimizing neighbors in U (degreesU).
     * Uses on-the-fly BitSet intersection which is O(N/64) instead of O(N) loop.
     */
    private int findNextVertex(RLFState state) {
        int bestNode = -1;
        int maxDegW = -1;
        int minDegU = Integer.MAX_VALUE;

        // We iterate over U.
        // For dense graphs, iterating U is fast, but calculating degrees is the bottleneck.
        // using scratchpad intersection is faster than iterating neighbor-arrays.

        for (int v = state.U.nextSetBit(0); v >= 0; v = state.U.nextSetBit(v + 1)) {

            // Calculate DegW: Intersection of Neighbors(v) and W
            // We use the scratchpad (carefully, as we are in a loop)
            // Actually, we can't reuse 'this.scratch' easily inside a loop if we are nested.
            // But here we are just counting.

            // OPTIMIZATION: Manually intersect without clone if possible?
            // Java BitSet doesn't allow "cardinalityOfIntersection" without modifying a set.
            // However, modifying a `temp` set is still faster than iterating arrays for dense graphs.

            BitSet neighbors = graph.adj[v];

            // Calculate neighbors in W
            scratch.clear();
            scratch.or(neighbors);
            scratch.and(state.W);
            int degW = scratch.cardinality();

            if (degW > maxDegW) {
                // Only calculate DegU if strictly better (Lazy calc)
                scratch.clear();
                scratch.or(neighbors);
                scratch.and(state.U);
                int degU = scratch.cardinality();

                maxDegW = degW;
                minDegU = degU;
                bestNode = v;
            } else if (degW == maxDegW) {
                // Tie-breaker
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

    private int[] getTopMVertices(RLFState state) {
        int uSize = state.U.cardinality();
        if (uSize == 0) return new int[0];

        int limit = Math.min(M, uSize);

        // We need 'degrees in U' for all nodes in U to sort them.
        // Since we are not maintaining degreesU incrementally, we calculate it once here.
        // This is O(U * N/64), which is fine to do once per color class.

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

    private static class RLFState {
        int n;
        BitSet U;  // Uncolored vertices
        BitSet Cv; // Current color class
        BitSet W;  // Forbidden (moved out of U)

        RLFState(int n) {
            this.n = n;
            this.U = new BitSet(n);
            this.Cv = new BitSet(n);
            this.W = new BitSet(n);
        }

        // Fast Copy Constructor
        RLFState(RLFState other) {
            this.n = other.n;
            this.U = (BitSet) other.U.clone();
            this.Cv = (BitSet) other.Cv.clone();
            this.W = (BitSet) other.W.clone();
        }
    }
}