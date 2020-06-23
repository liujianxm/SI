package com.example.si.IMG_PROCESSING;

import android.graphics.Bitmap;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.BitSet;

public class FourAreaLabel {
    int[][] binaryimage; //二值边界图，边界为grayscale-1，背景为0
    int width;
    int height;
    int count;
    int[][] label; //连通域识别
    int[][] label0; //像素被判断方向次数
    int[][] tempstore;//0:i,1:j//,2:左,3:右,4:上,5:下
    int tempnum;

    public FourAreaLabel(int[][] inbinaryimage) {
        binaryimage = inbinaryimage;
        width = inbinaryimage.length;
        height = inbinaryimage[0].length;
        count = 0;
        label = new int[width][height]; //连通域识别
        label0 = new int[width][height]; //像素被判断方向次数
        tempstore = new int[2][width*height];//0:i,1:j
        tempnum = 0;
        System.out.println("111");
        binaryAreaLabel();
        System.out.println("111");
    }

    public int getCount() { return count; }

    //public int[][] getLabel() { return label; }

    public int getLabelPoint(int i, int j) { return label[i][j]; }

    //向左
    private int left(int ii, int jj, int count) {
        if (ii - 1 >= 0 && label[ii - 1][jj] == 0 && binaryimage[ii - 1][jj] == 0) {
            label[ii - 1][jj] = count;
            tempstore[0][tempnum] = ii;
            tempstore[1][tempnum] = jj;
            //tempstore[2][tempnum] = 1;
            tempnum++;
            ii--;
        }
        //System.out.println("label0 = "+label0[ii][jj]);
        label0[ii][jj]++;
        //System.out.println("label0 = "+label0[ii][jj]);
        //System.out.println("ii = "+ii);
        return ii;
    }

    //向上
    private int up(int ii, int jj, int count) {
        if (jj - 1 >= 0 && label[ii][jj-1] == 0 && binaryimage[ii][jj-1] == 0) {
            label[ii][jj-1] = count;
            tempstore[0][tempnum] = ii;
            tempstore[1][tempnum] = jj;
            //tempstore[2][tempnum] = 1;
            tempnum++;

            jj--;
        }
        label0[ii][jj]++;
        return jj;
    }

    //向右
    private int right(int ii, int jj, int count) {
        if (ii + 1 < width && label[ii+1][jj] == 0 && binaryimage[ii+1][jj] == 0) {
            label[ii+1][jj] = count;
            tempstore[0][tempnum] = ii;
            tempstore[1][tempnum] = jj;
            //tempstore[2][tempnum] = 1;
            tempnum++;

            ii++;
        }
        label0[ii][jj]++;
        return ii;
    }

    //向下
    private int down(int ii, int jj, int count) {
        if (jj + 1 < height && label[ii][jj+1] == 0 && binaryimage[ii][jj+1] == 0) {
            //当向下遍历时，当前点其他三方向已遍历完成，向下遍历时当前像素四方向遍历完成，无需加入未处理完成数组
            label[ii][jj+1] = count;
            //tempstore[0][tempnum] = ii;
            //tempstore[1][tempnum] = jj;
            //tempstore[2][tempnum] = 1;
            //tempnum++;

            jj++;
        }
        label0[ii][jj]++;
        return jj;
    }


