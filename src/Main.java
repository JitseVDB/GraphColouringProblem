import java.io.IOException;

public class Main {

    private static final String GRAPH_DIR = "DIMACSGraphs/";

    public static void main(String[] args) {

        String[] testFiles = {
                "abb313GPIA.col",
                "ash331GPIA.col",
                "ash608GPIA.col",
                "ash958GPIA.col",
                "DSJC125.1.col",
                "DSJC125.5.col",
                "DSJC125.9.col",
                "DSJC250.1.col",
                "DSJC250.5.col",
                "DSJC250.9.col",
                "DSJC500.1.col"
        };

        for (String file : testFiles) {
            System.out.println("========================================");
            System.out.println("Loading graph: " + file);

            Graph g = new Graph();

            try {
                g.loadDIMACS(GRAPH_DIR + file);
            } catch (IOException e) {
                System.out.println("ERROR: Cannot open " + GRAPH_DIR + file);
                continue;
            }

            System.out.println("Loaded successfully.");
            System.out.println("Nodes: " + g.getNumberOfNodes());
            System.out.println("Edges: " + g.getNumberOfEdges());

            // Additional tests here ...
        }

        System.out.println("All tests complete.");
    }
}
