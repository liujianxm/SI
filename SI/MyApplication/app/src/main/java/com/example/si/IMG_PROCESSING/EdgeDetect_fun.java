package com.example.si.IMG_PROCESSING;

import android.graphics.Bitmap;
import android.os.Build;

import androidx.annotation.RequiresApi;

import static java.lang.Math.abs;

public class EdgeDetect_fun {
    ///////////roberEdgeDetect/////////////
    public static boolean EdgeDetect(RoberEdgeDetect ro, Bitmap img)throws Exception{
        //Bitmap img = ImageTools.getBitmapfromimageView(imageView);
        ro.readImage(img);
        ro.EdgeImage = ro.createEdgeImage();
        return true;
    }

    ///////////////////////////////////////////////////////////////////
    //////////Canny算子边缘检测//////////////
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static boolean Canny_edge(ImgObj_Para imgobj, Bitmap img, double dRationHigh, double dRationLow)throws Exception{
        imgobj.colorToGray2D(img);//获得灰度图
        System.out.println("///////////////"+imgobj.height+"   "+imgobj.width);
        //Bitmap img传入时已经经过高斯模糊
        int nThdHigh=0, nThdLow = 0;		//通过dRationHigh和dRationLow，计算得到的高阈值nThdHigh和低阈值nThdLow
        System.out.println("Have got the Gray_img!");
        System.out.println("//////////////////"+imgobj.gray_img[10][100]);
        GradSobel(imgobj.gray_img,imgobj.pnGradX,imgobj.pnGradY,imgobj.pnGradXY,imgobj.pnGradTheta,imgobj.width,imgobj.height);//sobel梯度，幅值以及角度
        System.out.println("Have got the GradSobel!");
        imgobj.ThHighLow= EstimateThreshold_new(imgobj.pnGradXY,imgobj.width,imgobj.height,dRationHigh,dRationLow);//估计高低阈值
        System.out.println("Have estimated the Threshold!");
        NonMaxSuppress_new(imgobj.pnGradXY,imgobj.pnGradTheta,imgobj.temp_img,imgobj.width,imgobj.height,imgobj.ThHighLow[1]);//非极大值抑制，细化边缘
        System.out.println("Have finished the NonMaxSuppress!");
        Hysteresis(imgobj.pnGradXY,imgobj.temp_img,imgobj.width,imgobj.height,nThdHigh,nThdLow);//连接边缘
        System.out.println("Here!!!!!!");
        imgobj.tar_img[1] = imgobj.temp_img;
        imgobj.EdgeImage = imgobj.gray2DToBitmap(imgobj.tar_img,imgobj.width,imgobj.height);
        return true;
    }


    //图像和模板进行卷积，ori_img原图像，tar_img卷积后目标，iWidth图像宽度，iHeight图像高度
    //templet滤波器模板，用一维数组表示 3*3模板 或 5*5模板 或 7*7模板	或 9*9模板  或 11*11模板
    //tmpsize滤波器模板尺寸3，5，7，9，11
    public static void templet(int[][] ori_img,int[][] tar_img,int iwidth,int iheight,double[] templet,int tmpsize){
        int i,j,pos;
        int row,col;
        int half_tmpSize = tmpsize/2;
        double sum;
        for(i = 0;i<iheight;i++){
            for(j = 0;j<iwidth;j++){
                pos = 0;//记录模板位置
                sum = 0.0;//记录该点与模板加权微分结果
                for(row=-half_tmpSize;row<=half_tmpSize;row++){
                    for(col=-half_tmpSize;col<=half_tmpSize;col++){
                        if((i+row>=0)&&(i+row<iheight)&&(j+col>=0)&&(j+col<iwidth)){
                            sum += ori_img[i+row][j+col]*templet[pos];
                        }
                        pos++;
                    }//end for(col)
                }//end for(row)
                tar_img[i][j] = (int)Math.abs(sum+0.5);
            }//end for(j)
        }//end for(i)
    }


    /* 利用Sobel算子得到X方向的梯度图proImageX和Y方向的梯度图proImageY，以及XY方向综合的梯度图proImageXY
    oriImage——存放高斯滤波后的图像,gradImageX——存放X方向的梯度值,gradImageY——存放Y方向的梯度值
    gradImageXY——存放XY方向的梯度幅度,iWidth——图像宽度,iHeight——图像高度 */

