package fr.ladybug;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Blossom extends Vertex {
    private double blossomVariable = 0;
    private Vertex base;
    private final List<Vertex> cycle;

    public Blossom(List<Vertex> leftBranch, List<Vertex> rightBranch, Edge triggeringEdge, Vertex base) {
        this.base = base;

        // Set parent edge for ends of the branches
        triggeringEdge.to.getRootContainer().setParentEdge(triggeringEdge);
        triggeringEdge.from.getRootContainer().setParentEdge(triggeringEdge.rev());

        // Set parent edge for S_TYPEs on the branches
        for (int i = 1; i < leftBranch.size(); i++) {
            if (leftBranch.get(i - 1).getPair().isEmpty()) {
                throw new RuntimeException("No match for an S_TYPE on the branch.");
            }
            var paired = leftBranch.get(i - 1).getPair().get().getRootContainer();
            if (paired.getParentEdge().isEmpty()) {
                throw new RuntimeException("No parent for a T_TYPE on the branch.");
            }
            leftBranch.get(i).setParentEdge(paired.getParentEdge().get().rev());
        }

        for (int i = 1; i < rightBranch.size(); i++) {
            if (rightBranch.get(i - 1).getPair().isEmpty()) {
                throw new RuntimeException("No match for an S_TYPE on the branch.");
            }
            var paired = rightBranch.get(i - 1).getPair().get().getRootContainer();
            if (paired.getParentEdge().isEmpty()) {
                throw new RuntimeException("No parent for a T_TYPE on the branch.");
            }
            rightBranch.get(i).setParentEdge(paired.getParentEdge().get().rev());
        }

        // We could accidently set parent edge for base, now we reset it
        base.setParentEdge(null);

        Collections.reverse(rightBranch);
        cycle = Stream.of(leftBranch, Arrays.asList(base), rightBranch)
                .flatMap(Collection::stream)
                .flatMap(vertex -> vertex.getPair().isPresent() && vertex != base ?
                        (Stream.of(vertex, vertex.getPair().get().getRootContainer())) : Stream.of(vertex))
                .collect(Collectors.toList());

        for (var vertex: cycle) {
            vertex.setContainer(this);
        }

//        System.out.println(String.format("Created blossom of a size: %d", cycle.size()));
//        System.out.println(cycle.stream().map(Object::toString).collect(Collectors.joining(", ")));
    }

    @Override
    public boolean isReal() {
        return false;
    }

    @Override
    public Optional<RealVertex> getPair() {
        return base.getPair();
    }

    @Override
    public void setPair(RealVertex pair) {
        base.setPair(pair);
    }

    @Override
    public Optional<Edge> getParentEdge() {
        return parentEdge == null ? Optional.empty() : Optional.of(parentEdge);
    }

    @Override
    public void setParentEdge(Edge parentEdge) {
        this.parentEdge = parentEdge;
    }

    @Override
    public RealVertex getRealBase() {
        return base.getRealBase();
    }

    public Vertex getBase() {
        return base;
    }

    @Override
    public List<RealVertex> getAllVertices() {
        return cycle.stream().flatMap(vertex -> vertex.getAllVertices().stream())
                .collect(Collectors.toList());
    }

    @Override
    public List<Edge> getEdges() {
        return cycle.stream().flatMap(vertex -> vertex.getEdges().stream())
                .collect(Collectors.toList());
    }

    @Override
    public void setMark(MarkType mark) {
        this.mark = mark;
        for (var vertex : cycle) {
            vertex.setMark(mark);
        }
    }

    public Vertex getRelativeRootContainer(Vertex v) {
        if (v.getContainer() == this)
            return v;
        if (v == v.getContainer()) {
            throw new RuntimeException("Loop.");
        }
        return getRelativeRootContainer(v.getContainer());
    }

    public List<VertexPair> edgeBacktrackToEntryPoint(Vertex start, Vertex entryPoint) {
        var edges = new ArrayList<VertexPair>();

        while (start != entryPoint) {
            if (start.getPair().isEmpty()) {
                throw new RuntimeException("Entry point is unreachable.");
            }
            var tRealVertex = start.getPair().get();
            edges.add(new VertexPair(tRealVertex, start.getRealBase()));
            var tVertex = getRelativeRootContainer(tRealVertex);
            if (tVertex.getParentEdge().isEmpty()) {
                throw new RuntimeException("T_TYPE must have parent edge.");
            }
            var edge = tVertex.getParentEdge().get();
            edges.add(new VertexPair(edge.from, edge.to));
            start = getRelativeRootContainer(edge.from);
        }

        return edges;
    }

    @Override
    public void invertPath(RealVertex rEntryPoint, RealVertex rExitPoint) {
        // If we backtrack out not through base, inverse the direction
        if (rExitPoint == getRealBase()) {
            var tmp = rExitPoint;
            rExitPoint = rEntryPoint;
            rEntryPoint = tmp;
        }

        // The vertices are strictly subvertices of a given blossom
        var entryPoint = getRelativeRootContainer(rEntryPoint);
        var exitPoint = getRelativeRootContainer(rExitPoint);

        var path = edgeBacktrackToEntryPoint(exitPoint, entryPoint);

        if (path.size() == 0) {
            entryPoint.invertPath(rEntryPoint, rExitPoint);
            return;
        }

        var theEdges = new ArrayList<Edge>();

        for (var vertex: this.cycle) {
            if (vertex.getParentEdge().isPresent() &&
                    getRelativeRootContainer(vertex.getParentEdge().get().from) ==
                            entryPoint) {
                theEdges.add(vertex.getParentEdge().get());
            }
        }

        if (theEdges.size() != 2) {
            for (var vertex: this.cycle) {
                if (vertex.getParentEdge().isPresent() &&
                        getRelativeRootContainer(vertex.getParentEdge().get().from) ==
                                entryPoint) {
                    theEdges.add(vertex.getParentEdge().get());
                }
            }
            throw new RuntimeException("Inconsistent number of parent edges for the old base");
        }

        // These are the real vertices which is connected by the path's edges
        // But inside blossom we must work with its subvertices only
        var currentExitPoint = rExitPoint;
        var currentEntryPoint = path.get(0).to;
        for (int i = 0; ; i++) {
            // Take the blossom subvertex
            var vertex = getRelativeRootContainer(currentEntryPoint);
            // First we work inside it
            vertex.invertPath(currentEntryPoint, currentExitPoint);

            if (i == path.size())
                break;

            if (i < path.size() - 1) {
                currentEntryPoint = path.get(i + 1).to;
            }
            else {
                currentEntryPoint = getRelativeRootContainer(path.get(i).from).getRealBase();
            }
            currentExitPoint = path.get(i).from;
        }

        currentExitPoint = rExitPoint;
        currentEntryPoint = path.get(0).to;
        for (int i = 0; i < path.size(); i++) {
            // Now we invert the path
            var edge = path.get(i).toEdge();
            if (i % 2 == 1) {
                edge.from.setPair(edge.to);
                edge.to.setPair(edge.from);
            }
            else {
                var paired = getRelativeRootContainer(edge.from);
                paired.setParentEdge(edge.rev());
                getRelativeRootContainer(currentEntryPoint).setParentEdge(edge);
            }

            currentExitPoint = path.get(i).from;
            if (i < path.size() - 1) {
                currentEntryPoint = path.get(i + 1).to;
            }
            else {
                currentEntryPoint = getRelativeRootContainer(path.get(i).from).getRealBase();
            }
        }

        entryPoint = getRelativeRootContainer(rEntryPoint);
        exitPoint = getRelativeRootContainer(rExitPoint);

        if (entryPoint.getPair().isEmpty()) {
            throw new RuntimeException("No pair for base.");
        }
        var theEdge = getRelativeRootContainer(theEdges.get(0).to) ==
                getRelativeRootContainer(entryPoint.getPair().get()) ? theEdges.get(1) : theEdges.get(0);

        exitPoint.setParentEdge(null);
        entryPoint.setParentEdge(theEdge.rev());
        this.base = exitPoint;
    }

    @Override
    public String toString() {
        return "Blossom (" + cycle.stream().map(Objects::toString).collect(Collectors.joining("; ")) + ")";
    }

    public double getBlossomVariable() {
        return blossomVariable;
    }

    public void setBlossomVariable(double blossomVariable) {
        this.blossomVariable = blossomVariable;
    }

    public List<Vertex> getCycle() {
        return cycle;
    }

    public void dissolve() {
        for (var vertex: cycle) {
            vertex.resetContainer();
        }
    }

    public void smartTDissolve() {
        // Only if this is T_TYPE
        // Unmark every vertex. Those which will stay unmarked after all, will be treated specially
        this.setMark(MarkType.UNMARKED);

        if (this.getParentEdge().isEmpty()) {
            throw new RuntimeException("The T_TYPE must have parent edge.");
        }

        var exitPoint = getRelativeRootContainer(this.getParentEdge().get().to);
        var path = edgeBacktrackToEntryPoint(exitPoint, this.base);
        Collections.reverse(path);

        for (int i = 0; i < path.size(); i += 2) {
            VertexPair edge = path.get(i);
            var from = getRelativeRootContainer(edge.from);
            var to = getRelativeRootContainer(edge.to);

            from.setMark(MarkType.T_TYPE);
            to.setMark(MarkType.S_TYPE);

            if (to.getParentEdge().isEmpty()) {
                throw new RuntimeException("The blossom vertex must have parent edge.");
            }

            from.setParentEdge(to.getParentEdge().get().rev());
            to.setParentEdge(null);
        }

        exitPoint.setMark(MarkType.T_TYPE);
        exitPoint.setParentEdge(this.getParentEdge().get());

        // We remove parent edge for vertices which stayed unmarked after all
        for (var vertex: cycle) {
            vertex.resetContainer();
            if (vertex.getMark() == MarkType.UNMARKED)
                vertex.setParentEdge(null);
        }
    }
}