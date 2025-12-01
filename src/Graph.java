import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * A class representing graphs.
 *
 * @author  Jitse Vandenberghe
 *
 * @version 1.0
 */
public class Graph implements GraphInterface {

    /**********************************************************
     * Variables
     **********************************************************/

    /**
     * Array of bit sets representing the adjacency structure of the graph.
     *
     * Each position in the array corresponds to a node in the graph.
     * The element at index i contains a BitSet in which each bit position j
     * indicates whether node j is a neighbor of node i.
     * A bit set to 1 means that an edge exists between node i and node j.
     * A bit set to 0 means that the nodes are not directly connected.
     */
    public BitSet[] adj;

    /**
     * Array of integers representing the degree of each node.
     *
     * Each position in the array corresponds to a node in the graph.
     * The value at index i stores the number of neighbors of node i.
     */
    private int[] degree;

    /**
     * Array of integers storing the colors assigned to the nodes.
     *
     * Each position in the array corresponds to a node in the graph.
     * The value at index i stores the color assigned to node i.
     * colors are represented by integer identifiers.
     */
    private int[] color;

    /**
     * Bit set indicating which nodes are currently active.
     *
     * Each bit corresponds to a node in the graph.
     * A bit set to 1 means that the node is active.
     * A bit set to 0 means that the node is inactive or has been removed.
     */
    private BitSet active;

    /**
     * The total number of nodes in the original graph.
     */
    private int totalNodes;

    /**
     * The current number of active nodes in the graph.
     */
    private int nodeCount;

    /**
     * The current number of edges in the graph.
     */
    private int edgeCount;

    /**
     * The number of distinct colors used in the coloring of the graph.
     */
    private int colorCount;

