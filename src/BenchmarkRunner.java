import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BenchmarkRunner {

    // --- Configuration ---
    private static final String GRAPH_DIR = "DIMACSGraphs/";
    private static final String RESULTS_DIR = "BenchmarkResults/";
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
            "qg.order30", "qg.order40", "qg.order60", //"qg.order100",
            "queen5_5", "queen6_6", "queen7_7", "queen8_8", "queen8_12",
            "queen9_9", "queen10_10", "queen11_11", "queen12_12",
            "queen13_13", "queen14_14", "queen15_15", "queen16_16",
            "wap01a", "wap02a", "wap03a", "wap04a",
            "wap05a", "wap06a", "wap07a", "wap08a",
            "abb313GPIA", "ash331GPIA", "ash608GPIA", "ash958GPIA", "will199GPIA",
    };

    public static void main(String[] args) {

        // 1. Setup Directory
        File resultsDir = new File(RESULTS_DIR);
        if (!resultsDir.exists()) {
            boolean created = resultsDir.mkdirs();
            if (!created) {
                System.err.println("ERROR: Could not create directory " + RESULTS_DIR);
                return;
            }
        }

        // 2. Determine Filename
        // CHANGE: Check if a filename was passed in args (from BatchRunner)
        String csvFileName;
        if (args.length > 0 && args[0] != null && !args[0].isEmpty()) {
            csvFileName = args[0];
        } else {
            // Default behavior if run manually
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
            String timestamp = LocalDateTime.now().format(dtf);
            csvFileName = "benchmark_results_" + timestamp + ".csv";
        }

        File csvFile = new File(resultsDir, csvFileName);

        System.out.println("Running benchmark on " + BENCHMARK_GRAPHS_BASE.length + " graphs...");
        System.out.println("Writing results to: " + csvFile.getAbsolutePath() + "\n");

        // 3. Open Writer and Run Benchmark
        try (PrintWriter csvWriter = new PrintWriter(new FileWriter(csvFile))) {

            // Write CSV Header
            csvWriter.println("Graph_Name,Original_Nodes,Original_Edges," +
                    "Constr_Colors,Constr_Valid,Constr_Time_ms," +
                    "Reduc_Nodes,Reduc_Edges,Reduc_Time_ms," +
                    "ILS_Colors,ILS_Valid,ILS_Time_ms," +
                    "Paper_Min,Paper_Med,Paper_Time_s,Comparison_Note,Speed_Ratio");

            for (String baseName : BENCHMARK_GRAPHS_BASE) {
                System.out.println("================================================");
                System.out.println("GRAPH: " + baseName);
                System.out.println("------------------------------------------------");

                String filename = baseName + EXTENSION;
                File f = new File(GRAPH_DIR + filename);

                if (!f.exists()) {
                    System.err.println("ERROR: File not found at " + f.getAbsolutePath());
                    csvWriter.println(baseName + ",FILE_NOT_FOUND,,,,,,,,,,,,,,,");
                    continue;
                }

                Graph g = new Graph();

                try {
                    // --- Load graph ---
                    g.loadDIMACS(GRAPH_DIR + filename);
                    int originalNodes = g.getNumberOfNodes();
                    int originalEdges = g.getNumberOfEdges();

                    System.out.println("\n[ ORIGINAL GRAPH ]");
                    System.out.printf("  Nodes: %-6d Edges: %-6d%n", originalNodes, originalEdges);

                    // --- Construction Heuristic ---
                    long startConstruction = System.nanoTime();
                    g.applyConstructionHeuristic();
                    long durationConstruction = System.nanoTime() - startConstruction;
                    long constTimeMs = durationConstruction / 1_000_000;

                    boolean constValid = g.isValidColoring();
                    int constColors = g.getColorCount();

                    System.out.println("\n[ CONSTRUCTION HEURISTIC ]");
                    System.out.printf("  Colors Used: %-6d Valid: %-6s Time: %d ms%n",
                            constColors, constValid, constTimeMs);

                    // --- Reduction ---
                    long startReduction = System.nanoTime();
                    g.applyReduction();
                    long durationReduction = System.nanoTime() - startReduction;
                    long reducTimeMs = durationReduction / 1_000_000;

                    int reducedNodes = g.getNumberOfNodes();
                    int reducedEdges = g.getNumberOfEdges();
                    int removedNodes = originalNodes - reducedNodes;

                    System.out.println("\n[ REDUCTION ]");
                    System.out.printf("  Nodes: %-6d Edges: %-6d Removed: %-6d Time: %d ms%n",
                            reducedNodes, reducedEdges, removedNodes, reducTimeMs);

                    // --- ILS Stochastic Search ---
                    long startStochasticSearch = System.nanoTime();
                    g.applyStochasticLocalSearchAlgorithm();
                    long durationStochasticSearch = System.nanoTime() - startStochasticSearch;
                    long ilsTimeMs = durationStochasticSearch / 1_000_000;

                    boolean ilsValid = g.isValidColoring();
                    int ilsColors = g.getColorCount();

                    System.out.println("\n[ STOCHASTIC LOCAL SEARCH ]");
                    System.out.printf("  Colors Used: %-6d Valid: %-6s Time: %d ms%n",
                            ilsColors, ilsValid, ilsTimeMs);

                    // --- Paper comparison ---
                    System.out.println("\n[ PAPER REFERENCE RESULTS ]");

                    String paperMinStr = "N/A";
                    String paperMedStr = "N/A";
                    String paperTimeStr = "N/A";
                    String comparisonNote = "N/A";
                    String speedRatioStr = "N/A";

                    try {
                        ILSResultsPaper.GraphResult paperResult = ILSResultsPaper.getResult(baseName);
                        int medPaperColors = paperResult.getMed();
                        int minPaperColors = paperResult.getMin();
                        double paperTimeSec = paperResult.getTimeSec();

                        paperMinStr = String.valueOf(minPaperColors);
                        paperMedStr = String.valueOf(medPaperColors);
                        paperTimeStr = String.format("%.2f", paperTimeSec);

                        System.out.printf("  Min Colors: %-6d Median Colors: %-6d Time: %.2f s%n",
                                minPaperColors, medPaperColors, paperTimeSec);

                        System.out.print("  Comparison:   ");

                        if (ilsColors < minPaperColors) {
                            comparisonNote = "Better than paper min";
                            System.out.println(comparisonNote + "!");
                        } else if (ilsColors == minPaperColors) {
                            comparisonNote = "Matches paper min";
                            System.out.println(comparisonNote + ".");
                        } else if (ilsColors <= medPaperColors) {
                            comparisonNote = "Between min and median";
                            System.out.println(comparisonNote + ".");
                        } else {
                            comparisonNote = "Worse than paper median";
                            System.out.println(comparisonNote + ".");
                        }

                        double totalTimeSec = (durationConstruction + durationReduction + durationStochasticSearch) / 1_000_000_000.0;
                        double speedRatio = totalTimeSec / paperTimeSec;

                        if (speedRatio < 1) {
                            speedRatioStr = String.format("Faster (%.2fx)", 1.0 / speedRatio);
                            System.out.printf("  Speed:        %s%n", speedRatioStr);
                        } else {
                            speedRatioStr = String.format("Slower (%.2fx)", speedRatio);
                            System.out.printf("  Speed:        %s%n", speedRatioStr);
                        }

                    } catch (Exception e) {
                        System.out.println("  No paper results available for this graph.");
                    }

                    // Write the CSV Row
                    csvWriter.printf("%s,%d,%d,%d,%b,%d,%d,%d,%d,%d,%b,%d,%s,%s,%s,%s,%s%n",
                            baseName,
                            originalNodes, originalEdges,
                            constColors, constValid, constTimeMs,
                            reducedNodes, reducedEdges, reducTimeMs,
                            ilsColors, ilsValid, ilsTimeMs,
                            paperMinStr, paperMedStr, paperTimeStr, comparisonNote, speedRatioStr
                    );

                    csvWriter.flush();
                    System.out.println();

                } catch (Exception e) {
                    System.err.println("Unexpected error on graph: " + filename);
                    e.printStackTrace();
                    csvWriter.println(baseName + ",ERROR,,,,,,,,,,,,,,,");
                }
                System.out.println("================================================\n");
            }
            System.out.println("Benchmark complete for file: " + csvFileName);
        } catch (IOException e) {
            System.err.println("CRITICAL ERROR: Could not write to CSV file.");
            e.printStackTrace();
        }
    }
}