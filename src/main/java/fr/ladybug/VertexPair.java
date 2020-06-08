package fr.ladybug;

public class VertexPair {
    public RealVertex from, to;
    public VertexPair(RealVertex from, RealVertex to) {
        this.from = from;
        this.to = to;
    }

    public Edge toEdge() {
        return from.getEdgeTo(to);
    }
}