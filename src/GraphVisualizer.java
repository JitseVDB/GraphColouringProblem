import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class GraphVisualizer extends JPanel {
    private final Map<Integer, BitSet> adjMap;
    private final Map<Integer, Integer> colorMap;
    private final int[][] coordinates; // Stores x,y for every node
    private final int nodeRadius = 15;

    // Colors for the graph nodes
    private final Color[] palette = {
            new Color(255, 102, 102), new Color(102, 255, 102), new Color(102, 102, 255),
            new Color(255, 255, 102), new Color(255, 102, 255), new Color(102, 255, 255),
            new Color(255, 179, 102), new Color(179, 255, 102), new Color(179, 102, 255)
    };

    public GraphVisualizer(Map<Integer, BitSet> adjMap, Map<Integer, Integer> colorMap) {
        this.adjMap = adjMap;
        this.colorMap = colorMap;

        // 1. Calculate Layout (Circle Layout)
        int n = adjMap.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        this.coordinates = new int[n + 1][2];

        int centerX = 400;
        int centerY = 400;
        int radius = 300;

        int index = 0;
        int totalNodes = adjMap.size();

        for (Integer node : adjMap.keySet()) {
            double angle = 2 * Math.PI * index / totalNodes;
            coordinates[node][0] = (int) (centerX + radius * Math.cos(angle));
            coordinates[node][1] = (int) (centerY + radius * Math.sin(angle));
            index++;
        }

        this.setPreferredSize(new Dimension(800, 800));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Enable Anti-aliasing for smooth circles/lines
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Draw Edges first (so they appear behind nodes)
        g2.setColor(new Color(200, 200, 200, 100)); // Light gray, semi-transparent
        g2.setStroke(new BasicStroke(1));

        for (Integer u : adjMap.keySet()) {
            BitSet neighbors = adjMap.get(u);
            for (int v = neighbors.nextSetBit(0); v >= 0; v = neighbors.nextSetBit(v + 1)) {
                if (u < v && adjMap.containsKey(v)) {
                    g2.draw(new Line2D.Double(
                            coordinates[u][0], coordinates[u][1],
                            coordinates[v][0], coordinates[v][1]
                    ));
                }
            }
        }

        // 2. Draw Nodes
        for (Integer node : adjMap.keySet()) {
            int x = coordinates[node][0] - nodeRadius;
            int y = coordinates[node][1] - nodeRadius;

            // Determine Color
            int cIndex = colorMap.getOrDefault(node, -1);
            if (cIndex == -1) {
                g2.setColor(Color.WHITE);
            } else {
                g2.setColor(palette[cIndex % palette.length]);
            }

            // Fill Node
            g2.fill(new Ellipse2D.Double(x, y, nodeRadius * 2, nodeRadius * 2));

            // Draw Border
            g2.setColor(Color.BLACK);
            g2.draw(new Ellipse2D.Double(x, y, nodeRadius * 2, nodeRadius * 2));

            // Draw Label (Node ID)
            String label = String.valueOf(node);
            FontMetrics fm = g2.getFontMetrics();
            int labelWidth = fm.stringWidth(label);
            g2.drawString(label, coordinates[node][0] - labelWidth / 2, coordinates[node][1] + fm.getAscent() / 2 - 2);
        }
    }

    /**
     * Displays a window with the current graph.
     *
     * @param graph Graph instance using updated internal structures
     * @param title Title for the JFrame window
     */
    public static void show(Graph graph, String title) {
        JFrame frame = new JFrame("Graph: " + title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        GraphVisualizer panel = new GraphVisualizer(graph.getAdjCopy(), graph.getColorsCopy());

        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
