import java.io.File;
import java.util.*;

public class ParameterTuner {

    private static final String GRAPH_DIR = "DIMACSGraphs/";
    private static final String EXTENSION = ".col";
    private static final long TIME_LIMIT_MS = 30000;

    public static void main(String[] args) {
        // 1. Define Parameter Profiles
        List<ParameterProfile> profiles = new ArrayList<>();
        profiles.add(new ParameterProfile("Balanced", 10, 0.6, 1500, 100));
        profiles.add(new ParameterProfile("Aggressive", 5, 0.4, 500, 200));
        profiles.add(new ParameterProfile("Patient", 20, 0.8, 4000, 50));
        profiles.add(new ParameterProfile("HighNoise", 12, 0.6, 800, 300));

        // 2. Sort graphs alphabetically
        List<String> graphNames = new ArrayList<>(ILSResultsPaper.ilsResults.keySet());
        Collections.sort(graphNames); // Ensure alphabetical order

        System.out.println("Starting Parameter Tuning on " + graphNames.size() + " graphs...");
        System.out.println("Time Limit per run: " + (TIME_LIMIT_MS / 1000) + "s\n");

        for (String graphName : graphNames) {
            ILSResultsPaper.GraphResult paperData = ILSResultsPaper.getResult(graphName);
            if (paperData == null) continue;

            File f = new File(GRAPH_DIR + graphName + EXTENSION);
            if (!f.exists()) continue;

            List<RunResult> graphResults = new ArrayList<>();

            // --- HEADER ---
            System.out.println("=================================================================================================================");
            // Display Paper Time in header for reference
            System.out.printf(" GRAPH: %-20s | Paper Min: %-3d | Paper Med: %-3d | Paper Time: %6.2fs%n",
                    graphName, paperData.getMin(), paperData.getMed(), paperData.getTimeSec());

            System.out.println("-----------------------------------------------------------------------------------------------------------------");
            // Added 'ILS Time' and 'vs Paper' columns
            System.out.printf("| %-12s | %-4s | %-9s | %-9s | %-12s | %-12s | %-15s |%n",
                    "Profile", "K", "Total(ms)", "ILS(ms)", "Beats Paper", "vs Paper", "Parameters");
            System.out.println("-----------------------------------------------------------------------------------------------------------------");

            int totalNodes = 0;
            int totalEdges = 0;

            // Run all profiles
            for (ParameterProfile profile : profiles) {
                RunResult result = runExperiment(graphName, f.getPath(), profile);

                if (result != null) {
                    graphResults.add(result);
                    if (totalNodes == 0) { totalNodes = result.nodes; totalEdges = result.edges; }

                    // --- COMPARISON LOGIC ---

                    // 1. Quality Check
                    String beatStr = (result.k <= paperData.getMin()) ? "YES" : "NO";
                    if (result.k < paperData.getMin()) beatStr = "NEW REC";

                    // 2. Time Comparison (Total Time vs Paper Time)
                    double paperTimeMs = paperData.getTimeSec() * 1000.0;
                    String speedComp;

                    // Avoid division by zero if paper lists 0.0s
                    if (paperTimeMs < 1) paperTimeMs = 1;

                    if (result.totalTime <= paperTimeMs) {
                        double speedup = paperTimeMs / (double) result.totalTime;
                        speedComp = String.format("%.1fx Fast", speedup);
                    } else {
                        double slowdown = (double) result.totalTime / paperTimeMs;
                        speedComp = String.format("%.1fx Slow", slowdown);
                    }

                    // Print Row
                    System.out.printf("| %-12s | %-4d | %-9d | %-9d | %-12s | %-12s | %-15s |%n",
                            profile.name,
                            result.k,
                            result.totalTime,
                            result.ilsTime,
                            beatStr,
                            speedComp,
                            String.format("T:%d M:%.1f", profile.tabuTenureBase, profile.tabuTenureMulti)
                    );
                }
            }
            System.out.println("-----------------------------------------------------------------------------------------------------------------");

            // FINAL SUMMARY
            if (!graphResults.isEmpty()) {
                // Sort by K (Quality), then by Total Time (Efficiency)
                graphResults.sort(Comparator.comparingInt((RunResult r) -> r.k)
                        .thenComparingLong(r -> r.totalTime));

                RunResult winner = graphResults.get(0);

                System.out.println(" SUMMARY FOR " + graphName);
                System.out.printf("  > Nodes: %d  Edges: %d%n", totalNodes, totalEdges);

                // Calculate winner comparison
                double paperTimeMs = paperData.getTimeSec() * 1000.0;
                String speedStatus = (winner.totalTime <= paperTimeMs) ? "FASTER" : "SLOWER";
                double factor = (winner.totalTime <= paperTimeMs)
                        ? (paperTimeMs / winner.totalTime)
                        : (winner.totalTime / paperTimeMs);

                System.out.printf("  > WINNER: %s (k=%d)%n", winner.profile.name, winner.k);
                System.out.printf("  > TIMING: %.2fs (ILS: %.2fs) | %s than paper (%.2fx)%n",
                        winner.totalTime / 1000.0,
                        winner.ilsTime / 1000.0,
                        speedStatus,
                        factor);

                if (winner.k <= paperData.getMin()) {
                    System.out.println("  > STATUS: OPTIMAL (Matches/Beats paper)");
                } else {
                    System.out.println("  > STATUS: GAP of " + (winner.k - paperData.getMin()) + " colors.");
                }
            }
            System.out.println("\n");
        }
    }

