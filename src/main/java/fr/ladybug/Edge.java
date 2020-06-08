package fr.ladybug;

import java.util.Objects;

public class Edge {
    double weight;
    RealVertex from, to;
    private Edge rev = null;

    public Edge(RealVertex from, RealVertex to, double weight) {
        this.from = from;
        this.to = to;
        this.weight = weight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge edge = (Edge) o;
        return ((Objects.equals(from, edge.from) && Objects.equals(to, edge.to)) ||
                (Objects.equals(from, edge.to) && Objects.equals(to, edge.from)));
    }

    @Override
    public int hashCode() {
        var b = from.hashCode() < to.hashCode();
        var hFrom = b ? from : to;
        var hTo = b ? to : from;
        return Objects.hash(hFrom, hTo);
    }

    public void setRev(Edge rev) {
        if (this.rev == null)
            this.rev = rev;
        else
            throw new IllegalStateException("Rev has already been set.");
    }

    public Edge rev() {
        return rev;
    }

    public double getWeight() {
        return weight;
    }

    public RealVertex from() {
        return from;
    }

    public RealVertex to() {
        return to;
    }
}
