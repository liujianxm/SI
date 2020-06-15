package com.example.si.IMG_PROCESSING;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.io.IOException;

public class RoberEdgeDetect {
    int width;//图像宽
    int height;
    int[] graydata;
    int size;
    int gradientThreshold = -1;
    public RoberEdgeDetect(int threshold){
        gradientThreshold = threshold;
    }


}
