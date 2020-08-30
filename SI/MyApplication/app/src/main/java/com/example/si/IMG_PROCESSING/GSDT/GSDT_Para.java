package com.example.si.IMG_PROCESSING.GSDT;

import com.example.si.IMG_PROCESSING.CircleDetect.Point;

import java.util.ArrayList;

public class GSDT_Para {
    public int cnn_type;
    public ArrayList<Point> centers = new ArrayList<>();
    public float[][] phi;
    public float[][] phi_1;
    public int bkg_thresh;
    public GSDT_Para(){
        cnn_type = 3;
        phi = null;
        phi_1 = null;
        bkg_thresh = 0;
    }




}
