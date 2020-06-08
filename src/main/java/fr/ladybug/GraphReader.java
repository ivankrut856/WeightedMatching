package fr.ladybug;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class GraphReader {

    private final String graphPath;

    public GraphReader(String graphPath) {
        this.graphPath = graphPath;
    }

    public Graph readGraph(boolean fromFile) {
        File file = null;
        if (fromFile)
            file = new File(getClass().getClassLoader().getResource(graphPath).getFile());
        Scanner in = null;
        try {
            if (fromFile)
                in = new Scanner(file);
            else
                in = new Scanner(System.in);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // Number of vertices
        int n = in.nextInt();
        List<RealVertex> vertices = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            vertices.add(new RealVertex());
        }
        // Number of edges
        int m = in.nextInt();
        // m edges in format: from, to, weight
        for (int i = 0; i < m; i++) {
            int from = in.nextInt();
            int to = in.nextInt();
            // All edges heading from the lesser one to greater one
            // UPD: No more
            double weight = in.nextDouble();
            var edgeA = new Edge(vertices.get(from), vertices.get(to), weight);
            var edgeB = new Edge(vertices.get(to), vertices.get(from), weight);

            edgeA.setRev(edgeB);
            edgeB.setRev(edgeA);

            vertices.get(from).addEdge(edgeA);
            vertices.get(to).addEdge(edgeB);
        }
        return new Graph(vertices);
    }

}
