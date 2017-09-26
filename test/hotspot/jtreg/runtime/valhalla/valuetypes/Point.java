package runtime.valhalla.valuetypes;

public __ByValue final class Point {
    final int x;
    final int y;

    private Point() {
	x = 0;
	y = 0;
    }
    
    public int getX() { return x; }
    public int getY() { return y; }

    public boolean isSamePoint(Point that) {
        return this.getX() == that.getX() && this.getY() == that.getY();
    }

    public String toString() {
        return "Point: x=" + getX() + " y=" + getY();
    }

    __ValueFactory public static Point createPoint(int x, int y) {
        Point p = __MakeDefault Point();
	p.x =x;
	p.y = y;
	return p;
    }
}
