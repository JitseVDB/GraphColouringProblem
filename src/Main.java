import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 * Entry point for testing graph reduction, visualization, and construction
 * heuristics on a set of DIMACS graph files.
 *
 * This class iterates over a predefined list of graph files located in the
 * `DIMACSGraphs/` directory. For each graph, it performs the following steps:
 * - Loads the graph from a DIMACS-format file.
 * - Displays the original graph using `GraphVisualizer`.
 * - Applies a reduction procedure that removes nodes with degree below a
 *   certain threshold.
 * - Displays the reduced graph.
 * - Applies a construction heuristic to generate a feasible coloring.
 * - Displays the constructed coloring.
 *
 * The user is prompted to press ENTER before moving to the next step to allow
 * interactive visualization of each stage.
 *
 * @author Jitse
 *
 * @version 1.0
 */
public class Main {

    /**
     * The directory where the DIMACS graph files are stored.
     */
    private static final String GRAPH_DIR = "DIMACSGraphs/";

    /**
     * Main entry point for processing a set of graph files.
     *
     * @param   args
     *          Command-line arguments (not used in this implementation).
     *
     * @post    Each graph in the predefined `testFiles` array is processed sequentially.
     *          The original graph, reduced graph, and constructed coloring are displayed
     *          via `GraphVisualizer`. Reduction and construction procedures are applied
     *          in-place to each graph.
     *          | for each file in testFiles :
     *          |     if file exists :
     *          |         Graph g = new Graph()
     *          |         g.loadDIMACS(file)
     *          |         GraphVisualizer.show(g, file + " [ORIGINAL]")
     *          |         g.applyReduction()
     *          |         GraphVisualizer.show(g, file + " [REDUCED]")
     *          |         g.applyConstructionHeuristic()
     *          |         GraphVisualizer.show(g, file + " [FEASIBLE COLORING]")
     *
     * @post    The program terminates cleanly after processing all files and
     *          closing the Scanner.
     *          | scanner.isClosed()
     *          | System.exit(0) is called
     */
    public static void main(String[] args) {
        String[] testFiles = {
                "DSJC125.1.col",
                "abb313GPIA.col",
                "ash331GPIA.col"
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

                System.out.println("Construction complete in " + (durationConstruction / 1000000) + "ms.");

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
