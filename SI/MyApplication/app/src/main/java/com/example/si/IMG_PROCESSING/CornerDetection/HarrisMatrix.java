package com.example.si.IMG_PROCESSING.CornerDetection;

public class HarrisMatrix {
    private double Ix;
    private double Iy;
    private double IxIy;
    private double r;
    private double max;
    private int x;
    private int y;

    public int getX() { return x; }

    public void setX(int inx) { x = inx; }

    public int getY() { return y; }

    public void setY(int iny) { y = iny; }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public HarrisMatrix()
    {
        max = 0; // always
    }

    public double getXGradient() {
        return Ix;
    }
    public void setXGradient(double ix) {
        Ix = ix;
    }
    public double getYGradient() {
        return Iy;
    }
    public void setYGradient(double iy) {
        Iy = iy;
    }

    public double getIxIy() {
        return IxIy;
    }

    public void setIxIy(double ixIy) {
        IxIy = ixIy;
    }

    public double getR() {
        return r;
    }

    public void setR(double r) {
        this.r = r;
    }

    public HarrisMatrix copy() {
        HarrisMatrix newHarrisMatrix = new HarrisMatrix();
        newHarrisMatrix.setXGradient(this.Ix);
        newHarrisMatrix.setYGradient(this.Iy);
        newHarrisMatrix.setIxIy(this.IxIy);
        newHarrisMatrix.setR(this.r);
        newHarrisMatrix.setMax(this.max);
        newHarrisMatrix.setX(this.x);
        newHarrisMatrix.setY(this.y);
        return newHarrisMatrix;
    }

    public int compareTo(HarrisMatrix h) {
        if (this.getR()-h.getR() > 0) {
            return 1;
        } else if (this.getR()-h.getR() < 0) {
            return -1;
        } else {
            return 0;
        }

    }
}
