import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class DeepParameterTuner {

    private static final String GRAPH_DIR = "DIMACSGraphs/";
    private static final String EXTENSION = ".col";

    // --- TUNING CONFIGURATION ---
    private static final int PHASE_1_RANDOM_SAMPLES = 20; // Exploration runs
    private static final int PHASE_2_MAX_CLIMB_STEPS = 10; // Max refinement steps

    // Adaptive Time Limits (Per single run)
    private static final long MIN_TIME_LIMIT_MS = 5000;    // Minimum 5s
    private static final long MAX_TIME_LIMIT_MS = 180000;  // Max 3 mins

    public static void main(String[] args) {
        List<String> graphNames = new ArrayList<>(ILSResultsPaper.ilsResults.keySet());
        Collections.sort(graphNames);

        System.out.println("=======================================================================================");
        System.out.println("   DEEP HYBRID TUNER: VERBOSE MODE");
        System.out.println("   Graphs: " + graphNames.size());
        System.out.println("=======================================================================================\n");

        for (String graphName : graphNames) {
            ILSResultsPaper.GraphResult paperData = ILSResultsPaper.getResult(graphName);
            if (paperData == null) continue;

            File f = new File(GRAPH_DIR + graphName + EXTENSION);
            if (!f.exists()) continue;

            // 1. Calculate Adaptive Time Limit
            double paperSec = paperData.getTimeSec();
            long adaptiveTimeLimit = (long) (paperSec * 1000.0 * 0.6); // 60% of paper time
            if (adaptiveTimeLimit < MIN_TIME_LIMIT_MS) adaptiveTimeLimit = MIN_TIME_LIMIT_MS;
            if (adaptiveTimeLimit > MAX_TIME_LIMIT_MS) adaptiveTimeLimit = MAX_TIME_LIMIT_MS;

            System.out.println(">>> TUNING GRAPH: " + graphName);
            System.out.printf("    Target (Paper): k=%d (%.2fs) | Tuner Limit: %.1fs%n",
                    paperData.getMin(), paperSec, adaptiveTimeLimit/1000.0);
            System.out.println("    -----------------------------------------------------------------------------------");

            // --- PHASE 1: GLOBAL EXPLORATION ---
            System.out.println("    [PHASE 1] Monte Carlo Exploration (" + PHASE_1_RANDOM_SAMPLES + " runs)");

            RunResult bestResult = null;

            for (int i = 0; i < PHASE_1_RANDOM_SAMPLES; i++) {
                ParameterSet params = ParameterSet.generateRandom();

                // Print BEFORE run so user knows what's happening
                System.out.printf("    Run %2d/%-2d : Testing %-35s ... ", (i + 1), PHASE_1_RANDOM_SAMPLES, params);

                RunResult result = runExperiment(f.getPath(), params, adaptiveTimeLimit);

                if (result == null) {
                    System.out.println("FAILED (Error/Timeout)");
                    continue;
                }

                // Analyze Result
                String status;
                if (bestResult == null) {
                    bestResult = result;
                    status = "★ INITIAL BEST";
                } else if (result.k < bestResult.k) {
                    bestResult = result;
                    status = "★ NEW BEST (K)";
                } else if (result.k == bestResult.k && result.ilsTime < bestResult.ilsTime) {
                    bestResult = result;
                    status = "★ NEW BEST (Time)";
                } else {
                    // Feedback on why it failed
                    if (result.k > bestResult.k) status = String.format("Discarded (k=%d vs %d)", result.k, bestResult.k);
                    else status = "Discarded (Slower)";
                }

                System.out.printf("Result: k=%-3d (%5dms) -> %s%n", result.k, result.ilsTime, status);
            }

            if (bestResult == null) {
                System.out.println("    !!! SKIPPING GRAPH (All runs failed) !!!\n");
                continue;
            }

            // --- PHASE 2: LOCAL REFINEMENT (HILL CLIMBING) ---
            System.out.println("    -----------------------------------------------------------------------------------");
            System.out.println("    [PHASE 2] Hill Climbing Refinement (Starting from k=" + bestResult.k + ")");

            boolean improvementFound = true;
            int step = 0;

            while (improvementFound && step < PHASE_2_MAX_CLIMB_STEPS) {
                improvementFound = false;
                step++;

                System.out.printf("    Step %d (Generating Neighbors)...%n", step);

                // Generate Neighbors
                List<ParameterSet> neighbors = bestResult.params.generateNeighbors();

                for (ParameterSet neighborParams : neighbors) {
                    System.out.printf("       > Neighbor: %-35s ... ", neighborParams);

                    RunResult neighborResult = runExperiment(f.getPath(), neighborParams, adaptiveTimeLimit);

                    if (neighborResult == null) {
                        System.out.println("Failed.");
                        continue;
                    }

                    if (isBetter(neighborResult, bestResult)) {
                        bestResult = neighborResult;
                        improvementFound = true;
                        System.out.printf("IMPROVED! k=%d (%dms)%n", bestResult.k, bestResult.ilsTime);
                        // Greedy: Take the first improvement and restart generation from there
                        break;
                    } else {
                        // Detailed failure reason
                        if (neighborResult.k > bestResult.k) System.out.printf("Worse (k=%d)%n", neighborResult.k);
                        else if (neighborResult.k == bestResult.k) System.out.printf("Same k, Slower (%dms)%n", neighborResult.ilsTime);
                        else System.out.println("No improvement.");
                    }
                }

                if (!improvementFound) {
                    System.out.println("    > No better neighbors found. Peak reached.");
                }
            }

            // --- FINAL REPORT FOR GRAPH ---
            System.out.println("    ===================================================================================");
            System.out.printf("    FINAL WINNER: k=%d (Paper: %d)%n", bestResult.k, paperData.getMin());

            // Logic check for optimality
            if (bestResult.k <= paperData.getMin()) System.out.println("    STATUS: OPTIMAL / SUPERIOR");
            else System.out.println("    STATUS: Gap of " + (bestResult.k - paperData.getMin()));

            System.out.println("    > COPY-PASTE CODE:");
            System.out.printf("      if (name.equals(\"%s\")) { TENURE_BASE=%d; TENURE_MULTI=%.2f; MAX_ITER=%d; PERTURB=%d; return; }%n",
                    graphName,
                    bestResult.params.base,
                    bestResult.params.multi,
                    bestResult.params.iter,
                    bestResult.params.kick
            );
            System.out.println("\n");
        }
    }

    /**
     * Comparison Logic:
     * 1. Fewer Colors (k) is ALWAYS better.
     * 2. If Colors are equal, Faster Time is better.
     */
    private static boolean isBetter(RunResult newResult, RunResult currentBest) {
        if (currentBest == null) return true;
        if (newResult.k < currentBest.k) return true;
        if (newResult.k == currentBest.k && newResult.ilsTime < currentBest.ilsTime) return true;
        return false;
    }

    private static RunResult runExperiment(String filePath, ParameterSet params, long timeLimit) {
        try {
            Graph g = new Graph();
            g.loadDIMACS(filePath);

            g.applyConstructionHeuristic();
            g.applyReduction();

            long startILS = System.currentTimeMillis();

            IteratedLocalSearch ils = new IteratedLocalSearch(
                    g,
                    timeLimit,
                    params.base,
                    params.multi,
                    params.iter,
                    params.kick
            );
            ils.solve();

            long endILS = System.currentTimeMillis();

            // Clean up
            int k = g.getNumberOfUsedColors();
            g = null; ils = null;

            return new RunResult(params, k, (endILS - startILS));

        } catch (Exception e) {
            return null;
        }
    }

    // --- Helper Classes ---

    static class ParameterSet {
        int base;
        double multi;
        int iter;
        int kick;

        public ParameterSet(int base, double multi, int iter, int kick) {
            this.base = base;
            this.multi = multi;
            this.iter = iter;
            this.kick = kick;
        }

        @Override
        public String toString() {
            return String.format("[B:%d, M:%.2f, I:%d, K:%d]", base, multi, iter, kick);
        }

        public static ParameterSet generateRandom() {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            return new ParameterSet(
                    rnd.nextInt(5, 40),           // Base: 5 - 40
                    0.2 + (1.0 * rnd.nextDouble()), // Multi: 0.2 - 1.2
                    rnd.nextInt(500, 8000),       // Iter: 500 - 8000
                    rnd.nextInt(20, 300)          // Kick: 20 - 300
            );
        }

        // Generate neighbors for Hill Climbing (Wiggle parameters)
        public List<ParameterSet> generateNeighbors() {
            List<ParameterSet> neighbors = new ArrayList<>();

            // 1. Tenure Base Tweaks (Sensitivity: +/- 3)
            neighbors.add(new ParameterSet(Math.max(2, base - 3), multi, iter, kick));
            neighbors.add(new ParameterSet(base + 3, multi, iter, kick));

            // 2. Multiplier Tweaks (Sensitivity: +/- 0.1)
            neighbors.add(new ParameterSet(base, Math.max(0.1, multi - 0.1), iter, kick));
            neighbors.add(new ParameterSet(base, multi + 0.1, iter, kick));

            // 3. Iteration Tweaks (Sensitivity: +/- 500)
            neighbors.add(new ParameterSet(base, multi, Math.max(100, iter - 500), kick));
            neighbors.add(new ParameterSet(base, multi, iter + 500, kick));

            // 4. Kick Strength Tweaks (Sensitivity: +/- 20)
            neighbors.add(new ParameterSet(base, multi, iter, Math.max(5, kick - 20)));
            neighbors.add(new ParameterSet(base, multi, iter, kick + 20));

            return neighbors;
        }
    }

    static class RunResult {
        ParameterSet params;
        int k;
        long ilsTime;

        public RunResult(ParameterSet p, int k, long t) {
            this.params = p;
            this.k = k;
            this.ilsTime = t;
        }
    }
}