    public static void GradSobel(int[][] ori_img,int[][] gradimageX,int[][] gradimageY,int[][] gradimageXY,double[][] gradimageTheta,int iwidth,int iheight){
        int i,j;
        double t;
        //double[] tmpX = {-1.0,0.0,1.0,-1.0,0.0,1.0,-1.0,0.0,1.0};
        //double[] tmpY = {1.0,1.0,1.0,0.0,0.0,0.0,-1.0,-1.0,-1.0};
        double[] tmpX = {-1.0,0.0,1.0,-2.0,0.0,2.0,-1.0,0.0,1.0};
        double[] tmpY = {1.0,2.0,1.0,0.0,0.0,0.0,-1.0,-2.0,-1.0};
        int tmpSize = 3;
        //计算水平x方向加权微分，结果存在proimageX中
        templet(ori_img,gradimageX,iwidth,iheight,tmpX,tmpSize);
        //计算水平y方向加权微分，结果存在proimageY中
        templet(ori_img,gradimageY,iwidth,iheight,tmpY,tmpSize);
        //求各点梯度值
        for(i=0;i<iheight;i++){
            for(j=0;j<iwidth;j++){
                t = Math.sqrt(1.0*gradimageX[i][j]*gradimageX[i][j]+gradimageY[i][j]*gradimageY[i][j])+0.5;
                if(t>255.0) t = 255;
                gradimageXY[i][j] = (int)t;
                //计算角度值
                if(gradimageX[i][j] == 0){
                    if(gradimageY[i][j] > 0){
                        gradimageTheta[i][j] = 90;
                    }else{
                        gradimageTheta[i][j] = -90;
                    }
                }else if(gradimageY[i][j] == 0){
                    gradimageTheta[i][j] = 0;
                }else{
                    gradimageTheta[i][j] = (double)((Math.atan2(gradimageY[i][j],gradimageX[i][j])*180)/Math.PI);
                }
                gradimageTheta[i][j] += 90;
            }
        }
    }

//===================================================================================================================================================//
        /*统计gradImageXY的直方图，确定两个阈值nThdHign和nThdLow，gradImageXY——梯度图
      UnchEdgeImage——经过非极大值抑制的图，nWidth——图像宽度，nHeight——图像高度
      dRationHigh——高于高阈值点的像素数量占总像素数量的比例，dRationLow——低于低阈值点的像素数量占总像素数量的比例
      nThdHigh——求得的高阈值（0<nThdHign<255 且nThdLow<nThdHign），nThdLow——求得的低阈值（0<nThdLow<255）
说明：	经过非极大值抑制后的数据UnchEdgeImage，统计梯度图gradImageXY的直方图，确定阈值。
		本函数只统计UnchEdgeImage中可能为边缘点的那些像素，然后利用直方图，根据dRationHigh确定高阈值nThdHign；
		然后利用dRationLow和高阈值nThdHign确定低阈值nThdLow。其中，dRationHigh = 梯度小于高阈值nThdHign的像素数量/总像素数量；
				dRationLow=nThdHign/nThdLow。*/
    public static int[] EstimateThreshold_new(int[][] gradimageXY, int iwidth, int iheight, double dRationHigh, double dRationLow){
        int i,j;
        int k;
        int[] nHist = new int[1024];//数组大小与梯度值范围有关，存放每个梯度值对应的像素数目
        int nEdgeNb = 0; //存放可能的边缘点数目
        int nMaxMag = 0; //存放最大梯度值
        int sum = 0;
        int nHighCount = 0;
        int[] ThHighLow = new int[2];
    //初始化
        for(i=0;i<1024;i++){
           nHist[i] = 0;
        }
    //统计梯度图的直方图
        for(i=1;i<iheight-1;i++){
           for(j=1;j<iwidth-1;j++){
              if(gradimageXY[i][j]<1024){
                 nHist[gradimageXY[i][j]] += 1;
              }
           }
        }
    //统计经像素总数nEdgeNb(不包括)和最大梯度值nMaxMag
    // nEdgeNb = 0;
    // nMaxMag = 0;
        for(k=1;k<1024;k++){//梯度为0的点一定不是边缘点，所以k从1开始
            nEdgeNb += nHist[k];
            if(nHist[k]!=0) nMaxMag = k;
        }
        System.out.println("nEdgeNb:" + nEdgeNb);
    //计算梯度比高阈值小的像素数目
        nHighCount = (int)(dRationHigh*nEdgeNb + 0.5);
        System.out.println("nHighCount:"+nHighCount);
    //计算高阈值nThHigh
        k = 1;
        sum = nHist[1];
        while((k<(nMaxMag-1))&&(sum<nHighCount)){
             k++;
             sum += nHist[k];
        }
        int nThHigh = k;
    //计算低阈值
        int nThLow = (int) (nThHigh * dRationLow + 0.5);
        ThHighLow[0] = nThHigh;
        ThHighLow[1] = nThLow;
        if(ThHighLow[1] < 13) {
            ThHighLow[1] = 15;
            if(ThHighLow[0] <= ThHighLow[1]) ThHighLow[0] = ThHighLow[0] + 5;
        }
        System.out.println("nThHigh:" + ThHighLow[0]);
        System.out.println("nThLow:" + ThHighLow[1]);
        return ThHighLow;
  }


