import java.util.*;

public class IteratedLocalSearch {

    private final Graph g;
    private final Random rng = new Random();
    private final long timeLimitMs;

    // Parameters
    private static final int MAX_LOCAL_NO_IMPROVE = 2000;
    private static final double PERTURB_FRACTION = 0.10;
    private static final double ACCEPT_WORSE_PROB = 0.05;

    // Cache node list to avoid recreating it repeatedly
    private final int[] nodes;

    public IteratedLocalSearch(Graph g, long timeLimitMs) {
        this.g = g;
        this.timeLimitMs = timeLimitMs;

        // Cache nodes for performance
        List<Integer> list = new ArrayList<>(g.getNodes());
        nodes = new int[list.size()];
        for (int i = 0; i < list.size(); i++) nodes[i] = list.get(i);
    }

    public int[] runIteratedLocalSearch() {

        long end = System.currentTimeMillis() + timeLimitMs;

        int[] sol = g.getColorArray().clone();
        int[] best = sol.clone();

        fixConflictsGreedy(sol);
        fixConflictsGreedy(best);

        int bestK = countColors(best);

        while (System.currentTimeMillis() < end) {

            localSearch(sol);

            int k = countColors(sol);
            if (k < bestK) {
                System.arraycopy(sol, 0, best, 0, sol.length);
                bestK = k;
            } else {
                if (rng.nextDouble() > ACCEPT_WORSE_PROB)
                    System.arraycopy(best, 0, sol, 0, sol.length);
            }

            perturb(sol);
            fixConflictsGreedy(sol);
        }

        compactColors(best);
        return best;
    }


    // ---------------------- Local Search --------------------------

    private void localSearch(int[] colors) {

        int noImprove = 0;
        int currentConf = countConflicts(colors);

        while (noImprove < MAX_LOCAL_NO_IMPROVE) {

            boolean improved = false;

            // Simple recoloring
            if (recolorSingle(colors)) {
                int newConf = countConflicts(colors);
                if (newConf < currentConf) {
                    currentConf = newConf;
                    improved = true;
                }
            }

            // Try Kempe move only if no improvement
            if (!improved && kempeMove(colors)) {
                int newConf = countConflicts(colors);
                if (newConf < currentConf) {
                    currentConf = newConf;
                    improved = true;
                }
            }

            if (improved) noImprove = 0;
            else noImprove++;
        }

        compactColors(colors);
    }


    private boolean recolorSingle(int[] colors) {

        // Shuffle cached node array
        shuffleArray(nodes);

        boolean improved = false;

        for (int v : nodes) {
            int old = colors[v];

            // try smaller colors first
            for (int c = 1; c < old; c++) {
                if (isColorAllowed(v, c, colors)) {
                    colors[v] = c;
                    improved = true;
                    break;
                }
            }
        }
        return improved;
    }


    private boolean kempeMove(int[] colors) {

        shuffleArray(nodes);

        for (int v : nodes) {
            int cv = colors[v];

            for (int target = 1; target < cv; target++) {

                // Build Kempe chain
                BitSet visited = new BitSet(colors.length);
                ArrayDeque<Integer> stack = new ArrayDeque<>();

                stack.push(v);
                visited.set(v);

                while (!stack.isEmpty()) {
                    int x = stack.pop();
                    BitSet nbrs = g.adj[x];

                    for (int u = nbrs.nextSetBit(0); u >= 0; u = nbrs.nextSetBit(u + 1)) {
                        int cu = colors[u];
                        if (cu != cv && cu != target) continue;
                        if (!visited.get(u)) {
                            visited.set(u);
                            stack.push(u);
                        }
                    }
                }

                // Simulate swap cheaply
                int oldConf = countConflicts(colors);
                int[] temp = colors.clone();

                for (int u = visited.nextSetBit(0); u >= 0; u = visited.nextSetBit(u + 1)) {
                    temp[u] = (temp[u] == cv) ? target : cv;
                }

                if (countConflicts(temp) <= oldConf) {
                    System.arraycopy(temp, 0, colors, 0, colors.length);
                    return true;
                }
            }
        }
        return false;
    }


    // ---------------------- Repair and Perturbation --------------------------

    private void fixConflictsGreedy(int[] colors) {
        boolean changed = true;

        while (changed) {
            changed = false;

            for (int v : nodes) {
                BitSet nbrs = g.adj[v];
                int cv = colors[v];

                for (int u = nbrs.nextSetBit(0); u >= 0; u = nbrs.nextSetBit(u + 1)) {
                    if (colors[u] == cv) {
                        int newc = 1;
                        while (!isColorAllowed(v, newc, colors)) newc++;
                        colors[v] = newc;
                        changed = true;
                    }
                }
            }
        }
        compactColors(colors);
    }


    private void perturb(int[] colors) {

        int numNodes = nodes.length;
        int target = (int)(PERTURB_FRACTION * numNodes);

        // Bucket colors ↓
        Map<Integer, List<Integer>> buckets = new HashMap<>();
        for (int v : nodes) {
            buckets.computeIfAbsent(colors[v], x -> new ArrayList<>()).add(v);
        }

        // Sort by class size
        List<List<Integer>> sorted = new ArrayList<>(buckets.values());
        sorted.sort((a, b) -> b.size() - a.size());

        int removed = 0;
        for (List<Integer> cls : sorted) {
            Collections.shuffle(cls, rng);
            for (int v : cls) {
                if (removed >= target) return;
                colors[v] = -1;
                removed++;
            }
        }
    }


    // ---------------------- Utilities --------------------------

    private int countConflicts(int[] c) {
        int conflicts = 0;

        for (int v : nodes) {
            int cv = c[v];
            BitSet nbrs = g.adj[v];

            for (int u = nbrs.nextSetBit(0); u >= 0; u = nbrs.nextSetBit(u + 1))
                if (c[u] == cv) conflicts++;
        }

        return conflicts;
    }


    private boolean isColorAllowed(int v, int col, int[] colors) {
        BitSet nbrs = g.adj[v];
        for (int u = nbrs.nextSetBit(0); u >= 0; u = nbrs.nextSetBit(u + 1))
            if (colors[u] == col) return false;
        return true;
    }


    private int countColors(int[] c) {
        BitSet used = new BitSet();
        for (int v : nodes) used.set(c[v]);
        return used.cardinality();
    }


    private void compactColors(int[] c) {
        Map<Integer,Integer> map = new HashMap<>();
        int next = 1;

        for (int v : nodes) {
            int col = c[v];
            Integer remap = map.get(col);
            if (remap == null) {
                remap = next++;
                map.put(col, remap);
            }
            c[v] = remap;
        }
    }


    // Fisher-Yates shuffle on primitive int array
    private void shuffleArray(int[] arr) {
        for (int i = arr.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
    }
}
