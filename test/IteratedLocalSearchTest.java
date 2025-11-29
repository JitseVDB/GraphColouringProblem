import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class IteratedLocalSearchTest {

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
    void testILSOnMediumGraph() throws IOException {
        // Graph: 8 nodes forming a cube (edges connect cube vertices)
        Path graphFile = createTempDIMACSFile(8, new int[][]{
                {1,2},{2,3},{3,4},{4,1}, // bottom square
                {5,6},{6,7},{7,8},{8,5}, // top square
                {1,5},{2,6},{3,7},{4,8}  // vertical edges
        });
        graph.loadDIMACS(graphFile.toString());

        // Assign bad coloring (all 8 colors)
        int color = 1;
        for (int v : graph.getNodes()) {
            graph.colorNode(v, color++);
        }

        // Run ILS
        graph.applyStochasticLocalSearchAlgorithm();

        // Verify valid coloring
        assertTrue(graph.isValidColoring(), "Coloring should be valid after ILS");

        // Cube can be 2-colored (bipartite)
        int usedColors = graph.getColorCount();
        System.out.println("ILS reduced to " + usedColors + " colors");
        assertTrue(usedColors <= 2, "ILS should reduce coloring to optimal or near-optimal number of colors");
    }

    @Test
    void testILSOnSmallGraph() throws IOException {
        // Graph: 6 nodes, connected like a hexagon with diagonals across
        Path graphFile = createTempDIMACSFile(6, new int[][]{
                {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {6, 1}, // hexagon edges
                {1, 4}, {2, 5}, {3, 6} // diagonals
        });
        graph.loadDIMACS(graphFile.toString());

        // Assign intentionally bad coloring (6 colors)
        int color = 1;
        for (int v : graph.getNodes()) {
            graph.colorNode(v, color++);
        }

        // Run ILS
        graph.applyStochasticLocalSearchAlgorithm();

        // Check coloring is valid
        assertTrue(graph.isValidColoring(), "Coloring should be valid after ILS");

        // Check number of colors used (should be <= 3 ideally for this graph)
        int usedColors = graph.getColorCount();
        System.out.println("ILS reduced to " + usedColors + " colors");
        assertTrue(usedColors <= 3, "ILS should reduce coloring to near-optimal number of colors");
    }

    @Test
    void testILSOnDisconnectedGraph() throws IOException {
        // Disconnected graph: 3 nodes in one triangle, 3 nodes in another triangle
        Path graphFile = createTempDIMACSFile(6, new int[][]{
                {1, 2}, {2, 3}, {3, 1}, // first triangle
                {4, 5}, {5, 6}, {6, 4}  // second triangle
        });
        graph.loadDIMACS(graphFile.toString());

        // Assign bad coloring (all 6 nodes different colors)
        int color = 1;
        for (int v : graph.getNodes()) {
            graph.colorNode(v, color++);
        }

        // Run ILS
        graph.applyStochasticLocalSearchAlgorithm();

        // Verify valid coloring
        assertTrue(graph.isValidColoring(), "Coloring should be valid after ILS");

        // Optimal coloring should be 3 colors (2 triangles)
        int usedColors = graph.getColorCount();
        System.out.println("ILS reduced to " + usedColors + " colors");
        assertTrue(usedColors <= 3, "ILS should reduce coloring to near-optimal number of colors");
    }
}
