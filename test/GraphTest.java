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
    void testGetTotalVertices() throws IOException {
        // 1. Regular Graph
        // Create a temporary graph with 3 nodes and 2 edges: 1-2, 2-3
        Path tempFileGraph = createTempDIMACSFile(3, new int[][]{{1, 2}, {2, 3}});
        graph.loadDIMACS(tempFileGraph.toString());

        // The total amount of vertices for the graph should be 3.
        assertEquals(3, graph.getTotalVertices());

        graph.removeNode(0);

        // The graph should still contain the same amount of vertices.
        assertEquals(3, graph.getTotalVertices());

        // 2. Empty Graph
        // Create an empty graph with 0 nodes and 0 edges
        Path tempFileEmptyGraph = createTempDIMACSFile(0, new int[][]{});
        graph.loadDIMACS(tempFileEmptyGraph.toString());

        // The graph should contain exactly no nodes
        assertEquals(0, graph.getTotalVertices());
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
    void testGetNumberOfUsedColors() throws IOException {
        // 1. Regular Graph
        // Create a temporary graph with 3 nodes and 2 edges: 1-2, 2-3
        Path tempFileGraph = createTempDIMACSFile(3, new int[][]{{1, 2}, {2, 3}});
        graph.loadDIMACS(tempFileGraph.toString());

        // Sanity check
        assertEquals(2, graph.getNumberOfEdges());
        assertEquals(3, graph.getNumberOfNodes());

        // Color nodes
        graph.colorNode(0,0);
        graph.colorNode(1,1);
        graph.colorNode(2,2);

        // The graph should contain 3 unique colors.
        assertEquals(3,  graph.getNumberOfUsedColors());

        // Recolor nodes with duplicate colors
        graph.colorNode(0,1);

        // The graph should contain 2 unique colors.
        assertEquals(2, graph.getNumberOfUsedColors());

        // 2. Empty Graph
        // Create an empty graph with 0 nodes and 0 edges
        Path tempFileEmptyGraph = createTempDIMACSFile(0, new int[][]{});
        graph.loadDIMACS(tempFileEmptyGraph.toString());

        // Sanity check
        assertEquals(0, graph.getNumberOfEdges());

        // Graph should contain nu unique colors.
        assertEquals(0, graph.getNumberOfUsedColors());
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

        // 1.6 Neighbours of non-existent node
        assertThrows(IllegalArgumentException.class, () -> graph.getNeighborsOf(5));

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
        assertThrows(IllegalArgumentException.class, () -> graph.areNeighbors(4, 1));
        assertThrows(IllegalArgumentException.class, () -> graph.areNeighbors(4, 5));
        assertThrows(IllegalArgumentException.class, () -> graph.areNeighbors(5, 5));

        // 2. Empty Graph
        Path tempFileEmptyGraph = createTempDIMACSFile(0, new int[][]{});
        graph.loadDIMACS(tempFileEmptyGraph.toString());

        // 2.1 Non-existent nodes
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
    }

    @Test
    void testResetColors() throws IOException {
        // 1. Regular Graph
        Path tempFileGraph = createTempDIMACSFile(4, new int[][]{{1, 2}, {2, 3}});
        graph.loadDIMACS(tempFileGraph.toString());

        graph.colorNode(0, 5);
        graph.colorNode(2, 2);

        assertEquals(5, graph.getColor(0));
        assertEquals(-1, graph.getColor(1));
        assertEquals(2, graph.getColor(2));
        assertEquals(-1, graph.getColor(3));

        graph.resetColors();

        assertEquals(-1, graph.getColor(0));
        assertEquals(-1, graph.getColor(1));
        assertEquals(-1, graph.getColor(2));
        assertEquals(-1, graph.getColor(3));

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
        assertThrows(IllegalArgumentException.class, () -> graph.removeNode(0));
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
        assertThrows(IllegalArgumentException.class, () -> graph.removeEdge(999, 888));
        assertEquals(4, graph.getNumberOfNodes());
        assertTrue(graph.getNodes().contains(0));
        assertTrue(graph.getNodes().contains(1));
        assertTrue(graph.getNodes().contains(3));
    }

    @Test
    void testGetMaxClique() throws IOException {
        // 1. Simple triangle (3-clique)
        // Graph: 1-2, 2-3, 1-3
        Path triangle = createTempDIMACSFile(3, new int[][]{
                {1, 2}, {2, 3}, {1, 3}
        });
        graph.loadDIMACS(triangle.toString());
        assertEquals(3, graph.getMaxClique());

        // 2. Line of 3 nodes (no triangle, max clique = 2)
        // Graph: 1-2, 2-3
        Path line = createTempDIMACSFile(3, new int[][]{
                {1, 2}, {2, 3}
        });
        graph.loadDIMACS(line.toString());
        assertEquals(2, graph.getMaxClique());

        // 3. Square with a diagonal (max clique = 3)
        // Graph: 1-2, 2-3, 3-4, 4-1, 1-3
        Path squareDiag = createTempDIMACSFile(4, new int[][]{
                {1, 2}, {2, 3}, {3, 4}, {4, 1}, {1, 3}
        });
        graph.loadDIMACS(squareDiag.toString());
        assertEquals(3, graph.getMaxClique());

        // 4. Disconnected nodes (max clique = 1)
        // Graph: 1, 2, 3 (no edges)
        Path disconnected = createTempDIMACSFile(3, new int[][]{});
        graph.loadDIMACS(disconnected.toString());
        assertEquals(1, graph.getMaxClique());

        // 5. Complete graph of 5 nodes (max clique = 5)
        int[][] edgesComplete5 = new int[10][2];
        int index = 0;
        for (int i = 1; i <= 5; i++) {
            for (int j = i + 1; j <= 5; j++) {
                edgesComplete5[index++] = new int[]{i, j};
            }
        }
        Path complete5 = createTempDIMACSFile(5, edgesComplete5);
        graph.loadDIMACS(complete5.toString());
        assertEquals(5, graph.getMaxClique());

        // 6. Empty graph
        Path tempFileEmptyGraph = createTempDIMACSFile(0, new int[][]{});
        graph.loadDIMACS(tempFileEmptyGraph.toString());
        assertEquals(0, graph.getMaxClique());
    }

    @Test
    void testApplyReduction() throws IOException {
        // 1. Reduction Scenario: Clique of 4 with a Tail
        // Graph Structure:
        // - Clique (Nodes 1, 2, 3, 4) -> Max Clique size = 4
        // - Tail connected to Node 4: 4-5, 5-6
        //
        // Degrees:
        // Node 1: 3 (neighbors 2,3,4)      -> 3 < 4 -> REMOVE
        // Node 2: 3 (neighbors 1,3,4)      -> 3 < 4 -> REMOVE
        // Node 3: 3 (neighbors 1,2,4)      -> 3 < 4 -> REMOVE
        // Node 4: 4 (neighbors 1,2,3,5)    -> 4 == 4 -> KEEP
        // Node 5: 2 (neighbors 4,6)        -> 2 < 4 -> REMOVE
        // Node 6: 1 (neighbors 5)          -> 1 < 4 -> REMOVE

        int[][] edges = {
                // Clique 1-2-3-4
                {1, 2}, {1, 3}, {1, 4},
                {2, 3}, {2, 4},
                {3, 4},
                // Tail 4-5-6
                {4, 5}, {5, 6}
        };

        Path file = createTempDIMACSFile(6, edges);
        graph.loadDIMACS(file.toString());

        // Pre-check: Ensure setup is correct
        assertEquals(6, graph.getNumberOfNodes());
        assertEquals(4, graph.getMaxClique()); // Threshold should be 4

        // Apply Reduction
        graph.applyReduction();

        // 1.1 Verify correct nodes were removed
        assertEquals(1, graph.getNumberOfNodes());
        assertTrue(graph.getNodes().contains(3));

        // Verify others are gone
        assertFalse(graph.getNodes().contains(0));
        assertFalse(graph.getNodes().contains(1));
        assertFalse(graph.getNodes().contains(2));
        assertFalse(graph.getNodes().contains(4));
        assertFalse(graph.getNodes().contains(5));

        // 1.2 Verify edges were cleaned up (Node 3 is now isolated in the reduced graph)
        assertEquals(0, graph.getNumberOfEdges());
        assertEquals(0, graph.getDegree(3));


        // 2. Stability Scenario: No nodes should be removed
        // Graph: A simple square (Cycle of 4)
        // 1-2, 2-3, 3-4, 4-1
        // Max Clique = 2 (any edge)
        // All degrees = 2
        // Condition: degree < 2? False. Keep all.
        Path square = createTempDIMACSFile(4, new int[][]{
                {1, 2}, {2, 3}, {3, 4}, {4, 1}
        });
        graph.loadDIMACS(square.toString());

        // Pre-check
        assertEquals(2, graph.getMaxClique());
        assertEquals(4, graph.getNumberOfNodes());

        // Apply Reduction
        graph.applyReduction();

        // 2.1 Verify nothing changed
        assertEquals(4, graph.getNumberOfNodes());
        assertEquals(4, graph.getNumberOfEdges());
        assertTrue(graph.getNodes().contains(0));
        assertTrue(graph.getNodes().contains(1));
        assertTrue(graph.getNodes().contains(2));
        assertTrue(graph.getNodes().contains(3));


        // 3. Empty Graph Scenario
        Path empty = createTempDIMACSFile(0, new int[][]{});
        graph.loadDIMACS(empty.toString());

        // Should not throw exception
        assertDoesNotThrow(() -> graph.applyReduction());
        assertEquals(0, graph.getNumberOfNodes());
    }
}
