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
        Path tempFileGraph = createTempDIMACSFile(4, new int[][]{{1, 2}, {2, 3}});
        graph.loadDIMACS(tempFileGraph.toString());

        Collection<Integer> neighbours1 = graph.getNeighborsOf(1);
        Collection<Integer> neighbours2 = graph.getNeighborsOf(2);
        Collection<Integer> neighbours3 = graph.getNeighborsOf(3);
        Collection<Integer> neighbours4 = graph.getNeighborsOf(4);

        // The graph should contain exactly nodes 1, 2, 3
        assertEquals(3, nodesGraph.size());
        assertTrue(nodesGraph.contains(1));
        assertTrue(nodesGraph.contains(2));
        assertTrue(nodesGraph.contains(3));

        // 2. Empty Graph
        // Create an empty graph with 0 nodes and 0 edges
        Path tempFileEmptyGraph = createTempDIMACSFile(0, new int[][]{});
        graph.loadDIMACS(tempFileEmptyGraph.toString());

        // 2.1 Non-existent nodes
        assertFalse(graph.areNeighbors(1,2));
    }
}
