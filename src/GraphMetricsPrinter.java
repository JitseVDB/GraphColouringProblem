import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GraphMetricsPrinter {

    private static final String GRAPH_DIR = "DIMACSGraphs/";
    private static final String EXTENSION = ".col";

    public static void main(String[] args) {
        // 1. Get all graph names from your results mapping
        // We use the keyset from ILSResultsPaper so we only check the relevant graphs
        List<String> graphNames = new ArrayList<>(ILSResultsPaper.ilsResults.keySet());

        // Sort alphabetically for cleaner output
        Collections.sort(graphNames);

        System.out.println("=======================================================================");
        System.out.printf("| %-20s | %-8s | %-8s | %-8s |%n", "Graph Name", "Nodes", "Edges", "Density");
        System.out.println("|----------------------|----------|----------|----------|");

        int successCount = 0;
        int failCount = 0;

        for (String graphName : graphNames) {
            String filename = graphName + EXTENSION;
            File f = new File(GRAPH_DIR + filename);

            // Handle potential file naming mismatches (e.g. le450-15a vs le450_15a)
            if (!f.exists()) {
                // Try replacing hyphens with underscores
                String altFilename = graphName.replace('-', '_') + EXTENSION;
                f = new File(GRAPH_DIR + altFilename);

                if (!f.exists()) {
                    System.out.printf("| %-20s | %-28s |%n", graphName, "FILE NOT FOUND");
                    failCount++;
                    continue;
                }
            }

            try {
                Graph g = new Graph();
                g.loadDIMACS(f.getPath());

                int n = g.getTotalNodes();
                int e = g.getNumberOfEdges();

                // Density formula for undirected graph: 2*E / (V * (V-1))
                double density = 0.0;
                if (n > 1) {
                    density = (2.0 * e) / ((double) n * (n - 1));
                }

                System.out.printf("| %-20s | %-8d | %-8d | %-8.4f |%n",
                        graphName, n, e, density);

                successCount++;

            } catch (Exception ex) {
                System.out.printf("| %-20s | %-28s |%n", graphName, "ERROR LOADING");
                failCount++;
            }
        }

        System.out.println("=======================================================================");
        System.out.println("Done. Loaded: " + successCount + " | Missing/Failed: " + failCount);
    }
}