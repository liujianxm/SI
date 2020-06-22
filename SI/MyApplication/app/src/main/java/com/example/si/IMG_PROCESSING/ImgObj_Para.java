package com.example.si.IMG_PROCESSING;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.RequiresApi;

public class ImgObj_Para {
    int[][] gray_img;
    int[][] temp_img;
    int[][][] tar_img;
    int width;//图像宽
    int height;
    int size;
    public Bitmap EdgeImage;

    int[] ThHighLow = new int[2];  //存放高低阈值，0-高阈值，1-低阈值；
    int[][] pnGradX;					//存放各点水平方向梯度值
    int[][] pnGradY;					//存放各点垂直方向梯度值
    int[][] pnGradXY;					//存放各点XY方向的综合梯度值
    double[][] pnGradTheta;                //存放各点的角度值

    public ImgObj_Para(Bitmap bitmap){
        width = bitmap.getWidth();
        height = bitmap.getHeight();
        size = width*height;
        temp_img = new int[height][width];
        pnGradX = new int[height][width];					//存放各点水平方向梯度值
        pnGradY = new int[height][width];					//存放各点垂直方向梯度值
        pnGradXY = new int[height][width];					//存放各点XY方向的综合梯度值
        pnGradTheta = new double[height][width];                //存放各点的角度值
        tar_img = new int[2][height][width];
        EdgeImage = null;
    }
    public void colorToGray2D(Bitmap myBitmap) {
        //myimage[0][][]为透明度
        //myimage[1][][]为灰度
        width = myBitmap.getWidth();
        height = myBitmap.getHeight();
        size = width*height;
        //int[][][] myimage = new int[2][width][height];  //存储透明度和灰度图像
        int[][][] myimage = new int[2][height][width];
        int color;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                color = myBitmap.getPixel(i,j);
                myimage[0][j][i] = Color.alpha(color);
                myimage[1][j][i] = (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114 + 500) / 1000;
            }
        }
        tar_img[0] = myimage[0];
        gray_img = myimage[1];
    }

    //灰度图转Bitmap
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public Bitmap gray2DToBitmap(int[][][] myimage, int width, int height) {
        Bitmap myBitmap = Bitmap.createBitmap( width, height, Bitmap.Config.ARGB_8888 );
        int color;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                color = Color.argb(myimage[0][j][i], myimage[1][j][i], myimage[1][j][i], myimage[1][j][i]);
                myBitmap.setPixel(i, j, color);
            }
        }
        return myBitmap;
    }

}
