package fr.ladybug;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class Vertex {
    protected MarkType mark = MarkType.UNMARKED;
    protected Edge parentEdge = null;

    protected Vertex container;

    protected List<Edge> _edgesFrom;

    private double vertexVariable = 0;

    public Vertex() {
        container = this;
        _edgesFrom = new ArrayList<>();
    }

    public abstract Optional<RealVertex> getPair();
    public abstract void setPair(RealVertex pair);

    public abstract Optional<Edge> getParentEdge();
    public abstract void setParentEdge(Edge parentEdge);

    public abstract List<RealVertex> getAllVertices();
    public abstract List<Edge> getEdges();
    public abstract boolean isReal();

    public double getVariable() {
        return vertexVariable;
    }
    public void setVariable(double value) {
        vertexVariable = value;
    }

    public abstract void setMark(MarkType mark);
    public MarkType getMark() { return mark; }

    public void setContainer(Vertex container) { this.container = container; }
    public void resetContainer() { this.container = this; }
    public Vertex getContainer() {
        return container;
    }
    public boolean isRootVertex() { return container == this; };

    public Vertex getRootContainer() {
        if (isRootVertex())
            return this;
        return getContainer().getRootContainer();
    }

    public abstract RealVertex getRealBase();

    public abstract void invertPath(RealVertex u, RealVertex v);

    public enum MarkType {
        S_TYPE,
        T_TYPE,
        UNMARKED;
    }
}
