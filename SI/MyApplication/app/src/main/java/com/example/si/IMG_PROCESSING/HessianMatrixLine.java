package com.example.si.IMG_PROCESSING;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

//import com.example.si.IMG_PROCESSING.CircleDetect.EdgeDetect_fun;
import com.example.si.IMG_PROCESSING.CircleDetect.EdgeDetect_fun;
import com.example.si.ImageTools;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.exp;
import static java.lang.Math.max;
import static java.lang.Math.pow;
import static java.lang.Math.round;
//import com.example.si.IMG_PROCESSING.FourAreaLabel;

public class HessianMatrixLine {
    int[][] grayimage;
    int[][] resultimage;
    int width;
    int height;

    public HessianMatrixLine(int[][] inimg) {
        width = inimg.length;
        System.out.println("width = "+width);
        height = inimg[0].length;
        System.out.println("height = "+height);
        System.out.println(inimg[0][0]+" "+inimg[0][1]+" "+inimg[0][2]+" ");
        grayimage = inimg;
        System.out.println(grayimage[0][0]+" "+grayimage[0][1]+" "+grayimage[0][2]+" ");
        resultimage = new int[width][height];

    }

    private void myHessian() {
        int r = 5;
        float sigma = 3f;

        //构建高斯二阶偏导数模板
        double[][] xxGauKernel = new double[2*r+1][2*r+1];
        double[][] xyGauKernel = new double[2*r+1][2*r+1];
        double[][] yyGauKernel = new double[2*r+1][2*r+1];
        for (int i = -r; i <= r;i++) {
            for (int j = -r; j <= r; j++) {
                xxGauKernel[i + r][j + r] = (1 - (i*i) / (sigma*sigma))*exp(-1 * (i*i + j*j) / (2 * sigma*sigma))*(-1 / (2 * PI*pow(sigma, 4)));
                yyGauKernel[i + r][j + r] = (1 - (j*j) / (sigma*sigma))*exp(-1 * (i*i + j*j) / (2 * sigma*sigma))*(-1 / (2 * PI*pow(sigma, 4)));
                xyGauKernel[i + r][j + r] = ((i*j))*exp(-1 * (i*i + j*j) / (2 * sigma*sigma))*(1 / (2 * PI*pow(sigma, 6)));
                //System.out.print(xxGauKernel[i + r][j + r]+" ");
            }
            //System.out.println();
        }

        //图像与高斯二阶偏导数模板进行卷积
        double[][] xxDerivae = new double[width][height];
        double[][] xyDerivae = new double[width][height];
        double[][] yyDerivae = new double[width][height];
        int[][] temp = new int[2*r+1][2*r+1];

        System.out.println("111");
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (i-r<0 || i+r>=width || j-r<0 || j+r>=height) {
                    //边缘补零后卷积
                    for (int ii = -r; ii <= r;ii++) {
                        for (int jj = -r; jj <= r; jj++) {
                            if (i+ii<0 || i+ii>=width || j+jj<0 || j+jj>=height) {
                                temp[r+ii][r+jj] = 0;
                            } else {
                                temp[r+ii][r+jj] = grayimage[i+ii][j+jj];
                            }
                            //System.out.println("("+(r+ii)+","+(r+jj)+") = "+temp[r+ii][r+jj]);
                        }
                    }
                    //卷积运算
                    for (int ii = 0; ii < 2*r+1; ii++) {
                        for (int jj = 0; jj < 2*r+1; jj++) {
                            xxDerivae[i][j] += temp[ii][jj] * xxGauKernel[ii][jj];
                            xyDerivae[i][j] += temp[ii][jj] * xyGauKernel[ii][jj];
                            yyDerivae[i][j] += temp[ii][jj] * yyGauKernel[ii][jj];
                        }
                    }
                } else {
                    //非边缘卷积
                    for (int ii = -r; ii <= r; ii++) {
                        for (int jj = -r; jj <= r; jj++) {
                            xxDerivae[i][j] += xxGauKernel[ii + r][jj + r] * grayimage[ii + i][jj + j];
                            xyDerivae[i][j] += xyGauKernel[ii + r][jj + r] * grayimage[ii + i][jj + j];
                            yyDerivae[i][j] += yyGauKernel[ii + r][jj + r] * grayimage[ii + i][jj + j];
                        }
                    }
                }
                //尺度修正
                xxDerivae[i][j] *= sigma*sigma;
                xyDerivae[i][j] *= sigma*sigma;
                yyDerivae[i][j] *= sigma*sigma;
            }
        }
        System.out.println(xxDerivae[0][0]+" "+xxDerivae[0][15]+" "+xxDerivae[20][10]+" ");
        System.out.println("222");

        //求各点对应二阶导矩阵的特征值
        //double[] eValue = new double[2];
        double[][] tempArray = new double[2][2];
        double[][] middleimage = new double[width][height];
        double mmin = 0;
        double mmax = 0;
        int count = 0;
        int count2 = 0;

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                //构造矩阵
                tempArray[0][0] = xxDerivae[i][j];
                tempArray[0][1] = xyDerivae[i][j];
                tempArray[1][0] = xyDerivae[i][j];
                tempArray[1][1] = yyDerivae[i][j];
                Matrix A = new Matrix(tempArray);
                //计算特征值
                double[] eValue = A.eig().getRealEigenvalues();
                //特征值排序
                if (abs(eValue[0]) < abs(eValue[1])) {
                    double a = eValue[0];
                    eValue[0] = eValue[1];
                    eValue[1] = a;
                }
                if (eValue[0] > 0) {    // - abs(eValue[1])
                    //middleimage[i][j] = 1;
                    count++;
                }
                //可能存在问题
                if ((eValue[0]>0) && (abs(eValue[0])>(1 + abs(eValue[1])))) {            //根据特征向量判断线性结构
                    //middleimage[i][j] = 1;
                    middleimage[i][j] =  pow((abs(eValue[0]) - abs(eValue[1])), 4);
                    //middleimage[i][j] = pow((abs(eValue[0]) / abs(eValue[1]))*(abs(eValue[0]) - abs(eValue[1])), 1.5);
                    count2++;

                }

                //求middleimage最值
                if (mmax < middleimage[i][j]) {
                    mmax = middleimage[i][j];
                } else if (mmin > middleimage[i][j]) {
                    mmin = middleimage[i][j];
                }

            }
        }
        System.out.println(middleimage[0][0]+" "+middleimage[0][15]+" "+middleimage[20][10]+" ");
        System.out.println("count = "+count);
        System.out.println("count2 = "+count2);
        System.out.println("mmax = "+mmax);
        System.out.println("mmin = "+mmin);
        System.out.println("333");

        //线性变换
        double rate = (mmax==mmin) ? 1 : (255)/(mmax-mmin);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                resultimage[i][j] = (int)((middleimage[i][j]-mmin) * rate + 0);
            }
        }

    }

    private boolean initializeBlackWhite() {
        boolean blackwhite;
        float origin, dilate;
        float mean = getMean2D(grayimage);
        //初始参数有待验证,不一定适用于所有图像
        FrangiFilter2D(1,2,2,true);
        int[][] binaryimage = thresholdBinary(resultimage,getMean2D(resultimage));
        origin = getWhiteAreaMean(grayimage, binaryimage, 256);

        FrangiFilter2D(1,2,2,false);
        binaryimage = thresholdBinary(resultimage,getMean2D(resultimage));
        dilate = getWhiteAreaMean(grayimage, binaryimage, 256);
//        //构造方形结构元
//        int[][] kernal = new int[3][3];
//        for(int i = 0; i < 3; i++) {
//            for(int j = 0; j < 3; j++) {
//                kernal[i][j] = 1;
//            }
//        }
//        //膨胀运算
//        binaryimage = mydilate(binaryimage, kernal, 256);
//        binaryimage = mydilate(binaryimage, kernal, 256);
//        dilate = getWhiteAreaMean(grayimage, binaryimage, 256);
        System.out.println(mean-origin);
        System.out.println(origin);
        System.out.println(dilate-mean);
        if ((mean - origin) > (dilate - mean)) {
            blackwhite = true;
        } else {
            blackwhite = false;
        }
//        System.out.println(blackwhite);

        return blackwhite;

    }

    //the binaryimage is generated from the grayimage
    //two input should have the same size
    private float getWhiteAreaMean(int[][] grayimage, int[][] binaryimage, int grayscale) {
        float meanwhite = 0;
        int count = 0;
        if ((grayimage.length != binaryimage.length) && (grayimage[0].length != binaryimage[0].length)) {
            return -1;
        }

        for (int i = 0; i < grayimage.length; i++) {
            for (int j =0; j < grayimage[0].length; j++) {
                if (binaryimage[i][j] == (grayscale -1)) {
                    meanwhite += grayimage[i][j];
                    count++;
                }
            }
        }

        return (meanwhite / count);
    }

    private float[][][] Hessian2D(float sigma) {
        float[][][] result = new float[3][width][height];
        int r = round((float)(3*sigma)); //MATLA中根据sigma确定r

        //构建高斯二阶偏导数模板
        float[][] xxGauKernel = new float[2*r+1][2*r+1];
        float[][] xyGauKernel = new float[2*r+1][2*r+1];
        float[][] yyGauKernel = new float[2*r+1][2*r+1];
        for (int i = -r; i <= r;i++) {
            for (int j = -r; j <= r; j++) {
                xxGauKernel[i + r][j + r] = (float)((1 - (i*i) / (sigma*sigma))*exp(-1 * (i*i + j*j) / (2 * sigma*sigma))*(-1 / (2 * PI*pow(sigma, 4))));
                yyGauKernel[i + r][j + r] = (float)((1 - (j*j) / (sigma*sigma))*exp(-1 * (i*i + j*j) / (2 * sigma*sigma))*(-1 / (2 * PI*pow(sigma, 4))));
                xyGauKernel[i + r][j + r] = (float)(((i*j))*exp(-1 * (i*i + j*j) / (2 * sigma*sigma))*(1 / (2 * PI*pow(sigma, 6))));
                //System.out.print(xxGauKernel[i + r][j + r]+" ");
            }
            //System.out.println();
        }

        //图像与高斯二阶偏导数模板进行卷积
        int[][] temp = new int[2*r+1][2*r+1];

        System.out.println("111");
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (i-r<0 || i+r>=width || j-r<0 || j+r>=height) {
                    //边缘补零后卷积
                    for (int ii = -r; ii <= r;ii++) {
                        for (int jj = -r; jj <= r; jj++) {
                            if (i+ii<0 || i+ii>=width || j+jj<0 || j+jj>=height) {
                                temp[r+ii][r+jj] = 0;
                            } else {
                                temp[r+ii][r+jj] = grayimage[i+ii][j+jj];
                            }
                            //System.out.println("("+(r+ii)+","+(r+jj)+") = "+temp[r+ii][r+jj]);
                        }
                    }
                    //卷积运算
                    for (int ii = 0; ii < 2*r+1; ii++) {
                        for (int jj = 0; jj < 2*r+1; jj++) {
                            result[0][i][j] += temp[ii][jj] * xxGauKernel[ii][jj];
                            result[1][i][j] += temp[ii][jj] * xyGauKernel[ii][jj];
                            result[2][i][j] += temp[ii][jj] * yyGauKernel[ii][jj];
                        }
                    }
                } else {
                    //非边缘卷积
                    for (int ii = -r; ii <= r; ii++) {
                        for (int jj = -r; jj <= r; jj++) {
                            result[0][i][j] += xxGauKernel[ii + r][jj + r] * grayimage[ii + i][jj + j];
                            result[1][i][j] += xyGauKernel[ii + r][jj + r] * grayimage[ii + i][jj + j];
                            result[2][i][j] += yyGauKernel[ii + r][jj + r] * grayimage[ii + i][jj + j];
                        }
                    }
                }
                //尺度修正
                result[0][i][j] *= sigma*sigma;
                result[1][i][j] *= sigma*sigma;
                result[2][i][j] *= sigma*sigma;
            }
        }
        return result;
    }

    private void FrangiFilter2D(float sigmaStart, float sigmaEnd, float sigmaStep, boolean BlackWhite) {
        ///BlackWhite = true  :  检测黑色条状物
        ///BlackWhite = false :  检测白色条状物
        float beta = 2*0.5f*0.5f;
        float c = 2*15*15;
        float[][] result = new float[width][height];
        int sigmaLength = (int)((sigmaEnd - sigmaStart) / sigmaStep) + 1;
        float[][][] diver2D; //= new double[3][width][height]
        float[] sigma = new float[sigmaLength];
        sigma[0] = sigmaStart;
        for (int i = 1; i < sigmaLength; i++) {
            sigma[i] = sigma[i-1] + sigmaStep;
        }
        //存储所有滤波后的图像
        float[][][] ALLfiltered = new float[sigmaLength][width][height];
        //float[][][] ALLangles = new float[sigmaLength][width][height];

        //对所有sigma进行Frangi滤波
        for (int sigmai = 0; sigmai < sigmaLength; sigmai++) {
            //输出处理进展
            System.out.println("Current Frangi Filter Sigma: "+sigma[sigmai]);

            //计算 hessian 二阶导
            diver2D = Hessian2D(sigma[sigmai]);

            //计算特征值
            //int count = 0;
            float Rb, S2, Ifiltered;
            double[][] tempArray = new double[2][2];
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    //构造矩阵
                    tempArray[0][0] = diver2D[0][i][j];
                    tempArray[0][1] = diver2D[1][i][j];
                    tempArray[1][0] = diver2D[1][i][j];
                    tempArray[1][1] = diver2D[2][i][j];
                    Matrix A = new Matrix(tempArray);
                    //计算特征值
                    double[] eValue = A.eig().getRealEigenvalues();
                    //特征值排序
                    if (abs(eValue[0]) < abs(eValue[1])) {
                        double a = eValue[0];
                        eValue[0] = eValue[1];
                        eValue[1] = a;
                    }
                    if (eValue[0] == 0) {    // - abs(eValue[1])
                        eValue[0] = Float.MIN_NORMAL;
                        //count++;
                    }

                    //
                    Rb = (float)pow(eValue[1]/eValue[0],2);
                    S2 = (float)(pow(eValue[0], 2) + pow(eValue[1], 2));

                    Ifiltered = (float)(exp(-Rb / beta) * (1 - exp(-S2 / c)));

                    if (BlackWhite) {
                        if (eValue[0]<0) Ifiltered = 0;
                    } else {
                        if (eValue[0]>0) Ifiltered = 0;
                    }
                    ALLfiltered[sigmai][i][j] = Ifiltered;

                }
            }
            //System.out.println("count = "+count);
        }

        //获取不同sigma处理后的最大值
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                result[i][j] = ALLfiltered[0][i][j];
                for (int sigmai = 0; sigmai < sigmaLength; sigmai++) {
                    if (ALLfiltered[sigmai][i][j] > result[i][j]) {
                        result[i][j] = ALLfiltered[sigmai][i][j];
                    }
                }
            }
        }

        //获取最值
        float mmin = result[0][0];
        float mmax = result[0][0];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (mmax < result[i][j]) {
                    mmax = result[i][j];
                } else if (mmin > result[i][j]) {
                    mmin = result[i][j];
                }
            }
        }

        //灰度线性变换
        float rate = (mmax==mmin) ? 1 : (255)/(mmax-mmin);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                resultimage[i][j] = (int)((result[i][j]-mmin) * rate + 0);
            }
        }


    }

    //二值图腐蚀运算
    //structelement形如
    // {{0,1,0},
    //  {1,1,1},
    //  {0,1,0}}
    // grayscale = 256 //本软件中采用的都是256
    public static int[][] myerode(int[][] binaryimage, int[][] structelement, int grayscale) {
        Log.v("Erode", "Image Eroding!");
        int width = binaryimage.length;
        int height = binaryimage[0].length;
        int sewidth = structelement.length;
        int seheight = structelement[0].length;
        int[][] newbinaryimage = new int[width][height];
        //newbinaryimage = binaryimage;//浅拷贝

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                newbinaryimage[i][j] = binaryimage[i][j];
            }
        }

        for (int i = sewidth / 2; i < width - sewidth / 2; i++) {
            for (int j = seheight / 2; j < height - seheight / 2; j++) {
                //结构单元运算
                if (binaryimage[i][j] == 0) {  //当前像素为黑色
                    int temp = 0;
                    for (int a = 0; a < sewidth; a++) {
                        for (int b = 0; b < seheight; b++) {
                            if (structelement[a][b] ==0) {
                                continue;
                            }
                            temp += binaryimage[i - sewidth / 2 + a][j -seheight / 2 + b];
                        }
                    }
                    if (temp / (grayscale - 1) > 0) {
                        newbinaryimage[i][j] = grayscale - 1;
                    }
                }
            }
        }

        return newbinaryimage;

    }

    //二值图膨胀运算
    public static int[][] mydilate(int[][] binaryimage, int[][] structelement, int grayscale) {
        Log.v("Dilate", "Image dilating!");
        int width = binaryimage.length;
        int height = binaryimage[0].length;
        int sewidth = structelement.length;
        int seheight = structelement[0].length;
        int[][] newbinaryimage = new int[width][height];
        //newbinaryimage = binaryimage;//浅拷贝

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                newbinaryimage[i][j] = binaryimage[i][j];
            }
        }

        //结构元非零元素个数
