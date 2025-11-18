import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

// TODO: Add safegaurd to ensure all graphs are valid.
// TODO: Ensure all functions handle incorrect input gracefully.

/**
 * A class representing graphs.
 *
 * @author  Jitse Vandenberghe
 * @version 1.0
 */
public class Graph implements GraphInterface {
    private final Map<Integer, Set<Integer>> adjList = new HashMap<>();
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
                        adjList.put(i, new HashSet<>());
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
     *          | result == adjList.containsKey(v)
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

        return Collections.unmodifiableSet(adjList.getOrDefault(v, Collections.emptySet()));
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
     *          | result == (adjList.containsKey(u) && adjList.get(u).contains(v))
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

        return adjList.getOrDefault(u, Collections.emptySet()).contains(v);
    }

    /**
     * Returns the degree of the given node.
     *
     * @param   v
     *          The node whose degree is requested.
     *
     * @return  The number of neighbors of node v if the node v exists.
     *          | result == (adjList.containsKey(v))
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

        return adjList.getOrDefault(v, Collections.emptySet()).size();
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
     *          |         exists neighbor in adjList.get(v) :
     *          |             colors.containsKey(neighbor) &&
     *          |             colors.get(neighbor) != -1 &&
     *          |             c == colors.get(neighbor))
     *
     * @throws  IllegalArgumentException
     *          If the given node does not exist in the adjacency list.
     *          | !adjList.containsKey(v)
     */
    public int getSaturationDegree(int v) {
        if (!adjList.containsKey(v)) {
            throw new IllegalArgumentException("Node " + v + " does not exist in the graph.");
        }

        Set<Integer> neighborColors = new HashSet<>();
        for (int neighbor : adjList.get(v)) {
            int c = colors.getOrDefault(neighbor, -1);
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
     * Remove the given node from the graph, along with all edges connected to it.
     * If the node does not exist, this method does nothing.
     *
     * @param   v
     *          The node to remove.
     *
     * @effect  For all neighbors u of the given node v, the node v is removed from
     *          their adjacency sets, and the edge count is decreased by one for each
     *          removed edge.
     *          | for each u in adjList.get(v):
     *          |     adjList.get(u).remove(v)
     *          | edgeCount == old(edgeCount) - adjList.get(v).size()
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
        for (int neighbor : adjList.get(v)) {
            adjList.get(neighbor).remove(v);
            edgeCount--;
        }
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
     * @effect  The node v is added to the adjacency set of u, and
     *          the node u is added to the adjacency set of v.
     *          | adjList.get(u).contains(v)
     *          | adjList.get(v).contains(u)
     *
     * @effect  The edge count is increased by one if the edge did not already exist.
     *          | if (!old(adjList.get(u)).contains(v))
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

        if (adjList.get(u).add(v)) {
            adjList.get(v).add(u);
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
     * @effect  If there exists an edge between u and v, then v is removed from the
     *          adjacency set of u, and u is removed from the adjacency set of v.
     *          | if ((adjList.containsKey(u) && adjList.get(u).contains(v)) ||
     *          | (adjList.containsKey(v) && adjList.get(v).contains(u)))
     *          |    then adjList.get(u).remove(v) &&
     *          |         adjList.get(v).remove(u)
     *
     * @effect  The edge count is decreased by one if the edge (u, v) existed.
     *          | if (old(adjList.get(u)).contains(v))
     *          |     then edgeCount == old(edgeCount) - 1
     *          | else
     *          |     edgeCount == old(edgeCount)
     *
     * @throws  IllegalArgumentException
     *          If either u or v does not exist in the adjacency list.
     *          | !adjList.containsKey(u) || !adjList.containsKey(v)
     *
     * @throws  IllegalArgumentException
     *          If either u or v does not contain the other node within its neighbours.
     *          | !adjList.get(u).contains(v) || !adjList.get(v).contains(u)
     */
    @Override
    public void removeEdge(int u, int v) {
        if (!adjList.containsKey(u) || !adjList.containsKey(v)) {
            throw new IllegalArgumentException(
                    "Both nodes must exist before removing an edge: u=" + u + ", v=" + v
            );
        }
        if (!adjList.get(u).contains(v) || !adjList.get(v).contains(u)) {
            throw new IllegalArgumentException(
                    "The given edge does not exist: u=" + u + ", v=" + v
            );
        }

        if (adjList.getOrDefault(u, Collections.emptySet()).remove(v)) edgeCount--;
        adjList.getOrDefault(v, Collections.emptySet()).remove(u);
    }

    @Override
    public void applyReduction() {
        // Placeholder
    }

    @Override
    public void applyConstructionHeuristic() {
        // Placeholder
    }

    @Override
    public void applyStochasticLocalSearchAlgorithm() {
        // Placeholder
    }
}
