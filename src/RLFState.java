import java.util.Arrays;
import java.util.BitSet;

public class RLFState {
    public BitSet U;          // uncolored vertices
    public BitSet W;          // neighbors of the current color class
    public BitSet Cv;         // vertices in the current color class

    public int[] degreesU;    // degrees restricted to U
    public int[] degreesW;    // degrees restricted to W
    public int[] colors;      // current coloring

    public int n;             // total number of vertices (including deleted vertices)

    /**
     * Initialize the RLF state from a graph.
     * U contains all active vertices initially.
     * DegreesU is copied from the graph's degrees.
     */
    public RLFState(Graph graph) {
        n = graph.getTotalVertices();

        U = new BitSet(n);
        W = new BitSet(n);
        Cv = new BitSet(n);

        degreesU = graph.getDegreesCopy();
        degreesW = new int[n];

        colors = new int[n];
        Arrays.fill(colors, -1);

        for (int v : graph.getNodes()) {
            U.set(v); // all vertices initially uncolored
        }
    }

    /**
     * Copy constructor for trial classes.
     * Produces a deep copy of the current state.
     */
    public RLFState copy() {
        RLFState copy = new RLFState();
        copy.n = this.n;
        copy.U = (BitSet) this.U.clone();
        copy.W = (BitSet) this.W.clone();
        copy.Cv = (BitSet) this.Cv.clone();
        copy.degreesU = Arrays.copyOf(this.degreesU, n);
        copy.degreesW = Arrays.copyOf(this.degreesW, n);
        copy.colors = Arrays.copyOf(this.colors, n);
        return copy;
    }

    // Private empty constructor for copy()
    RLFState() {}

    /**
     * Utility: returns true if there are any uncolored vertices left
     */
    public boolean hasUncoloredVertices() {
        return !U.isEmpty();
    }

    /**
     * Returns the number of uncolored vertices
     */
    public int getUncoloredCount() {
        return U.cardinality();
    }
}