    private static RunResult runExperiment(String graphName, String filePath, ParameterProfile profile) {
        try {
            Graph g = new Graph();
            g.loadDIMACS(filePath);

            int nodes = g.getTotalNodes();
            int edges = g.getNumberOfEdges();

            // 1. Start Total Timer
            long startTotal = System.currentTimeMillis();

            g.applyConstructionHeuristic();
            g.applyReduction();

            // 2. Start ILS Timer (after deterministic phases)
            long startILS = System.currentTimeMillis();

            IteratedLocalSearch ils = new IteratedLocalSearch(
                    g,
                    TIME_LIMIT_MS,
                    profile.tabuTenureBase,
                    profile.tabuTenureMulti,
                    profile.maxIterationsWithoutImprovement,
                    profile.maxPerturbationsPerK
            );

            ils.solve();

            long endObj = System.currentTimeMillis();

            // 3. Calculate Durations
            long ilsDuration = endObj - startILS;
            long totalDuration = endObj - startTotal;

            int k = g.getColorCount();

            g = null;
            ils = null;

            return new RunResult(profile, k, totalDuration, ilsDuration, nodes, edges);

        } catch (Exception e) {
            System.err.println("Error running " + graphName + " [" + profile.name + "]: " + e.getMessage());
            return null;
        }
    }

    // --- Helper Classes ---

    static class ParameterProfile {
        String name;
        int tabuTenureBase;
        double tabuTenureMulti;
        int maxIterationsWithoutImprovement;
        int maxPerturbationsPerK;

        public ParameterProfile(String name, int base, double multi, int maxIter, int maxPerturb) {
            this.name = name;
            this.tabuTenureBase = base;
            this.tabuTenureMulti = multi;
            this.maxIterationsWithoutImprovement = maxIter;
            this.maxPerturbationsPerK = maxPerturb;
        }
    }

    static class RunResult {
        ParameterProfile profile;
        int k;
        long totalTime; // Construction + Reduction + ILS
        long ilsTime;   // Only ILS
        int nodes;
        int edges;

        public RunResult(ParameterProfile p, int k, long totalTime, long ilsTime, int nodes, int edges) {
            this.profile = p;
            this.k = k;
            this.totalTime = totalTime;
            this.ilsTime = ilsTime;
            this.nodes = nodes;
            this.edges = edges;
        }
    }
}