//        int senum = 0;
//        for (int i = 0; i < sewidth; i++) {
//            for (int j = 0; j < seheight; j++) {
//                if (structelement[i][j] != 0) {
//                    senum++;
//                }
//            }
//        }
//        System.out.println("senum = "+senum);

        for (int i = sewidth / 2; i < width - sewidth / 2; i++) {
            for (int j = seheight / 2; j < height - seheight / 2; j++) {
                //结构单元运算
                if (binaryimage[i][j] == (grayscale - 1)) {  //当前像素为白色
                    int temp = 0;
                    for (int a = 0; a < sewidth; a++) {
                        for (int b = 0; b < seheight; b++) {
                            if (structelement[a][b] == 0) {
                                continue;
                            }
                            if (binaryimage[i - sewidth / 2 + a][j -seheight / 2 + b] == 0) {
                                temp++;
                            }
                        }
                    }
                    //System.out.println(temp);
                    if (temp > 0) {
                        newbinaryimage[i][j] = 0;
                    }
                }

            }
        }

        return newbinaryimage;

    }

    //二值图取反
    public static int[][] reverseBinary(int[][] binaryimage, int grayscale) {
        int[][] newbinaryimage = new int[binaryimage.length][binaryimage[0].length];
        for (int i = 0; i < binaryimage.length; i++) {
            for (int j = 0; j < binaryimage[0].length; j++) {
                if (binaryimage[i][j] == 0) {
                    newbinaryimage[i][j] = grayscale-1;
                } else {
                    newbinaryimage[i][j] = 0;
                }
            }
        }
        return newbinaryimage;
    }

    public static int[][] thresholdBinary(int [][] grayimage, float threshold) {
        int[][] newimage = new int[grayimage.length][grayimage[0].length];
        for (int i = 0; i < grayimage.length; i++) {
            for (int j = 0; j < grayimage[0].length; j++) {
                //meanvalue += grayimage[i][j];
                if (grayimage[i][j] > threshold) {
                    newimage[i][j] = 255;
                } else {
                    newimage[i][j] =0;
                }
            }
        }
        return newimage;
    }

    public static float getMean2D(int[][] grayimage) {
        //compute the mean intensity
        float meanvalue = 0;
        for (int i = 0; i < grayimage.length; i++) {
            for (int j = 0; j < grayimage[0].length; j++) {
                meanvalue += grayimage[i][j];
            }
        }
        meanvalue /= (grayimage.length * grayimage[0].length);
        return meanvalue;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private Bitmap binaryMergeToColor(Bitmap binarybitmap, Bitmap originbitmap) throws Exception {
        //边界提取
        ImgObj_Para iobj = new ImgObj_Para(binarybitmap);
        double dRationHigh=0.9,dRationLow=0.78;///可调
        //double dRationHigh=0.85,dRationLow=0.5;///可调
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            EdgeDetect_fun.Canny_edge(iobj,binarybitmap,dRationHigh,dRationLow);
        }

        //边界加粗（腐蚀）,图像腐蚀检测黑色像素点，128和255均
        //构造方形结构元
        int[][] kernal = new int[3][3];
        for(int i = 0; i < 3; i++) {
            for(int j = 0; j < 3; j++) {
                kernal[i][j] = 1;
            }
        }
        iobj.tar_img[1] = myerode(iobj.tar_img[1],kernal,256);

        //边界覆盖于原图(red: 0xcd0000)
        int color;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (iobj.tar_img[1][j][i] == 255) {
                    color = Color.argb(iobj.tar_img[0][j][i], 205, 0, 0);
                    originbitmap.setPixel(i, j, color);
                }
            }
        }
        return originbitmap;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static Bitmap HessianLine(Bitmap bitmap) throws Exception {
        //对图像进行压缩
        int kzoom = (int)Math.ceil((float)(Math.max(bitmap.getWidth(),bitmap.getHeight())) / 500);
        System.out.println("kzoom = "+kzoom);
        Bitmap newbitmap = ImageTools.zoomBitmap(bitmap, bitmap.getWidth() / kzoom, bitmap.getHeight() / kzoom);

        ImgObj_Para imgobj = new ImgObj_Para(newbitmap);
        imgobj.colorToGray2D(newbitmap);
        HessianMatrixLine objHM = new HessianMatrixLine(imgobj.gray_img);

        //先确定图像种类（白背景/黑背景）
        boolean BlackWhite;
        BlackWhite = objHM.initializeBlackWhite();

        //进行分区间运算
        int[][][] resulttemp = new int[4][objHM.width][objHM.height];
        for (int i = 0; i < 4; i++) {
            System.out.println("***");
            objHM.FrangiFilter2D(1+i,2+i,0.4f,BlackWhite); //改变sigmaStep可改变运算次数
            System.out.println("---");

            // compute the mean intensity
            // threshold based gary image to binary image
            resulttemp[i] = thresholdBinary(objHM.resultimage,getMean2D(objHM.resultimage));
        }

        //尽可能保留多的点
        for (int i = 0; i < objHM.width; i++) {
            for (int j = 0; j < objHM.height; j++) {
                resulttemp[0][i][j] = Math.max(Math.max(resulttemp[0][i][j],resulttemp[1][i][j]),Math.max(resulttemp[2][i][j],resulttemp[3][i][j]));
            }
        }

        //图形学处理,去除大部分小区域
        //构造方形结构元
        int[][] kernal = new int[3][3];
        for(int i = 0; i < 3; i++) {
            for(int j = 0; j < 3; j++) {
                kernal[i][j] = 1;
            }
        }

        //去除大部分白色小区域
//        resulttemp[0] = mydilate(resulttemp[0],kernal, 256);
//        resulttemp[0] = myerode(resulttemp[0],kernal, 256);
//        resulttemp[0] = reverseBinary(resulttemp[0],256); //ImgObj_Para.java类中有
//        FourAreaLabel label = new FourAreaLabel(resulttemp[0]);
//        label.deleteSmall(); //如何确定该阈值
//        resulttemp[0] = label.binaryimage;
//
//        System.out.println("*******");
        //去除大部分黑色小区域
//        resulttemp[0] = mydilate(resulttemp[0],kernal, 256);
//        resulttemp[0] = myerode(resulttemp[0],kernal, 256);
//        resulttemp[0] = reverseBinary(resulttemp[0],256);
//        label.resetFourAreaLabel(resulttemp[0]);
//        label.deleteSmall(30); //如何确定该阈值
//        resulttemp[0] = label.binaryimage;


//        float sigmaStart, sigmaEnd, sigmaStep;
//        sigmaStart = (float)Math.ceil(Math.max(width, height) / 500);
//        sigmaEnd = (float)Math.ceil(Math.max(width, height) / 100);
//        sigmaStep = (sigmaEnd - sigmaStart) / 4;
//        FrangiFilter2D(sigmaStart,sigmaEnd,sigmaStep,BlackWhite);




        //grayimage = myOTSU(grayimage,grayimage.length,grayimage[0].length,256);



        imgobj.tar_img[1] = resulttemp[0];
        newbitmap = imgobj.gray2DToBitmap(imgobj.tar_img,imgobj.width,imgobj.height);
        System.out.println("==========");
        newbitmap = ImageTools.zoomBitmap(newbitmap, newbitmap.getWidth() * kzoom, newbitmap.getHeight() * kzoom);
        System.out.println("newwidth = "+newbitmap.getWidth());
        System.out.println("newheight = "+newbitmap.getHeight());
        //newbitmap = objHM.binaryMergeToColor(newbitmap,bitmap);
        return newbitmap;
    }

    void basicMatrixGetRt(float[][] basicmatrix) {
        double[][] newbasicmatrix = new double[basicmatrix.length][basicmatrix[0].length];
        for (int i = 0; i < basicmatrix.length; i++) {
            for (int j = 0; j < basicmatrix[0].length; j++) {
                newbasicmatrix[i][j] = basicmatrix[i][j];
            }
        }
        Matrix A = new Matrix(newbasicmatrix);
        System.out.println("A = U S V^T");
        //进行奇异值分解
        SingularValueDecomposition s = A.svd();
        Matrix U = s.getU();
        Matrix S = s.getS();
        Matrix V = s.getV();

        double[][] temp = new double[3][3];
//        for (int i = 0; i < 3; i++) {
//            for (int j = 0; j < 3; j++){}
//        }
        temp[0][1] = -1;
        temp[1][0] = 1;
        temp[2][2] = 1;
        Matrix W = new Matrix(temp);
        Matrix R1 = U.times(W.transpose()).times(V.transpose());
        Matrix R2 = U.times(W).times(V.transpose());
        if (R1.det() < 0) R1 = R1.times(-1);
        if (R2.det() < 0) R2 = R2.times(-1);
        



    }

}
