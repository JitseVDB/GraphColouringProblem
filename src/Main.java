import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class Main {

    private static final String GRAPH_DIR = "DIMACSGraphs/";

    public static void main(String[] args) {
        String[] testFiles = {
                "abb313GPIA.col",
                "ash331GPIA.col",
                "DSJC125.1.col"
        };

        Scanner scanner = new Scanner(System.in);

        for (String file : testFiles) {
            System.out.println("================================================");
            System.out.println("PROCESSING: " + file);

            File f = new File(GRAPH_DIR + file);
            if (!f.exists()) {
                System.err.println("ERROR: File not found at " + f.getAbsolutePath());
                continue;
            }

            Graph g = new Graph();

            try {
                // 1. Load Graph
                g.loadDIMACS(GRAPH_DIR + file);
                int originalNodeCount = g.getNumberOfNodes();
                int originalEdgeCount = g.getNumberOfEdges();

                // STEP 1: SHOW ORIGINAL GRAPH
                System.out.println("Original Graph -> Nodes: " + originalNodeCount + ", Edges: " + originalEdgeCount);

                // Color a few nodes just to see orientation better
                if (originalNodeCount > 0) g.colorNode(1, 0);

                GraphVisualizer.show(g, file + " [ORIGINAL]");

                System.out.println(">> Original graph displayed.");
                System.out.println(">> Press ENTER to apply reduction...");
                scanner.nextLine();

                // STEP 2: APPLY REDUCTION
                System.out.println("Applying reduction (removing nodes with degree < MaxClique)...");
                long startTimerReduction = System.nanoTime();
                g.applyReduction();
                long durationReduction = System.nanoTime() - startTimerReduction;

                int newNodeCountReduction = g.getNumberOfNodes();
                int newEdgeCountReduction = g.getNumberOfEdges();

                System.out.println("Reduction complete in " + (durationReduction / 1000000) + "ms.");
                System.out.println("Removed " + (originalNodeCount - newNodeCountReduction) + " nodes.");
                System.out.println("Reduced Graph  -> Nodes: " + newNodeCountReduction + ", Edges: " + newEdgeCountReduction);

                // STEP 3: SHOW REDUCED GRAPH
                GraphVisualizer.show(g, file + " [REDUCED]");

                System.out.println(">> Reduced graph displayed.");
                System.out.println(">> Press ENTER to load the next file...");
                scanner.nextLine();

                // STEP 4: APPLY CONSTRUCTION
                System.out.println("Applying construction to generate a feasible coloring...");
                long startTimeConstruction = System.nanoTime();
                g.applyConstructionHeuristic();
                long durationConstruction = System.nanoTime() - startTimeConstruction;

                System.out.println("Construction complete in " + (durationReduction / 1000000) + "ms.");

                // STEP 5: SHOW CONSTRUCTED COLORING
                GraphVisualizer.show(g, file + " [FEASIBLE COLORING]");

                System.out.println(">> Constructed graph coloring displayed.");
                System.out.println(">> Press ENTER to load the next file...");
                scanner.nextLine();


            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Done.");
        scanner.close();
        System.exit(0);
    }
}