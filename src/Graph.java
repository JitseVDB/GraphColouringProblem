import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

// TODO: Add safeguard to ensure all graphs are valid.
// TODO: In reduction I assume we should keep track of nodes and edges to restore later.

/**
 * A class representing graphs.
 *
 * @author  Jitse Vandenberghe
 * @version 1.0
 */
public class Graph implements GraphInterface {
    private final Map<Integer, BitSet> adjList = new HashMap<>();
    private final Map<Integer, Integer> colors = new HashMap<>();
    private int edgeCount = 0;

    // Load DIMACS .col file
    public void loadDIMACS(String filename) throws IOException {
        // Clear previous graph
        adjList.clear();
        colors.clear();
        edgeCount = 0;

        // Load current graph
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            int n = 0;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("c")) continue;

                if (line.startsWith("p")) {
                    String[] parts = line.split("\\s+");
                    n = Integer.parseInt(parts[2]);
                    for (int i = 1; i <= n; i++) {
                        adjList.put(i, new BitSet(n + 1)); // 1-based indexing
                        colors.put(i, -1); // -1 indicates uncolored
                    }
                } else if (line.startsWith("e")) {
                    String[] parts = line.split("\\s+");
                    int u = Integer.parseInt(parts[1]);
                    int v = Integer.parseInt(parts[2]);
                    addEdge(u, v);
                }
            }
        }
    }

    /**
     * Returns a collection of nodes.
     */
    @Override
    public Collection<Integer> getNodes() {
        return Collections.unmodifiableSet(adjList.keySet());
    }

    /**
     * Returns the numbers of nodes.
     */
    @Override
    public int getNumberOfNodes() {
        return adjList.size();
    }

    /**
     * Returns the number of edges.
     */
    @Override
    public int getNumberOfEdges() {
        return edgeCount;
    }

    /**
     * Returns an unmodifiable collection of all neighbors of the given node.
     *
     * @param   v
     *          The node whose neighbors are requested.
     *
     * @return  A read-only collection containing all nodes adjacent to v, if v exists.
     *          | result != null && forall n in result: adjList.get(v).get(n)
     *
     * @throws  IllegalArgumentException
     *          If the node does not exist in the graph
     *          | !adjList.containsKey(v)
     */
    @Override
    public Collection<Integer> getNeighborsOf(int v) {
        if (!adjList.containsKey(v)) {
            throw new IllegalArgumentException("Node " + v + " does not exist in the graph.");
        }

        BitSet neighborsBitSet = adjList.get(v);
        Set<Integer> neighbors = new HashSet<>();
        for (int i = neighborsBitSet.nextSetBit(1); i >= 0; i = neighborsBitSet.nextSetBit(i + 1)) {
            neighbors.add(i);
        }

        return Collections.unmodifiableSet(neighbors);
    }

    /**
     * Check whether the given nodes are neighbors (i.e., connected by an edge).
     *
     * @param   u
     *          The first node you want to check.
     *
     * @param   v
     *          The second node you want to check.
     *
     * @return  True if the given nodes exist and are neighbors.
     *          | result == (adjList.containsKey(u) && adjList.get(u).get(v))
     *
     * @throws  IllegalArgumentException
     *          If either u or v does not exist in the adjacency list.
     *          | !adjList.containsKey(u) || !adjList.containsKey(v)
     */
    @Override
    public boolean areNeighbors(int u, int v) {
        if (!adjList.containsKey(u) || !adjList.containsKey(v)) {
            throw new IllegalArgumentException(
                    "Both nodes must exist before adding an edge: u=" + u + ", v=" + v
            );
        }

        return adjList.get(u).get(v);
    }

    /**
     * Returns the degree of the given node.
     *
     * @param   v
     *          The node whose degree is requested.
     *
     * @return  The number of neighbors of node v if the node v exists.
     *          | result == (adjList..get(v).cardinality())
     *
     * @throws  IllegalArgumentException
     *          If the node does not exist in the graph
     *          | !adjList.containsKey(v)
     */
    @Override
    public int getDegree(int v) {
        if (!adjList.containsKey(v)) {
            throw new IllegalArgumentException("Node " + v + " does not exist in the graph.");
        }

        BitSet neighbors = adjList.get(v);
        return neighbors.cardinality();
    }

    /**
     * Returns the saturation degree of the given node.
     * The saturation degree is defined as the number of distinct colors
     * assigned to the neighbors of that node.
     *
     * @param   v
     *          The node whose saturation degree is to be computed.
     *
     * @return  The number of unique colors among all colored neighbors of the given node.
     *          Uncolored neighbors (with color -1) are ignored.
     *          | result ==
     *          |     (the number of distinct c such that
     *          |         exists neighbor i where adjList.get(v).get(i) == true :
     *          |             colors.containsKey(i) &&
     *          |             colors.get(i) != -1 &&
     *          |             c == colors.get(i))
     *
     * @throws  IllegalArgumentException
     *          If the given node does not exist in the adjacency list.
     *          | !adjList.containsKey(v)
     */
    public int getSaturationDegree(int v) {
        if (!adjList.containsKey(v)) {
            throw new IllegalArgumentException("Node " + v + " does not exist in the graph.");
        }

        BitSet neighbors = adjList.get(v);

        Set<Integer> neighborColors = new HashSet<>();
        for (int i = neighbors.nextSetBit(1); i >= 0; i = neighbors.nextSetBit(i + 1)) {
            int c = colors.getOrDefault(i, -1);
            if (c != -1) neighborColors.add(c);
        }

        return neighborColors.size();
    }

    /**
     * Returns the color assigned to the given node.
     * If the node is uncolored or does not exist in the coloring map,
     * this method returns -1.
     *
     * @param   v
     *          The node whose color is requested.
     *
     * @return  The color assigned to the given node, or -1 if the node
     *          is uncolored.
     *          | result == (colors.containsKey(v) ? colors.get(v) : -1)
     *
     * @throws  IllegalArgumentException
     *          If the node does not exist in the graph
     *          | !adjList.containsKey(v)
     */
    @Override
    public int getColor(int v) {
        if (!adjList.containsKey(v)) {
            throw new IllegalArgumentException("Node " + v + " does not exist in the graph.");
        }

        return colors.getOrDefault(v, -1);
    }

    /**
     * Assigns a color to the given node.
     *
     * @param   v
     *          The node to color
     *
     * @param   color
     *          The color to assign
     *
     * @effect  The color of the given node v is updated in the coloring map to the given color.
     *          | colors.get(v) == color
     *
     * @throws IllegalArgumentException
     *          If the node does not exist in the graph
     *          | !adjList.containsKey(v)
     */
    public void colorNode(int v, int color) {
        if (!adjList.containsKey(v)) {
            throw new IllegalArgumentException("Node " + v + " does not exist in the graph.");
        }
        colors.put(v, color);
    }

    /**
     * Resets the coloring of all nodes in the graph.
     * After this operation, all nodes are considered uncolored.
     *
     * @effect  For every node u in the adjacency list, its color is set to -1.
     *          | for each u in adjList.keySet():
     *          |     colors.get(u) == -1
     */
    public void resetColors() {
        for (int node : adjList.keySet()) {
            colors.put(node, -1);
        }
    }

    /**
     * Computes the size of the maximum clique using a BitBoard Max Clique (BBMC) approach.
     *
     * @return the size of the maximum clique found in the graph
     */
    public int getMaxClique() {
        int n = adjList.size();
        if (n == 0) return 0;

        // Convert adjacency to 0-based BitSet array (adjBits) for efficient bitwise operations.
        BitSet[] adjBits = new BitSet[n];
        for (int i = 0; i < n; i++) {
            adjBits[i] = new BitSet(n);
            int node = i + 1;
            BitSet neighbors = adjList.get(node);
            // Map 1-based node IDs from adjList to 0-based array indices.
            for (int neighbor = neighbors.nextSetBit(1); neighbor >= 0; neighbor = neighbors.nextSetBit(neighbor + 1)) {
                adjBits[i].set(neighbor - 1);
            }
        }

        // Recursive BBMC solver class.
        class BitBoardMaxClique {
            int maxCliqueSize = 0;

            /**
             * Recursively expands a candidate set to find the maximum clique.
             * This is the core of the Bron-Kerbosch algorithm with pivoting.
             *
             * @param   clique
             * The current clique represented as a BitSet of 0-based node indices.
             *
             * @param   candidates
             * The set of nodes that can potentially be added to the current clique.
             *
             * @effect  Updates the maximum clique size found so far (maxCliqueSize) if the
             * current clique is larger than the previous maximum.
             * | if candidates.isEmpty()
             * |       then maxCliqueSize == max(maxCliqueSize, clique.cardinality())
             */
            void expand(BitSet clique, BitSet candidates) {
                // Base Case: If no more candidates, update max clique size.
                if (candidates.isEmpty()) {
                    maxCliqueSize = Math.max(maxCliqueSize, clique.cardinality());
                    return;
                }

                // Pivot Selection: Find the node (pivot) in candidates with the maximum degree.
                int pivot = -1;
                int maxDegree = -1;
                for (int v = candidates.nextSetBit(0); v >= 0; v = candidates.nextSetBit(v + 1)) {
                    // Calculate degree of node v.
                    int degree = adjBits[v].cardinality();
                    if (degree > maxDegree) {
                        maxDegree = degree;
                        pivot = v;
                    }
                }

                // Pruning: Determine the set of candidates (ext) not connected to the pivot.
                BitSet ext = (BitSet) candidates.clone();
                ext.andNot(adjBits[pivot]);

                // Iterate over the reduced candidate set (ext) to start new branches.
                for (int v = ext.nextSetBit(0); v >= 0; v = ext.nextSetBit(v + 1)) {
                    // Add the current node 'v' to the current clique.
                    clique.set(v);

                    // New candidates are the intersection of current candidates and neighbors of 'v'.
                    BitSet newCandidates = (BitSet) candidates.clone();
                    newCandidates.and(adjBits[v]);

                    // Recursive call with the expanded clique and new candidates.
                    expand(clique, newCandidates);

                    // Backtrack: Remove 'v' from clique and candidates set.
                    clique.clear(v);
                    candidates.clear(v);
                }
            }
        }

        // Initialize the solver and start the search with an empty clique and all nodes as candidates.
        BitBoardMaxClique  solver = new BitBoardMaxClique();
        BitSet emptyClique = new BitSet(n);
        BitSet allNodes = new BitSet(n);
        allNodes.set(0, n);
        solver.expand(emptyClique, allNodes);

        // Return the maximum clique size found.
        return solver.maxCliqueSize;
    }

    /**
     * Remove the given node from the graph, along with all edges connected to it.
     * If the node does not exist, this method throws an exception.
     *
     * @param   v
     *          The node to remove.
     *
     * @effect  For all neighbors u of the given node v (stored as set bits in a BitSet),
     *          the bit corresponding to v is cleared in u's adjacency BitSet, and the
     *          edge count is decreased by one for each removed edge.
     *          | for each u in adjList.get(v).nextSetBit(1..):
     *          |     adjList.get(u).clear(v)
     *          | edgeCount == old(edgeCount) - adjList.get(v).cardinality()
     *
     * @effect  The given node is removed from the adjacency list.
     *          | !adjList.containsKey(v)
     *
     * @effect  The given node is removed from the coloring map.
     *          | !colors.containsKey(v)
     *
     * @throws  IllegalArgumentException
     *          If v does not exist in the adjacency list.
     *          | !adjList.containsKey(v)
     */
    @Override
    public void removeNode(int v) {
        if (!adjList.containsKey(v))
            throw new IllegalArgumentException(
                    "The node must exist in order to remove it: v=" + v
            );

        BitSet neighbors = adjList.get(v);

        // Remove v from all neighbors
        for (int u = neighbors.nextSetBit(1); u >= 0; u = neighbors.nextSetBit(u + 1)) {
            adjList.get(u).clear(v);
            edgeCount--;
        }

        // Remove v itself
        adjList.remove(v);
        colors.remove(v);
    }

    /**
     * Adds an undirected edge between the given nodes u and v.
     * Both nodes must already exist in the adjacency list.
     *
     * @param   u
     *          One endpoint of the edge.
     *
     * @param   v
     *          The other endpoint of the edge.
     *
     * @effect  The node v is added to the adjacency BitSet of u, and
     *          the node u is added to the adjacency BitSet of v.
     *          | adjList.get(u).get(v) == true
     *          | adjList.get(v).get(u) == true
     *
     * @effect  The edge count is increased by one if the edge did not already exist.
     *          | if (!old(adjList.get(u)).get(v))
     *          |     then edgeCount == old(edgeCount) + 1
     *          | else
     *          |     edgeCount == old(edgeCount)
     *
     * @throws  IllegalArgumentException
     *          If either u or v does not exist in the adjacency list.
     *          | !adjList.containsKey(u) || !adjList.containsKey(v)
     */
    private void addEdge(int u, int v) {
        if (!adjList.containsKey(u) || !adjList.containsKey(v)) {
            throw new IllegalArgumentException(
                    "Both nodes must exist before adding an edge: u=" + u + ", v=" + v
            );
        }

        // Only increment edgeCount if this edge does not exist yet
        if (!adjList.get(u).get(v)) {
            adjList.get(u).set(v);
            adjList.get(v).set(u);
            edgeCount++;
        }
    }

    /**
     * Remove the edge between the given nodes u and v, if it exists.
     *
     * @param   u
     *          One endpoint of the edge.
     *
     * @param   v
     *          The other endpoint of the edge.
     *
     * @effect  If there exists an edge between u and v (represented by set bits in the BitSets),
     *          then the bits are cleared in both adjacency BitSets.
     *          | if (adjList.get(u).get(v))
     *          |     then adjList.get(u).clear(v) &&
     *          |          adjList.get(v).clear(u)
     *
     * @effect  The edge count is decreased by one if the edge (u, v) existed.
     *          | if (old(adjList.get(u)).get(v))
     *          |     then edgeCount == old(edgeCount) - 1
     *          | else
     *          |     edgeCount == old(edgeCount)
     *
     * @throws  IllegalArgumentException
     *          If either u or v does not exist in the adjacency list.
     *          | !adjList.containsKey(u) || !adjList.containsKey(v)
     *
     * @throws  IllegalArgumentException
     *          If there is no edge between u and v.
     *          | !adjList.get(u).get(v) || !adjList.get(v).get(u)
     */
    @Override
    public void removeEdge(int u, int v) {
        if (!adjList.containsKey(u) || !adjList.containsKey(v)) {
            throw new IllegalArgumentException(
                    "Both nodes must exist before removing an edge: u=" + u + ", v=" + v
            );
        }
        if (!adjList.get(u).get(v) || !adjList.get(v).get(u)) {
            throw new IllegalArgumentException(
                    "The given edge does not exist: u=" + u + ", v=" + v
            );
        }

        adjList.get(u).clear(v);
        adjList.get(v).clear(u);
        edgeCount--;
    }

    @Override
    public void applyReduction() {
        // Determine the threshold: largest known clique or k for k-coloring
        int threshold = getMaxClique();

        // Collect nodes to remove
        List<Integer> toRemove = new ArrayList<>();
        for (int node : adjList.keySet()) {
            if (getDegree(node) < threshold) {
                toRemove.add(node);
            }
        }

        // Remove nodes safely
        for (int node : toRemove) {
            removeNode(node);
        }
    }

    @Override
    public void applyConstructionHeuristic() {
        // Placeholder
    }

    @Override
    public void applyStochasticLocalSearchAlgorithm() {
        // Placeholder
    }

    // Helper methods to allow GraphVisualizer acces to internal representation graph
    public Map<Integer, BitSet> getAdjListCopy() {
        return new HashMap<>(this.adjList);
    }
    public Map<Integer, Integer> getColorsCopy() {
        return new HashMap<>(this.colors);
    }
}
