package com.example.si.IMG_PROCESSING;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;

public class ImgObj_Para {
    public int[][] gray_img;
    public int[][][] tar_img;
    public int width;//图像宽
    public int height;
    int size;
    public Bitmap EdgeImage;
    int[] ThHighLow = new int[2];  //存放高低阈值，0-高阈值，1-低阈值；
    ArrayList<Point> circles;           //存放找到的圆
    //ArrayList<Point> line = new ArrayList<>();           //存放找到的圆
    int circle_max;                    //输出最多圆的个数

    public ImgObj_Para(Bitmap bitmap){
        width = bitmap.getWidth();
        height = bitmap.getHeight();
        size = width*height;
        tar_img = new int[2][height][width];
        EdgeImage = null;
        circles = new ArrayList<>();
        circle_max = 5;
    }
    public void colorToGray2D(Bitmap myBitmap) {
        //myimage[0][][]为透明度
        //myimage[1][][]为灰度
        width = myBitmap.getWidth();
        height = myBitmap.getHeight();
        size = width*height;
        //int[][][] myimage = new int[2][width][height];  //存储透明度和灰度图像
        //int[][][] myimage = new int[2][height][width];
        int color;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                color = myBitmap.getPixel(i,j);
                tar_img[0][j][i] = Color.alpha(color);
                tar_img[1][j][i] = (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114 + 500) / 1000;
            }
        }
        gray_img = tar_img[1];
    }

    //二值图反转
    public int[][] binaryReverse(int[][] binaryimage) {
        int width = binaryimage.length;
        int height = binaryimage[0].length;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (binaryimage[i][j] == 0) {
                    binaryimage[i][j] = 255;
                } else {
                    binaryimage[i][j] = 0;
                }
            }
        }
        return binaryimage;
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
