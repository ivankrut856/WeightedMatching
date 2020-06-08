package fr.ladybug.test;

import fr.ladybug.Edge;
import fr.ladybug.Graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BruteSolver {

    public static double solve(Graph graph) {
        var edges = graph.allEdges().stream().distinct().collect(Collectors.toList());
        int size = edges.size();
        if (size > 30) {
            throw new IllegalArgumentException("The graph is too big.");
        }
        double maxWeight = 0;
        for (int i = 0; i < (1 << size); i++) {
            var takenEdges = new ArrayList<Edge>();
            for (int j = 0; j < size; j++) {
                if ((i & (1 << j)) != 0) {
                    takenEdges.add(edges.get(j));
                }
            }
            var distinctEnds = takenEdges.stream()
                    .flatMap(edge -> Stream.of(edge.from(), edge.to()))
                    .distinct()
                    .collect(Collectors.toList());
            if (distinctEnds.size() != takenEdges.size() * 2)
                continue;
            double weight = takenEdges.stream()
                    .reduce(0., (acc, edge) -> acc + edge.getWeight(), Double::sum);
            maxWeight = Math.max(maxWeight, weight);
        }

        return maxWeight;
    }
}
