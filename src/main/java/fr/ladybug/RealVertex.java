package fr.ladybug;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class RealVertex extends Vertex {
    private RealVertex pair = null;
    public static int global_id = 0;
    private final int id;

    public RealVertex() {
        id = global_id++;
    }

    @Override
    public boolean isReal() {
        return true;
    }

    @Override
    public Optional<RealVertex> getPair() {
        return pair == null ? Optional.empty() : Optional.of(pair);
    }

    @Override
    public void setPair(RealVertex pair) {
        this.pair = pair;
    }

    @Override
    public RealVertex getRealBase() {
        return this;
    }

    @Override
    public List<RealVertex> getAllVertices() {
        return Arrays.asList(this);
    }

    @Override
    public List<Edge> getEdges() {
        return _edgesFrom;
    }

    @Override
    public void setMark(MarkType mark) {
        this.mark = mark;
    }

    @Override
    public void setParentEdge(Edge parentEdge) {
        this.parentEdge = parentEdge;
    }

    @Override
    public Optional<Edge> getParentEdge() {
        return parentEdge == null ? Optional.empty() : Optional.of(parentEdge);
    }

    public void addEdge(Edge edge) {
        _edgesFrom.add(edge);
    }
    public Edge getEdgeTo(RealVertex to) {
        for (var edge : _edgesFrom) {
            if (edge.to == to)
                return edge;
        }
        return null;
    }

    @Override
    public void invertPath(RealVertex u, RealVertex v) {
        if (u != v) {
            throw new RuntimeException("Trying to not trivially invert path inside a vertex.");
        }
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Vertex (" + String.valueOf(id) + ")";
    }
}