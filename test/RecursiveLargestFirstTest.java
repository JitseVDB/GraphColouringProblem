import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class RecursiveLargestFirstTest {

    private Graph graph;

    // Helper to create temporary DIMACS file
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
    void testRLFOnSimplePathGraph() throws IOException {
        // Path graph: 1 - 2 - 3 (0-based: 0 - 1 - 2)
        Path pathGraph = createTempDIMACSFile(3, new int[][]{
                {1, 2}, {2, 3}
        });
        graph.loadDIMACS(pathGraph.toString());

        // Apply RLF algorithm
        graph.applyConstructionHeuristic();

        // Verify coloring is proper: no two neighbors share the same color
        for (int node : graph.getNodes()) {
            int color = graph.getColor(node);
            for (int neighbor : graph.getNeighborsOf(node)) {
                assertNotEquals(color, graph.getColor(neighbor),
                        "Neighbor nodes " + node + " and " + neighbor + " have the same color");
            }
        }

        // Path graph of length 3 should need only 2 colors
        assertEquals(2, graph.getColorCount());
    }

    @Test
    void testRLFOnTriangleGraph() throws IOException {
        // Complete graph K3: 1-2, 2-3, 1-3
        Path triangleGraph = createTempDIMACSFile(3, new int[][]{
                {1, 2}, {2, 3}, {1, 3}
        });
        graph.loadDIMACS(triangleGraph.toString());

        // Apply RLF algorithm
        graph.applyConstructionHeuristic();

        // Verify coloring is proper: no two neighbors share the same color
        for (int node : graph.getNodes()) {
            int color = graph.getColor(node);
            for (int neighbor : graph.getNeighborsOf(node)) {
                assertNotEquals(color, graph.getColor(neighbor),
                        "Neighbor nodes " + node + " and " + neighbor + " have the same color");
            }
        }

        // Triangle graph should use exactly 3 colors
        assertEquals(3, graph.getColorCount());
    }

    @Test
    void testRLFOnSquareGraph() throws IOException {
        // Square with 4 nodes: 1-2, 2-3, 3-4, 4-1
        Path squareGraph = createTempDIMACSFile(4, new int[][]{
                {1, 2}, {2, 3}, {3, 4}, {4, 1}
        });
        graph.loadDIMACS(squareGraph.toString());

        // Apply RLF algorithm
        graph.applyConstructionHeuristic();

        // Verify coloring is proper: no two neighbors share the same color
        for (int node : graph.getNodes()) {
            int color = graph.getColor(node);
            for (int neighbor : graph.getNeighborsOf(node)) {
                assertNotEquals(color, graph.getColor(neighbor),
                        "Neighbor nodes " + node + " and " + neighbor + " have the same color");
            }
        }

        // Square (cycle of length 4) should only need 2 colors
        assertEquals(2, graph.getColorCount());
    }

    @Test
    void testRLFOnEmptyGraph() throws IOException {
        Path emptyGraph = createTempDIMACSFile(0, new int[][]{});
        graph.loadDIMACS(emptyGraph.toString());

        assertDoesNotThrow(() -> graph.applyConstructionHeuristic());
        assertEquals(0, graph.getNumberOfNodes());
        assertEquals(0, graph.getColorCount());
    }

    @Test
    void testRLFOnDisconnectedGraph() throws IOException {
        // Disconnected graph: 1-2, 3-4
        Path disconnectedGraph = createTempDIMACSFile(4, new int[][]{
                {1, 2}, {3, 4}
        });
        graph.loadDIMACS(disconnectedGraph.toString());

        // Apply RLF algorithm
        graph.applyConstructionHeuristic();

        // Proper coloring
        for (int node : graph.getNodes()) {
            int color = graph.getColor(node);
            for (int neighbor : graph.getNeighborsOf(node)) {
                assertNotEquals(color, graph.getColor(neighbor),
                        "Neighbor nodes " + node + " and " + neighbor + " have the same color");
            }
        }

        // The disconnected part should only contain 2 nodes
        assertEquals(2, graph.getColorCount());
    }
}
