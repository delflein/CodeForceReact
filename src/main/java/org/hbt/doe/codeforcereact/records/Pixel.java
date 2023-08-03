package org.hbt.doe.codeforcereact.records;

public record Pixel(int x, int y, int r, int g, int b) {
    boolean isSamePosition(Pixel other) {
        return x == other.x && y == other.y;
    }

    public boolean isSameColor(int rFrom, int gFrom, int bFrom) {
        return r == rFrom && g == gFrom && b == bFrom;
    }

    public Pixel recolor(int rTo, int gTo, int bTo) {
        return new Pixel(x, y, rTo, gTo, bTo);
    }

    public Pixel toTeamColor() {
        return recolor(240, 24, 236);
    }

    public Pixel move(int pixelOffset) {
        return new Pixel(x + pixelOffset, y, r, g, b);
    }

    public Pixel withPosition(int x, int y) {
        return new Pixel(x, y, r, g, b);
    }

    public boolean hasColor() {
        return r > 5 && g > 5 && b > 5;
    }
}
