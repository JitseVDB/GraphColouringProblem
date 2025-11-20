import java.util.Arrays;
import java.util.BitSet;

/**
 * A class storing the state needed for the Recursive Largest First (RLF) algorithm.
 *
 * Each RLFState object represents the current uncolored vertices (U), the forbidden
 * neighbors for the current color class (W), and the vertices currently assigned
 * to the color class (Cv), along with degree counts restricted to U and W and the
 * global coloring array.
 *
 * @author  Jitse Vandenberghe
 * @version 1.1
 */
public class RLFState {
    /**
     * BitSet representing the uncolored vertices.
     * A bit at index v is set to 1 if vertex v is currently uncolored and active.
     */
    public BitSet U;

    /**
     * BitSet representing the neighbors of the current color class.
     * A bit at index v is set to 1 if vertex v is a neighbor of any vertex in Cv.
     */
    public BitSet W;

    /**
     * BitSet representing the current color class.
     * A bit at index v is set to 1 if vertex v has been added to the current color class.
     */
    public BitSet Cv;

    /**
     * Array storing degrees restricted to U.
     * degreesU[v] = number of neighbors of vertex v that are still uncolored.
     */
    public int[] degreesU;

    /**
     * Array storing degrees restricted to W.
     * degreesW[v] = number of neighbors of vertex v that are in W.
     */
    public int[] degreesW;

    /**
     * Array storing the current coloring of each vertex.
     * colors[v] = -1 if vertex v is uncolored; otherwise, the color assigned to vertex v.
     */
    public int[] colors;

    /**
     * Total number of vertices including inactive or deleted vertices.
     * Determines the maximum index range for BitSets and arrays.
     */
    public int n;

    /**
     * Initialize a new RLFState object for the given graph.
     *
     * @param   graph The graph to represent.
     *
     * @post    n is set to the total number of vertices in the graph.
     *          | new.n == graph.getTotalVertices()
     *
     * @post    U contains all active vertices set as uncolored.
     *          | for each v in graph.getNodes(): new.U.get(v) == true
     *          | for each v not in graph.getNodes(): new.U.get(v) == false
     *
     * @post    W is initialized as an empty BitSet.
     *          | new.W.equals(new BitSet(new.n))
     *
     * @post    Cv is initialized as an empty BitSet.
     *          | new.Cv.equals(new BitSet(new.n))
     *
     * @post    degreesU is initialized as a copy of the graph's degree array.
     *          | for each v in [0, n): new.degreesU[v] == graph.getDegreesCopy()[v]
     *
     * @post    degreesW is initialized to zero for all vertices.
     *          | new.degreesW.length == new.n
     *          | for each v in [0, n): new.degreesW[v] == 0
     *
     * @post    colors is initialized to -1 for all vertices.
     *          | new.colors.length == new.n
     *          | for each v in [0, n): new.colors[v] == -1
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
            U.set(v);
        }
    }

    /**
     * Copy constructor for trial classes.
     *
     * @post    Returns a deep copy of this RLFState object.
     *          | result.n == this.n
     *          | result.U.equals(this.U)
     *          | result.W.equals(this.W)
     *          | result.Cv.equals(this.Cv)
     *          | Arrays.equals(result.degreesU, this.degreesU)
     *          | Arrays.equals(result.degreesW, this.degreesW)
     *          | Arrays.equals(result.colors, this.colors)
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
     * Returns true if there are any uncolored vertices left in this state.
     *
     * @post    result == true if there exists v in [0, n) such that U.get(v) == true
     *          | result == !U.isEmpty()
     */
    public boolean hasUncoloredVertices() {
        return !U.isEmpty();
    }

    /**
     * Returns the number of uncolored vertices in this state.
     *
     * @post    result == the number of bits set in U
     *          | result == U.cardinality()
     */
    public int getUncoloredCount() {
        return U.cardinality();
    }
}