    /**
     * Loads a graph from a DIMACS format file (.col).
     * Initializes all internal data structures: adjacency representation,
     * node activity, degrees, and colours.
     *
     * @param   filename
     *          The path to the DIMACS file to be loaded.
     *
     * @post    The number of nodes in the graph is set according to the
     *          problem definition line ('p').
     *          | nodeCount == declaredNodeCount
     *          | totalNodes == nodeCount
     *
     * @post    The adjacency structure, degree array, colour array and
     *          activity BitSet are recreated to match the declared number
     *          of nodes.
     *          | adj.length == nodeCount
     *          | degree.length == nodeCount
     *          | color.length == nodeCount
     *          | active.size() >= nodeCount
     *
     * @post    All nodes are marked as active and initialized as uncoloured.
     *          | for each i in 0..nodeCount-1 :
     *          |     active.get(i) == true
     *          |     color[i] == -1
     *
     * @post    All edges defined by 'e' lines are inserted into the adjacency
     *          representation. Duplicate edges in the file are ignored.
     *          | for each edge (u,v) in the file :
     *          |     adj[u].get(v) == true
     *          |     adj[v].get(u) == true
     *
     * @post    Node degrees and the global edge count are updated to match the
     *          number of distinct edges defined in the file.
     *          | degree[i] == number of neighbours of i
     *          | edgeCount == number of distinct undirected edges
     *
     * @throws  IOException
     *          If the file cannot be read or does not exist.
     */
    public void loadDIMACS(String filename) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            nodeCount = 0;
            edgeCount = 0;
            colorCount = 0;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("c")) continue;

                if (line.startsWith("p")) {
                    String[] parts = line.split("\\s+");
                    nodeCount = Integer.parseInt(parts[2]);
                    totalNodes = nodeCount;
                    adj = new BitSet[nodeCount];
                    degree = new int[nodeCount];
                    color = new int[nodeCount];
                    active = new BitSet(nodeCount);

                    for (int i = 0; i < nodeCount; i++) {
                        adj[i] = new BitSet(nodeCount);
                        color[i] = -1;      // uncolored
                        active.set(i);       // mark all nodes as active
                    }
                } else if (line.startsWith("e")) {
                    String[] parts = line.split("\\s+");
                    int u = Integer.parseInt(parts[1]) - 1; // convert to 0-based
                    int v = Integer.parseInt(parts[2]) - 1;

                    if (!adj[u].get(v)) { // avoid double counting
                        adj[u].set(v);
                        adj[v].set(u);
                        degree[u]++;
                        degree[v]++;
                        edgeCount++;
                    }
                }
            }
        }
    }

    /**
     * Returns a collection of all active nodes.
     *
     * @return An unmodifiable collection containing all active nodes.
     *         | for each v in result: active.get(v)
     */
    @Override
    public Collection<Integer> getNodes() {
        BitSet activeCopy = (BitSet) active.clone();
        List<Integer> nodes = new ArrayList<>();
        for (int v = activeCopy.nextSetBit(0); v >= 0; v = activeCopy.nextSetBit(v + 1)) {
            nodes.add(v);
        }
        return Collections.unmodifiableList(nodes);
    }

    /**
     * Returns the current number of active nodes.
     *
     * @return The number of active nodes.
     *         | result == this.nodeCount
     */
    @Override
    public int getNumberOfNodes() {
        return nodeCount;
    }

    /**
     * Returns the number of nodes initially present.
     *
     * @return The total number of nodes initially present.
     *         | result == this.totalNodes
     */
    public int getTotalNodes() {
        return totalNodes;
    }

    /**
     * Returns the number of edges.
     *
     * @return The number of edges.
     *         | result == this.edgeCount
     */
    @Override
    public int getNumberOfEdges() {
        return edgeCount;
    }

    /**
     * Returns an unmodifiable collection of all neighbors of the given node.
     *
     * @param   v
     *          The node whose neighbors are requested (0-based).
     *
     * @return  A read-only collection containing all nodes adjacent to v.
     *          | for each n in result: adj[v].get(n)
     *
     * @throws  IllegalArgumentException
     *          If the node does not exist or is inactive.
     *          | v < 0 || v >= totalNodes || !active.get(v)
     */
    @Override
    public Collection<Integer> getNeighborsOf(int v) {
        if (v < 0 || v >= totalNodes || !active.get(v)) {
            throw new IllegalArgumentException("Node " + v + " does not exist in the graph.");
        }

        BitSet neighborsBitSet = adj[v];
        Set<Integer> neighbors = new HashSet<>();
        for (int i = neighborsBitSet.nextSetBit(0); i >= 0; i = neighborsBitSet.nextSetBit(i + 1)) {
            neighbors.add(i);
        }

        return Collections.unmodifiableSet(neighbors);
    }

    /**
     * Checks whether the given nodes are neighbors (i.e. connected by an edge).
     *
     * @param   u
     *          The first node (0-based).
     *
     * @param   v
     *          The second node (0-based).
     *
     * @return  True if both nodes exist, are active, and have an edge between them.
     *          | result == (active.get(u) && active.get(v) && adj[u].get(v))
     *
     * @throws  IllegalArgumentException
     *          If either node does not exist or is inactive.
     *          | u < 0 || u >= totalNodes
     *          | v < 0 || v >= totalNodes
     *          | !active.get(u) || !active.get(v)
     */
    @Override
    public boolean areNeighbors(int u, int v) {
        if (u < 0 || u >= totalNodes || v < 0 || v >= totalNodes || !active.get(v) || !active.get(u)) {
            throw new IllegalArgumentException(
                    "Both nodes must exist in the graph: u=" + u + ", v=" + v
            );
        }

        return adj[u].get(v);
    }

    /**
     * Checks whether the given node is active.
     *
     * @param   v
     *          The node to check (0-based).
     *
     * @return  True if the node is active, false otherwise.
     *          | result == active.get(v)
     *
     * @throws  IllegalArgumentException
     *          If the node index is invalid.
     *          | v < 0 || v >= totalNodes
     */
    public boolean isActive(int v) {
        if (v < 0 || v >= totalNodes) {
            throw new IllegalArgumentException("Node " + v + " does not exist in the graph.");
        }
        return active.get(v);
    }

    /**
     * Returns the degree of the given node.
     *
     * @param   v
     *          The node whose degree is requested (0-based).
     *
     * @return  The number of neighbors of node v.
     *          | result == adj[v].cardinality()
     *
     * @throws  IllegalArgumentException
     *          If the node does not exist or is inactive.
     *          | v < 0 || v >= totalNodes || !active.get(v)
     */
    @Override
    public int getDegree(int v) {
        if (v < 0 || v >= totalNodes || !active.get(v)) {
            throw new IllegalArgumentException("Node " + v + " does not exist in the graph.");
        }

        return adj[v].cardinality();
    }

    /**
     * Returns the saturation degree of the given node.
     * The saturation degree is the number of distinct colors assigned to
     * its colored neighbors. Uncolored neighbors (color -1) are ignored.
     *
     * @param   v
     *          The node whose saturation degree is requested (0-based).
     *
     * @return  The number of different colors used by colored neighbors.
     *
     * @throws  IllegalArgumentException
     *          If the node does not exist or is inactive.
     *          | v < 0 || v >= totalNodes || !active.get(v)
     */
    public int getSaturationDegree(int v) {
        if (v < 0 || v >= totalNodes || !active.get(v)) {
            throw new IllegalArgumentException("Node " + v + " does not exist in the graph.");
        }

        BitSet neighbors = adj[v];
        Set<Integer> neighborColors = new HashSet<>();
        for (int i = neighbors.nextSetBit(0); i >= 0; i = neighbors.nextSetBit(i + 1)) {
            int c = color[i];
            if (c != -1) {
                neighborColors.add(c);
            }
        }

        return neighborColors.size();
    }

    /**
     * Returns the color assigned to the given node.
     * Uncolored nodes have color -1.
     *
     * @param   v
     *          The node whose color is requested (0-based).
     *
     * @return  The color of the node, or -1 if uncolored.
     *          | result == color[v]
     *
     * @throws  IllegalArgumentException
     *          If the node does not exist or is inactive.
     *          | v < 0 || v >= totalNodes || !active.get(v)
     */
    @Override
    public int getColor(int v) {
        if (v < 0 || v >= totalNodes || !active.get(v)) {
            throw new IllegalArgumentException("Node " + v + " does not exist in the graph.");
        }

        return color[v];
    }

    /**
     * Assigns a color to the given node.
     *
     * @param   v
     *          The node to color (0-based).
     *
     * @param   color
     *          The color to assign.
     *
     * @post    The color of node v is updated.
     *          | this.color[v] == color
     *
     * @throws  IllegalArgumentException
     *          If the node does not exist or is inactive.
     *          | v < 0 || v >= totalNodes || !active.get(v)
     *
     * @note    This method is intended as a low-level helper for algorithms such
     *          as RLF and ILS. Calling this method directly does NOT update
     *          colorCount. The calling algorithm is responsible for
     *          keeping the color usage statistics consistent.
     *          Updating colorCount inside this method would be computationally
     *          expensive as you would have to check if the new color was already
     *          in use by any other node.
     */
    public void colorNode(int v, int color) {
        if (v < 0 || v >= totalNodes || !active.get(v)) {
            throw new IllegalArgumentException("Node " + v + " does not exist in the graph.");
        }

        this.color[v] = color;
    }

    /**
     * Resets the coloring of all active nodes.
     *
     * @post    For every active node u:
     *          | for each u where active[u]:
     *          |     color[u] == -1
     */
    public void resetColors() {
        for (int u = 0; u < totalNodes; u++) {
            if (active.get(u)) {
                color[u] = -1;
            }
        }

        colorCount = 0;
    }

    /**
     * Returns the number of distinct colors currently used in the graph.
     * Uncolored nodes (color -1) are ignored.
     *
     * @return  The number of unique colors among all colored nodes.
     *          | for each i in 0 .. color.length - 1:
     *          |     if color[i] != -1:
     *          |         used.set(color[i])
     *          | result == used.cardinality()
     */
    public int getNumberOfUsedColors() {
        BitSet used = new BitSet();
        for (int c : color) {
            if (c != -1) {
                used.set(c);
            }
        }
        return used.cardinality();
    }

    /**
     * Returns the number of distinct colors currently used.
     *
     * @return The number of colors in use.
     *         | result == colorCount
     */
    public int getColorCount() {
        return colorCount;
    }

    /**
     * Checks whether the current coloring is valid.
     * A coloring is valid if no edge connects two nodes of the same color.
     *
     * @return  True if no two adjacent active nodes have the same color, false otherwise.
     *          | for each u in 0 .. totalNodes - 1:
     *          |     if active.get(u) && color[u] != -1:
     *          |         for each v in neighbors(u):
     *          |             if active.get(v) && color[v] != -1:
     *          |                 if color[u] == color[v]:
     *          |                     result == false
     *          | result == true if no such conflict exists
     */
    public boolean isValidColoring() {
        for (int u = active.nextSetBit(0); u >= 0; u = active.nextSetBit(u + 1)) {
            int colorU = color[u];
            if (colorU == -1) continue;

            BitSet neighbors = adj[u];
            for (int v = neighbors.nextSetBit(0); v >= 0; v = neighbors.nextSetBit(v + 1)) {
                if (!active.get(v)) continue;
                if (color[v] == colorU) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Removes the given node from the graph, along with all edges incident to it.
     * The node becomes inactive and cannot be used afterwards.
     *
     * @param   v
     *          The node to remove (0-based).
     *
     * @post    For every active neighbor u of v, the edge (u, v) is removed:
     *          | for each u where adj[v].get(u):
     *          |     adj[u].get(v) == false
     *          |     degree[u] == old(degree[u]) - 1
     *          | edgeCount == old(edgeCount) - old(degree[v])
     *
     * @psot    Node v becomes inactive.
     *          | active.get(v) == false
     *
     * @post    The adjacency of v is cleared, and its degree and color are reset.
     *          | adj[v].cardinality() == 0
     *          | degree[v] == 0
     *          | color[v] == -1
     *
     * @post    The node count decreases by one.
     *          | nodeCount == old(nodeCount) - 1
     *
     * @throws  IllegalArgumentException
     *          If v is invalid or already inactive.
     *          | v < 0 || v >= totalNodes || !active.get(v)
     */
    @Override
    public void removeNode(int v) {
        if (v < 0 || v >= totalNodes || !active.get(v)) {
            throw new IllegalArgumentException("Node does not exist or is already removed: v=" + v);
        }

        // Remove v from all its neighbors
        BitSet neighbors = adj[v];
        for (int u = neighbors.nextSetBit(0); u >= 0; u = neighbors.nextSetBit(u + 1)) {
            adj[u].clear(v);
            degree[u]--;
            edgeCount--;
        }

        // Reset node v
        neighbors.clear();
        degree[v] = 0;
        color[v] = -1;
        active.clear(v); // mark as inactive
        nodeCount--;
    }

    /**
     * Removes the edge between nodes u and v.
     *
     * @param   u
     *          One endpoint of the edge (0-based).
     *
     * @param   v
     *          The other endpoint of the edge (0-based).
     *
     * @post    If the edge exists, it is removed from both adjacency sets:
     *          | adj[u].get(v) == false
     *          | adj[v].get(u) == false
     *          | degree[u] == old(degree[u]) - 1
     *          | degree[v] == old(degree[v]) - 1
     *          | edgeCount == old(edgeCount) - 1
     *
     * @throws  IllegalArgumentException
     *          If either node does not exist or is inactive.
     *          | u < 0 || u >= totalNodes
     *          | v < 0 || v >= totalNodes
     *          | !active.get(u) || !active.get(v)
     *
     * @throws  IllegalArgumentException
     *          If no edge exists between u and v.
     *          | !adj[u].get(v)
     */
    @Override
    public void removeEdge(int u, int v) {
        if (u < 0 || u >= totalNodes || v < 0 || v >= totalNodes || !active.get(u) || !active.get(v)) {
            throw new IllegalArgumentException(
                    "Both nodes must exist and be active before removing an edge: u=" + u + ", v=" + v
            );
        }

        if (!adj[u].get(v) || !adj[v].get(u)) {
            throw new IllegalArgumentException(
                    "The given edge does not exist: u=" + u + ", v=" + v
            );
        }

        adj[u].clear(v);
        adj[v].clear(u);
        degree[u]--;
        degree[v]--;
        edgeCount--;
    }

    /**
     * Applies a reduction heuristic that removes all active nodes whose degree
     * is strictly less than the current number of used colors in the graph.
     *
     * @post    For each node v that is removed, removes edges from v to its neighbors.
     *          | for each u in neighbors(v):
     *          |     adj[u].clear(v)
     *
     * @post    For each neighbor u of removed nodes, decreases its degree by 1.
     *          | for each u in neighbors(v):
     *          |     new.degree[u] == this.degree[u] - 1
     *
     * @post    For each removed edge, decreases the edge count by 1.
     *          | new.edgeCount == this.edgeCount - 1 per removed edge
     *
     * @post    Clears the adjacency list of each removed node.
     *          | adj[v].isEmpty() == true
     *
     * @post    Sets the degree of each removed node to 0.
     *          | new.degree[v] == 0
     *
     * @post    Marks each removed node as inactive.
     *          | active.get(v) == false
     *
     * @post    Decreases the total number of active nodes by 1 for each removed node.
     *          | new.nodeCount == this.nodeCount - 1 per removed node
     */
    @Override
    public void applyReduction() {
        // Determine threshold: largest known clique equivalent to k of initial coloring.
        int threshold = getNumberOfUsedColors();

        // Collect nodes to remove
        List<Integer> toRemove = new ArrayList<>();
        for (int v = active.nextSetBit(0); v >= 0; v = active.nextSetBit(v + 1)) {
            if (degree[v] < threshold) {
                toRemove.add(v);
            }
        }

        // Remove nodes safely
        for (int v : toRemove) {
            // Remove edges from neighbors
            BitSet neighbors = (BitSet) adj[v].clone();
            for (int u = neighbors.nextSetBit(0); u >= 0; u = neighbors.nextSetBit(u + 1)) {
                adj[u].clear(v);
                degree[u]--;
                edgeCount--;
            }

            // Clear adjacency and mark inactive
            adj[v].clear();
            degree[v] = 0;
            active.clear(v);
            nodeCount--;
        }
    }

    /**
     * Applies a constructive coloring heuristic based on the Recursive Largest
     * First (RLF) algorithm. A fraction parameter controls the candidate set size.
     * The coloring computed by RLF replaces the current coloring of the graph.
     *
     * @effect  The graph receives a new valid coloring.
     *          | for each active v
     *          |   this.colorNode(node, rlfColors[node])
     *
     * @post    The color count is updated.
     *          | colorCount == rlf.colorGraph()
     */
    @Override
    public void applyConstructionHeuristic() {
        // 1. Choose a reasonable P (e.g., 0.2 for 20% of nodes as trial candidates)
        double P = 0.2;

        // 2. Create RLF instance with this graph and P
        RecursiveLargestFirst rlf = new RecursiveLargestFirst(this, P);

        // 3. Run the RLF heuristic to color the graph
        colorCount = rlf.colorGraph();

        // 4. Update the Graph's node colors with the RLF result
        int[] rlfColors = rlf.getColors();
        for (int node : getNodes()) {
            this.colorNode(node, rlfColors[node]);
        }
    }

    /**
     * Applies Iterated Local Search (ILS) combined with Tabu Search to improve
     * the current coloring. This method assumes that an initial valid coloring
     * is already present. The solver may update the graph's coloring directly.
     *
     * @effect  Executes the ILS solver, which may update the color of nodes.
     *          | ils.solve()
     *
     * @post    Updates the cached number of used colors to match the current coloring.
     *          | new this.colorCount == this.getNumberOfUsedColors()
     */
    @Override
    public void applyStochasticLocalSearchAlgorithm() {
        // 1. Define a time limit (e.g., 60 seconds or based on benchmark rules)
        long timeLimitMillis = 300000; // 10 seconds for now

        // 2. Create the solver
        IteratedLocalSearch ils = new IteratedLocalSearch(this, timeLimitMillis);

        // 3. Run the solver.
        ils.solve();

        // 4. Update the internal colorCount cache of the Graph class
        this.colorCount = this.getNumberOfUsedColors();
    }

    /**
     * Returns a deep copy of the adjacency structure for all active nodes.
     *
     * @return A map from active nodes to clones of their adjacency BitSets.
     *         | for each active v:
     *         |     result.get(v) is a clone of adj[v]
     */
    public Map<Integer, BitSet> getAdjCopy() {
        Map<Integer, BitSet> copy = new HashMap<>();
        for (int v = active.nextSetBit(0); v >= 0; v = active.nextSetBit(v + 1)) {
            copy.put(v, (BitSet) adj[v].clone());
        }
        return copy;
    }

    /**
     * Returns a copy of the current coloring of all active nodes.
     *
     * @return A map from active nodes to their assigned color.
     *         | for each active v:
     *         |     result.get(v) == color[v]
     */
    public Map<Integer, Integer> getColorsCopy() {
        Map<Integer, Integer> copy = new HashMap<>();
        for (int v = active.nextSetBit(0); v >= 0; v = active.nextSetBit(v + 1)) {
            copy.put(v, color[v]);
        }
        return copy;
    }

    /**
     * Returns a copy of the internal degree array.
     *
     * @return A new array containing the degree of every node.
     *         | result[i] == degree[i]
     */
    public int[] getDegreesCopy() {
        return Arrays.copyOf(degree, degree.length);
    }

    /**
     * Returns the internal adjacency BitSet of node v.
     *
     * @param   v
     *          The node whose adjacency BitSet is requested (0-based).
     *
     * @return  The raw BitSet storing neighbors of v.
     *          | result == adj[v]
     */
    public BitSet getAdjacencyRules(int v) {
        return adj[v];
    }

    /**
     * Returns the internal color array.
     */
    public int[] getColorArray() {
        return color;
    }
}
