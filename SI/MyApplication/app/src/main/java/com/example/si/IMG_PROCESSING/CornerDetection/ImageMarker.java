package com.example.si.IMG_PROCESSING.CornerDetection;


// .marker marker files

public class ImageMarker extends BasicSurfObj {
    public int type;			// 0-pxUnknown, 1-pxLocaNotUseful, 2-pxLocaUseful, 3-pxLocaUnsure, 4-pxTemp
    public int shape;			// 0-pxUnset, 1-pxSphere, 2-pxCube, 3-pxCircleX, 4-pxCircleY, 5-pxCircleZ,
                                // 6-pxSquareX, 7-pxSquareY, 8-pxSquareZ, 9-pxLineX, 10-pxLineY, 11-pxLineZ,
                                // 12-pxTriangle, 13-pxDot;
    public double x, y, z;		        // point coordinates
    public float radius;

    public ImageMarker() {type=shape=0; radius=0.0f; x=y=z=0;}
    public ImageMarker(double x0, double y0, double z0) {type=shape=0; x=x0; y=y0; z=z0; radius=0;}
    public ImageMarker(double x0, double y0, double z0, int ty) {type = ty;  shape=0; x=x0; y=y0; z=z0; radius=0;}
    public ImageMarker(int t0, int s0, double x0, double y0, double z0, float r0) {type=t0; shape=s0; x=x0; y=y0; z=z0; radius=r0;}

    public XYZ getXYZ(){
        return new XYZ(x, y, z);
    }

}
