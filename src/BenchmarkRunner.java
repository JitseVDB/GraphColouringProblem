import java.io.File;
import java.io.IOException;
import java.util.*;

public class BenchmarkRunner {

    private static final String GRAPH_DIR = "DIMACSGraphs/";

    private static final String EXTENSION = ".col";

    private static final String[] BENCHMARK_GRAPHS_BASE = {
            "DSJC125.1", "DSJC250.1", "DSJC500.1", "DSJC1000.1", "DSJR500.1",
            "DSJC125.5", "DSJC250.5", "DSJC500.5", "DSJC1000.5", "DSJR500.1c",
            "DSJC125.9", "DSJC250.9", "DSJC500.9", "DSJC1000.9", "DSJR500.5",
            "flat300_20_0", "flat300_26_0", "flat300_28_0",
            "flat1000_50_0", "flat1000_60_0", "flat1000_76_0",
            "fpsol2.i.1", "fpsol2.i.2", "fpsol2.i.3",
            "inithx.i.1",  "inithx.i.2", "inithx.i.3",
            "le450_5a", "le450_5b", "le450_5c", "le450_5d",
            "le450_15a", "le450_15b", "le450_15c", "le450_15d",
            "le450_25a", "le450_25b", "le450_25c", "le450_25d",
            "mulsol.i.1", "mulsol.i.2", "mulsol.i.3", "mulsol.i.4", "mulsol.i.5",
            "latin_square_10",
            "qg.order30", "qg.order40", "qg.order60", "qg.order100",
            "queen5_5", "queen6_6", "queen7_7", "queen8_8", "queen8_12",
            "queen9_9", "queen10_10", "queen11_11", "queen12_12",
            "queen13_13", "queen14_14", "queen15_15", "queen16_16",
            "wap01a", "wap02a", "wap03a", "wap04a",
            "wap05a", "wap06a", "wap07a", "wap08a",
            "abb313GPIA", "ash331GPIA", "ash608GPIA", "ash958GPIA", "will199GPIA",
    };

    public static void main(String[] args) {

        System.out.println("Running benchmark on " + BENCHMARK_GRAPHS_BASE.length + " graphs...\n");

        for (String baseName : BENCHMARK_GRAPHS_BASE) {
            System.out.println("================================================");
            System.out.println("GRAPH: " + baseName);
            System.out.println("------------------------------------------------");

            String filename = baseName + EXTENSION;
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

                System.out.println("\n[ ORIGINAL GRAPH ]");
                System.out.printf("  Nodes: %-6d Edges: %-6d%n", originalNodes, originalEdges);

                // Construction Heuristic
                long startConstruction = System.nanoTime();
                g.applyConstructionHeuristic();
                long durationConstruction = System.nanoTime() - startConstruction;

                boolean validColoring = g.isValidColoring();
                int colorsUsed = g.getColorCount();

                System.out.println("\n[ CONSTRUCTION HEURISTIC ]");
                System.out.printf("  Colors Used: %-6d Valid: %-6s Time: %d ms%n",
                        colorsUsed, validColoring, durationConstruction / 1_000_000);

                // Reduction
                long startReduction = System.nanoTime();
                g.applyReduction();
                long durationReduction = System.nanoTime() - startReduction;

                int reducedNodes = g.getNumberOfNodes();
                int reducedEdges = g.getNumberOfEdges();
                int removedNodes = originalNodes - reducedNodes;

                System.out.println("\n[ REDUCTION ]");
                System.out.printf("  Nodes: %-6d Edges: %-6d Removed: %-6d Time: %d ms%n",
                        reducedNodes, reducedEdges, removedNodes, durationReduction / 1_000_000);

                // ILS Stochastic Search
                long startStochasticSearch = System.nanoTime();
                g.applyStochasticLocalSearchAlgorithm();
                long durationStochasticSearch = System.nanoTime() - startStochasticSearch;

                validColoring = g.isValidColoring();
                colorsUsed = g.getColorCount();

                System.out.println("\n[ STOCHASTIC LOCAL SEARCH ]");
                System.out.printf("  Colors Used: %-6d Valid: %-6s Time: %d ms%n",
                        colorsUsed, validColoring, durationStochasticSearch / 1_000_000);

                // Paper comparison
                System.out.println("\n[ PAPER REFERENCE RESULTS ]");

                ILSResultsPaper.GraphResult paperResult = ILSResultsPaper.getResult(baseName);

                try {
                    int medPaperColors = paperResult.getMed();
                    int minPaperColors = paperResult.getMin();
                    double paperTimeSec = paperResult.getTimeSec();

                    System.out.printf("  Min Colors: %-6d Median Colors: %-6d Time: %.2f s%n",
                            minPaperColors, medPaperColors, paperTimeSec);

                    // Compare results
                    System.out.print("  Comparison:   ");

                    if (colorsUsed < minPaperColors) {
                        System.out.println("Better than paper minimum!");
                    } else if (colorsUsed == minPaperColors) {
                        System.out.println("Matches paper minimum.");
                    } else if (colorsUsed <= medPaperColors) {
                        System.out.println("Between minimum and median.");
                    } else {
                        System.out.println("Worse than paper median.");
                    }

                    double speedRatio = (durationConstruction + durationReduction + durationStochasticSearch)
                            / (paperTimeSec * 1_000_000_000);

                    if (speedRatio < 1) {
                        System.out.printf("  Speed:        Faster (%.2fx)%n", 1.0 / speedRatio);
                    } else {
                        System.out.printf("  Speed:        Slower (%.2fx)%n", speedRatio);
                    }

                } catch (Exception e) {
                    System.out.println("  No paper results available for this graph.");
                }

                System.out.println();

            } catch (Exception e) {
                System.err.println("Unexpected error on graph: " + filename);
                e.printStackTrace();
            }

            System.out.println("================================================\n");
        }

        System.out.println("Benchmark complete.");
    }
}
