package fr.ladybug;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

import java.util.*;
import java.util.stream.Collectors;

public class Graph {
    private List<Vertex> vertices;

    public Graph(List<RealVertex> vertices) {
        this.vertices = new ArrayList<>(vertices);
    }

    public Graph(org.jgrapht.Graph<Integer, DefaultEdge> graph, int seed) {
        var rnd = new Random(seed);
        vertices = new ArrayList<>();
        var map = new HashMap<Integer, RealVertex>();
        var edges = new HashSet<Edge>();
        for (var objectVertex: graph.vertexSet()) {
            var vertex = new RealVertex();
            vertices.add(vertex);
            map.put(objectVertex, vertex);
        }
        for (var objectVertex: graph.vertexSet()) {
            var vertex = map.get(objectVertex);
            var edgesFromVertex = graph.outgoingEdgesOf(objectVertex).stream()
                    .map(edge -> {
                        return map.get((graph.getEdgeTarget(edge).equals(objectVertex)) ?
                                graph.getEdgeSource(edge) :
                                graph.getEdgeTarget(edge));
                    })
                    .map(target -> new Edge(vertex, target, seed != 0 ? rnd.nextDouble() * 100 : 1))
                    .collect(Collectors.toList());
            edges.addAll(edgesFromVertex);
        }

        for (var edge: edges) {
            var rev = new Edge(edge.to, edge.from, edge.weight);
            edge.setRev(rev);
            rev.setRev(edge);

            edge.from.addEdge(edge);
            edge.to.addEdge(rev);
        }
    }

    public List<Edge> allEdges() {
        return vertices.stream().flatMap(vertex -> vertex.getEdges().stream()).collect(Collectors.toList());
    }

    public void initializeVertexVariables() {
        double maxWeight = Collections.max(allEdges().stream().map(edge -> edge.weight).collect(Collectors.toList()));

        for (var vertex : vertices) {
            vertex.setVariable(maxWeight / 2);
        }
    }

