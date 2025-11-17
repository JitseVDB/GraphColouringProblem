import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A JUnit (5) test class for testing the non-private methods of the Graph Class.
 *
 * @author  Jitse Vandenberghe
 * @version 1.0
 */
class GraphTest {

    private Graph graph;

    // AUXILIARY METHODS

    // Helper method to invoke private method getSaturationDegree
    private int invokeGetSaturationDegree(int node) {
        try {
            var method = Graph.class.getDeclaredMethod("getSaturationDegree", int.class);
            method.setAccessible(true);
            return (int) method.invoke(graph, node);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Helper to create a temporary DIMACS file
    private Path createTempDIMACSFile(int n, int[][] edges) throws IOException {
        Path tempFile = Files.createTempFile("graph", ".col");
        StringBuilder sb = new StringBuilder();
        sb.append("p edge ").append(n).append(" ").append(edges.length).append("\n");
        for (int[] edge : edges) {
            sb.append("e ").append(edge[0]).append(" ").append(edge[1]).append("\n");
        }
        Files.writeString(tempFile, sb.toString());
        return tempFile;
    }

    @BeforeEach
    void setUp() {
        graph = new Graph();
    }

    @Test
    void testGetNodes() throws IOException {
        // 1. Regular Graph
        // Create a temporary graph with 3 nodes and 2 edges: 1-2, 2-3
        Path tempFileGraph = createTempDIMACSFile(3, new int[][]{{1, 2}, {2, 3}});
        graph.loadDIMACS(tempFileGraph.toString());

        Collection<Integer> nodesGraph = graph.getNodes();

        // The graph should contain exactly nodes 1, 2, 3
        assertEquals(3, nodesGraph.size());
        assertTrue(nodesGraph.contains(1));
        assertTrue(nodesGraph.contains(2));
        assertTrue(nodesGraph.contains(3));

        // Ensure the returned collection is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> nodesGraph.add(4));

        // 2. Empty Graph
        // Create an empty graph with 0 nodes and 0 edges
        Path tempFileEmptyGraph = createTempDIMACSFile(0, new int[][]{});
        graph.loadDIMACS(tempFileEmptyGraph.toString());

        Collection<Integer> nodesEmptyGraph = graph.getNodes();

        // The graph should contain exactly no nodes
        assertEquals(0, nodesEmptyGraph.size());
        assertFalse(nodesEmptyGraph.contains(1));

        // Ensure the returned collection is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> nodesEmptyGraph.add(1));
    }

    @Test
    void testGetNumberOfEdges() throws IOException {
        // 1. Regular Graph
        // Create a temporary graph with 3 nodes and 2 edges: 1-2, 2-3
        Path tempFileGraph = createTempDIMACSFile(3, new int[][]{{1, 2}, {2, 3}});
        graph.loadDIMACS(tempFileGraph.toString());

        // The graph edge count should be exactly 2.
        assertEquals(2, graph.getNumberOfEdges());

        // 2. Empty Graph
        // Create an empty graph with 0 nodes and 0 edges
        Path tempFileEmptyGraph = createTempDIMACSFile(0, new int[][]{});
        graph.loadDIMACS(tempFileEmptyGraph.toString());

        // The graph should contain exactly no edges.
        assertEquals(0, graph.getNumberOfEdges());
    }

    @Test
    void testGetNumberOfNodes() throws IOException {
        // 1. Regular Graph
        // Create a temporary graph with 3 nodes and 2 edges: 1-2, 2-3
        Path tempFileGraph = createTempDIMACSFile(3, new int[][]{{1, 2}, {2, 3}});
        graph.loadDIMACS(tempFileGraph.toString());

        // The graph edge count should be exactly 2.
        assertEquals(3, graph.getNumberOfNodes());

        // 2. Empty Graph
        // Create an empty graph with 0 nodes and 0 edges
        Path tempFileEmptyGraph = createTempDIMACSFile(0, new int[][]{});
        graph.loadDIMACS(tempFileEmptyGraph.toString());

        // The graph should contain exactly no nodes
        assertEquals(0, graph.getNumberOfNodes());
    }

    @Test
    void testGetDegree() throws IOException {
        // 1. Regular Graph
        // Create a temporary graph with 4 nodes and 2 edges: 1-2, 2-3
        Path tempFileGraph = createTempDIMACSFile(4, new int[][]{{1, 2}, {2, 3}});
        graph.loadDIMACS(tempFileGraph.toString());

        // The graph edge count should be exactly 2.
        assertEquals(1, graph.getDegree(1));
        assertEquals(2, graph.getDegree(2));
        assertEquals(1, graph.getDegree(3));
        assertEquals(0, graph.getDegree(4)); // isolated node

        // 2. Empty Graph
        // Create an empty graph with 0 nodes and 0 edges
        Path tempFileEmptyGraph = createTempDIMACSFile(0, new int[][]{});
        graph.loadDIMACS(tempFileEmptyGraph.toString());

        // The getDegree function should return 0 for a non-existent node.
        assertEquals(0, graph.getDegree(0));
    }

    @Test
    void testGetNeighboursOf() throws IOException {
        // 1. Regular Graph
        // Create a temporary graph with 4 nodes and 2 edges: 1-2, 2-3
        Path tempFileGraph = createTempDIMACSFile(4, new int[][]{{1, 2}, {2, 3}});
        graph.loadDIMACS(tempFileGraph.toString());

        // 1.1 Neighbour nodes
        assertTrue(graph.areNeighbors(1,2));
        assertTrue(graph.areNeighbors(2,1));
        assertTrue(graph.areNeighbors(2,3));
        assertTrue(graph.areNeighbors(3,2));
        // 1.2 Non-neighbour nodes
        assertFalse(graph.areNeighbors(1,3));
        assertFalse(graph.areNeighbors(3,1));
        // 1.3 Node itself
        assertFalse(graph.areNeighbors(1,1));
        assertFalse(graph.areNeighbors(2,2));
        assertFalse(graph.areNeighbors(3,3));
        // 1.4 Non-existent nodes
        assertFalse(graph.areNeighbors(4,5));

        // 2. Empty Graph
        // Create an empty graph with 0 nodes and 0 edges
        Path tempFileEmptyGraph = createTempDIMACSFile(0, new int[][]{});
        graph.loadDIMACS(tempFileEmptyGraph.toString());

        // 2.1 Non-existent nodes
        assertFalse(graph.areNeighbors(1,2));
    }

    @Test
    void testAreNeighbour() throws IOException {
        // 1. Regular Graph
        // Create a temporary graph with 4 nodes and 2 edges: 1-2, 2-3
        Path tempFileGraph = createTempDIMACSFile(5, new int[][]{{1, 2}, {1,4}, {2, 3}});
        graph.loadDIMACS(tempFileGraph.toString());

        Collection<Integer> neighbours1 = graph.getNeighborsOf(1);
        Collection<Integer> neighbours2 = graph.getNeighborsOf(2);
        Collection<Integer> neighbours3 = graph.getNeighborsOf(3);
        Collection<Integer> neighbours4 = graph.getNeighborsOf(4);
        Collection<Integer> neighbours5 = graph.getNeighborsOf(5);

        // 1.1 Neighbours of node 1
        assertEquals(2, neighbours1.size());
        assertTrue(neighbours1.contains(2));
        assertTrue(neighbours1.contains(4));
        assertFalse(neighbours1.contains(3));
        assertFalse(neighbours1.contains(1));
        // 1.2 Neighbours of node 2
        assertEquals(2, neighbours1.size());
        assertTrue(neighbours2.contains(1));
        assertTrue(neighbours2.contains(3));
        assertFalse(neighbours2.contains(4));
        assertFalse(neighbours2.contains(2));
        // 1.3 Neighbours of node 3
        assertEquals(1, neighbours3.size());
        assertTrue(neighbours3.contains(2));
        assertFalse(neighbours3.contains(4));
        assertFalse(neighbours3.contains(3));
        // 1.4 Neighbours of node 4
        assertEquals(1, neighbours4.size());
        assertTrue(neighbours4.contains(1));
        assertFalse(neighbours4.contains(2));
        assertFalse(neighbours4.contains(4));
        // 1.5 Neighbours of node 5
        assertEquals(0, neighbours5.size());
        assertFalse(neighbours5.contains(1));
        assertFalse(neighbours5.contains(5));

        // 2. Empty Graph
        // Create an empty graph with 0 nodes and 0 edges
        Path tempFileEmptyGraph = createTempDIMACSFile(0, new int[][]{});
        graph.loadDIMACS(tempFileEmptyGraph.toString());

        Collection<Integer> neighboursNonExistentNode = graph.getNeighborsOf(1);

        assertFalse(neighboursNonExistentNode.contains(1));
    }

    @Test
    void testGetColor() throws IOException {
        // 1. Regular Graph
        // Create a temporary graph with 4 nodes and 2 edges: 1-2, 2-3
        Path tempFileGraph = createTempDIMACSFile(4, new int[][]{{1, 2}, {2, 3}});
        graph.loadDIMACS(tempFileGraph.toString());

        // 1.1 All nodes should initially be uncolored (-1)
        assertEquals(-1, graph.getColor(1));
        assertEquals(-1, graph.getColor(2));
        assertEquals(-1, graph.getColor(3));
        assertEquals(-1, graph.getColor(4));

        // 1.2 Color a few nodes
        graph.colorNode(1, 5);
        graph.colorNode(3, 2);

        assertEquals(5, graph.getColor(1));
        assertEquals(2, graph.getColor(3));

        // 1.3 Uncolored nodes remain -1
        assertEquals(-1, graph.getColor(2));
        assertEquals(-1, graph.getColor(4));

        // 1.4 Overwrite a color
        graph.colorNode(1, 7);
        assertEquals(7, graph.getColor(1));

        // 2. Non-existent node
        // Should return -1, not throw
        assertEquals(-1, graph.getColor(999));

        // 3. After resetColors()
        graph.resetColors();
        assertEquals(-1, graph.getColor(1));
        assertEquals(-1, graph.getColor(2));
        assertEquals(-1, graph.getColor(3));
        assertEquals(-1, graph.getColor(4));

        // 4. Remove a node
        graph.loadDIMACS(tempFileGraph.toString()); // reload
        graph.colorNode(2, 9);
        graph.removeNode(2);

        // Removed node, getColor should return -1
        assertEquals(-1, graph.getColor(2));

        // Neighbouring nodes unaffected
        assertEquals(-1, graph.getColor(1));
        assertEquals(-1, graph.getColor(3));
    }

    @Test
    void testGetSaturationDegree() throws IOException {
        // Edges:
        // 1-2, 1-4, 2-4, 2-5, 4-5, 5-3, 5-6, 3-6
        int[][] edges = {
                {1,2}, {1,4}, {2,4}, {2,5}, {4,5},
                {5,3}, {5,6}, {3,6}
        };

        Path file = createTempDIMACSFile(7, edges);
        graph.loadDIMACS(file.toString());

        // 1. All nodes initially uncolored -> saturation = 0
        for (int v = 1; v <= 6; v++) {
            assertEquals(0, graph.getSaturationDegree(v));
        }

        // 2. Color one neighbor
        // Color neighbor 2 red (color 1)
        graph.colorNode(2, 1);

        // Node 1 neighbors: {2,4} → colors {1, -1} → unique colors = {1} → deg=1
        assertEquals(1, graph.getSaturationDegree(1));

        // Node 4 neighbors: {1,2,5} → colors {-1,1,-1} → unique {1} → deg=1
        assertEquals(1, graph.getSaturationDegree(4));

        // Node 3 neighbors uncolored → deg=0
        assertEquals(0, graph.getSaturationDegree(3));

        // 3. Add more colors
        // Color node 4 blue (color 2)
        graph.colorNode(4, 2);

        // Node 1 neighbors = {2 (1), 4 (2)} → distinct {1,2} → deg=2
        assertEquals(2, graph.getSaturationDegree(1));

        // Node 5 neighbors = {2 (1),4 (2),3(-1),6(-1)} → distinct {1,2} → deg=2
        assertEquals(2, graph.getSaturationDegree(5));

        // 4. Color neighbors with duplicate colors → duplicates shouldn't count twice
        // Color node 5 also with color 1 (same as node 2)
        graph.colorNode(5, 1);

        // Node 4 neighbors = {1(-1),2(1),5(1)} → distinct {1} → deg still = 1
        assertEquals(1, graph.getSaturationDegree(4));

        // Node 2 neighbors = {1(-1),4(2),5(1)} → distinct {1,2} → deg=2
        assertEquals(2, graph.getSaturationDegree(2));

        // 5. Mixed colored & uncolored in complex structure
        // Color node 3 green (color 3)
        graph.colorNode(3, 3);

        // Node 5 neighbors = {2(1),4(2),3(3),6(-1)} → distinct {1,2,3} → deg=3
        assertEquals(3, graph.getSaturationDegree(5));

        // Node 3 neighbors = {5(1),6(-1)} → distinct {1} → deg=1
        assertEquals(1, graph.getSaturationDegree(3));

        // 6. Node with no neighbors should stay 0
        assertEquals(0, graph.getSaturationDegree(7));

        // 7. Non-existent node returns 0, not exception
        assertThrows(IllegalArgumentException.class, () -> graph.getSaturationDegree(10));

        // 8. After resetColors → all degrees = 0 again
        graph.resetColors();
        for (int v = 1; v <= 6; v++) {
            assertEquals(0, graph.getSaturationDegree(v));
        }

        // 9. After removing a node
        graph.loadDIMACS(file.toString());
        graph.colorNode(2, 1);
        graph.colorNode(4, 2);

        graph.removeNode(2);

        // Node 1 neighbors = {4} → distinct {2} → deg=1
        assertEquals(1, graph.getSaturationDegree(1));

        // Node 4 neighbors = {1,5} (node 2 removed) → both uncolored → deg=0
        assertEquals(0, graph.getSaturationDegree(4));

        // Removed node always → deg=0
        assertThrows(IllegalArgumentException.class, () -> graph.getSaturationDegree(2));
    }

}
