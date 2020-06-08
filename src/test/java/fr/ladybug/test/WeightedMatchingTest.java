package fr.ladybug.test;

import fr.ladybug.Graph;
import fr.ladybug.GraphReader;
import fr.ladybug.RealVertex;
import org.jgrapht.alg.matching.SparseEdmondsMaximumCardinalityMatching;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.generate.ScaleFreeGraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.util.SupplierUtil;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.util.Random;

import static fr.ladybug.test.BruteSolver.solve;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WeightedMatchingTest {

    void testGraph(Graph graph, double trueWeight) {
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
        double weight = graph.getCurrentMatching().stream()
                .reduce(0., (acc, edge) -> acc + edge.getWeight(), Double::sum);
        try {
            assertEquals(trueWeight, weight, 1e-6);
        }
        catch (AssertionFailedError e) {
            System.out.println(graph.toString());
            throw e;
        }
        System.out.println(String.format("Max weight is %f", trueWeight));
    }

    void testGraphFromFile(String graphPath, double trueWeight) {
        var graph = new GraphReader(graphPath).readGraph(true);
        testGraph(graph, trueWeight);
    }

    @Test
    void graph1() {
        testGraphFromFile("1w.grph", 11);
    }

    @Test
    void graph2() {
        testGraphFromFile("2w.grph", 11);
    }
    @Test
    void graph3() {
        testGraphFromFile("3w.grph", 11);
    }

    @Test
    void graph4() {
        // create S-blossom and use it for augmentation
        testGraphFromFile("4w.grph", 15);
        testGraphFromFile("4w2.grph", 21);
    }

    @Test
    void graph5() {
        // create S-blossom, relabel as T-blossom, use for augmentation
        testGraphFromFile("5w.grph", 17);
        testGraphFromFile("5w2.grph", 17);
        testGraphFromFile("5w3.grph", 16);
    }

    @Test
    void graph6() {
        // create nested S-blossom, use for augmentation
        testGraphFromFile("6w.grph", 23);
    }

    @Test
    void graph7() {
        // create S-blossom, relabel as S, include in nested S-blossom
        testGraphFromFile("7w.grph", 48);
    }

    @Test
    void graph8() {
        // create nested S-blossom, augment, expand recursively
        testGraphFromFile("8w.grph", 44);
    }

    @Test
    void graph9() {
        // create S-blossom, relabel as T, expand
        testGraphFromFile("9w.grph", 67);
    }

    @Test
    void graph10() {
        // create nested S-blossom, relabel as T, expand
        testGraphFromFile("10w.grph", 47);
    }

    @Test
    void graph11() {
        // again but slightly different
        testGraphFromFile("11w.grph", 146);
    }

    @Test
    void graph12() {
        // again but slightly different
        testGraphFromFile("12w.grph", 151);
    }

    @Test
    void graph13() {
        // create blossom, relabel as T, expand such that a new least-slack S-to-free edge is produced, augment
        testGraphFromFile("13w.grph", 139);
    }

    @Test
    void graph14() {
        // create nested blossom, relabel as T in more than one way, expand outer blossom such that inner blossom ends up on an augmenting path
        testGraphFromFile("14w.grph", 241);
    }

    @Test
    void graph15() {
        // # create nested S-blossom, relabel as S, expand recursively
        testGraphFromFile("15w.grph", 145);
    }

    @Test
    void graph16() {
        // Just test brutted out
        testGraphFromFile("16w.grph", 297);
    }


    @Test
    void random2() {
        stress(3, 2, 1, 500);
        stress(4, 2, 2, 500);
        stress(4, 3, 3, 500);
        stress(4, 4, 4, 500);
        stress(4, 5, 5, 500);
        stress(4, 6, 6, 500);
        stress(5, 2, 8, 500);
        stress(5, 3, 9, 500);
        stress(5, 4, 10, 500);
        stress(5, 6, 11, 500);
        stress(5, 8, 12, 500);
        stress(5, 10, 13, 500);
        stress(6, 2, 14, 500);
        stress(6, 4, 15, 500);
        stress(6, 6, 16, 500);
        stress(6, 8, 17, 500);
        stress(6, 10, 18, 500);
        stress(6, 12, 19, 500);
        stress(6, 14, 20, 500);
        stress(7,  2, 21, 500);
        stress(7,  6, 22, 500);
        stress(7,  8, 23, 500);
        stress(7,  12, 24, 500);
        stress(8, 12, 25, 500);
        stress(9, 12, 26, 500);
        stress(10, 12, 27, 500);
        stress(11, 12, 28, 500);
        stress(12, 12, 29, 500);
        stress(13, 12, 30, 500);
        stress(14, 12, 31, 500);
    }

    void stress(int n, int m, int seed, int iterations) {
        System.out.println(seed);
        var rnd = new Random(seed);
        for (int i = 0; i < iterations; i++) {
            RealVertex.global_id = 0;
            org.jgrapht.Graph<Integer, DefaultEdge> graph = new SimpleGraph<Integer, DefaultEdge>(
                    SupplierUtil.createIntegerSupplier(),
                    SupplierUtil.createDefaultEdgeSupplier(),
                    false);
            new GnmRandomGraphGenerator<Integer, DefaultEdge>(n, m, rnd.nextInt(), false, false)
                    .generateGraph(graph);

            var ourGraph = new Graph(graph, seed);
            var trueWeight = solve(ourGraph);
            System.out.println(trueWeight);
            testGraph(ourGraph, trueWeight);
        }
    }

}
