import java.util.HashMap;
import java.util.Map;

public class ILSResultsPaper {

    // Inner class to hold min, med, and time_sec
    public static class GraphResult {
        private final int min;
        private final int med;
        private final double timeSec;

        public GraphResult(int min, int med, double timeSec) {
            this.min = min;
            this.med = med;
            this.timeSec = timeSec;
        }

        public int getMin() {
            return min;
        }

        public int getMed() {
            return med;
        }

        public double getTimeSec() {
            return timeSec;
        }

        @Override
        public String toString() {
            return "GraphResult{" +
                    "min=" + min +
                    ", med=" + med +
                    ", timeSec=" + timeSec +
                    '}';
        }
    }

    // Map of graph name -> GraphResult
    private static final Map<String, GraphResult> ilsResults = new HashMap<>();

    static {
        // DSJC series
        ilsResults.put("DSJC125.1", new GraphResult(5, 5, 0));
        ilsResults.put("DSJC250.1", new GraphResult(8, 8, 0.2));
        ilsResults.put("DSJC500.1", new GraphResult(13, 13, 0.1));
        ilsResults.put("DSJC1000.1", new GraphResult(21, 21, 5.9));
        ilsResults.put("DSJC125.5", new GraphResult(17, 17, 1.6));
        ilsResults.put("DSJC250.5", new GraphResult(28, 28, 33.6));
        ilsResults.put("DSJC500.5", new GraphResult(50, 50, 105.8));
        ilsResults.put("DSJC1000.5", new GraphResult(90, 91, 303.5));
        ilsResults.put("DSJC125.9", new GraphResult(44, 44, 0.1));
        ilsResults.put("DSJC250.9", new GraphResult(72, 72, 5.6));
        ilsResults.put("DSJC500.9", new GraphResult(127, 128, 82.3));
        ilsResults.put("DSJC1000.9", new GraphResult(227, 228, 2245));

        // flat series
        ilsResults.put("flat300_20_0", new GraphResult(20, 20, 0.4));
        ilsResults.put("flat300_26_0", new GraphResult(26, 26, 16.6));
        ilsResults.put("flat300_28_0", new GraphResult(31, 32, 3.3));
        ilsResults.put("flat1000_50_0", new GraphResult(88, 88, 729.5));
        ilsResults.put("flat1000_60_0", new GraphResult(89, 90, 128.2));
        ilsResults.put("flat1000_76_0", new GraphResult(89, 90, 188.7));

        // le450 series
        ilsResults.put("le450-5a", new GraphResult(5, 5, 0.1));
        ilsResults.put("le450-5b", new GraphResult(5, 5, 0.6));
        ilsResults.put("le450-5d", new GraphResult(5, 5, 0));
        ilsResults.put("le450-15a", new GraphResult(15, 15, 0.1));
        ilsResults.put("le450-15b", new GraphResult(15, 15, 0.1));
        ilsResults.put("le450-15c", new GraphResult(15, 15, 19.1));
        ilsResults.put("le450-15d", new GraphResult(15, 15, 20.3));
        ilsResults.put("le450-25c", new GraphResult(26, 26, 2));
        ilsResults.put("le450-25d", new GraphResult(26, 26, 0.8));

        // latin_square
        ilsResults.put("latin_square_10", new GraphResult(103, 104, 510.4));

        // qg
        ilsResults.put("qg_order100", new GraphResult(100, 100, 18.3));

        // queen series
        ilsResults.put("queen6_6", new GraphResult(7, 7, 0));
        ilsResults.put("queen7_7", new GraphResult(7, 7, 0));
        ilsResults.put("queen8_12", new GraphResult(12, 12, 0));
        ilsResults.put("queen8_8", new GraphResult(9, 9, 0));
        ilsResults.put("queen9_9", new GraphResult(10, 10, 0));
        ilsResults.put("queen10_10", new GraphResult(11, 11, 0));
        ilsResults.put("queen11_11", new GraphResult(12, 12, 0.2));
        ilsResults.put("queen12_12", new GraphResult(13, 13, 0.9));
        ilsResults.put("queen13_13", new GraphResult(14, 14, 1.3));
        ilsResults.put("queen14_14", new GraphResult(15, 15, 20));
        ilsResults.put("queen15_15", new GraphResult(16, 16, 23.9));
        ilsResults.put("queen16_16", new GraphResult(18, 18, 0));

        // wap series
        ilsResults.put("wap01a", new GraphResult(43, 44, 1.5));
        ilsResults.put("wap02a", new GraphResult(42, 42, 251.6));
        ilsResults.put("wap03a", new GraphResult(46, 46, 365.2));
        ilsResults.put("wap04a", new GraphResult(44, 44, 484.3));
        ilsResults.put("wap06a", new GraphResult(42, 42, 0.5));
        ilsResults.put("wap07a", new GraphResult(43, 44, 0.7));
        ilsResults.put("wap08a", new GraphResult(43, 43, 56.1));

        // GPIA series
        ilsResults.put("abb313GPIA", new GraphResult(9, 9, 0.9));
        ilsResults.put("ash331GPIA", new GraphResult(4, 4, 0));
        ilsResults.put("ash608GPIA", new GraphResult(4, 4, 0.1));
        ilsResults.put("ash958GPIA", new GraphResult(4, 4, 0.6));
        ilsResults.put("will199GPIA", new GraphResult(7, 7, 0));
    }

    // Public methods to get results
    public static int getMin(String graphName) {
        GraphResult result = ilsResults.get(graphName);
        return result != null ? result.getMin() : -1;
    }

    public static int getMed(String graphName) {
        GraphResult result = ilsResults.get(graphName);
        return result != null ? result.getMed() : -1;
    }

    public static double getTimeSec(String graphName) {
        GraphResult result = ilsResults.get(graphName);
        return result != null ? result.getTimeSec() : -1;
    }

    public static GraphResult getResult(String graphName) {
        return ilsResults.get(graphName);
    }
}
