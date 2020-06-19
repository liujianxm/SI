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
    public Bitmap EdgeImage;
    public RoberEdgeDetect(int threshold){
        gradientThreshold = threshold;
    }
    public void readImage(Bitmap bitmap)throws IOException{
        width = bitmap.getWidth();
        height = bitmap.getHeight();
        size = width*height;
        //获取图像像素值
        int imageData[] = new int[size];
        int count = 0;
        for(int i = 0;i<height;i++){
            for(int j = 0;j<width;j++){
                imageData[count] = bitmap.getPixel(j,i);
                count++;
            }
        }
        graydata = new int[size];
        for(int i=0;i<imageData.length;i++){
            graydata[i] = (imageData[i] & 0xff0000) >> 16;
        }
    }

    public Bitmap createEdgeImage(){
        int[] colors = new int[size];
        float[] gradient = gradientM();
        float maxGradient = gradient[0];
        for(int i=1;i<gradient.length;++i){
            if(gradient[i] > maxGradient) maxGradient = gradient[i];
        }
        float scaleFactor = 255.0f/maxGradient;
        int[][] cc = new int[width][height];
        if(gradientThreshold >= 0){
            for(int y=1;y<height-1;++y)
                for(int x=1;x<(width-1);++x)
                    if(Math.round(scaleFactor*gradient[y*width + x]) >= gradientThreshold){
                        cc[x][y] = Color.BLUE;
                    }else{
                        cc[x][y] = Color.parseColor("#00000000");
                    }
        }
        int count = 0;
        for(int i = 0;i<height;i++)
            for(int j = 0;j<width;j++){
                colors[count] = cc[j][i];
                count++;
            }
        Bitmap bitmap = Bitmap.createBitmap(colors,width,height,Bitmap.Config.ARGB_4444);
        return bitmap;
    }
    //得到点（x,y）处的灰度值
    public int getGreyPoint(int x, int y){
        return graydata[y*width + x];
    }

    //算子计算 图像每个像素点 的 梯度大小
    protected float[] gradientM(){
        float[] mag = new float[size];
        int gx, gy;
        for(int y = 1;y < height - 1;++y)
            for(int x= 1;x < width - 1;++x){
                gx = GradientX(x,y);
                mag[y*width + x] = (float)(Math.abs(gx));
            }
        return mag;
    }

    protected final int GradientX(int x, int y){
        return getGreyPoint(x,y) - getGreyPoint(x+1,y+1)
                + getGreyPoint(x+1,y) - getGreyPoint(x,y+1);
    }
}