    //四邻域
    //像素点领域判断优先：左>上>右>下
    //识别黑色点连通域
    //非递归法
    private void binaryAreaLabel() {
        //int count = 0;
        boolean flag;


        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (label[i][j] == 0 && binaryimage[i][j] == 0) {
                    flag = true; //标志当前连通域是否遍历完成
                    count++;
                    //System.out.println("count = "+count);
                    label[i][j] = count;
                    int ii=i;
                    int jj=j;
                    //binaryimage[i][j] = 255;  //grayscale-1
                    while (flag) {
                        switch (label0[ii][jj]) {
                            case 0:
                                //向左
                                ii=left(ii, jj, count);
                                //--;
                                //System.out.println("count = "+count+"ii,jj = "+ii+","+jj);
                                break;
                            case 1:
                                //向上
                                jj=up(ii, jj, count);
                                //--;
                                //System.out.println("2");
                                break;
                            case 2:
                                //向右
                                ii=right(ii, jj, count);
                                //++;
                                //System.out.println("3");
                                break;
                            case 3:
                                //向下
                                jj=down(ii, jj, count);
                                //++;
                                //System.out.println("4");
                                break;
                            default:
                                //返回未完全处理像素点
                                //tempstore中只有label0[i][j]为1,2,3三种
                                if (tempnum == 0) {
                                    flag = false;
                                } else {
                                    //System.out.println("5");
                                    tempnum--;
                                    ii = tempstore[0][tempnum];
                                    jj = tempstore[1][tempnum];


                                }
                                break;
                        }
                    }
                }
            }
        }
    }

    public static int[][] myOTSU(int[][] grayimage, int width, int height, int grayscale) {
        int[] pixelCount = new int[grayscale];
        float[] pixelPro = new float[grayscale];
        int i, j, pixelSum = width * height, threshold = 0;
        float w0, w1, u0tmp, u1tmp, u0, u1, deltaTmp, deltaMax = 0;

        //统计每个灰度级中像素的个数
        for(i = 0; i < width; i++) {
            for(j = 0;j < height;j++) {
                pixelCount[grayimage[i][j]]++;
            }
        }

        //计算每个灰度级的像素数目占整幅图像的比例
        for(i = 0; i < grayscale; i++) {
            pixelPro[i] = (float)pixelCount[i] / pixelSum;
        }
        //遍历所有从0到255灰度级的阈值分割条件，测试哪一个的类间方差最大
        for(i = 0; i < grayscale; i++) {
            w0 = w1 = u0tmp = u1tmp = u0 = u1 = deltaTmp = 0;
            for(j = 0; j < grayscale; j++) {
                if(j <= i) {  //背景部分
                    w0 += pixelPro[j];
                    u0tmp += j * pixelPro[j];
                } else {  //前景部分
                    w1 += pixelPro[j];
                    u1tmp += j * pixelPro[j];
                }
            }
            u0 = u0tmp / w0;
            u1 = u1tmp / w1;
            deltaTmp = (float)(w0 *w1* Math.pow((u0 - u1), 2)) ;
            if(deltaTmp > deltaMax) {
                deltaMax = deltaTmp;
                threshold = i;
            }
        }
        //根据threshold对图像进行二值化
        for(i = 0; i < width; i++) {
            for(j = 0; j < height; j++) {
                if (grayimage[i][j] < threshold) {
                    grayimage[i][j] = 0;  //忘记哪个是零哪个是一
                } else {
                    grayimage[i][j] = grayscale - 1;
                }
            }
        }

        return grayimage;

    }

    //连通域标记函数
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static Bitmap AreaLabel(Bitmap BinaryImage) {
        ImgObj_Para imgobj = new ImgObj_Para(BinaryImage);
        imgobj.colorToGray2D(BinaryImage);
        int[][] grayimage = imgobj.gray_img;
        grayimage = myOTSU(grayimage,grayimage.length,grayimage[0].length,256);

        FourAreaLabel label = new FourAreaLabel(imgobj.gray_img);
        //grayimage = imgobj.binaryReverse(grayimage);
        bwlable mybw = new bwlable(grayimage, 256);
        System.out.println("----"+mybw.getCounter());
        //int count = lable.getCount();
        System.out.println("****"+label.getCount());

        for (int i = 0; i < imgobj.width; i++) {
            for (int j = 0; j < imgobj.height; j++) {
                if (label.getCount() > 0) {
                    //mydemo
                    if (label.getLabelPoint(j,i) != 0) {
                        //System.out.println(grayimage[j][i]);
                        grayimage[j][i] = label.getLabelPoint(j,i)*100;
                        //System.out.println(grayimage[j][i]);
                    }
                    /*//网络递归法有问题
                    if (mybw.getimagepoint(j,i) > 0 && mybw.getimagepoint(j,i) < 500) {
                        grayimage[j][i] = 200;
                    }*/
                }
            }
        }
        imgobj.tar_img[1] = grayimage;
        return imgobj.gray2DToBitmap(imgobj.tar_img,imgobj.width,imgobj.height);
    }

}
