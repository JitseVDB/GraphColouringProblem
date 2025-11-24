import java.util.*;

/**
 * Implements the Iterated Local Search (ILS) algorithm for graph coloring.
 *
 * Works on the active vertices of a graph and tries to iteratively reduce
 * the number of colors using local search with perturbations.
 *
 * @author  Jitse Vandenberghe
 * @version 1.1
 */
public class IteratedLocalSearch {
    private Graph graph;         // The graph to color
    private int[] bestColors;    // Best coloring found
    private int bestK;           // Number of colors in the best coloring
    private Random rng = new Random();
    private long timeLimitMs;    // Time limit in milliseconds

    public IteratedLocalSearch(Graph g, long timeLimitMs) {
        this.graph = g;
        this.timeLimitMs = timeLimitMs;
    }

    public int[] runIteratedLocalSearch(Graph g) {
        long endTime = System.currentTimeMillis() + timeLimitMs;

        int[] current = g.getColorArray().clone();
        int currentK = countColors(current);

        bestColors = current.clone();
        bestK = currentK;

        List<Integer> activeNodes = new ArrayList<>(graph.getNodes());

        while (System.currentTimeMillis() < endTime) {

            boolean improved = true;
            while (improved) {
                improved = tryReduceColors(current, activeNodes);
                if (improved) {
                    currentK = countColors(current);
                    if (currentK < bestK) {
                        bestK = currentK;
                        bestColors = current.clone();
                    }
                }
            }

            // local minimum reached → perturb
            perturb(current, Math.max(1, bestK / 3), activeNodes);

            // repair → get feasible coloring again
            greedyRepair(current, activeNodes);
            currentK = countColors(current);

            if (currentK < bestK) {
                bestK = currentK;
                bestColors = current.clone();
            }
        }

        return bestColors;
    }

    private boolean tryReduceColors(int[] colors, List<Integer> activeNodes) {
        int k = countColors(colors);

        for (int target = k; target >= 1; target--) {
            List<Integer> classVertices = new ArrayList<>();
            for (int v : activeNodes) {
                if (colors[v] == target)
                    classVertices.add(v);
            }

            if (canRecolorClass(classVertices, colors, target)) {
                compactColorNumbers(colors, activeNodes);
                return true;
            }
        }
        return false;
    }

    private boolean canRecolorClass(List<Integer> classVerts, int[] colors, int removedColor) {
        for (int v : classVerts) {
            boolean assigned = false;
            for (int c = 1; c < removedColor; c++) {
                if (isColorAllowed(v, c, colors)) {
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

    private void perturb(int[] colors, int strength, List<Integer> activeNodes) {
        Collections.shuffle(activeNodes, rng);
        for (int i = 0; i < strength && i < activeNodes.size(); i++) {
            colors[activeNodes.get(i)] = -1;
        }
        greedyRepair(colors, activeNodes);
    }

    private void greedyRepair(int[] colors, List<Integer> activeNodes) {
        for (int v : activeNodes) {
            if (colors[v] != -1) continue;
            int c = 1;
            while (!isColorAllowed(v, c, colors)) c++;
            colors[v] = c;
        }
    }

    private int countColors(int[] colors) {
        int max = 0;
        for (int v : graph.getNodes()) {
            if (colors[v] > max) max = colors[v];
        }
        return max;
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
}
