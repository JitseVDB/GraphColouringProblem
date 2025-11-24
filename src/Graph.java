import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

// TODO: Add safeguard to ensure all graphs are valid.
// TODO: In reduction I assume we should keep track of nodes and edges to restore later.
// TODO: Finish documentation apply... methods.
// TODO: Currently RLF is unaware of deleted nodes and assumes every node is active.
// TODO: Check how edges connecting non-existent nodes get handled.

/**
 * A class representing graphs.
 *
 * @author  Jitse Vandenberghe
 *
 * @version 1.0
 */
public class Graph implements GraphInterface {
    public BitSet[] adj;         // adjacency[i] = neighbors of i
    private int[] degree;         // cached degrees
    private int[] color;          // node colors
    private BitSet active;        // which vertices are still in the graph
    private int totalVertices;    // original number of vertices
    private int verticeCount;     // number of vertices
    private int edgeCount;        // number of edges
    private int colorCount;

    // Load DIMACS .col file into adj BitSets (0-based)
    public void loadDIMACS(String filename) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            verticeCount = 0;
            edgeCount = 0;
            colorCount = 0;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("c")) continue;

                if (line.startsWith("p")) {
                    String[] parts = line.split("\\s+");
                    verticeCount = Integer.parseInt(parts[2]);
                    totalVertices = verticeCount;
                    adj = new BitSet[verticeCount];
                    degree = new int[verticeCount];
                    color = new int[verticeCount];
                    active = new BitSet(verticeCount);

                    for (int i = 0; i < verticeCount; i++) {
                        adj[i] = new BitSet(verticeCount);
                        color[i] = -1;      // uncolored
                        active.set(i);       // mark all vertices as active
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
     * Returns a collection of active nodes (0-based).
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
     * Returns the number of nodes.
     */
    @Override
    public int getNumberOfNodes() {
        return verticeCount;
    }

    /**
     * Returns the original number of nodes.
     */
    public int getTotalVertices() {
        return totalVertices;
    }

    /**
     * Returns the number of edges.
     */
    @Override
    public int getNumberOfEdges() {
        return edgeCount;
    }

    /**
     * Returns the number of unique colors.
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
     * Returns an unmodifiable collection of all neighbors of the given node.
     *
     * @param   v
     *          The node whose neighbors are requested (0-based).
     *
     * @return  A read-only collection containing all nodes adjacent to v.
     *          | result != null
     *          | forall n in result: adj[v].get(n)
     *
     * @throws  IllegalArgumentException
     *          If the node does not exist in the graph
     *          | v < 0 || v >= totalVertices || !active.get(v)
     */
    @Override
    public Collection<Integer> getNeighborsOf(int v) {
        if (v < 0 || v >= totalVertices || !active.get(v)) {
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
     * Check whether the given nodes are neighbors (i.e., connected by an edge).
     *
     * @param   u
     *          The first node to check (0-based).
     *
     * @param   v
     *          The second node to check (0-based).
     *
     * @return  True if both nodes exist and are neighbors.
     *          | result == (0 <= u < totalVertices && 0 <= v < totalVertices && adj[u].get(v))
     *
     * @throws  IllegalArgumentException
     *          If either u or v does not exist in the graph.
     *          | u < 0 || u >= totalVertices || v < 0 || v >= totalVertices || !active.get(v) || !active.get(u)
     */
    @Override
    public boolean areNeighbors(int u, int v) {
        if (u < 0 || u >= totalVertices || v < 0 || v >= totalVertices || !active.get(v) || !active.get(u)) {
            throw new IllegalArgumentException(
                    "Both nodes must exist in the graph: u=" + u + ", v=" + v
            );
        }

        return adj[u].get(v);
    }

    /**
     * Check wether the given node is active (i.e. not deleted).
     *
     * @param   v
     *          The node to check (0-based)
     *
     * @return  True if the node is active, false if it is deleted/inactive.
     *          | result == (active.get(v))
     *
     * @throws  IllegalArgumentException
     *          If either u or v does not exist in the graph.
     *          | v < 0 || v >= totalVertices
     */
    public boolean isActive(int v) {
        if (v < 0 || v >= totalVertices) {
            throw new IllegalArgumentException("Node " + v + " does not exist in the graph.");
        }

        return  active.get(v);
    }

    /**
     * Returns the degree of the given node.
     *
     * @param   v
     *          The node whose degree is requested (0-based).
     *
     * @return  The number of neighbors of node v if the node exists.
     *          | result == (0 <= v < totalVertices ? adj[v].cardinality() : throw)
     *
     * @throws  IllegalArgumentException
     *          If the node does not exist in the graph.
     *          | v < 0 || v >= totalVertices || !active.get(v)
     */
    @Override
    public int getDegree(int v) {
        if (v < 0 || v >= totalVertices || !active.get(v)) {
            throw new IllegalArgumentException("Node " + v + " does not exist in the graph.");
        }

        return adj[v].cardinality();
    }

    /**
     * Returns the saturation degree of the given node.
     * The saturation degree is defined as the number of distinct colors
     * assigned to the neighbors of that node.
     *
     * @param   v
     *          The node whose saturation degree is to be computed (0-based).
     *
     * @return  The number of unique colors among all colored neighbors of the given node.
     *          Uncolored neighbors (with color -1) are ignored.
     *          | result ==
     *          |     (the number of distinct c such that
     *          |         exists neighbor i where adj[v].get(i) == true :
     *          |             color[i] != -1 &&
     *          |             c == color[i])
     *
     * @throws  IllegalArgumentException
     *          If the given node does not exist in the graph.
     *          | v < 0 || v >= totalVertices || !active.get(v)
     */
    public int getSaturationDegree(int v) {
        if (v < 0 || v >= totalVertices || !active.get(v)) {
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
     * If the node is uncolored, this method returns -1.
     *
     * @param   v
     *          The node whose color is requested (0-based).
     *
     * @return  The color assigned to the given node, or -1 if the node is uncolored.
     *          | result == (0 <= v < totalVertices ? color[v] : throw)
     *
     * @throws  IllegalArgumentException
     *          If the node does not exist in the graph.
     *          | v < 0 || v >= totalVertices || !active.get(v)
     */
    @Override
    public int getColor(int v) {
        if (v < 0 || v >= totalVertices || !active.get(v)) {
            throw new IllegalArgumentException("Node " + v + " does not exist in the graph.");
        }

        return color[v];
    }

    public int[] getColorArray() {
        return color;
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
     * @throws  IllegalArgumentException
     *          If the node does not exist in the graph or is inactive.
     *          | v < 0 || v >= totalVertices || !active.get(v)
     *
     * @effect  The color of the given node v is updated in the color array to the given color.
     *          | this.color[v] == color
     */
    public void colorNode(int v, int color) {
        if (v < 0 || v >= totalVertices || !active.get(v)) {
            throw new IllegalArgumentException("Node " + v + " does not exist in the graph.");
        }

        this.color[v] = color;
    }

    /**
     * Resets the coloring of all active nodes in the graph.
     * After this operation, all active nodes are considered uncolored.
     *
     * @effect  For every active node u in the graph, its color is set to -1.
     *          | for each u where active.get(u):
     *          |     color[u] == -1
     */
    public void resetColors() {
        for (int u = 0; u < totalVertices; u++) {
            if (active.get(u)) {
                color[u] = -1;
            }
        }
    }

    /**
     * Returns the color count of the coloring of the graph.
     */
    public int getColorCount() {
        return colorCount;
    }

    /**
     * Checks if the current coloring of the graph is valid.
     * A coloring is valid if no two adjacent nodes share the same color.
     *
     * @return true if the coloring is valid, false otherwise
     */
    public boolean isValidColoring() {
        for (int u = active.nextSetBit(0); u >= 0; u = active.nextSetBit(u + 1)) {
            int colorU = color[u];
            if (colorU == -1) continue; // skip uncolored nodes

            BitSet neighbors = adj[u];
            for (int v = neighbors.nextSetBit(0); v >= 0; v = neighbors.nextSetBit(v + 1)) {
                if (!active.get(v)) continue; // skip inactive nodes
                if (color[v] == colorU) {
                    return false; // conflict found
                }
            }
        }
        return true;
    }


    /**
     * Computes the size of the maximum clique using a BitBoard Max Clique (BBMC) approach.
     * Only considers active nodes in the graph.
     *
     * @return  the size of the maximum clique found in the graph
     */
    public int getMaxClique() {
        if (active.isEmpty()) return 0; // no active nodes

        int n = verticeCount;

        // Recursive BBMC solver class
        class BitBoardMaxClique {
            int maxCliqueSize = 0;

            /**
             * Recursively expands a candidate set to find the maximum clique.
             *
             * @param   clique
             *          The current clique as a BitSet of node indices.
             *
             * @param   candidates
             *          The set of active nodes that can be added to the current clique.
             *
             * @effect Updates maxCliqueSize if the current clique is larger than previously found.
             *          | if candidates.isEmpty()
             *          |     then maxCliqueSize == max(maxCliqueSize, clique.cardinality())
             */
            void expand(BitSet clique, BitSet candidates) {
                if (candidates.isEmpty()) {
                    maxCliqueSize = Math.max(maxCliqueSize, clique.cardinality());
                    return;
                }

                // Pivot: node in candidates with max degree among active neighbors
                int pivot = -1, maxDegree = -1;
                for (int v = candidates.nextSetBit(0); v >= 0; v = candidates.nextSetBit(v + 1)) {
                    // Count only neighbors that are still active
                    BitSet neighbors = (BitSet) adj[v].clone();
                    neighbors.and(candidates);
                    int degree = neighbors.cardinality();
                    if (degree > maxDegree) {
                        maxDegree = degree;
                        pivot = v;
                    }
                }

                // Candidates not connected to pivot
                BitSet ext = (BitSet) candidates.clone();
                ext.andNot(adj[pivot]);

                for (int v = ext.nextSetBit(0); v >= 0; v = ext.nextSetBit(v + 1)) {
                    clique.set(v);

                    // New candidates are intersection of current candidates and neighbors of v
                    BitSet newCandidates = (BitSet) candidates.clone();
                    newCandidates.and(adj[v]);

                    expand(clique, newCandidates);

                    clique.clear(v);
                    candidates.clear(v);
                }
            }
        }

        BitBoardMaxClique solver = new BitBoardMaxClique();
        BitSet emptyClique = new BitSet(n);

        // Start with all active nodes as candidates
        BitSet allCandidates = (BitSet) active.clone();
        solver.expand(emptyClique, allCandidates);

        return solver.maxCliqueSize;
    }

    /**
     * Remove the given node from the graph, along with all edges connected to it.
     * If the node does not exist or is inactive, this method throws an exception.
     *
     * @param   v
     *          The node to remove (0-based).
     *
     * @effect  For all neighbors u of the given node v (stored as set bits in adj[v]),
     *          the bit corresponding to v is cleared in u's adjacency BitSet, and the
     *          degree of u and edgeCount are updated accordingly.
     *          | for each u in adj[v].nextSetBit(0..):
     *          |     adj[u].clear(v)
     *          |     degree[u] == old(degree[u]) - 1
     *          | edgeCount == old(edgeCount) - degree[v]
     *
     * @effect  The given node is marked as inactive.
     *          | !active.get(v)
     *
     * @effect  The given node's degree and color are reset.
     *          | degree[v] == 0
     *          | color[v] == -1
     *
     * @throws  IllegalArgumentException
     *          If v does not exist or is already inactive.
     *          | v < 0 || v >= totalVertices || !active.get(v)
     */
    @Override
    public void removeNode(int v) {
        if (v < 0 || v >= totalVertices || !active.get(v)) {
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
        verticeCount--;
    }

    /**
     * Adds an undirected edge between the given nodes u and v.
     * Both nodes must already be active in the graph.
     *
     * @param   u
     *          One endpoint of the edge (0-based).
     *
     * @param   v
     *          The other endpoint of the edge (0-based).
     *
     * @effect  The node v is added to the adjacency BitSet of u, and
     *          the node u is added to the adjacency BitSet of v.
     *          | adj[u].get(v) == true
     *          | adj[v].get(u) == true
     *
     * @effect  The degrees of u and v are updated accordingly.
     *          | degree[u] == old(degree[u]) + 1 (if edge was new)
     *          | degree[v] == old(degree[v]) + 1 (if edge was new)
     *
     * @effect  The edge count is increased by one if the edge did not already exist.
     *          | if (!old(adj[u]).get(v))
     *          |     then edgeCount == old(edgeCount) + 1
     *
     * @throws  IllegalArgumentException
     *          If either u or v does not exist or is inactive.
     *          | u < 0 || u >= totalVertices || v < 0 || v >= totalVertices || !active.get(u) || !active.get(v)
     */
    private void addEdge(int u, int v) {
        if (u < 0 || u >= totalVertices || v < 0 || v >= totalVertices || !active.get(u) || !active.get(v)) {
            throw new IllegalArgumentException(
                    "Both nodes must exist and be active before adding an edge: u=" + u + ", v=" + v
            );
        }

        // Only add the edge if it does not exist
        if (!adj[u].get(v)) {
            adj[u].set(v);
            adj[v].set(u);
            degree[u]++;
            degree[v]++;
            edgeCount++;
        }
    }

    /**
     * Remove the edge between the given nodes u and v, if it exists.
     *
     * @param   u
     *          One endpoint of the edge (0-based).
     *
     * @param   v
     *          The other endpoint of the edge (0-based).
     *
     * @effect  If there exists an edge between u and v (represented in the adjacency BitSets),
     *          the bits are cleared in both adjacency BitSets, and degrees and edgeCount are updated.
     *          | if (adj[u].get(v))
     *          |     then adj[u].get(v) == false &&
     *          |          adj[v].get(u) == false &&
     *          |          degree[u] == old(degree[u]) - 1 &&
     *          |          degree[v] == old(degree[v]) - 1 &&
     *          |          edgeCount == old(edgeCount) - 1
     *
     * @throws  IllegalArgumentException
     *          If either u or v does not exist or is inactive.
     *          | u < 0 || u >= totalVertices || v < 0 || v >= totalVertices || !active.get(u) || !active.get(v)
     *
     * @throws  IllegalArgumentException
     *          If there is no edge between u and v.
     *          | !adj[u].get(v) || !adj[v].get(u)
     */
    @Override
    public void removeEdge(int u, int v) {
        if (u < 0 || u >= totalVertices || v < 0 || v >= totalVertices || !active.get(u) || !active.get(v)) {
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
     * Applies a heuristic reduction to the graph by removing vertices whose
     * degree is strictly less than the threshold. The threshold is determined
     * as the size of the largest known clique in the graph.
     */
    @Override
    public void applyReduction() {
        // Determine threshold: largest known clique
        int threshold = getMaxClique();

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
            verticeCount--;
        }
    }

    @Override
    public void applyConstructionHeuristic() {
        // 1. Choose a reasonable P (e.g., 0.2 for 20% of vertices as trial candidates)
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

    @Override
    public void applyStochasticLocalSearchAlgorithm() {
        // 1. Create an ILS instance with this graph and a time limit (e.g., 5 seconds)
        long timeLimitMs = 5000; // you can adjust as needed
        IteratedLocalSearch ils = new IteratedLocalSearch(this, timeLimitMs);

        // 2. Run ILS on the graph to get the best coloring
        int[] bestColors = ils.runIteratedLocalSearch(this);

        // 3. Update the graph's internal color array
        for (int v : getNodes()) {
            color[v] = bestColors[v]; // assign best color to each active node
        }

        // 4. Update the graph's colorCount to match the best coloring
        // We calculate the max color used among active nodes
        BitSet usedColors = new BitSet();
        for (int v : getNodes()) {
            if (color[v] != -1) {
                usedColors.set(color[v]);
            }
        }
        colorCount = usedColors.cardinality();
    }


    // Helper methods to allow access to internal representation in other Classes
    public Map<Integer, BitSet> getAdjCopy() {
        Map<Integer, BitSet> copy = new HashMap<>();
        for (int v = active.nextSetBit(0); v >= 0; v = active.nextSetBit(v + 1)) {
            copy.put(v, (BitSet) adj[v].clone());
        }
        return copy;
    }

    public Map<Integer, Integer> getColorsCopy() {
        Map<Integer, Integer> copy = new HashMap<>();
        for (int v = active.nextSetBit(0); v >= 0; v = active.nextSetBit(v + 1)) {
            copy.put(v, color[v]);
        }
        return copy;
    }

    /**
     * Returns a copy of the degrees of all vertices in the graph.
     */
    public int[] getDegreesCopy() {
        return Arrays.copyOf(degree, degree.length);
    }
}
