import jdk.incubator.mvt.ValueCapableClass;

@ValueCapableClass
public final class Interval {
    public final int l;
    public final int u;

    public Interval(int l, int u) {
        if (l > u) throw new IllegalArgumentException();
        this.l = l;
        this.u = u;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Interval that = (Interval) o;

        if (l != that.l) return false;
        return u == that.u;
    }

    @Override
    public int hashCode() {
        int result = l;
        result = 31 * result + u;
        return result;
    }
}
