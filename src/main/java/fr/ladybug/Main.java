package fr.ladybug;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        var graph = new GraphReader(null).readGraph(false);

        graph.initializeVertexVariables();

        while (true) {
            Graph.GrowingResult result = graph.growTree();
            if (result == Graph.GrowingResult.OPTIMAL_TREE)
                break;
        }
        double weight = graph.getCurrentMatching().stream()
                .reduce(0., (acc, edge) -> acc + edge.getWeight(), Double::sum);

        System.out.println((int)weight);
    }
}
