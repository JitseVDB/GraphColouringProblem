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
            // Intensify: local search starting from current
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

            // Generate a perturbed candidate from bestColors (work on a copy)
            int[] candidate = bestColors.clone();

            // Clear tabu list (paper empties tabu before perturbation)
            for (int v : activeNodes) tabuList.get(v).clear();

            // Apply perturbation to the candidate (use existing perturb method but applied on the copy)
            perturb(candidate, activeNodes);

            // Local search on candidate
            boolean candImproved = true;
            while (candImproved) {
                candImproved = tryReduceColors(candidate, activeNodes);
            }

            int candidateK = countColors(candidate);

            // Acceptance: only accept candidate if it improves bestK
            if (candidateK < bestK) {
                bestK = candidateK;
                bestColors = candidate.clone();
                // set current to this improved candidate, reset noImprovementCount
                current = candidate.clone();
                noImprovementCount = 0;
            } else {
                // do NOT overwrite bestColors; optionally set current = bestColors to restart intensification
                current = bestColors.clone();
                noImprovementCount++;
            }

            if (noImprovementCount >= maxNoImprovement) break;
        }

        return bestColors;
    }

    private boolean tryReduceColors(int[] colors, List<Integer> activeNodes) {
        boolean improved = false;

        while (true) {
            int k = countColors(colors);
            boolean localChange = false;

            for (int target = k; target >= 1; target--) {
                // collect vertices in the target class
                List<Integer> classVertices = new ArrayList<>();
                for (int v : activeNodes) {
                    if (colors[v] == target) classVertices.add(v);
                }
                if (classVertices.isEmpty()) continue;

                if (canRecolorClass(classVertices, colors, target)) {
                    compactColorNumbers(colors, activeNodes);
                    localChange = true;
                    improved = true;
                    // after a successful recolor we should restart from the new k
                    break;
                }
            }

            if (!localChange) break; // no recolor possible this pass
        }

        return improved;
    }

    private boolean canRecolorClass(List<Integer> classVerts, int[] colors, int removedColor) {
        // Work on a temporary copy to avoid partial commits on failure
        int[] tempColors = colors.clone();
        List<int[]> tabuAdds = new ArrayList<>(); // store pairs (v, colorAdded)

        for (int v : classVerts) {
            boolean assigned = false;
            for (int c = 1; c < removedColor; c++) {
                if (isColorAllowed(v, c, tempColors) && !isTabu(v, c)) {
                    // record the move in tempColors and record tabu addition to be performed later
                    tabuAdds.add(new int[]{v, tempColors[v]}); // record old color for tabu
                    tempColors[v] = c;
                    assigned = true;
                    break;
                }
            }
            if (!assigned) {
                // rollback not necessary for tempColors, but we must not commit anything
                return false;
            }
        }

        // All vertices could be assigned -> commit changes and commit tabu entries
        for (int v : classVerts) {
            colors[v] = tempColors[v];
        }
        // commit tabu additions
        for (int[] entry : tabuAdds) {
            int v = entry[0];
            int oldColor = entry[1];
            addToTabu(v, oldColor);
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
