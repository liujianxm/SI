package com.example.si.IMG_PROCESSING;

public class bwlable {
    int[][] image;
    //int[][] grayImage;
    //BufferedImage colorImage;
    int counter=1;

    public int getCounter() { return counter-1; }
    public int getimagepoint(int i, int j) { return image[i][j]; }

    public bwlable(int[][] grayImageData, int grayscale) {
        //识别白色点连通域,递归法
        // TODO Auto-generated constructor stub

        image = new int[grayImageData.length][grayImageData[1].length];
        //图像外圈边界为0，只需要在边界内处理即可
        System.out.println("+++");
        for (int i = 1; i < grayImageData.length-1; i++) {
            for (int j = 1; j < grayImageData[0].length-1; j++) {
                image[i][j] = grayImageData[i][j];
            }
        }
        System.out.println("+++");
        for (int i = 1; i < grayImageData.length-1; i++) {
            for (int j = 1; j < grayImageData[0].length-1; j++) {
                if (image[i][j] == grayscale-1) {
                    counter++;
                    dealBwlabe(i,j);
                }
            }
        }
        System.out.println(counter-1);

        //color();
        //dialog();

    }

    private void dealBwlabe(int i, int j) {
        // TODO Auto-generated method stub
        //上
        if (i-1 >= 0 && image[i-1][j] == 1) {
            image[i-1][j] = counter;
            dealBwlabe(i-1, j);
        }
        //左
        if (j-1 >= 0 &&image[i][j-1] == 1) {
            image[i][j-1] = counter;
            dealBwlabe(i, j-1);
        }
        //下
        if (i+1 < image.length && image[i+1][j] == 1) {
            image[i+1][j] = counter;
            dealBwlabe(i+1, j);
        }
        //右
        if (j+1 < image[0].length && image[i][j+1] == 1) {
            image[i][j+1] = counter;
            dealBwlabe(i, j+1);
        }

////八连通需要
//      //上左
//      if (image[i-1][j-1] == 1) {
//          image[i-1][j-1] = counter;
//          dealBwlabe(i-1, j-1);
//      }
//      //上右
//      if (image[i-1][j+1] == 1) {
//          image[i-1][j+1] = counter;
//          dealBwlabe(i-1, j+1);
//      }
//      //下左
//      if (image[i+1][j-1] == 1) {
//          image[i+1][j-1] = counter;
//          dealBwlabe(i+1, j-1);
//      }
//      //下右
//      if (image[i+1][j+1] == 1) {
//          image[i+1][j+1] = counter;
//          dealBwlabe(i+1, j+1);
//      }

    }




}