    public GrowingResult growTree() {
        var result = GrowingResult.OPTIMAL_TREE;

        for (var vertex: vertices) {
            // Skip non-roots, because their mark will be defined by their container
            if (!vertex.isRootVertex()) {
                continue;
            }
            // Unmark all
            vertex.setMark(Vertex.MarkType.UNMARKED);
            if (vertex.getPair().isEmpty()) {
                // Mark unmatched back to S_TYPE
                vertex.setMark(Vertex.MarkType.S_TYPE);
            }
        }
        while (true) {
            Queue<RealVertex> searchQueue = new ArrayDeque<>();
            for (var vertex: vertices) {
                if (vertex.isReal() && vertex.getMark() == Vertex.MarkType.S_TYPE) {
                    searchQueue.add((RealVertex)vertex);
                }
            }

            // While there're some unseen vertices
            while (!searchQueue.isEmpty()) {
                var current = searchQueue.poll();
                for (var edge : current.getEdges()) {
                    // If they're in the same blossom, skip the edge
                    if (edge.from.getRootContainer() == edge.to.getRootContainer())
                        continue;
                    // If the edge isn't tight (enough), skip the edge
                    if (Math.abs(edge.weight - edge.from.getVariable() - edge.to.getVariable()) > 1e-6)
                        continue;

                    // If the edge heads to unmarked (= married)
                    if (edge.to.mark == Vertex.MarkType.UNMARKED) {
                        // Married
                        var pair = edge.to.getRootContainer().getPair();
                        if (pair.isEmpty()) {
                            throw new RuntimeException("Unmarked means married.");
                        }
                        var pairVertex = pair.get();

                        // Mark used to be unmarked vertex with T_TYPE (and all subvertices)
                        edge.to.getRootContainer().setMark(Vertex.MarkType.T_TYPE);
                        // Set the edge as parent edge for the T_TYPE
                        edge.to.getRootContainer().setParentEdge(edge);
                        // Mark the pair of the newborn T_TYPE with S_TYPE (and all subvertices)
                        pairVertex.getRootContainer().setMark(Vertex.MarkType.S_TYPE);
                        // Add the S_TYPE (as its subvertices) to Q
                        searchQueue.addAll(pairVertex.getRootContainer().getAllVertices());
                        // EZ
                    } else if (edge.to.mark == Vertex.MarkType.T_TYPE) {
                        // Discarded
                    } else if (edge.to.mark == Vertex.MarkType.S_TYPE) {
                        var ourOrigin = getOrigin(edge.from.getRootContainer());
                        var foreignOrigin = getOrigin(edge.to.getRootContainer());

                        if (ourOrigin != foreignOrigin) {
                            // Path found

                            var ourBranch = edgeBacktrackToOrigin(edge.from.getRootContainer());
                            var foreignBranch = edgeBacktrackToOrigin(edge.to.getRootContainer());

                            invertPath(ourBranch, edge.from);
                            invertPath(foreignBranch, edge.to);

                            edge.from.setPair(edge.to);
                            edge.to.setPair(edge.from);

                            result = GrowingResult.PATH_FOUND;
                            // Since the path found, we need not to proceed with other S_TYPEs
                            break;
                        } else {
                            // Blossom found
                            var commonBase = getCommonBase(edge.from.getRootContainer(),
                                    edge.to.getRootContainer());
                            var firstPart = backtrackToGivenVertex(edge.from.getRootContainer(), commonBase);
                            var secondPart = backtrackToGivenVertex(edge.to.getRootContainer(), commonBase);

                            var newBlossom = blossomize(firstPart, secondPart, edge, commonBase);
                            // Add future STYPEs of blossom which hasn't been previously seen
                            searchQueue.addAll(new ArrayList<>(newBlossom.getAllVertices()));
                        }
                    }
                }

                // Since the path found, we need not to proceed with other S_TYPEs
                if (result == GrowingResult.PATH_FOUND) {
                    break;
                }
            }

            // If the path found, we end this stage, but not trying to adjust weights.
            if (result == GrowingResult.PATH_FOUND)
                break;

            // Now we adjust weights
            {
                double delta1 = Double.POSITIVE_INFINITY;
                for (var vertex : vertices) {
                    // Minimum over all real S_TYPEs
                    if (vertex.isReal() && vertex.getMark() == Vertex.MarkType.S_TYPE)
                        delta1 = Math.min(delta1, vertex.getVariable());
                }

                double delta2 = Double.POSITIVE_INFINITY;
                for (var vertexU : vertices) {
                    // Edge must go from real S_TYPE
                    if (!vertexU.isReal() || vertexU.getMark() != Vertex.MarkType.S_TYPE)
                        continue;
                    for (var edge : vertexU.getEdges()) {
                        var vertexV = edge.to;
                        // Edge must go to real unmarked vertex
                        if (vertexV.getMark() == Vertex.MarkType.UNMARKED) {
                            delta2 = Math.min(delta2, vertexU.getVariable() + vertexV.getVariable() - edge.weight);
                        }
                    }
                }

                double delta3 = Double.POSITIVE_INFINITY;
                for (var vertexU : vertices) {
                    // Edge must go from real S_TYPE
                    if (!vertexU.isReal() || vertexU.getMark() != Vertex.MarkType.S_TYPE)
                        continue;
                    for (var edge : vertexU.getEdges()) {
                        var vertexV = edge.to;
                        // Edge must go to another root vertex and the vertex must be S_TYPE
                        if (vertexV.getRootContainer() != vertexU.getRootContainer() &&
                                vertexV.getMark() == Vertex.MarkType.S_TYPE) {
                            delta3 = Math.min(delta3, (vertexU.getVariable() + vertexV.getVariable() - edge.weight) / 2);
                        }
                    }
                }

                double delta4 = Double.POSITIVE_INFINITY;
                for (var vertex : vertices) {
                    // Minimum over all root T_TYPE blossoms
                    if (!vertex.isRootVertex() || vertex.isReal() || vertex.getMark() != Vertex.MarkType.T_TYPE)
                        continue;
                    var blossom = (Blossom) vertex;
                    delta4 = Math.min(delta4, blossom.getBlossomVariable() / 2);
                }

                double delta = Collections.min(Arrays.asList(delta1, delta2, delta3, delta4));

                for (var vertex : vertices) {
                    // do nothing for unmarked guys
                    if (vertex.getMark() == Vertex.MarkType.UNMARKED)
                        continue;

                    // +- delta for real vertex
                    if (vertex.isReal()) {
                        // Add delta for T_TYPE and subtract otherwise
                        var valueToAdd = vertex.getMark() == Vertex.MarkType.T_TYPE ? delta : -delta;
                        vertex.setVariable(vertex.getVariable() + valueToAdd);
                    }
                    // -+ 2delta for root blossom
                    else if (vertex.isRootVertex()) {
                        // Add delta for S_TYPE and subtract otherwise
                        var valueToAdd = vertex.getMark() == Vertex.MarkType.S_TYPE ? 2 * delta : -2 * delta;
                        var blossom = (Blossom) vertex;
                        blossom.setBlossomVariable(blossom.getBlossomVariable() + valueToAdd);
                    }
                    // nothing for others
                }

                if (delta == delta1) {
                    break;
                }

                // Dissolve (with extra steps) T_TYPE blossoms with zero variable
                for (Iterator<Vertex> iterator = vertices.iterator(); iterator.hasNext(); ) {
                    Vertex vertex = iterator.next();
                    if (vertex.isReal() || !vertex.isRootVertex() || vertex.getMark() != Vertex.MarkType.T_TYPE)
                        continue;
                    var blossom = (Blossom) vertex;
                    if (blossom.getBlossomVariable() > 0)
                        continue;
                    blossom.smartTDissolve();
                    for (var blossomVertex: blossom.getCycle()) {
                        if (blossomVertex.getMark() == Vertex.MarkType.S_TYPE)
                            searchQueue.addAll(blossomVertex.getAllVertices());
                    }
                    iterator.remove();
                }
            }
        }


        // Remove all blossoms which have zero variable
        var dissolveQueue = vertices.stream()
                .filter(vertex -> !vertex.isReal() && vertex.isRootVertex())
                .map(vertex -> (Blossom) vertex)
                .collect(Collectors.toCollection(ArrayDeque::new));

        while (!dissolveQueue.isEmpty()) {
            var current = dissolveQueue.poll();
            // If the blossom is not eligible for dissolving
            if (current.getBlossomVariable() > 0) {
//                throw new RuntimeException("All graphs are unweighted.");
                continue;
            }

//            System.out.println("Dissolve " + current.toString());
            current.dissolve();
            vertices.remove(current);
            dissolveQueue.addAll(current.getCycle().stream()
                    .filter(vertex -> !vertex.isReal() && vertex.isRootVertex())
                    .map(vertex -> (Blossom) vertex)
                    .collect(Collectors.toList())
            );
        }

        // For each root vertex we clear the tree edge
        for (var vertex : vertices) {
            if (!vertex.isRootVertex()) {
//                throw new RuntimeException("All graphs are unweighted.");
                continue;
            }
            vertex.setParentEdge(null);
        }


        return result;
    }

