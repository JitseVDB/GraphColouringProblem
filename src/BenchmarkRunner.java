import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class BenchmarkRunner {

    private static final String GRAPH_DIR = "DIMACSGraphs/";

    // All graph filenames in your benchmark set
    private static final String[] BENCHMARK_GRAPHS = {
            "DSJC125.1.col", "DSJC250.1.col", "DSJC500.1.col", "DSJC1000.1.col",
            "DSJC125.5.col", "DSJC250.5.col", "DSJC500.5.col", "DSJC1000.5.col",
            "DSJC125.9.col", "DSJC250.9.col", "DSJC500.9.col", "DSJC1000.9.col",
            "flat300_20_0.col", "flat300_26_0.col", "flat300_28_0.col",
            "flat1000_50_0.col", "flat1000_60_0.col", "flat1000_76_0.col",
            "le450_5a.col", "le450_5b.col", "le450_5d.col",
            "le450_15a.col", "le450_15b.col", "le450_15c.col", "le450_15d.col",
            "le450_25c.col", "le450_25d.col",
            "latin_square_10.col",
            "qg_order100.col",
            "queen6_6.col", "queen7_7.col", "queen8_8.col", "queen8_12.col",
            "queen9_9.col", "queen10_10.col", "queen11_11.col", "queen12_12.col",
            "queen13_13.col", "queen14_14.col", "queen15_15.col", "queen16_16.col",
            "wap01a.col", "wap02a.col", "wap03a.col", "wap04a.col",
            "wap06a.col", "wap07a.col", "wap08a.col",
            "abb313GPIA.col", "ash331GPIA.col", "ash608GPIA.col", "ash958GPIA.col", "will199GPIA.col"
    };

    public static void main(String[] args) {

        System.out.println("Running benchmark on " + BENCHMARK_GRAPHS.length + " graphs...\n");

        for (String filename : BENCHMARK_GRAPHS) {
            System.out.println("================================================");
            System.out.println("GRAPH: " + filename);

            File f = new File(GRAPH_DIR + filename);
            if (!f.exists()) {
                System.err.println("ERROR: File not found at " + f.getAbsolutePath());
                continue;
            }

            Graph g = new Graph();

            try {
                // Load graph
                g.loadDIMACS(GRAPH_DIR + filename);
                int originalNodes = g.getNumberOfNodes();
                int originalEdges = g.getNumberOfEdges();
                System.out.println("Original Graph -> Nodes: " + originalNodes + ", Edges: " + originalEdges);

                // Apply reduction
                long startReduction = System.nanoTime();
                g.applyReduction();
                long durationReduction = System.nanoTime() - startReduction;

                int reducedNodes = g.getNumberOfNodes();
                int reducedEdges = g.getNumberOfEdges();
                int removedNodes = originalNodes - reducedNodes;

                System.out.println("Reduction -> Nodes: " + reducedNodes + ", Edges: " + reducedEdges
                        + ", Removed Nodes: " + removedNodes
                        + ", Time: " + (durationReduction / 1_000_000) + " ms");

                // Apply construction heuristic (e.g., RLF)
                long startConstruction = System.nanoTime();
                g.applyConstructionHeuristic();
                long durationConstruction = System.nanoTime() - startConstruction;

                // Check coloring
                boolean validColoring = g.isValidColoring();
                int colorsUsed = g.getColorCount();

                System.out.println("Construction -> Colors used: " + colorsUsed
                        + ", Valid Coloring: " + validColoring
                        + ", Time: " + (durationConstruction / 1_000_000) + " ms");

            } catch (IOException e) {
                System.err.println("Error loading graph: " + filename);
                e.printStackTrace();
            } catch (Exception e) {
                System.err.println("Unexpected error on graph: " + filename);
                e.printStackTrace();
            }

            System.out.println();
        }

        System.out.println("Benchmark complete.");
    }
}