    //==========================非极大值抑制==================================//
        /* gradImageXY——XY方向综合的梯度图，UnchEdgeImage——非极大值抑制之后的图像，将一些明确的非边缘点置为0，其他点置为128
      nWidth——图像宽度，nHeight——图像高度 */
    public static void NonMaxSuppress_new(int[][] gradimageXY,double[][] gradimageTheta,int[][] UnchEdgeImage,int iwidth,int iheight,int ThLow){
        int i,j;
        int gradXY,temp1,temp2;
        //临时变量
        double angle;
        //设置图像四个边肯定不是边缘点的
        for(i=0;i<iheight;i++){
            UnchEdgeImage[i][0] = 0;
            UnchEdgeImage[i][iwidth-1] = 0;
        }
        for(i=0;i<iwidth;i++){
            UnchEdgeImage[0][i] = 0;
            UnchEdgeImage[iheight-1][i] = 0;
        }
        //设置那些肯定不是边缘点的像素，对应的UnchEdgeImage[i][j]=0
        for(i=1;i<iheight-1;i++) {
            for (j = 1; j < iwidth - 1; j++) {
                //如果当前像素点的梯度的幅度值为0，则该点一定不是边缘点，对应的UnchEdgeImage[i][j]=0
                angle = gradimageTheta[i][j];
                gradXY = gradimageXY[i][j];
                if (gradXY == 0) {
                    UnchEdgeImage[i][j] = 0;
                    continue;
                }
                if(gradimageXY[i][j] > ThLow) {
                    UnchEdgeImage[i][j] = 128;
                    if ((angle >= 0 && angle < 22.5) || angle >= 157.7) {
                        temp1 = gradimageXY[i - 1][j];
                        temp2 = gradimageXY[i + 1][j];
                        if (gradXY < temp1 || gradXY < temp2) UnchEdgeImage[i][j] = 0;
                    } else if (angle >= 22.5 && angle < 67.5) {
                        temp1 = gradimageXY[i + 1][j - 1];
                        temp2 = gradimageXY[i - 1][j + 1];
                        if (gradXY < temp1 || gradXY < temp2) UnchEdgeImage[i][j] = 0;
                    } else if (angle >= 67.5 && angle < 112.5) {
                        temp1 = gradimageXY[i][j + 1];
                        temp2 = gradimageXY[i][j - 1];
                        if (gradXY < temp1 || gradXY < temp2) UnchEdgeImage[i][j] = 0;
                    } else if (angle >= 112.5 && angle < 157.5) {
                        temp1 = gradimageXY[i - 1][j - 1];
                        temp2 = gradimageXY[i + 1][j + 1];
                        if (gradXY < temp1 || gradXY < temp2) UnchEdgeImage[i][j] = 0;
                    }
                }
            }
        }

    }

    //===========================================================================================================================================//


    public static void TraceEdge(int y,int x,int nthLow,int[][]UnchEdgeImage,int[][] gradimageXY,int iwidth,int iheight){
        //对邻域像素进行查询
        int[] yNb = {0,1,1,1,0,-1,-1,-1};
        int[] xNb = {1,1,0,-1,-1,-1,0,1};
        int yy,xx,i;
        for(i=0;i<8;i++){
            yy = y+yNb[i];
            if(yy<0 || yy>=iheight) continue;
            xx = x+xNb[i];
            if(xx<0 || xx>=iwidth) continue;
            //如果该像素点为可能的边缘点，又没有处理过，并且梯度值大于阈值
            if(UnchEdgeImage[yy][xx]==128 && gradimageXY[yy][xx]>=nthLow){
                UnchEdgeImage[yy][xx] = 255;
                //以该点(yy,xx)为起点继续跟踪寻找边缘点
                TraceEdge(yy,xx,nthLow,UnchEdgeImage,gradimageXY,iwidth,iheight);
            }
        }
    }


    /* 双阈值算法检测和连接边缘，gradImageXY——XY方向的梯度图，UnchEdgeImage——非极大值抑制之后的图，
    函数结束时存放边缘检测之后的二值图像（边缘值为255，非边缘值为0），nWidth——图像宽度
    nHeight——图像高度，nThdHigh——高阈值  nThdLow<nThdHigh，nThdLow——低阈值 */

    public static void Hysteresis(int[][] gradimageXY,int[][]UnchEdgeImage,int iwidth,int iheight,int nThHigh,int nThLow){
        int i,j;
        for(i=0;i<iheight;i++){
            for(j=0;j<iwidth;j++){
                if((UnchEdgeImage[i][j]==128)&&(gradimageXY[i][j]>=nThHigh)){
                    UnchEdgeImage[i][j] = 255;
                    TraceEdge(i,j,nThLow,UnchEdgeImage,gradimageXY,iwidth,iheight);
                }
            }
        }
        for(i=0;i<iheight;i++){
            for(j=0;j<iwidth;j++){
                if(UnchEdgeImage[i][j]!=255) UnchEdgeImage[i][j] = 0;
            }
        }
    }
}
