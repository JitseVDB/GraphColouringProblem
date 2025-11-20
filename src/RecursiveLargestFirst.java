import java.util.*;

public class RecursiveLargestFirst {

    private Graph graph;
    private int n;
    private int[] colors;       // final coloring
    private int colorCount;

    private double P;           // percentage of vertices for trial classes
    private int M;              // number of vertices for trial classes

    public RecursiveLargestFirst(Graph graph, double P) {
        this.graph = graph;
        this.n = graph.getTotalVertices();
        this.colors = new int[n];
        Arrays.fill(this.colors, -1);
        this.colorCount = 0;
        this.P = P;

        // Ensure at least 1 vertex is considered for top-M trials
        this.M = Math.max(1, (int)(P * n));
    }


    /**
     * Main RLF solver with M-trial optimization.
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
     * Construct a color class in a given state starting from firstVertex.
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
     * Updates sets and degrees after coloring a node.
     */
    private void updateSets(RLFState state, int node) {
        BitSet neighbors = (BitSet) graph.getAdjCopy().get(node).clone();
        neighbors.and(state.U);

        // Move neighbors to W
        state.W.or(neighbors);
        state.U.andNot(neighbors);

        for (int v = neighbors.nextSetBit(0); v >= 0; v = neighbors.nextSetBit(v + 1)) {
            BitSet secondNeighbors = (BitSet) graph.getAdjCopy().get(v).clone();
            secondNeighbors.and(state.U);
            for (int u = secondNeighbors.nextSetBit(0); u >= 0; u = secondNeighbors.nextSetBit(u + 1)) {
                state.degreesU[u]--;
                state.degreesW[u]++;
            }
        }
    }

    /**
     * Pick the next vertex to add to color class in a state.
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