    private void invertPath(List<VertexPair> path, RealVertex first) {
        // The path must connect two S_TYPEs
        // If the path is empty, we work with inner part only
        if (path.size() == 0) {
            var vertex = first.getRootContainer();
            // Empty path means we're dealing with S_TYPE, then first isn't base
            vertex.invertPath(vertex.getRealBase(), first);

            return;
        }

        // These are the real vertices which is connected by the path's edges
        // But inside blossom we must work with its subvertices only
        var exitPoint = first;
        var entryPoint = path.get(0).to;
        for (int i = 0; ; i++) {
            // Take the blossom subvertex
            var vertex = exitPoint.getRootContainer();
            // First we work inside it
            vertex.invertPath(entryPoint, exitPoint);

            if (i == path.size())
                break;

            exitPoint = path.get(i).from;
            if (i < path.size() - 1) {
                entryPoint = path.get(i + 1).to;
            }
            else {
                entryPoint = path.get(i).from.getRootContainer().getRealBase();
            }
        }
        for (int i = 0; i < path.size(); i++) {

            // Now we invert the path
            if (i % 2 == 1) {
                var edge = path.get(i);
                edge.from.setPair(edge.to);
                edge.to.setPair(edge.from);
            }
        }
    }

    private List<VertexPair> edgeBacktrackToOrigin(Vertex start) {
        // From S_TYPE only
        var edges = new ArrayList<VertexPair>();
        while (start.getPair().isPresent()) {
            // Add pair edge which is connecting start's base and paired vertex
            edges.add(new VertexPair(start.getPair().get(), start.getRealBase()));

            var tVertex = start.getPair().get().getRootContainer();
            if (tVertex.getParentEdge().isEmpty()) {
                throw new RuntimeException("T_TYPE must have parent edge.");
            }

            // Add parent edge which is directly stored in the T_TYPE
            var edge = tVertex.getParentEdge().get();
            edges.add(new VertexPair(edge.from, edge.to));

            start = edge.from.getRootContainer();
        }
        return edges;
    }

