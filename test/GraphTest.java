import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

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

        // The graph should contain exactly nodes 0, 1, 2 (0-based indexing)
        assertEquals(3, nodesGraph.size());
        assertTrue(nodesGraph.contains(0));
        assertTrue(nodesGraph.contains(1));
        assertTrue(nodesGraph.contains(2));

        // Ensure the returned collection is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> nodesGraph.add(3));

        // 2. Empty Graph
        // Create an empty graph with 0 nodes and 0 edges
        Path tempFileEmptyGraph = createTempDIMACSFile(0, new int[][]{});
        graph.loadDIMACS(tempFileEmptyGraph.toString());

        Collection<Integer> nodesEmptyGraph = graph.getNodes();

        // The graph should contain exactly no nodes
        assertEquals(0, nodesEmptyGraph.size());
        assertFalse(nodesEmptyGraph.contains(0));

        // Ensure the returned collection is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> nodesEmptyGraph.add(0));
    }

    @Test
    void testGetNumberOfNodes() throws IOException {
        // 1. Regular Graph
        // Create a temporary graph with 3 nodes and 2 edges: 1-2, 2-3
        Path tempFileGraph = createTempDIMACSFile(3, new int[][]{{1, 2}, {2, 3}});
        graph.loadDIMACS(tempFileGraph.toString());

        // The graph node count should be exactly 3.
        assertEquals(3, graph.getNumberOfNodes());

        // 2. Empty Graph
        // Create an empty graph with 0 nodes and 0 edges
        Path tempFileEmptyGraph = createTempDIMACSFile(0, new int[][]{});
        graph.loadDIMACS(tempFileEmptyGraph.toString());

        // The graph should contain exactly no nodes
        assertEquals(0, graph.getNumberOfNodes());
    }

    @Test
    void testGetTotalNodes() throws IOException {
        // 1. Regular Graph
        // Create a temporary graph with 3 nodes and 2 edges: 1-2, 2-3
        Path tempFileGraph = createTempDIMACSFile(3, new int[][]{{1, 2}, {2, 3}});
        graph.loadDIMACS(tempFileGraph.toString());

        // The total amount of vertices for the graph should be 3.
        assertEquals(3, graph.getTotalNodes());

        graph.removeNode(0);

        // The graph should still contain the same amount of vertices.
        assertEquals(3, graph.getTotalNodes());

        // 2. Empty Graph
        // Create an empty graph with 0 nodes and 0 edges
        Path tempFileEmptyGraph = createTempDIMACSFile(0, new int[][]{});
        graph.loadDIMACS(tempFileEmptyGraph.toString());

        // The graph should contain exactly no nodes
        assertEquals(0, graph.getTotalNodes());
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
    void testGetNeighbor() throws IOException {
        // 1. Regular Graph
        // Create a temporary graph with 5 nodes and edges: 1-2, 1-4, 2-3
        Path tempFileGraph = createTempDIMACSFile(5, new int[][]{{1, 2}, {1, 4}, {2, 3}});
        graph.loadDIMACS(tempFileGraph.toString());

        // Convert to 0-based indices
        Collection<Integer> neighbours0 = graph.getNeighborsOf(0);
        Collection<Integer> neighbours1 = graph.getNeighborsOf(1);
        Collection<Integer> neighbours2 = graph.getNeighborsOf(2);
        Collection<Integer> neighbours3 = graph.getNeighborsOf(3);
        Collection<Integer> neighbours4 = graph.getNeighborsOf(4);

        // 1.1 Neighbours of node 0
        assertEquals(2, neighbours0.size());
        assertTrue(neighbours0.contains(1));
        assertTrue(neighbours0.contains(3));
        assertFalse(neighbours0.contains(2));
        assertFalse(neighbours0.contains(0));

        // 1.2 Neighbours of node 1
        assertEquals(2, neighbours1.size());
        assertTrue(neighbours1.contains(0));
        assertTrue(neighbours1.contains(2));
        assertFalse(neighbours1.contains(3));
        assertFalse(neighbours1.contains(1));

        // 1.3 Neighbours of node 2
        assertEquals(1, neighbours2.size());
        assertTrue(neighbours2.contains(1));
        assertFalse(neighbours2.contains(0));
        assertFalse(neighbours2.contains(3));
        assertFalse(neighbours2.contains(2));

        // 1.4 Neighbours of node 3
        assertEquals(1, neighbours3.size());
        assertTrue(neighbours3.contains(0));
        assertFalse(neighbours3.contains(1));
        assertFalse(neighbours3.contains(3));

        // 1.5 Neighbours of node 4
        assertEquals(0, neighbours4.size());

        // 1.6 Neighbours of non-existent nodes
        assertThrows(IllegalArgumentException.class, () -> graph.getNeighborsOf(-1));
        assertThrows(IllegalArgumentException.class, () -> graph.getNeighborsOf(5));
        graph.removeNode(0);
        assertThrows(IllegalArgumentException.class, () -> graph.getNeighborsOf(0));

        // 2. Empty Graph
        Path tempFileEmptyGraph = createTempDIMACSFile(0, new int[][]{});
        graph.loadDIMACS(tempFileEmptyGraph.toString());

        assertThrows(IllegalArgumentException.class, () -> graph.getNeighborsOf(0));
    }

    @Test
    void testAreNeighbors() throws IOException {
        // 1. Regular Graph
        // Create a temporary graph with 4 nodes and 2 edges: 1-2, 2-3
        Path tempFileGraph = createTempDIMACSFile(4, new int[][]{{1, 2}, {2, 3}});
        graph.loadDIMACS(tempFileGraph.toString());

        // 1.1 Neighbor nodes
        assertTrue(graph.areNeighbors(0, 1));
        assertTrue(graph.areNeighbors(1, 0));
        assertTrue(graph.areNeighbors(1, 2));
        assertTrue(graph.areNeighbors(2, 1));

        // 1.2 Non-neighbor nodes
        assertFalse(graph.areNeighbors(0, 2));
        assertFalse(graph.areNeighbors(2, 0));

        // 1.3 Node itself
        assertFalse(graph.areNeighbors(0, 0));
        assertFalse(graph.areNeighbors(1, 1));
        assertFalse(graph.areNeighbors(2, 2));

        // 1.4 Non-existent or inactive nodes
        assertThrows(IllegalArgumentException.class, () -> graph.areNeighbors(0, 4));
        assertThrows(IllegalArgumentException.class, () -> graph.areNeighbors(4, 0));
        assertThrows(IllegalArgumentException.class, () -> graph.areNeighbors(4, 4));
        assertThrows(IllegalArgumentException.class, () -> graph.areNeighbors(4, 5));
        assertThrows(IllegalArgumentException.class, () -> graph.areNeighbors(-1, 4));
        assertThrows(IllegalArgumentException.class, () -> graph.areNeighbors(4, -1));
        graph.removeNode(0);
        assertThrows(IllegalArgumentException.class, () -> graph.areNeighbors(0,1));
        assertThrows(IllegalArgumentException.class, () -> graph.areNeighbors(1,0));

        // 2. Empty Graph
        Path tempFileEmptyGraph = createTempDIMACSFile(0, new int[][]{});
        graph.loadDIMACS(tempFileEmptyGraph.toString());

        // 2.1 Non-existent nodes
        assertThrows(IllegalArgumentException.class, () -> graph.areNeighbors(0, -1));
        assertThrows(IllegalArgumentException.class, () -> graph.areNeighbors(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> graph.areNeighbors(0, 1));
    }

    @Test
    void testIsActive() throws IOException {
        // 1. Regular Graph
        Path file = createTempDIMACSFile(3, new int[][]{{1, 2}, {2, 3}});
        graph.loadDIMACS(file.toString());

        int originalEdgeCount = graph.getNumberOfEdges();
        int numberOfNeighbors1 = graph.getNeighborsOf(1).size(); // node 2 in 0-based

        // 1.1 Remove a node with multiple neighbors (node 1 in 0-based)
        assertEquals(3, graph.getNumberOfNodes());
        assertEquals(2, graph.getNumberOfEdges());

        graph.removeNode(0);

        assertFalse(graph.isActive(0));
        assertTrue(graph.isActive(1));
        assertTrue(graph.isActive(2));
        assertThrows(IllegalArgumentException.class, () -> graph.isActive(3));
        assertThrows(IllegalArgumentException.class, () -> graph.isActive(-1));
    }

    @Test
    void testGetDegree() throws IOException {
        // 1. Regular Graph
        // Create a temporary graph with 4 nodes and 2 edges: 1-2, 2-3
        Path tempFileGraph = createTempDIMACSFile(4, new int[][]{{1, 2}, {2, 3}});
        graph.loadDIMACS(tempFileGraph.toString());

        // Degrees of active nodes
        assertEquals(1, graph.getDegree(0));
        assertEquals(2, graph.getDegree(1));
        assertEquals(1, graph.getDegree(2));
        assertEquals(0, graph.getDegree(3));

        // Accessing non-existent node
        assertThrows(IllegalArgumentException.class, () -> graph.getDegree(4));
        assertThrows(IllegalArgumentException.class, () -> graph.getDegree(-1));
        graph.removeNode(0);
        assertThrows(IllegalArgumentException.class, () -> graph.getDegree(0));

        // 2. Empty Graph
        Path tempFileEmptyGraph = createTempDIMACSFile(0, new int[][]{});
        graph.loadDIMACS(tempFileEmptyGraph.toString());

        // Any access should throw for non-existent node
        assertThrows(IllegalArgumentException.class, () -> graph.getDegree(0));
    }

    @Test
    void testGetSaturationDegree() throws IOException {
        // Edges: 1-2, 1-4, 2-4, 2-5, 4-5, 5-3, 5-6, 3-6
        int[][] edges = {
                {1,2}, {1,4}, {2,4}, {2,5}, {4,5},
                {5,3}, {5,6}, {3,6}
        };

        Path file = createTempDIMACSFile(7, edges);
        graph.loadDIMACS(file.toString());

        // 1. All nodes initially uncolored → saturation = 0
        for (int v = 0; v <= 6; v++) {
            assertEquals(0, graph.getSaturationDegree(v));
        }

        // 2. Color one neighbor
        graph.colorNode(1, 1);

        assertEquals(1, graph.getSaturationDegree(0));
        assertEquals(1, graph.getSaturationDegree(3));
        assertEquals(0, graph.getSaturationDegree(2));

        // 3. Add more colors
        graph.colorNode(3, 2);

        assertEquals(2, graph.getSaturationDegree(0));
        assertEquals(2, graph.getSaturationDegree(4));

        // 4. Color neighbors with duplicate colors
        graph.colorNode(4, 1);

        assertEquals(1, graph.getSaturationDegree(3));
        assertEquals(2, graph.getSaturationDegree(1));

        // 5. Mixed colored & uncolored in complex structure
        graph.colorNode(2, 3);

        assertEquals(3, graph.getSaturationDegree(4));
        assertEquals(1, graph.getSaturationDegree(2));

        // 6. Node with no neighbors should stay 0
        assertEquals(0, graph.getSaturationDegree(6));

        // 7. Non-existent node throws exception
        assertThrows(IllegalArgumentException.class, () -> graph.getSaturationDegree(10));
        assertThrows(IllegalArgumentException.class, () -> graph.getSaturationDegree(-1));
        graph.removeNode(0);
        assertThrows(IllegalArgumentException.class, () -> graph.getSaturationDegree(0));
    }

    @Test
    void testGetColor() throws IOException {
        // 1. Regular Graph
        // Create a temporary graph with 4 nodes and 2 edges: 1-2, 2-3
        Path tempFileGraph = createTempDIMACSFile(4, new int[][]{{1, 2}, {2, 3}});
        graph.loadDIMACS(tempFileGraph.toString());

        graph.colorNode(0, 5); // node 1
        graph.colorNode(2, 2); // node 3

        assertEquals(5, graph.getColor(0));
        assertEquals(-1, graph.getColor(1));
        assertEquals(2, graph.getColor(2));
        assertEquals(-1, graph.getColor(3));
        assertThrows(IllegalArgumentException.class, () -> graph.getColor(9));
        assertThrows(IllegalArgumentException.class, () -> graph.getColor(-1));
        graph.removeNode(0);
        assertThrows(IllegalArgumentException.class, () -> graph.getColor(0));
    }

    @Test
    void testGetColorArray() throws IOException {
        // 1. Regular Graph
        Path tempFileGraph = createTempDIMACSFile(4, new int[][]{{1, 2}, {2, 3}});
        graph.loadDIMACS(tempFileGraph.toString());

        // 1.1 Initially, all nodes should be uncolored (-1)
        int[] initialColors = graph.getColorArray();
        assertNotNull(initialColors);
        assertEquals(4, initialColors.length);
        assertArrayEquals(new int[]{-1, -1, -1, -1}, initialColors);

        // 1.2 Color some nodes
        graph.colorNode(0, 7);
        graph.colorNode(3, 1);

        int[] updatedColors = graph.getColorArray();
        assertArrayEquals(new int[]{7, -1, -1, 1}, updatedColors);

        // 1.3 Overwrite colors
        graph.colorNode(0, 3);
        graph.colorNode(3, 9);

        int[] overwrittenColors = graph.getColorArray();
        assertArrayEquals(new int[]{3, -1, -1, 9}, overwrittenColors);

        // 1.4 Invalid operations shouldn't affect array size
        assertThrows(IllegalArgumentException.class, () -> graph.colorNode(4, 2));
        assertEquals(4, graph.getColorArray().length);

        // 2. Empty graph
        Path tempFileEmptyGraph = createTempDIMACSFile(0, new int[][]{});
        graph.loadDIMACS(tempFileEmptyGraph.toString());

        int[] emptyColors = graph.getColorArray();
        assertNotNull(emptyColors);
        assertEquals(0, emptyColors.length);
    }

    @Test
    void testColorNode() throws IOException {
        // 1. Regular Graph
        Path tempFileGraph = createTempDIMACSFile(4, new int[][]{{1, 2}, {2, 3}});
        graph.loadDIMACS(tempFileGraph.toString());

        // 1.1 All nodes should initially be uncolored (-1)
        assertEquals(-1, graph.getColor(0));
        assertEquals(-1, graph.getColor(1));
        assertEquals(-1, graph.getColor(2));
        assertEquals(-1, graph.getColor(3));

        // 1.2 Color uncolored nodes
        graph.colorNode(0, 5);
        graph.colorNode(2, 2);

        assertEquals(5, graph.getColor(0));
        assertEquals(2, graph.getColor(2));

        // 1.3 Color already colored nodes
        graph.colorNode(0, 4);
        graph.colorNode(2, 3);

        assertEquals(4, graph.getColor(0));
        assertEquals(3, graph.getColor(2));

        // 1.4 Color non-existent node
        assertThrows(IllegalArgumentException.class, () -> graph.colorNode(4, 1));
        assertThrows(IllegalArgumentException.class, () -> graph.colorNode(-1, 1));
        graph.removeNode(0);
        assertThrows(IllegalArgumentException.class, () -> graph.colorNode(0, 1));
    }

    @Test
    void testResetColors() throws IOException {
        // 1. Regular Graph
        Path tempFileGraph = createTempDIMACSFile(4, new int[][]{{1, 2}, {2, 3}});
        graph.loadDIMACS(tempFileGraph.toString());

        graph.removeNode(3);
        graph.colorNode(0, 5);
        graph.colorNode(2, 2);

        assertEquals(5, graph.getColor(0));
        assertEquals(-1, graph.getColor(1));
        assertEquals(2, graph.getColor(2));

        graph.resetColors();

        assertEquals(-1, graph.getColor(0));
        assertEquals(-1, graph.getColor(1));
        assertEquals(-1, graph.getColor(2));

        // 2. Empty graph
        Path tempFileEmptyGraph = createTempDIMACSFile(0, new int[][]{});
        graph.loadDIMACS(tempFileEmptyGraph.toString());

        assertEquals(0, graph.getNumberOfNodes());
        assertTrue(graph.getNodes().isEmpty());

        assertDoesNotThrow(() -> graph.resetColors());

        assertEquals(0, graph.getNumberOfNodes());
        assertTrue(graph.getNodes().isEmpty());
    }

    @Test
    void testGetNumberOfUsedColors() throws IOException {
        // 1. Regular Graph
        // Create a temporary graph with 3 nodes and 2 edges: 1-2, 2-3
        Path tempFileGraph = createTempDIMACSFile(3, new int[][]{{1, 2}, {2, 3}});
        graph.loadDIMACS(tempFileGraph.toString());

        // Initial graph should have 0 colors
        assertEquals(0, graph.getNumberOfUsedColors());

        // Partially color the graph with 2 unique colors
        graph.colorNode(0,1);
        graph.colorNode(1,2);

        // The number of used colors should return 2
        assertEquals(2, graph.getNumberOfUsedColors());

        // Finish coloring by coloring final node with unique color
        graph.colorNode(2,3);

        // The number of used colors should return 3
        assertEquals(3, graph.getNumberOfUsedColors());

        // 2. Empty Graph
        // Create an empty graph with 0 nodes and 0 edges
        Path tempFileEmptyGraph = createTempDIMACSFile(0, new int[][]{});
        graph.loadDIMACS(tempFileEmptyGraph.toString());

        // The graph should contain exactly no nodes
        assertEquals(0, graph.getNumberOfUsedColors());
    }

    @Test
    void testgetNumberOfUsedColors() throws IOException {
        // 1. Regular Graph
        Path tempFileGraph = createTempDIMACSFile(4, new int[][]{{1, 2}, {2, 3}});
        graph.loadDIMACS(tempFileGraph.toString());

        // Initially no colors should be used
        assertEquals(0, graph.getNumberOfUsedColors());

        // Apply construction heuristic to ensure color count is consistent with coloring
        graph.applyConstructionHeuristic();

        // Coloring of the graph should consist of 2 colors.
        assertEquals(2, graph.getNumberOfUsedColors());

        // Test resetColors effect
        graph.resetColors();
        assertEquals(0, graph.getNumberOfUsedColors());

        // 2. Empty graph
        Path tempFileEmptyGraph = createTempDIMACSFile(0, new int[][]{});
        graph.loadDIMACS(tempFileEmptyGraph.toString());

        assertEquals(0, graph.getNumberOfNodes());
        assertEquals(0, graph.getNumberOfUsedColors());
    }

    @Test
    void testIsValidColoring() throws IOException {
        // 1. Regular Graph
        Path tempFileGraph = createTempDIMACSFile(4, new int[][]{{1, 2}, {2, 3}, {3, 4}});
        graph.loadDIMACS(tempFileGraph.toString());

        // 1.1 Initially, all nodes uncolored -> coloring should be valid
        assertTrue(graph.isValidColoring());

        // 1.2 Color nodes with no conflicts
        graph.colorNode(0, 1);
        graph.colorNode(1, 2);
        graph.colorNode(2, 3);
        graph.colorNode(3, 4);

        assertTrue(graph.isValidColoring());

        // 1.3 Introduce a conflict
        graph.colorNode(1, 3); // Node 1 now conflicts with Node 2
        assertFalse(graph.isValidColoring());

        // 1.4 Remove conflict
        graph.colorNode(1, 2);
        assertTrue(graph.isValidColoring());

        // 1.5 Remove a node and ensure coloring is still valid
        graph.removeNode(2); // Node 2 removed
        assertTrue(graph.isValidColoring());

        // 2. Partially colored graph with removed node
        graph.resetColors();
        graph.colorNode(0, 1);
        graph.removeNode(1);
        graph.colorNode(3, 2); // Nodes 1 deleted and 2 uncolored
        assertTrue(graph.isValidColoring());

        // 3. Empty graph
        Path tempFileEmptyGraph = createTempDIMACSFile(0, new int[][]{});
        graph.loadDIMACS(tempFileEmptyGraph.toString());
        assertTrue(graph.isValidColoring());
    }

    @Test
    void testRemoveNode() throws IOException {
        // 1. Regular Graph
        Path file = createTempDIMACSFile(3, new int[][]{{1, 2}, {2, 3}});
        graph.loadDIMACS(file.toString());

        int originalEdgeCount = graph.getNumberOfEdges();
        int numberOfNeighbors1 = graph.getNeighborsOf(1).size(); // node 2 in 0-based

        // 1.1 Remove a node with multiple neighbors (node 1 in 0-based)
        assertEquals(3, graph.getNumberOfNodes());
        assertEquals(2, graph.getNumberOfEdges());

        graph.removeNode(1); // remove node 2

        assertFalse(graph.getNeighborsOf(0).contains(1));
        assertFalse(graph.getNeighborsOf(2).contains(1));
        assertEquals(originalEdgeCount - numberOfNeighbors1, graph.getNumberOfEdges());
        assertFalse(graph.getNodes().contains(1));
        assertThrows(IllegalArgumentException.class, () -> graph.getColor(1));
        assertEquals(2, graph.getNumberOfNodes());
        assertTrue(graph.getNodes().contains(0));
        assertTrue(graph.getNodes().contains(2));

        // 1.2 Remove an isolated node (node 0)
        graph.removeNode(0);
        assertFalse(graph.getNodes().contains(0));
        assertThrows(IllegalArgumentException.class, () -> graph.getColor(0));
        assertEquals(1, graph.getNumberOfNodes());
        assertTrue(graph.getNodes().contains(2));

        // 1.3 Remove last node (node 2)
        graph.removeNode(2);
        assertFalse(graph.getNodes().contains(2));
        assertThrows(IllegalArgumentException.class, () -> graph.getColor(2));
        assertEquals(0, graph.getNumberOfNodes());
        assertEquals(0, graph.getNumberOfEdges());

        // 1.4 Remove non-existent node
        assertThrows(IllegalArgumentException.class, () -> graph.removeNode(4));
        assertThrows(IllegalArgumentException.class, () -> graph.removeNode(0));
        assertThrows(IllegalArgumentException.class, () -> graph.removeNode(-1));
        assertEquals(0, graph.getNumberOfNodes());
        assertEquals(0, graph.getNumberOfEdges());
    }

    @Test
    void testRemoveEdge() throws IOException {
        // Create a temporary graph with 4 nodes and edges: 1-2, 2-3, 2-4, 3-4
        Path file = createTempDIMACSFile(4, new int[][]{
                {1, 2}, {2, 3}, {2, 4}, {3, 4}
        });
        graph.loadDIMACS(file.toString()); // internally converts to 0-based

        assertEquals(4, graph.getNumberOfNodes());
        assertEquals(4, graph.getNumberOfEdges());

        // Remove edge 1-2 (0-based: 0-1)
        int oldEdgeCount = graph.getNumberOfEdges();
        graph.removeEdge(1, 2); // 1-based from file converted to 0-based in graph
        assertFalse(graph.getNeighborsOf(1).contains(2));
        assertFalse(graph.getNeighborsOf(2).contains(1));
        assertEquals(oldEdgeCount - 1, graph.getNumberOfEdges());
        assertTrue(graph.areNeighbors(0, 1));
        assertTrue(graph.areNeighbors(1, 3));
        assertTrue(graph.areNeighbors(2, 3));

        // Remove edge 2-3 (0-based: 1-2)
        oldEdgeCount = graph.getNumberOfEdges();
        graph.removeEdge(2, 3);
        assertFalse(graph.getNeighborsOf(2).contains(3));
        assertFalse(graph.getNeighborsOf(3).contains(2));
        assertEquals(oldEdgeCount - 1, graph.getNumberOfEdges());
        assertTrue(graph.areNeighbors(0, 1));
        assertTrue(graph.areNeighbors(1, 3));
        assertFalse(graph.areNeighbors(1, 2));

        // Remove non-existent edge
        assertFalse(graph.areNeighbors(0, 2));
        int unchangedEdgeCount = graph.getNumberOfEdges();
        assertThrows(IllegalArgumentException.class, () -> graph.removeEdge(0, 2));
        assertEquals(unchangedEdgeCount, graph.getNumberOfEdges());

        // Remove edge with non-existent node
        assertThrows(IllegalArgumentException.class, () -> graph.removeEdge(0, 999));
        assertThrows(IllegalArgumentException.class, () -> graph.removeEdge(999, 1));
        assertEquals(unchangedEdgeCount, graph.getNumberOfEdges());

        // Remove edge leaving a node isolated
        oldEdgeCount = graph.getNumberOfEdges();
        graph.removeEdge(0, 1);
        assertFalse(graph.getNeighborsOf(0).contains(1));
        assertFalse(graph.getNeighborsOf(1).contains(0));
        assertEquals(oldEdgeCount - 1, graph.getNumberOfEdges());
        assertTrue(graph.getNodes().contains(0));
        assertEquals(0, graph.getNeighborsOf(0).size());

        // Remove edge between two non-existent nodes
        assertThrows(IllegalArgumentException.class, () -> graph.removeEdge(10, 0));
        assertThrows(IllegalArgumentException.class, () -> graph.removeEdge(0, 10));
        assertThrows(IllegalArgumentException.class, () -> graph.removeEdge(0, -1));
        assertThrows(IllegalArgumentException.class, () -> graph.removeEdge(-1, 0));
        assertEquals(4, graph.getNumberOfNodes());
        assertTrue(graph.getNodes().contains(0));
        assertTrue(graph.getNodes().contains(1));
        assertTrue(graph.getNodes().contains(3));

        // Remove edge between removed node
        graph.removeNode(0);
        assertThrows(IllegalArgumentException.class, () -> graph.removeEdge(0, 1));
        assertThrows(IllegalArgumentException.class, () -> graph.removeEdge(1, 0));

        // Remove removed edge
        assertThrows(IllegalArgumentException.class, () -> graph.removeEdge(2, 1));
    }

    @Test
    void testApplyReduction_NoReduction_SquareGraph() throws IOException {
        // SCENARIO 1: No Reduction
        // Topology: Square Cycle (C4).
        // - Chromatic Number: 2 (Bipartite)
        // - Degrees: All nodes have degree 2.
        // - Expected Threshold: 2
        // - Logic: 2 is NOT strictly less than 2. No nodes removed.

        // Edges for a Square: 1-2, 2-3, 3-4, 4-1 (1-based input)
        int[][] squareEdges = {{1, 2}, {2, 3}, {3, 4}, {4, 1}};
        Path squareFile = createTempDIMACSFile(4, squareEdges);
        graph.loadDIMACS(squareFile.toString());

        // 1. Setup: Color the graph to set the threshold
        graph.applyConstructionHeuristic();
        int threshold = graph.getColorCount();

        // Check assumption: A valid heuristic on a C4 graph uses 2 colors.
        assertEquals(2, threshold, "A square graph should be colored with 2 colors");

        // 2. Action: Apply reduction
        graph.applyReduction();

        // 3. Verification
        assertEquals(4, graph.getNumberOfNodes(), "C4: No nodes should be removed (Deg 2 is not < Threshold 2)");
        assertEquals(4, graph.getNumberOfEdges(), "C4: No edges should be removed");
        assertTrue(graph.isActive(0));
    }

    @Test
    void testApplyReduction_PartialReduction_TailRemoval() throws IOException {
        // SCENARIO 2: Partial Reduction
        // Topology: Square Cycle (Nodes 0-3) with a 'Tail' (Node 4).
        // - Chromatic Number: 2
        // - Degrees: Node 4 has degree 1. Others >= 2.
        // - Expected Threshold: 2
        // - Logic: Node 4 (1 < 2) should be REMOVED.

        // Edges: Square + Tail (1-5)
        int[][] tailEdges = {{1, 2}, {2, 3}, {3, 4}, {4, 1}, {1, 5}};
        Path tailFile = createTempDIMACSFile(5, tailEdges);
        graph.loadDIMACS(tailFile.toString());

        // 1. Setup
        graph.applyConstructionHeuristic();
        int threshold = graph.getColorCount();
        assertEquals(2, threshold, "Square+Tail should still be 2-colorable");

        // 2. Action
        graph.applyReduction();

        // 3. Verification
        assertEquals(4, graph.getNumberOfNodes(), "Tail node should be removed");
        assertFalse(graph.isActive(4), "Node 4 (Tail) had degree 1 < 2, should be gone");
        assertTrue(graph.isActive(0), "Node 0 had degree 3, should remain");

        // 3b. Verify Edge Cleanup
        assertFalse(graph.getNeighborsOf(0).contains(4), "Node 0 should no longer list 4 as a neighbor");
        assertThrows(IllegalArgumentException.class, () -> graph.areNeighbors(0, 4));

        // 3c. Verify Neighbor Update
        assertEquals(2, graph.getDegree(0), "Neighbor's degree should decrease when node is removed");
        assertEquals(4, graph.getNumberOfEdges(), "Total edges should decrease by 1");
    }

    @Test
    void testApplyReduction_TotalReduction_TriangleGraph() throws IOException {
        // SCENARIO 3: Total Reduction
        // Topology: Triangle (K3).
        // - Chromatic Number: 3
        // - Degrees: All nodes have degree 2.
        // - Expected Threshold: 3
        // - Logic: Degree 2 < Threshold 3. ALL nodes removed.

        int[][] triangleEdges = {{1, 2}, {2, 3}, {3, 1}};
        Path triangleFile = createTempDIMACSFile(3, triangleEdges);
        graph.loadDIMACS(triangleFile.toString());

        // 1. Setup
        graph.applyConstructionHeuristic();
        int threshold = graph.getColorCount();
        assertEquals(3, threshold, "Triangle (K3) must use 3 colors");

        // 2. Action
        graph.applyReduction();

        // 3. Verification
        assertEquals(0, graph.getNumberOfNodes(), "All nodes (deg 2) < threshold (3) should be removed");
        assertEquals(0, graph.getNumberOfEdges(), "All edges should be removed");
        assertFalse(graph.isActive(0));

        // 4. Safety Check
        assertEquals(0, graph.getNodes().size());
        assertThrows(IllegalArgumentException.class, () -> graph.getDegree(0));
    }

    @Test
    void testConstructionHeuristic_Triangle() throws IOException {
        // SCENARIO 1: The Triangle (K3)
        // Expected: Exactly 3 colors (Clique constraint).

        int[][] triangleEdges = {{1, 2}, {2, 3}, {3, 1}};
        Path triangleFile = createTempDIMACSFile(3, triangleEdges);
        graph.loadDIMACS(triangleFile.toString());

        // Action
        graph.applyConstructionHeuristic();

        // Verification
        assertEquals(3, graph.getColorCount(), "Triangle must use exactly 3 colors");
        assertEquals(graph.getNumberOfUsedColors(), graph.getColorCount());

        // Validity Check
        assertTrue(graph.isValidColoring(), "Triangle coloring must be valid");
        assertNotEquals(graph.getColor(0), graph.getColor(1));
        assertNotEquals(graph.getColor(1), graph.getColor(2));
        assertNotEquals(graph.getColor(0), graph.getColor(2));
    }

    @Test
    void testConstructionHeuristic_Square() throws IOException {
        // SCENARIO 2: The Square (C4)
        // Expected: Exactly 2 colors (Bipartite constraint).

        int[][] squareEdges = {{1, 2}, {2, 3}, {3, 4}, {4, 1}};
        Path squareFile = createTempDIMACSFile(4, squareEdges);
        graph.loadDIMACS(squareFile.toString());

        // Action
        graph.applyConstructionHeuristic();

        // Verification
        assertEquals(2, graph.getColorCount(), "Square (Bipartite) should be colored with 2 colors");
        assertTrue(graph.isValidColoring(), "Square coloring must be valid");

        // Completeness Check
        for (int v : graph.getNodes()) {
            assertNotEquals(-1, graph.getColor(v), "Node " + v + " remained uncolored!");
        }
    }

    @Test
    void testConstructionHeuristic_DisconnectedGraph() throws IOException {
        // SCENARIO 3: Disconnected Graph
        // Topology: Triangle (3 colors) + Isolated Edge (2 colors).
        // Expected: 3 colors total.

        int[][] disconnectedEdges = {
                {1, 2}, {2, 3}, {3, 1}, // Triangle (Nodes 0,1,2)
                {4, 5}                  // Line (Nodes 3,4)
        };
        Path disFile = createTempDIMACSFile(5, disconnectedEdges);
        graph.loadDIMACS(disFile.toString());

        // Action
        graph.applyConstructionHeuristic();

        // Verification
        assertEquals(3, graph.getColorCount(), "Disconnected K3 + K2 should use max(3, 2) = 3 colors");
        assertTrue(graph.isValidColoring());

        // Verify distinct components are colored
        assertNotEquals(-1, graph.getColor(0));
        assertNotEquals(-1, graph.getColor(4));
    }

    @Test
    void testConstructionHeuristic_SingleNode() throws IOException {
        // SCENARIO 4: Single Node
        // Expected: 1 color.

        Path singleFile = createTempDIMACSFile(1, new int[][]{});
        graph.loadDIMACS(singleFile.toString());

        graph.applyConstructionHeuristic();

        assertEquals(1, graph.getColorCount(), "Single node graph requires 1 color");
        assertEquals(1, graph.getNumberOfUsedColors());
        assertNotEquals(-1, graph.getColor(0));
    }

    @Test
    void testConstructionHeuristic_EmptyGraph() throws IOException {
        // SCENARIO 5: Empty Graph
        // Expected: 0 colors, no errors.

        Path emptyFile = createTempDIMACSFile(0, new int[][]{});
        graph.loadDIMACS(emptyFile.toString());

        assertDoesNotThrow(() -> graph.applyConstructionHeuristic());
        assertEquals(0, graph.getColorCount());
    }

    @Test
    void testApplyStochasticLocalSearchAlgorithm_Optimization() throws IOException {
        // SCENARIO 1: Optimizing a "Bad" Coloring
        // Topology: Star Graph (Center 1, Leaves 2,3,4,5,6)
        // - Chromatic Number: 2 (Center needs color A, all leaves can be color B).
        // - We will start by giving every node a unique color (6 colors).

        // Edges: 1-2, 1-3, 1-4, 1-5, 1-6
        int[][] starEdges = {
                {1, 2}, {1, 3}, {1, 4}, {1, 5}, {1, 6}
        };
        Path starFile = createTempDIMACSFile(6, starEdges);
        graph.loadDIMACS(starFile.toString());

        // 1. Setup: Manually assign a "Bad" coloring (Unique color for everyone)
        // Node 0 gets color 0, Node 1 gets color 1, etc.
        for (int i = 0; i < 6; i++) {
            graph.colorNode(i, i);
        }

        // Verify initial bad state
        assertEquals(6, graph.getNumberOfUsedColors(), "Initial state should use 6 colors");
        assertTrue(graph.isValidColoring(), "Initial state is valid (just inefficient)");

        // 2. Action: Run ILS
        // logic: ILS will try k=5, k=4, ..., k=2. It will succeed.
        // It will try k=1, fail, and stop.
        graph.applyStochasticLocalSearchAlgorithm();

        // 3. Verification
        assertTrue(graph.isValidColoring(), "Resulting coloring must be valid");
        assertEquals(2, graph.getColorCount(), "ILS should optimize Star Graph to 2 colors");
        assertEquals(2, graph.getNumberOfUsedColors(), "Internal state should match");
    }

    @Test
    void testApplyStochasticLocalSearchAlgorithm_Uncolored() throws IOException {
        // SCENARIO 2: Starting from Uncolored State
        // Topology: Cycle Graph C5 (0-1-2-3-4-0)
        // - Chromatic Number: 3 (Odd cycle)
        // - We leave the graph uncolored (colors are -1).
        // - ILS logic handles uncolored graphs by assigning distinct colors first.

        int[][] cycleEdges = {
                {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 1}
        };
        Path cycleFile = createTempDIMACSFile(5, cycleEdges);
        graph.loadDIMACS(cycleFile.toString());

        // 1. Verify uncolored
        assertEquals(0, graph.getNumberOfUsedColors());

        // 2. Action
        graph.applyStochasticLocalSearchAlgorithm();

        // 3. Verification
        assertTrue(graph.isValidColoring());
        // It must find the optimal 3 colors for C5
        assertEquals(3, graph.getColorCount(), "ILS should find 3 colors for C5");

        // Ensure every node actually has a color
        for(int v : graph.getNodes()) {
            assertNotEquals(-1, graph.getColor(v));
        }
    }

    @Test
    void testApplyStochasticLocalSearchAlgorithm_AlreadyOptimal() throws IOException {
        // SCENARIO 3: Starting with an already Optimal Coloring
        // Topology: Triangle (K3)
        // - Chromatic Number: 3
        // - We provide a valid 3-coloring.
        // - ILS should try to find 2, fail, and return the 3-coloring.

        int[][] triangleEdges = {{1, 2}, {2, 3}, {3, 1}};
        Path file = createTempDIMACSFile(3, triangleEdges);
        graph.loadDIMACS(file.toString());

        // 1. Setup: Optimal coloring
        graph.colorNode(0, 0);
        graph.colorNode(1, 1);
        graph.colorNode(2, 2);

        assertEquals(3, graph.getNumberOfUsedColors());

        // 2. Action
        graph.applyStochasticLocalSearchAlgorithm();

        // 3. Verification
        assertEquals(3, graph.getColorCount(), "Should remain at 3 (optimal)");
        assertTrue(graph.isValidColoring());
    }

    @Test
    void testApplyStochasticLocalSearchAlgorithm_Disconnected() throws IOException {
        // SCENARIO 4: Disconnected Components
        // Topology: Two separate edges (1-2) and (3-4)
        // - Chromatic Number: 2
        // - We color them badly: 0:1, 1:2, 2:3, 3:4 (4 colors)

        int[][] edges = {{1, 2}, {3, 4}};
        Path file = createTempDIMACSFile(4, edges);
        graph.loadDIMACS(file.toString());

        graph.colorNode(0, 0);
        graph.colorNode(1, 1);
        graph.colorNode(2, 2);
        graph.colorNode(3, 3);

        assertEquals(4, graph.getNumberOfUsedColors());

        // Action
        graph.applyStochasticLocalSearchAlgorithm();

        // Verify
        assertEquals(2, graph.getColorCount(), "Should optimize disconnected edges to 2 colors");
        assertTrue(graph.isValidColoring());
    }

    @Test
    public void testGetAdjCopy() throws IOException {
        Path graphFile = createTempDIMACSFile(4, new int[][]{
                {1, 2}, {2, 3}, {3, 4}
        });

        graph.loadDIMACS(graphFile.toString());

        // Initially all nodes active: 0-3
        // Edges (0-based): 0–1, 1–2, 2–3

        Map<Integer, BitSet> adjCopy = graph.getAdjCopy();

        // All active nodes must appear
        assertEquals(Set.of(0,1,2,3), adjCopy.keySet());

        // Ensure adjacency correctness
        assertTrue(adjCopy.get(0).get(1));
        assertFalse(adjCopy.get(0).get(2));

        assertTrue(adjCopy.get(1).get(0));
        assertTrue(adjCopy.get(1).get(2));

        // Ensure deep copy (modifying returned copy must not modify graph)
        adjCopy.get(1).clear(2);
        assertTrue(graph.getAdjacencyRules(1).get(2));  // original untouched

        // Remove a node and recompute
        graph.applyReduction(); // depending on colors threshold may remove low-degree nodes
        graph.colorNode(1, 1);
        graph.colorNode(2, 1);

        Map<Integer, BitSet> adjCopy2 = graph.getAdjCopy();
        for (int v : adjCopy2.keySet()) {
            assertTrue(graph.isActive(v));
        }
    }

    @Test
    public void testGetColorsCopy() throws IOException {
        Path graphFile = createTempDIMACSFile(4, new int[][]{
                {1, 2}, {2, 3}, {3, 4}
        });

        graph.loadDIMACS(graphFile.toString());

        // Assign some colors
        graph.colorNode(0, 5);
        graph.colorNode(1, 3);
        graph.colorNode(2, 1);
        graph.colorNode(3, -1); // legal, means "uncolored"

        Map<Integer, Integer> colorsCopy = graph.getColorsCopy();

        assertEquals(5, colorsCopy.get(0));
        assertEquals(3, colorsCopy.get(1));
        assertEquals(1, colorsCopy.get(2));
        assertEquals(-1, colorsCopy.get(3));

        // Ensure deep copy: modifying returned map does not modify graph
        colorsCopy.put(1, 999);
        assertEquals(3, graph.getColor(1));

        // Test after removing nodes
        graph.applyReduction();
        Map<Integer, Integer> colorsCopy2 = graph.getColorsCopy();

        for (int v : colorsCopy2.keySet()) {
            assertTrue(graph.isActive(v));
        }
    }

    @Test
    public void testGetDegreesCopy() throws IOException {
        Path graphFile = createTempDIMACSFile(4, new int[][]{
                {1, 2}, {2, 3}, {3, 4} // 0–1–2–3
        });
        graph.loadDIMACS(graphFile.toString());

        // Degrees should be [1,2,2,1]
        int[] degrees = graph.getDegreesCopy();

        assertArrayEquals(new int[]{1,2,2,1}, degrees);

        // Modify returned copy; internal graph must not change
        degrees[1] = 0;
        assertEquals(2, graph.getDegree(1));

        // Reduce graph → degrees must reflect removal
        graph.applyReduction(); // likely removes 0 and 3 if threshold=2
        int[] degrees2 = graph.getDegreesCopy();

        for (int v = 0; v < degrees2.length; v++) {
            if (!graph.isActive(v))
                assertEquals(0, degrees2[v]);  // removed nodes must have degree 0
        }
    }

    @Test
    public void testGetAdjacencyRules() throws IOException {
        Path graphFile = createTempDIMACSFile(4, new int[][]{
                {1, 2}, {2, 3}, {3, 4}
        });

        graph.loadDIMACS(graphFile.toString());

        // Raw adjacency must match internal representation
        BitSet adj1 = graph.getAdjacencyRules(1);
        assertTrue(adj1.get(0));
        assertTrue(adj1.get(2));
        assertFalse(adj1.get(3));

        // Ensure it returns actual internal reference (NOT a clone)
        BitSet raw = graph.getAdjacencyRules(2);
        raw.clear(3);

        // Because it is the internal reference, the modification must affect the graph
        assertFalse(graph.getAdjacencyRules(2).get(3));

        // After reduction
        graph.applyReduction();
        for (int v = 0; v < 4; v++) {
            BitSet bs = graph.getAdjacencyRules(v);
            if (!graph.isActive(v)) {
                assertEquals(0, bs.cardinality());
            }
        }
    }
}