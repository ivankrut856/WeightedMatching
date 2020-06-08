package fr.ladybug.test;

import fr.ladybug.Graph;
import fr.ladybug.GraphReader;
import fr.ladybug.RealVertex;
import fr.ladybug.Vertex;
import org.jgrapht.alg.matching.SparseEdmondsMaximumCardinalityMatching;
import org.jgrapht.generate.*;
import org.jgrapht.util.SupplierUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.traverse.*;

import java.util.Random;

public class UnweightedMatchingTest {

    void testGraph(Graph graph) {
        int targetCardinality = new SparseEdmondsMaximumCardinalityMatching<Integer, DefaultEdge>(graph.toJgraphtGraph())
                .getMatching().getEdges().size();

        graph.initializeVertexVariables();

        try {
            int i = 0;
            while (true) {
                Graph.GrowingResult result = graph.growTree();
                if (result == Graph.GrowingResult.OPTIMAL_TREE) {
                    break;
                }
            }
        }
        catch (RuntimeException e) {
            System.out.println(graph.toString());
            throw e;
        }
        assertEquals(targetCardinality, graph.getCurrentMatching().size());
        System.out.println(String.format("Max cardinality is %d", targetCardinality));
    }

    void testGraphFromFile(String graphPath) {
        var graph = new GraphReader(graphPath).readGraph(true);
        testGraph(graph);
    }

    @Test
    void graph1() {
        testGraphFromFile("1.grph");
    }

    @Test
    void graph2() {
        testGraphFromFile("2.grph");
    }

    @Test
    void graph3() {
        testGraphFromFile("3.grph");
    }

    @Test
    void graph4() {
        testGraphFromFile("4.grph");
    }

    @Test
    void graph5() {
        testGraphFromFile("5.grph");
    }

    @Test
    void graph6() {
        testGraphFromFile("6.grph");
    }

    @Test
    void graph7() {
        testGraphFromFile("7.grph");
    }

//    @Test
    void random1() {
        var rnd = new Random(0);
        while (true) {
            RealVertex.global_id = 0;
            org.jgrapht.Graph<Integer, DefaultEdge> graph = new SimpleGraph<Integer, DefaultEdge>(
                    SupplierUtil.createIntegerSupplier(),
                    SupplierUtil.createDefaultEdgeSupplier(),
                    false);
            new ScaleFreeGraphGenerator<Integer, DefaultEdge>(100, rnd.nextLong())
                    .generateGraph(graph);
            testGraph(new Graph(graph, 0));
        }
    }

}