    private Blossom blossomize(List<Vertex> firstPart, List<Vertex> secondPart, Edge triggeringEdge, Vertex commonBase) {
        var newBlossom = new Blossom(firstPart, secondPart, triggeringEdge, commonBase);
        newBlossom.setMark(Vertex.MarkType.S_TYPE);
        vertices.add(newBlossom);
        return newBlossom;
    }

    private List<Vertex> backtrackToGivenVertex(Vertex start, Vertex base) {
        // Backtrack with base excluded
        // From S_TYPE only (base shall be S_TYPE as well)

        var path = new ArrayList<Vertex>();

        while (start != base) {
            path.add(start);

            if (start.getPair().isEmpty()) {
                throw new RuntimeException("Base is unreachable.");
            }
            var tVertex = start.getPair().get().getRootContainer();
            if (tVertex.getParentEdge().isEmpty()) {
                throw new RuntimeException("T_TYPE must have parent edge.");
            }
            start = tVertex.getParentEdge().get().from.getRootContainer();
        }
        return path;
    }

    private Vertex getOrigin(Vertex start) {
        // From S_TYPE only (origin shall be S_TYPE as well)
        var path = inclusiveBacktrackToOrigin(start);
        return path.get(path.size() - 1);
    }

    private List<Vertex> inclusiveBacktrackToOrigin(Vertex start) {
        // Backtrack with origin included
        // From S_TYPE only (origin shall be S_TYPE as well)
        assert start.getMark() == Vertex.MarkType.S_TYPE;

        var path = new ArrayList<Vertex>();
        path.add(start);

        // Until we've reached origin which is the S_TYPE without pair edge
        while (start.getPair().isPresent()) {
            // Jump back via pair edge
            var tVertex = start.getPair().get().getRootContainer();
            if (tVertex.getParentEdge().isEmpty()) {
                throw new RuntimeException("T_TYPE must have parent edge.");
            }
            // Jump back via parent edge
            if (start == tVertex.getParentEdge().get().from.getRootContainer()) {
                throw new RuntimeException("Loop.");
            }
            start = tVertex.getParentEdge().get().from.getRootContainer();
            path.add(start);
        }
        return path;
    }

    private Vertex getCommonBase(Vertex u, Vertex v) {
        // From S_TYPE only (common base shall be S_TYPE as well)
        var uPath = inclusiveBacktrackToOrigin(u);
        var vPath = inclusiveBacktrackToOrigin(v);

        Collections.reverse(uPath);
        Collections.reverse(vPath);

        int i = 0;
        for (; i < Math.min(uPath.size(), vPath.size()) && uPath.get(i) == vPath.get(i); i++);

        if (i == 0) {
            throw new RuntimeException("No common base found.");
        }
        return uPath.get(i - 1);
    }

    public List<Edge> getCurrentMatching() {
        return allEdges().stream()
                .filter(edge -> edge.to.getPair().isPresent() && edge.to.getPair().get() == edge.from)
                .distinct()
                .collect(Collectors.toList());
    }

    public org.jgrapht.Graph<Integer, DefaultEdge> toJgraphtGraph() {
        DefaultUndirectedGraph<Integer, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
        for (var vertex: vertices) {
            if (!vertex.isReal())
                continue;
            graph.addVertex(((RealVertex)vertex).getId());
        }
        for (var vertex: vertices) {
            if (!vertex.isReal())
                continue;
            for (var edge: vertex._edgesFrom) {
                graph.addEdge(edge.from.getId(), edge.to.getId());
            }
        }
        return graph;
    }

    public enum GrowingResult {
        PATH_FOUND,
        OPTIMAL_TREE
    }

    @Override
    public String toString() {
        return new HashSet<Edge>(allEdges()).stream()
                .map(edge -> "(" + edge.from.getId() + "; " + edge.to.getId() + "; " + edge.weight + ")")
                .collect(Collectors.joining("\n"));
    }
}

