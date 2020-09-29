package com.example.si.IMG_PROCESSING.CornerDetection;

public class RGB32f implements Cloneable{
    public float r;
    public float g;
    public float b;

    public RGB32f(){
        r = 0;
        g = 0;
        b = 0;
    }

    public RGB32f(XYZ a){
        r = (float) a.x;
        g = (float) a.y;
        b = (float) a.z;
    }

    @Override
    public RGB32f clone() throws CloneNotSupportedException {
        return (RGB32f) super.clone();
    }
}
