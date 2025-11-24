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
        ilsResults.put("DSJC1000.5", new GraphResult(90, 91, 496.9));
        ilsResults.put("DSJC125.9", new GraphResult(44, 44, 0.2));
        ilsResults.put("DSJC250.9", new GraphResult(72, 72, 26.7));
        ilsResults.put("DSJC500.9", new GraphResult(127, 128, 127));
        ilsResults.put("DSJC1000.9", new GraphResult(227, 230, 2245));

        // flat series
        ilsResults.put("flat300-20", new GraphResult(20, 20, 0.3));
        ilsResults.put("flat300-26", new GraphResult(26, 26, 16.1));
        ilsResults.put("flat300-28", new GraphResult(31, 32, 7.8));
        ilsResults.put("flat1000-50", new GraphResult(50, 50, 636.3));
        ilsResults.put("flat1000-60", new GraphResult(87, 88, 624.5));
        ilsResults.put("flat1000-76", new GraphResult(88, 89, 869));

        // le450 series
        ilsResults.put("le450-5a", new GraphResult(5, 5, 0.2));
        ilsResults.put("le450-5b", new GraphResult(5, 5, 0.3));
        ilsResults.put("le450-5d", new GraphResult(55, 55, 0));
        ilsResults.put("le450-15a", new GraphResult(15, 15, 2.2));
        ilsResults.put("le450-15b", new GraphResult(15, 15, 0.3));
        ilsResults.put("le450-15c", new GraphResult(15, 16, 8));
        ilsResults.put("le450-15d", new GraphResult(15, 16, 7.1));
        ilsResults.put("le450-25c", new GraphResult(26, 27, 0));
        ilsResults.put("le450-25d", new GraphResult(26, 27, 0.2));

        // latin_square
        ilsResults.put("latin_square_10", new GraphResult(104, 105, 458.4));

        // qg
        ilsResults.put("qg_order100", new GraphResult(100, 100, 19.9));

        // queen series
        ilsResults.put("queen6_6", new GraphResult(7, 7, 0));
        ilsResults.put("queen7_7", new GraphResult(7, 7, 0));
        ilsResults.put("queen8_12", new GraphResult(12, 12, 0.1));
        ilsResults.put("queen8_8", new GraphResult(9, 9, 0.1));
        ilsResults.put("queen9_9", new GraphResult(10, 10, 0.1));
        ilsResults.put("queen10_10", new GraphResult(11, 11, 0.8));
        ilsResults.put("queen11_11", new GraphResult(12, 12, 0.1));
        ilsResults.put("queen12_12", new GraphResult(13, 13, 0.2));
        ilsResults.put("queen13_13", new GraphResult(14, 14, 0));
        ilsResults.put("queen14_14", new GraphResult(15, 16, 0));
        ilsResults.put("queen15_15", new GraphResult(16, 17, 0.8));
        ilsResults.put("queen16_16", new GraphResult(17, 18, 1.2));

        // wap series
        ilsResults.put("wap01a", new GraphResult(42, 44, 107.8));
        ilsResults.put("wap02a", new GraphResult(41, 42, 4.9));
        ilsResults.put("wap03a", new GraphResult(44, 47, 198.9));
        ilsResults.put("wap04a", new GraphResult(43, 44, 31.2));
        ilsResults.put("wap06a", new GraphResult(40, 41, 8.1));
        ilsResults.put("wap07a", new GraphResult(42, 45, 39));
        ilsResults.put("wap08a", new GraphResult(44, 45, 0.4));

        // GPIA series
        ilsResults.put("abb313GPIA", new GraphResult(9, 9, 0.1));
        ilsResults.put("ash331GPIA", new GraphResult(44, 44, 0));
        ilsResults.put("ash608GPIA", new GraphResult(4, 4, 0.1));
        ilsResults.put("ash958GPIA", new GraphResult(4, 4, 0.4));
        ilsResults.put("will199GPIA", new GraphResult(7, 8, 43.9));
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
