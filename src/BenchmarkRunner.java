import java.io.File;
import java.io.IOException;
import java.util.*;

public class BenchmarkRunner {

    private static final String GRAPH_DIR = "DIMACSGraphs/";

    private static final String EXTENSION = ".col";

    private static final String[] BENCHMARK_GRAPHS_BASE = {
            "DSJC125.1", "DSJC250.1", "DSJC500.1", "DSJC1000.1",
            "DSJC125.5", "DSJC250.5", "DSJC500.5", "DSJC1000.5",
            "DSJC125.9", "DSJC250.9", "DSJC500.9", "DSJC1000.9",
            "flat300_20_0", "flat300_26_0", "flat300_28_0",
            "flat1000_50_0", "flat1000_60_0", "flat1000_76_0",
            "le450_5a", "le450_5b", "le450_5d",
            "le450_15a", "le450_15b", "le450_15c", "le450_15d",
            "le450_25c", "le450_25d",
            "latin_square_10",
            "qg_order100",
            "queen6_6", "queen7_7", "queen8_8", "queen8_12",
            "queen9_9", "queen10_10", "queen11_11", "queen12_12",
            "queen13_13", "queen14_14", "queen15_15", "queen16_16",
            "wap01a", "wap02a", "wap03a", "wap04a",
            "wap06a", "wap07a", "wap08a",
            "abb313GPIA", "ash331GPIA", "ash608GPIA", "ash958GPIA", "will199GPIA"
    };

    public static void main(String[] args) {

        System.out.println("Running benchmark on " + BENCHMARK_GRAPHS_BASE.length + " graphs...\n");

        for (String baseName : BENCHMARK_GRAPHS_BASE) {
            System.out.println("================================================");
            System.out.println("GRAPH: " + baseName);

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

                // Apply construction heuristic (RLF)
                long startConstruction = System.nanoTime();
                g.applyConstructionHeuristic();
                long durationConstruction = System.nanoTime() - startConstruction;

                // Check coloring
                boolean validColoring = g.isValidColoring();
                int colorsUsed = g.getColorCount();

                System.out.println("Construction -> Colors used: " + colorsUsed
                        + ", Valid Coloring: " + validColoring
                        + ", Time: " + (durationConstruction / 1_000_000) + " ms");

                // Apply Stochastic local search algorithm (ILS)
                long startStochasticSearch  = System.nanoTime();
                g.applyStochasticLocalSearchAlgorithm();
                long durationStochasticSearch = System.nanoTime() - startStochasticSearch;

                // Check coloring
                validColoring = g.isValidColoring();
                colorsUsed = g.getColorCount();

                System.out.println("Stochastic Search -> Colors used: " + colorsUsed
                        + ", Valid Coloring: " + validColoring
                        + ", Time: " + (durationStochasticSearch / 1_000_000) + " ms");

                ILSResults.GraphResult paperResult = ILSResults.getResult(baseName);

                int medPaperColors = paperResult.getMed();
                int minPaperColors = paperResult.getMin();
                double paperTimeSec = paperResult.getTimeSec();

                double speedRatio = durationStochasticSearch / (paperTimeSec > 0 ? paperTimeSec : 1); // avoid division by zero
                String speedComparison;
                if (speedRatio < 1) {
                    speedComparison = String.format("faster by %.2fx", 1.0 / speedRatio);
                } else if (speedRatio > 1) {
                    speedComparison = String.format("slower by %.2fx", speedRatio);
                } else {
                    speedComparison = "same speed";
                }


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
