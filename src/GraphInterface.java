import java.util.Collection;

interface GraphInterface {
    /**
     *  Returns a collection of integers representing the vertices of the graph.
     */
    Collection<Integer> getNodes();

    /**
     *  Returns the number of edges present in the graph.
     */
    int getNumberOfEdges();

    /**
     *  Returns the number of nodes present in the graph.
     */
    int getNumberOfNodes();

    /**
     *  Returns whether node u and node v are neighbours.
     */
    boolean areNeighbors(int u,int v);

    /**
     *  Returns the degree of node u.
     */
    int getDegree(int u);

    /**
     *  Removes node u from the graph.
     */
    void removeNode(int u);

    /**
     *  Removes edge (u,v) from the graph.
     */
    void removeEdge(int u, int v);

    /**
     *  Returns a collection of integers representing the neighbours of node u.
     */
    Collection<Integer> getNeighborsOf(int u);

    /**
     *  Applies the chosen reduction heuristic to the graph.
     */
    void applyReduction();

    /**
     *  Apply the chosen construction heuristic to get an initial graph coloring.
     */
    void applyConstructionHeuristic();

    /**
     *  Apply the chosen stochastic local search algorithm.
     */
    void applyStochasticLocalSearchAlgorithm();

    /**
     *  Gets the color of node u in the current coloring.
     */
    int getColor(int u);

}
