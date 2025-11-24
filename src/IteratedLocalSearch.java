import java.util.*;

public class IteratedLocalSearch {
    private Graph graph;
    private int[] bestColors;
    private int bestK;
    private Random rng = new Random();
    private long timeLimitMs;

    private int maxNoImprovement = 1000;
    private final int tabuTenure = 10;
    private Map<Integer, Queue<Integer>> tabuList = new HashMap<>();

    public IteratedLocalSearch(Graph g, long timeLimitMs) {
        this.graph = g;
        this.timeLimitMs = timeLimitMs;
        for (int v : g.getNodes()) {
            tabuList.put(v, new LinkedList<>());
        }
    }

    public int[] runIteratedLocalSearch(Graph g) {
        long endTime = System.currentTimeMillis() + timeLimitMs;

        int[] current = g.getColorArray().clone();
        bestColors = current.clone();
        bestK = countColors(bestColors);

        List<Integer> activeNodes = new ArrayList<>(graph.getNodes());
        int noImprovementCount = 0;

        while (System.currentTimeMillis() < endTime) {
            // Local search (TS1-ex / tryReduceColors)
            boolean improved = true;
            while (improved) {
                improved = tryReduceColors(current, activeNodes);
                int currentK = countColors(current);
                if (currentK < bestK) {
                    bestK = currentK;
                    bestColors = current.clone();
                    noImprovementCount = 0;
                }
            }

            // Perturbation applied to bestColors, not current
            perturb(bestColors, activeNodes);

            // Copy perturbed bestColors into current for next local search
            current = bestColors.clone();

            // Local search on perturbed solution
            improved = true;
            while (improved) {
                improved = tryReduceColors(current, activeNodes);
                int currentK = countColors(current);
                if (currentK < bestK) {
                    bestK = currentK;
                    bestColors = current.clone();
                    noImprovementCount = 0;
                }
            }

            // Early stopping
            noImprovementCount++;
            if (noImprovementCount >= maxNoImprovement) break;
        }

        return bestColors;
    }

    private boolean tryReduceColors(int[] colors, List<Integer> activeNodes) {
        int k = countColors(colors);
        boolean improved = false;

        for (int target = k; target >= 1; target--) {
            List<Integer> classVertices = new ArrayList<>();
            for (int v : activeNodes) {
                if (colors[v] == target) classVertices.add(v);
            }

            if (canRecolorClass(classVertices, colors, target)) {
                compactColorNumbers(colors, activeNodes);
                improved = true;
            }
        }

        return improved;
    }

    private boolean canRecolorClass(List<Integer> classVerts, int[] colors, int removedColor) {
        for (int v : classVerts) {
            boolean assigned = false;
            for (int c = 1; c < removedColor; c++) {
                if (isColorAllowed(v, c, colors) && !isTabu(v, c)) {
                    addToTabu(v, colors[v]); // mark old color as tabu
                    colors[v] = c;
                    assigned = true;
                    break;
                }
            }
            if (!assigned) return false;
        }
        return true;
    }

    private boolean isColorAllowed(int v, int c, int[] colors) {
        if (!graph.isActive(v)) return false;

        BitSet neighbors = graph.adj[v];
        for (int u = neighbors.nextSetBit(0); u >= 0; u = neighbors.nextSetBit(u + 1)) {
            if (!graph.isActive(u)) continue;
            if (colors[u] == c) return false;
        }
        return true;
    }

    private void perturb(int[] colors, List<Integer> activeNodes) {
        // Clear tabu list
        for (int v : activeNodes) tabuList.get(v).clear();

        // Select kr < k color classes randomly
        int k = countColors(colors);
        int kr = Math.max(1, k / 3);
        List<Integer> colorClasses = new ArrayList<>();
        for (int c = 1; c <= k; c++) colorClasses.add(c);
        Collections.shuffle(colorClasses, rng);

        Set<Integer> uncolorVertices = new HashSet<>();
        for (int i = 0; i < kr && i < colorClasses.size(); i++) {
            int colorToRemove = colorClasses.get(i);
            for (int v : activeNodes) {
                if (colors[v] == colorToRemove) uncolorVertices.add(v);
            }
        }

        // Uncolor vertices and mark old colors in tabu
        for (int v : uncolorVertices) {
            addToTabu(v, colors[v]);
            colors[v] = -1;
        }

        // Repair with ROS-like heuristic
        greedyRepair(colors, activeNodes);
    }

    private void greedyRepair(int[] colors, List<Integer> activeNodes) {
        for (int v : activeNodes) {
            if (colors[v] != -1) continue;
            int c = 1;
            while (!isColorAllowed(v, c, colors) || isTabu(v, c)) c++;
            colors[v] = c; // assign color
        }
    }

    private int countColors(int[] colors) {
        BitSet used = new BitSet();
        for (int v : graph.getNodes()) {
            if (colors[v] != -1) used.set(colors[v]);
        }
        return used.cardinality();
    }

    private void compactColorNumbers(int[] colors, List<Integer> activeNodes) {
        Map<Integer, Integer> remap = new HashMap<>();
        int next = 1;
        for (int v : activeNodes) {
            int c = colors[v];
            if (c == -1) continue;
            if (!remap.containsKey(c)) remap.put(c, next++);
            colors[v] = remap.get(c);
        }
    }

    private boolean isTabu(int v, int c) {
        return tabuList.get(v).contains(c);
    }

    private void addToTabu(int v, int c) {
        if (c == -1) return;
        Queue<Integer> queue = tabuList.get(v);
        queue.add(c);
        if (queue.size() > tabuTenure) queue.poll();
    }
}
