package com.example.si.IMG_PROCESSING.Reconstruction3D;



import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.example.si.IMG_PROCESSING.CornerDetection.ImageMarker;
import com.example.si.MainActivity;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Vector;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

import static java.lang.Double.doubleToLongBits;
import static java.lang.Double.isFinite;
import static java.lang.StrictMath.PI;
import static java.lang.StrictMath.abs;
import static java.lang.StrictMath.acos;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.min;
import static java.lang.StrictMath.pow;
import static java.lang.StrictMath.round;
import static java.lang.StrictMath.sqrt;
import static java.lang.System.in;
import static java.lang.System.out;


public class Convert2DTo3D_new {
    public enum MobileModel { MIX2, HUAWEI };//枚举手机型号
    //    private static Matrix I_Matrix = new Matrix(new double[][] {{1,0,0},{0,1,0},{0,0,1}});
    public MobileModel MyMobileModel;
    public Matrix X_3D = null;//用来存放恢复匹配点的三维坐标
    public Matrix P1 = new Matrix(new double[][]{{1, 0, 0, 0}, {0, 1, 0, 0}, {0, 0, 1, 0}});//3*4矩阵
    public Matrix P2_Selected;//筛选出的角度二的投影矩阵
    public ArrayList<ImageMarker> FeaturePoints3D;//特征点对应的三维点坐标
    public ArrayList<ImageMarker> Point3Dto2D1;//特征点对应的三维点坐标
    public ArrayList<ImageMarker> Point3Dto2D2;//特征点对应的三维点坐标
    public int[] OpticalCenter = new int[2];
    public Matrix intrinsic;
    public double[][] EpiLines1_Para = null;
    public double[][] EpiLines2_Para = null;
    public int[] maxXYZ = new int[3];
    ArrayList<Double> distance1;
    ArrayList<Double> distance2;
    Matrix PoSelected1 = null;
    Matrix PoSelected2 = null;
    float sigma = 2.0f;
    double mSigma2 = sigma*sigma;
    Matrix R = new Matrix(3, 3);
    Matrix t21 = new Matrix(1, 3);
    Vector<Boolean> vbTriangulatedF;
    Vector<Boolean> vbTriangulatedH;
    final String TAG = "Convert2DTo3D";
    Matrix vP3D4_item = null;
    Matrix R1 = new Matrix(3,3), R2 = new Matrix(3,3), t = new Matrix(1,3);
    ArrayList<Float> ParallaxList = new ArrayList<>(4);//用来存放四种RT下的视角差
//    ArrayList<double[]> vP3D1 = new ArrayList<double[]>();


    /**
     * 2D-3D主函数, MobileModel mm
     * @param PoList_1 n*2格式储存的点数组
     * @param PoList_2 n*2格式储存
     */
    @SuppressLint("ShowToast")
    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean Convert2DTo3D_Fun_new(double[][] PoList_1, double[][] PoList_2, DataBuilder dataBuilder, ArrayList<double[]> FfList){
//    public boolean Convert2DTo3D_Fun(double[][] PoList_1, double[][] PoList_2){
        ////////////////////此处经过修改！！！！！！！！
/*        Matrix K = get_IntrinsicMatrix(mm).transpose();//3*3矩阵///此处经过修改！！！！！！！！
        Matrix Po1 = Point2DToHomogeneous(PoList_1);//n*3矩阵
        Matrix Po2 = Point2DToHomogeneous(PoList_2);//n*3矩阵
        // //////////////////////
        // RANSAC ra = new RANSAC();
        Matrix F = RANSAC.MyRansac(Po1,Po2,9,500,5*10E-3,0.6);
        out.println("==============F的值为==================");
        for (int i=0; i<3; i++){
            for (int j=0; j<3; j++){
                out.println("点("+i+","+j+")的值：" + F.get(i,j));
            }
        }
        PoSelected1 = new Matrix(RANSAC.PoListHoSelected_1);
        PoSelected2 = new Matrix(RANSAC.PoListHoSelected_2);
        for (int i = 0; i < PoSelected1.getRowDimension(); i++) {
            out.println("****************" + PoSelected1.get(i, 0) + "," + PoSelected1.get(i, 1) + "," + PoSelected1.get(i, 2));
        }

//        Matrix PoListHo_1 = K.inverse().times(PoSelected1.transpose()).transpose();
//        Matrix PoListHo_2 = ((K.inverse()).times(PoSelected2.transpose())).transpose();//3*n矩阵
//        Matrix F1 = compute_fundamental_normalized(PoListHo_1, PoListHo_2);
        Matrix F1 = compute_fundamental_normalized(PoSelected1, PoSelected2);
        out.println("==============本质矩阵F1的值为==================");
        for (int i=0; i<3; i++){
            for (int j=0; j<3; j++){
                out.println("点("+i+","+j+")的值：" + F1.get(i,j));
            }
        }
//
//        Matrix E1 = K.inverse().transpose().times(F.times(K.inverse()));/////////////
        F = compute_fundamental_normalized(PoSelected1, PoSelected2);////////////重新计算一次F
        X_3D = new Matrix(PoSelected1.getRowDimension(),4);
        boolean Flag = ReconstructF(F, K,1,12);
        Log.d("Convert2DTo3D_new","Flag:"+Flag);
        P2_Selected = new Matrix(3,4);
        P2_Selected.setMatrix(0,2,0,2,R);
        P2_Selected.setMatrix(0,2,3,3,t21);
        P2_Selected = K.times(P2_Selected);
        Matrix e = compute_epipole(F.transpose());  //计算外极点
*//*        Matrix E1 = K.transpose().times(F.times(K));
        out.println("==============本质矩阵E1的值为==================");
        for (int i=0; i<3; i++){
            for (int j=0; j<3; j++){
                out.println("点("+i+","+j+")的值：" + E1.get(i,j));
            }
        }
        ArrayList<Matrix> P2 = ComputePFromEssential(E1);//计算第二角度可能的投影矩阵
        //Matrix F = compute_fundamental_normalized(Po1, Po2);
        //Matrix F = compute_fundamental_normalized(PoSelected1, PoSelected2);*//*
        ////////计算极线参数abc/////////
        ComputeCorrespondEpiLines(PoSelected1, PoSelected2, F);
        CalculateError(X_3D, PoSelected1.getArray(), PoSelected2.getArray());*/


//////////////////////////////内参自标定///////////////////////////////////
        Matrix Po1 = Point2D3DToHomogeneous(PoList_1, 0);//n*3矩阵齐次坐标
        Matrix Po2 = Point2D3DToHomogeneous(PoList_2, 0);//n*3矩阵


        Matrix F = RANSAC.CheckFundamentalRansac(Po1, Po2, 9, 500);
//        Matrix F = RANSAC.CheckHomographyRansac(Po1, Po2, 9, 500);
        out.println("==============F的值为==================");
        for (int i=0; i<3; i++){
            for (int j=0; j<3; j++){
                out.println("点("+i+","+j+")的值：" + F.get(i,j));
            }
        }
        PoSelected1 = new Matrix(RANSAC.PoListHoSelected_1);
        PoSelected2 = new Matrix(RANSAC.PoListHoSelected_2);
//        PoSelected1 = Po1;
//        PoSelected2 = Po2;
        F = compute_fundamental_normalized(PoSelected1, PoSelected2);
//        F = compute_Homography_normalized(PoSelected1, PoSelected2);
        //计算内参矩阵
        Matrix e = compute_epipole(F);  //计算外极点

        out.println("==============重新计算矩阵F的值为==================");
        for (int i=0; i<3; i++){
            for (int j=0; j<3; j++){
                out.println("点("+i+","+j+")的值：" + F.get(i,j));
            }
        }

        double [] paras = cameraSelfCalibrate(F,e); //[f1 f2 a]
        if (paras == null) {
            Log.v("Convert2DTo3D", "Given points can not get initiate matrix!");
            return false;
        }

        System.out.println(paras[0]+","+paras[1]+","+paras[2]);
//        intrinsic = buildIntrinsicMatrix(3200);
        P1 = new Matrix(new double[][]{{1, 0, 0, 0}, {0, 1, 0, 0}, {0, 0, 1, 0}});//恢复原P1
        intrinsic = buildIntrinsicMatrix((paras[0]+paras[1])/2);
        P1 = intrinsic.times(P1);
//        F = compute_fundamental_normalized(PoSelected1, PoSelected2);
//        Matrix F0 = new Matrix(new double[][]{{1.83931547e-08, -3.48196882e-07, -1.78018112e-03},{4.55207839e-07, -2.52908003e-09, -5.49841251e-04},{1.60451019e-03,  5.36483237e-04,  8.16320457e-01}});
//        Matrix F0 = new Matrix(new double[][]{{2.25317821e-08,-4.26544354e-07,-2.18073810e-03},{5.57633751e-07,-3.09814609e-09,-6.73560544e-04},{1.96553960e-03,6.57196855e-04,1.00000000e+00}});
        Matrix F0 = dataBuilder.F;
        Matrix temp;
        temp = (F0.times(1/F0.normF())).minus(F.times(1/F.normF()));
        ///////////////存放〘∆F〙/〘F〙和|delta f|/f//////////
        double[] errFf = new double[2];
        errFf[0] = temp.normF()/F0.normF();
        errFf[1] = abs(intrinsic.get(0,0)-3200)/3200;
        FfList.add(errFf);
        /////////////////////////////////////////////////////
/*        out.println("==============误差************的值为==================");
        out.println("  〘∆F〙/〘F〙:" + temp.normF()/F0.normF());
        out.println("  |delta f|/f::" + abs(intrinsic.get(0,0)-3200)/3200);
        for (int i=0; i<3; i++){
            for (int j=0; j<3; j++){
                out.println("第"+i+","+j+"处的 |deltaF|/F:"+abs(temp.get(i,j))/F0.get(i,j));
            }
        }*/
        X_3D = new Matrix(PoSelected1.getRowDimension(),4);
        boolean Flag = ReconstructF(F,0.5f,9);
        Log.d("Convert2DTo3D_new","Flag:"+Flag);
        if(!Flag){
            Toast.makeText(MainActivity.getContext(),"Fail to 3D Reconstruction!!", Toast.LENGTH_SHORT).show();
        }
        P2_Selected = new Matrix(3,4);
        P2_Selected.setMatrix(0,2,0,2,R);
        P2_Selected.setMatrix(0,2,3,3,t21.transpose());
        P2_Selected = intrinsic.times(P2_Selected);
///////////////////////////////////////////////////////////////////////

        ////////计算极线参数abc/////////
        ComputeCorrespondEpiLines(PoSelected1, PoSelected2, F);
//        X_3D = TriangulateMultiPoints(PoSelected1, PoSelected2,P1,this.P2_Selected);//3*n
        ///////////////////////////////
//        SelectPFrom4P(PoSelected1, PoSelected2, P1, P2);//恢复的三位坐标矩阵
        CalculateError(X_3D, PoSelected1.getArray(), PoSelected2.getArray());
        return true;
    }

    /////////////////////////////////////////////

    @SuppressLint("ShowToast")
    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean Convert2DTo3D_Fun_init(double[][] PoList_1, double[][] PoList_2, Matrix dataBuilderF, ArrayList<double[]> FfList) throws InterruptedException {

//////////////////////////////内参自标定///////////////////////////////////
        Matrix Po1 = Point2D3DToHomogeneous(PoList_1, 0);//n*3矩阵齐次坐标
        Matrix Po2 = Point2D3DToHomogeneous(PoList_2, 0);//n*3矩阵
        //判断使用哪个矩阵
        float score = RANSAC.InitializeRANSAC(Po1, Po2);
        Log.v("Convert2DTo3D_Fun_init","The score is :::"+score);
        double[][] PoListHoSelected_1;
        double[][] PoListHoSelected_2;
        Matrix matrixFH;
        if(score > 0.5){
            PoListHoSelected_1 = new double[RANSAC.BestInlierIdxH.size()][3];
            PoListHoSelected_2 = new double[RANSAC.BestInlierIdxH.size()][3];
            int c=0;
            for(Integer id : RANSAC.BestInlierIdxH){
                Log.d(TAG, "选取的id："+id);
                PoListHoSelected_1[c] = Po1.getMatrix(id, id, 0,2).getRowPackedCopy();
                Log.d(TAG, "PoListHoSelected_1："+PoListHoSelected_1[c][0]+","+PoListHoSelected_1[c][1]+","+PoListHoSelected_1[c][2]);
                PoListHoSelected_2[c] = Po2.getMatrix(id, id, 0,2).getRowPackedCopy();
                Log.d(TAG, "PoListHoSelected_2："+PoListHoSelected_2[c][0]+","+PoListHoSelected_2[c][1]+","+PoListHoSelected_2[c][2]);
//            double distTmp = abs(PoListHo_1.getMatrix(id, id, 0,2).times(bestH.times(PoListHo_2.getMatrix(id, id, 0,2).transpose())).get(0,0));
//            Log.d(TAG, "dist为："+distTmp);
                c++;
            }
            PoSelected1 = new Matrix(PoListHoSelected_1);
            PoSelected2 = new Matrix(PoListHoSelected_2);
//            matrixFH = compute_Homography_normalized(PoSelected1, PoSelected2);
            matrixFH = RANSAC.bestH;
        }else {
            PoListHoSelected_1 = new double[RANSAC.BestInlierIdxF.size()][3];
            PoListHoSelected_2 = new double[RANSAC.BestInlierIdxF.size()][3];
            int cc=0;
            for(Integer id : RANSAC.BestInlierIdxF){
                // Log.d(TAG, "选取的id："+id);
                PoListHoSelected_1[cc] = Po1.getMatrix(id, id, 0,2).getRowPackedCopy();
            Log.d(TAG, "FFFFFPoListHoSelected_1："+PoListHoSelected_1[cc][0]+","+PoListHoSelected_1[cc][1]+","+PoListHoSelected_1[cc][2]);
                PoListHoSelected_2[cc] = Po2.getMatrix(id, id, 0,2).getRowPackedCopy();
            Log.d(TAG, "PoListHoSelected_2："+PoListHoSelected_2[cc][0]+","+PoListHoSelected_2[cc][1]+","+PoListHoSelected_2[cc][2]);
//            double distTmp = abs(PoListHo_1.getMatrix(id, id, 0,2).times(bestH.times(PoListHo_2.getMatrix(id, id, 0,2).transpose())).get(0,0));
//            Log.d(TAG, "dist为："+distTmp);
                cc++;
            }
            PoSelected1 = new Matrix(PoListHoSelected_1);
            PoSelected2 = new Matrix(PoListHoSelected_2);
//            matrixFH = compute_fundamental_normalized(Po1, Po2);//////////////
            matrixFH = RANSAC.bestF;
        }
//        Matrix matrixFH = compute_fundamental_normalized(Po1, Po2);
        //计算内参矩阵
        Matrix e = compute_epipole(matrixFH);  //计算外极点
        out.println("==============重新计算矩阵matrixFH的值为==================");
        for (int i=0; i<3; i++){
            for (int j=0; j<3; j++){
                out.println("点("+i+","+j+")的值：" + matrixFH.get(i,j));
            }
        }

        double [] paras = cameraSelfCalibrate(matrixFH, e); //[f1 f2 a]
        if (paras == null) {
            Log.v("Convert2DTo3D", "Given points can not get initiate matrix!");
            return false;
        }

        System.out.println(paras[0]+","+paras[1]+","+paras[2]);
//        intrinsic = buildIntrinsicMatrix(3200);


        P1 = new Matrix(new double[][]{{1, 0, 0, 0}, {0, 1, 0, 0}, {0, 0, 1, 0}});//恢复原P1
        intrinsic = buildIntrinsicMatrix((paras[0]+paras[1])/2);


/*        Matrix temp;
        temp = (dataBuilderF.times(1/ dataBuilderF.normF())).minus(matrixFH.times(1/matrixFH.normF()));
        ///////////////存放〘∆F〙/〘F〙和|delta f|/f//////////
        double[] errFf = new double[2];
        errFf[0] = temp.normF()/ dataBuilderF.normF();
        errFf[1] = abs(intrinsic.get(0,0)-3200)/3200;
        FfList.add(errFf);
        out.println("==============误差************的值为==================");
        out.println("  〘∆F〙/〘F〙:" + temp.normF()/ dataBuilderF.normF());
        out.println("  |delta f|/f::" + abs(intrinsic.get(0,0)-3200)/3200);
        for (int i=0; i<3; i++){
            for (int j=0; j<3; j++){
                out.println("第"+i+","+j+"处的 |deltaF|/F:"+abs(temp.get(i,j))/ dataBuilderF.get(i,j));
            }
        }*/

        P1 = intrinsic.times(P1);
        X_3D = new Matrix(PoSelected1.getRowDimension(),4);
        boolean Flag;
        if(score>0.5) {
            Flag = ReconstructH(matrixFH, 0.35f, 1);///
        }else {
            Flag = ReconstructF(matrixFH, 0.35f, 1);///
        }
        Log.d("Convert2DTo3D_new","Flag:"+Flag);
        if(!Flag){
            //Toast_in_Thread("Point number is insufficient!");
            Looper.prepare();
            Toast.makeText(MainActivity.getContext(),"Fail to 3D Reconstruction!!", Toast.LENGTH_SHORT).show();
            Looper.loop();
        }
        double[] minmaxXYZ = getMinMaxXYZ3D();
        getMinusMin3DPoints(minmaxXYZ);
        P2_Selected = new Matrix(3,4);
        P2_Selected.setMatrix(0,2,0,2,R);
        P2_Selected.setMatrix(0,2,3,3,t21.transpose());
        P2_Selected = intrinsic.times(P2_Selected);
///////////////////////////////////////////////////////////////////////

        ////////计算极线参数abc/////////
        ComputeCorrespondEpiLines(PoSelected1, PoSelected2, matrixFH);
//        X_3D = TriangulateMultiPoints(PoSelected1, PoSelected2,P1,this.P2_Selected);//3*n
        ///////////////////////////////
//        SelectPFrom4P(PoSelected1, PoSelected2, P1, P2);//恢复的三位坐标矩阵
        CalculateError(X_3D, PoSelected1.getArray(), PoSelected2.getArray());
        return true;
    }




    /**
     * 根据手机型号获得对应的相机内参，通过 MATLAB 标定
     * @param mm 手机型号
     * @return Intrinsic_matrix
     */
    public Matrix get_IntrinsicMatrix(MobileModel mm){
        Matrix Intrinsic_matrix;
        switch (mm){
            case MIX2:
//                Intrinsic_matrix = new Matrix(new double[][] {{3053.5,0.0,0.0},{0.0,3063.3,0.0},{0,0,1.0}});
                Intrinsic_matrix = new Matrix(new double[][] {{3053.5,0.0,0.0},{0.0,3063.3,0.0},{1513.5,2018.1,1.0}});
                //Intrinsic_matrix = new Matrix(new double[][] {{1500.5,0.0,0.0},{0.0,1500.3,0.0},{750.5,1009.1,1.0}});
                break;
            case HUAWEI:
                Intrinsic_matrix = new Matrix(new double[][] {{3305.4,0.0,0.0},{0.0,3298.8,0.0},{1473.6,1923.7,1.0}});
                break;
            default:
                Intrinsic_matrix = null;
                break;
        }
        return Intrinsic_matrix;
    }

    /**
     * 将普通2D坐标点转化为齐次坐标
     * @param Po_list 坐标点矩阵，n*2格式
     * @param Type 点坐标类型 0--2d  1--3d
     * @return n*3的 Matrix 格式的齐次坐标
     */
    public Matrix Point2D3DToHomogeneous(double[][] Po_list, int Type){
        int List_len = Po_list.length;
        double[][] temp = null;
        if(Type == 0) {
            temp = new double[List_len][3];
            for (int i = 0; i < List_len; i++) {
                for (int j = 0; j <= 2; j++) {
                    if (j == 2) temp[i][j] = 1.0;
                    else temp[i][j] = Po_list[i][j];
                }
            }
            return new Matrix(temp);
        }else if(Type == 1){
            temp = new double[List_len][4];
            for (int i = 0; i < List_len; i++) {
                for (int j = 0; j <= 3; j++) {
                    if (j == 3) temp[i][j] = 1.0;
                    else temp[i][j] = Po_list[i][j];
                }
            }
        }
        assert temp != null;
        return new Matrix(temp);
    }

    /**
     * 类似 MATLAB 中的 reshape 功能
     * @param nums 原数组
     * @param r 新数组的列数
     * @param c 新数组的行数
     * @return Matrix 新数组
     */

    public static Matrix matrixReshape(double[][] nums, int r, int c) {
        if(nums == null){
            return null;
        }
        if(r == 0 || c == 0){
            return new Matrix(nums);
        }
        int row ;//行
        int columns;//列
        columns = nums[0].length;
        row = nums.length;
        if(columns < 1 || row < 1 || columns > 100 || row > 100){
            return new Matrix(nums);
        }
        if(columns * row < r * c){
            return new Matrix(nums);
        }
        double [][]result = new double[r][c];
        int rr, cc, index;
        for(int i = 0; i < row; i ++){
            for(int j = 0; j < columns; j ++){
                index = i * columns + j + 1;
                if(index > r * c){
                    break;
                }
                rr = index / c;//行
                cc = index % c;//列
                if(rr > 0 && cc ==0){
                    rr = rr -1;
                    cc = c - 1;
                }else if(rr > 0 && cc > 0){
                    cc = cc -1;
                }else{
                    cc = cc -1;
                }
                result[rr][cc] = nums[i][j];
            }
        }
        return new Matrix(result);
    }

    /**
     * 使用SVD算法来计算最小二乘解。由于上面算法得出的解可能秩不为2，
     * 所以通过将最后一个奇异值置为0来得到秩最接近2的基础矩阵。
     * 如果计算本质矩阵则需要将匹配点矩阵首先左乘相机内参矩阵的逆矩阵
     * SVD 分解中，U（row*min(row,col)）, S(col*col), V(col*col)
     * @param PoList_1 第一张图的匹配点坐标 n*3
     * @param PoList_2 第二张图的匹配点坐标
     * @return 基础矩阵 F//也可用来算 E
     */
    public static Matrix ComputeFundamental(Matrix PoList_1, Matrix PoList_2){
        int Po_num = PoList_1.getRowDimension();//获取匹配点对数
        if(PoList_2.getRowDimension() != Po_num || Po_num <= 8){
            throw new IllegalArgumentException("Number of points don't match OR Need more than 8 matched points.");
        }
        double[][] A_array = new double[Po_num][9];
        for(int i=0; i<Po_num; i++){

            final double u1 = PoList_1.get(i,0);
            final double v1 = PoList_1.get(i,1);
            final double u2 = PoList_2.get(i,0);
            final double v2 = PoList_2.get(i,1);
            A_array[i] = new double[]{u2*u1, u2*v1, u2, v2*u1, v2*v1, v2, u1,v1,1};
        }
        Matrix A = new Matrix(A_array);
        //进行奇异值分解
        SingularValueDecomposition s = A.svd();
        Matrix V = s.getV();//9*9
        Matrix F = matrixReshape(V.getMatrix(0,8,8,8).getArray() ,3,3);
        //进行奇异值分解
        SingularValueDecomposition f = F.svd();
        Matrix Uf = f.getU();
        Matrix Sf = f.getS();//
        Matrix Vf = f.getV();//非转置
        //  out.println("S22:"+Sf.get(2,2));
        Sf.set(2,0,0);//最后一行置零
        Sf.set(2,1,0);
        Sf.set(2,2,0);
        F = Uf.times(Sf.times(Vf.transpose()));
        return F;
    }

    /**
     * 计算单应矩阵
     * @param PoList_1 第一张图的匹配点坐标 n*3
     * @param PoList_2 第二张图的匹配点坐标
     * @return H单应矩阵
     */
    public static Matrix ComputeHomography(Matrix PoList_1, Matrix PoList_2){
        int Po_num = PoList_1.getRowDimension();//获取匹配点对数
        if(PoList_2.getRowDimension() != Po_num || Po_num <= 8){
            throw new IllegalArgumentException("Number of points don't match OR Need more than 8 matched points.");
        }
        double[][] A_array = new double[2*Po_num][9];
        for(int i=0; i<Po_num; i++){
            final double u1 = PoList_1.get(i,0);
            final double v1 = PoList_1.get(i,1);
            final double u2 = PoList_2.get(i,0);
            final double v2 = PoList_2.get(i,1);
            A_array[2*i] = new double[]{0.0, 0.0, 0.0, -u1, -v1, -1, v2*u1, v2*v1, v2};
            A_array[2*i+1] = new double[]{u1, v1, 1, 0.0, 0.0, 0.0, -u2*u1, -u2*v1, -u2};
        }
        Matrix A = new Matrix(A_array);
        //进行奇异值分解
        SingularValueDecomposition s = A.svd();
        Matrix V = s.getV();//9*9
        return matrixReshape(V.getMatrix(0,8,8,8).getArray() ,3,3);
    }

    /**
     * 计算E
     */
    public static Matrix compute_Homography_normalized(Matrix PoList_1, Matrix PoList_2){//n*3
        int Po_num = PoList_1.getRowDimension();//获取匹配点对数
//        out.println("==here" + Po_num);
        if(PoList_2.getRowDimension() != Po_num){
            throw new IllegalArgumentException("Number of points don't match.");
        }
        double[] po1mean = AvgValue(PoList_1);
        double[] po2mean = AvgValue(PoList_2);//计算均值
        double std1 = Objects.requireNonNull(getWholeMeanStdValue(PoList_1.getMatrix(0, Po_num-1, 0, 1).getArray()))[1];//未求齐次坐标
        double std2 = Objects.requireNonNull(getWholeMeanStdValue(PoList_2.getMatrix(0, Po_num-1, 0, 1).getArray()))[1];
        double S1 = sqrt(2)/std1;
        double S2 = sqrt(2)/std2;
        double[][] temp1 = {{S1,0,-S1*po1mean[0]}, {0,S1,-S1*po1mean[1]}, {0,0,1}};
/*        double[] std1 = Objects.requireNonNull(getXYMeanStdValue(PoList_1.getMatrix(0, Po_num-1, 0, 1).getArray()));//未求齐次坐标
        double[] std2 = Objects.requireNonNull(getXYMeanStdValue(PoList_2.getMatrix(0, Po_num-1, 0, 1).getArray()));
        double S1X = sqrt(1)/std1[2];
        double S1Y = sqrt(1)/std1[3];
        double S2X = sqrt(1)/std2[2];
        double S2Y = sqrt(1)/std2[3];
        double[][] temp1 = {{S1X,0,-S1X*po1mean[0]}, {0,S1Y,-S1Y*po1mean[1]}, {0,0,1}};*/
        Matrix T1 = new Matrix(temp1);
        PoList_1 = T1.times(PoList_1.transpose()).transpose();
        double[][] temp2 = {{S2,0,-S2*po2mean[0]}, {0,S2,-S2*po2mean[1]}, {0,0,1}};
//        double[][] temp2 = {{S2X,0,-S2X*po2mean[0]}, {0,S2Y,-S2Y*po2mean[1]}, {0,0,1}};
        Matrix T2 = new Matrix(temp2);
        PoList_2 = T2.times(PoList_2.transpose()).transpose();
        Matrix H = ComputeHomography(PoList_1,PoList_2);
        H = (T2.inverse()).times(H.times(T1));//此处有改动，T2左乘
//        out.println("==here" + "=====compute_fundamental_norma");
        return H.times(1/H.get(2,2));
//         return F;
    }

    /**
     * 计算E
     */
    public static Matrix compute_fundamental_normalized(Matrix PoList_1, Matrix PoList_2){//n*3
        int Po_num = PoList_1.getRowDimension();//获取匹配点对数
//        out.println("==here" + Po_num);
        if(PoList_2.getRowDimension() != Po_num){
            throw new IllegalArgumentException("Number of points don't match.");
        }
        double[] po1mean = AvgValue(PoList_1);
        double[] po2mean = AvgValue(PoList_2);//计算均值
        double std1 = Objects.requireNonNull(getWholeMeanStdValue(PoList_1.getMatrix(0, Po_num-1, 0, 1).getArray()))[1];//未求齐次坐标
        double std2 = Objects.requireNonNull(getWholeMeanStdValue(PoList_2.getMatrix(0, Po_num-1, 0, 1).getArray()))[1];
        double S1 = sqrt(2)/std1;
        double S2 = sqrt(2)/std2;
        double[][] temp1 = {{S1,0,-S1*po1mean[0]}, {0,S1,-S1*po1mean[1]}, {0,0,1}};
/*        double[] std1 = Objects.requireNonNull(getXYMeanStdValue(PoList_1.getMatrix(0, Po_num-1, 0, 1).getArray()));//未求齐次坐标
        double[] std2 = Objects.requireNonNull(getXYMeanStdValue(PoList_2.getMatrix(0, Po_num-1, 0, 1).getArray()));
        double S1X = sqrt(1)/std1[2];
        double S1Y = sqrt(1)/std1[3];
        double S2X = sqrt(1)/std2[2];
        double S2Y = sqrt(1)/std2[3];
        double[][] temp1 = {{S1X,0,-S1X*po1mean[0]}, {0,S1Y,-S1Y*po1mean[1]}, {0,0,1}};*/
        Matrix T1 = new Matrix(temp1);
        PoList_1 = T1.times(PoList_1.transpose()).transpose();
        double[][] temp2 = {{S2,0,-S2*po2mean[0]}, {0,S2,-S2*po2mean[1]}, {0,0,1}};
//        double[][] temp2 = {{S2X,0,-S2X*po2mean[0]}, {0,S2Y,-S2Y*po2mean[1]}, {0,0,1}};
        Matrix T2 = new Matrix(temp2);
        PoList_2 = T2.times(PoList_2.transpose()).transpose();
        Matrix F = ComputeFundamental(PoList_1,PoList_2);
        F = (T2.transpose()).times(F.times(T1));//此处有改动，T2左乘
//        out.println("==here" + "=====compute_fundamental_norma");
        return F.times(1/F.get(2,2));
//         return F;
    }


    /**
     * 计算x ，y 维度的均值,此时已经变为齐次坐标 Matrix
     * @param Po_list 匹配点
     * @return 维度均值
     */
    public static double[] AvgValue(Matrix Po_list){
        double[] avg = new double[3];
        int Total_len = Po_list.getRowDimension();
        double sum_x = 0.0f;
        double sum_y = 0.0f;
        for(int i=0; i<Total_len; i++){
            sum_x += Po_list.get(i,0);
            sum_y += Po_list.get(i,1);
        }
        avg[0] = sum_x/Total_len;
        avg[1] = sum_y/Total_len;
        avg[2] = 1;
        return avg;
    }

    /**
     * 获取XY均值以及标准差
     * @param Po_list 匹配点
     * @return 分xy的 meanStd
     */

    public static double[] getXYMeanStdValue(double[][] Po_list){
        double[] meanStd;
        if(Po_list == null){
            out.println("out of data!");
            return null;
        }else{
            int Height = Po_list.length;//图像高
            int Width = Po_list[0].length;//图像宽（反）
            meanStd = new double[4];
            double meanX = 0.0;
            double meanY = 0.0;
            double stdX = 0.0;
            double stdY = 0.0;
            int j,i;
            for (j = 0; j < Height; j++)
                for (i = 0; i < Width; i++) {
                    meanX += Po_list[j][0];
                    meanY += Po_list[j][1];
                }
//            mean /= Height*Width;
            meanX /= Height;
            meanY /= Height;
            for (j = 0; j < Height; j++) {
                for (i = 0; i < Width; i++) {
                    stdX += Math.pow(Po_list[j][0] - meanX, 2);
                    stdY += Math.pow(Po_list[j][1] - meanY, 2);
                }
            }
//            std /= Height*Width-1;
            stdX /= Height-1;
            stdY /= Height-1;
            stdX = Math.sqrt(stdX);
            stdY = Math.sqrt(stdY);

            meanStd[0] = meanX;
            meanStd[1] = meanY;
            meanStd[2] = stdX;
            meanStd[3] = stdY;
            return meanStd;
        }
    }


    /**
     * 获取全局均值以及标准差
     * @param Po_list 匹配点
     * @return 全局 meanStd
     */
    public static double[] getWholeMeanStdValue(double[][] Po_list){
        double[] meanStd;
        if(Po_list == null){
            out.println("out of data!");
            return null;
        }else{
            int Height = Po_list.length;//图像高
            int Width = Po_list[0].length;//图像宽（反）
            meanStd = new double[4];
            double mean = 0.0;
            double std = 0.0;
            int j,i;
            for (j = 0; j < Height; j++)
                for (i = 0; i < Width; i++) {
                    mean += Po_list[j][i];
                }
            mean /= Height*Width;
            for (j = 0; j < Height; j++) {
                for (i = 0; i < Width; i++) {
                    std += Math.pow(Po_list[j][i] - mean, 2);
                }
            }
            std /= Height*Width-1;
            std = Math.sqrt(std);

            meanStd[0] = mean;
            meanStd[1] = std;
            return meanStd;
        }
    }




    /**
     * 计算 inv(k)*point 用于后续代入ComputeFundamental()函数计算本质矩阵
     * @param Po_list  图中的匹配点坐标
     * @param mm 拍照手机型号匹配内参
     * @return n*3矩阵
     */

    public Matrix CalculateKPoList(double[][] Po_list, MobileModel mm){
        Matrix K = get_IntrinsicMatrix(mm).transpose();//3*3矩阵
        Matrix Po = Point2D3DToHomogeneous(Po_list, 0);//n*3矩阵
        Po = (K.inverse()).times(Po.transpose());//3*n矩阵
        return Po.transpose();
    }




    /**
     * 从E中分解出旋转和平移矩阵
     * @param E
     */
//    private void DecomposeE(Matrix E, Matrix R1, Matrix R2, Matrix t){
    private void DecomposeE(Matrix E){
        SingularValueDecomposition e = E.svd();
        Matrix Se = e.getS();
        Matrix Ue = e.getU();
        Matrix VeT = e.getV().transpose();
        //限制E满足奇异值两个相等，一个为0
        double sigmaave = (Se.get(0,0)+Se.get(1,1))/2;
        Se.set(0,0,sigmaave);
        Se.set(1,1,sigmaave);
        Se.set(2,2,0);
        E = Ue.times(Se.times(VeT));
        E = E.times(1/E.get(2,2));
        out.println("==============E重构后的值为=================="+sigmaave);
        for (int i=0; i<3; i++){
            for (int j=0; j<3; j++){
                out.println("点("+i+","+j+")的值：" + E.get(i,j));
            }
        }
        e = E.svd();
        Ue = e.getU();
        VeT = e.getV().transpose();
        Matrix W = new Matrix(new double[][]{{0, -1, 0}, {1, 0, 0}, {0, 0, 1}});
        Matrix tx = Ue.times(W.times(Se.times(Ue.transpose())));
//        Matrix tx = Ue.times(W.times(Ue.transpose()));
//        t = new Matrix(new double[][]{{tx.get(2,1),tx.get(0,2),tx.get(1,0)}});//1*3


        t = Ue.getMatrix(0,2,2,2).transpose();
        out.println("Ue2::"+ Ue.get(0,2)+"  ,"+Ue.get(1,2)+","+Ue.get(2,2));
//        out.println(t.norm2()+"  t"+t.get(0,0)+""+t.get(0,1)+""+t.get(0,2));
        t = t.times(1/t.normF());//3*1
        R1 = Ue.times(W.times(VeT));
        if(R1.det()<0) R1 = R1.times(-1);
        R2 = Ue.times((W.transpose()).times(VeT));
        if(R2.det()<0) R2 = R2.times(-1);
        out.println("==============R1的值为==================");
        for (int i=0; i<3; i++){
            for (int j=0; j<3; j++){
                out.println("点("+i+","+j+")的值：" + R1.get(i,j));
            }
        }
        out.println("==============R2的值为==================");
        for (int i=0; i<3; i++){
            for (int j=0; j<3; j++){
                out.println("点("+i+","+j+")的值：" + R2.get(i,j)+"   t"+t.get(0,i));
            }
        }

    }

    /**
     * 计算三角化重投影成功的数量
     * @param R
     * @param t
     * @param K
     * @param Po_1
     * @param Po_2
     * @param th2
     * @param vbGood
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private int CheckRT(Matrix R, Matrix t, Matrix K, Matrix Po_1, Matrix Po_2, float th2, Vector<Boolean> vbGood){
//        vP3D1.clear();
        int num = Po_1.getRowDimension();
        //    vP3D = new Matrix(num,3);
        final double fx = K.get(0,0);
        final double fy = K.get(1,1);
        final double cx = K.get(0,2);
        final double cy = K.get(1,2);
        out.println("====="+fx+","+fy+","+cx+","+cy);
        List<Double> vCosParallax = new ArrayList<>(num);
        vbGood = new Vector<>(num);
        // Camera 1 Projection Matrix K[I|0]
//        P1 = K.times(P1);
        out.println("==============P1的值为==================");
        for (int i=0; i<3; i++){
            for (int j=0; j<4; j++){
                out.println("点("+i+","+j+")的值：" + P1.get(i,j));
            }
        }
        Matrix O1 = new Matrix(new double[][]{{0, 0, 0}});//1*3
//        out.println("*************"+O1.getRowDimension());
        // Camera 2 Projection Matrix K[R|t]
        Matrix P2 = new Matrix(3,4);
        P2.setMatrix(0,2,0,2,R);
        P2.setMatrix(0,2,3,3,t.transpose());
        P2 = K.times(P2);
        Matrix O2 = (R.transpose().times(t.transpose())).times(-1);//3*1
        out.println("O2的值：" + O2.get(0,0)+""+ O2.get(1,0)+""+ O2.get(2,0));
        out.println("==============P2的值为==================");
        for (int i=0; i<3; i++){
            for (int j=0; j<4; j++){
                out.println("点("+i+","+j+")的值：" + P2.get(i,j));
            }
        }


        out.println("==============开始筛选过程==================");
        int nGood = 0;
        for(int i=0; i<num; i++){
            double point1_x = Po_1.getMatrix(i,i,0,1).get(0,0);
            double point1_y = Po_1.getMatrix(i,i,0,1).get(0,1);
            double point2_x = Po_2.getMatrix(i,i,0,1).get(0,0);
            double point2_y = Po_2.getMatrix(i,i,0,1).get(0,1);
            //单点三角化
            Matrix p3dC1 = TriangulatePoints(Po_1.getMatrix(i,i,0,2), Po_2.getMatrix(i,i,0,2), P1, P2).getMatrix(0,0,0,2);//1*3
            Log.v("CheckRT","p3dC1+++++");
            p3dC1.print(1,3);
            if(!isFinite(p3dC1.get(0,0))||!isFinite(p3dC1.get(0,1))||!isFinite(p3dC1.get(0,2))){
                vbGood.set(i,false);
                out.println("****111111111111***被筛除*************");
                continue;
            }
            // Check parallax
            Matrix normal1 = p3dC1.minus(O1);//1*3
            double dist1 = normal1.norm2();
            Log.v("CheckRT","dist:"+dist1);
            Matrix normal2 = p3dC1.minus(O2.transpose());
            double dist2 = normal2.norm2();
            double cosParallax = normal1.times(normal2.transpose()).get(0,0)/(dist1*dist2);
            Log.v("CheckRT","cosParallax:"+cosParallax);
            // Check depth in front of first camera (only if enough parallax, as "infinite" points can easily go to negative depth)
            if(p3dC1.get(0,2)<=0 && cosParallax<0.99998){
                out.println("****2222222222222***被筛除*************");
                continue;
            }
            // Check depth in front of second camera (only if enough parallax, as "infinite" points can easily go to negative depth)
            Matrix p3dC2 = R.times(p3dC1.transpose()).plus(t.transpose()).transpose();//1*3
            if(p3dC2.get(0,2)<=0 && cosParallax<0.99998){
                out.println("****3333333333333***被筛除*************");
                continue;
            }
            // Check reprojection error in first image
            Log.v("CheckRT","Have added one 3d Point..++++++++++++++");
            double im1x, im1y;
            double invZ1 = 1.0/p3dC1.get(0,2);
            im1x = fx*p3dC1.get(0,0)*invZ1 + cx;
            im1y = fy*p3dC1.get(0,1)*invZ1 + cy;
            double squareError1 = pow(im1x-point1_x,2)+pow(im1y-point1_y,2);
            out.println("squareError1********"+squareError1);
            if(squareError1 > th2) {
                out.println("****4444444444444444***被筛除*************");
                continue;
            }
            // Check reprojection error in second image
            double im2x, im2y;
            double invZ2 = 1.0/p3dC2.get(0,2);
            im2x = fx*p3dC2.get(0,0)*invZ2 + cx;
            im2y = fy*p3dC2.get(0,1)*invZ2 + cy;
            double squareError2 = pow(im2x-point2_x,2)+pow(im2y-point2_y,2);
            out.println("squareError2********"+squareError2+","+th2);
            if(squareError2 > th2){
                out.println("****555555555555***被筛除*************");
                continue;
            }
            vCosParallax.add(cosParallax);
            vP3D4_item.setMatrix(i, i,0,2, p3dC1);
            Log.v("CheckRT",vCosParallax.size()+"Have added one 3d Point..++++++++++++++");
/*            for(int j=0; j<vP3D4_item.getRowDimension(); j++){
                out.println("第"+j+"个："+vP3D4_item.get(j,0)+","+vP3D4_item.get(j,1)+","+vP3D4_item.get(j,2));
            }*/

//            vP3D1.add(p3dC1.getRowPackedCopy());


           // p3dC1.print(1,3);
            nGood++;
/*            if(cosParallax<0.99998){
                vbGood.set(i, true);
            }*/
        }
        // 得到3D点中较大的视差角
        if(nGood>0){
            Log.v("CheckRT","Have enter here "+ nGood);
            //将视差角余弦由小到大排序, 取出第50个，或者最后那个也就是最大那个
            vCosParallax.sort((o1, o2) -> o2 - o1 > 0 ? 1:-1);
            int idx = min(50, vCosParallax.size()-1);
            ParallaxList.add((float) (acos(vCosParallax.get(idx))*180/PI));
            Log.v("CheckRT","ParallaxList "+ ParallaxList.toString());
        }else {
            ParallaxList.add(0.0f);
        }
        return nGood;
    }

    /**
     * 从基础矩阵恢复位姿
     * @param F
     * @param minParallax
     * @param minTriangulated
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean ReconstructF(Matrix F, float minParallax, int minTriangulated){
        Log.v("ReconstructH","Here enter the ReconstructF fun>>>>>>>>>>>>>>>>>>>");
        // Compute Essential Matrix from Fundamental Matrix
        Matrix E = intrinsic.transpose().times(F.times(intrinsic));
//        Matrix R1 = new Matrix(3,3), R2 = new Matrix(3,3), t = new Matrix(3,1);
        // Recover the 4 motion hypotheses
/*        E = new Matrix(new double[][]{{0.04353816,-1.19007965,-1.68130081}
                ,{1.95370921,0.70878012,-4.82419685}
                ,{1.8028886,4.83133697,1.00}});*/
        out.println("==============E矩阵的值为==================");
        for (int i=0; i<3; i++){
            for (int j=0; j<3; j++){
                out.println("点("+i+","+j+")的值：" + E.get(i,j));
            }
        }
        DecomposeE(E);
        Matrix t1 = t;
        Matrix t2 = t.times(-1);
        // Reconstruct with the 4 hyphoteses and check
        ArrayList<Matrix> vP3D4 = new ArrayList<>(4);
        int num = PoSelected1.getRowDimension();
        Log.v(TAG,"++++++++++++num+++++++++++++"+num);
        Vector<Boolean> vbTriangulated1 = new Vector<>(num);
        Vector<Boolean> vbTriangulated2 = new Vector<>(num);
        Vector<Boolean> vbTriangulated3 = new Vector<>(num);
        Vector<Boolean> vbTriangulated4 = new Vector<>(num);
//        float parallax1 = 0,parallax2 = 0, parallax3 = 0, parallax4 = 0;
        vP3D4_item = new Matrix(num, 3);
        int nGood1 = CheckRT(R1, t1, intrinsic, PoSelected1, PoSelected2, (float) (4*mSigma2), vbTriangulated1);
//        Matrix vp3d = ArrayListToMatrix(vP3D1);
        Log.v(TAG,"++++++++++++++nGood1++++++++++++++++"+nGood1+"  parallax1:  "+ParallaxList.get(0));
        for(int j=0; j<vP3D4_item .getRowDimension(); j++){
            out.println("第"+j+"个："+vP3D4_item .get(j,0)+","+vP3D4_item .get(j,1)+","+vP3D4_item.get(j,2));
        }
        vP3D4.add(vP3D4_item);
        vP3D4_item = new Matrix(num, 3);
        int nGood2 = CheckRT(R2, t1, intrinsic, PoSelected1, PoSelected2, (float) (4*mSigma2), vbTriangulated2);
        Log.v(TAG,"++++++++++++++nGood2++++++++++++++++"+nGood2+"  parallax2:  "+ParallaxList.get(1));
        for(int j=0; j<vP3D4_item.getRowDimension(); j++){
            out.println("第"+j+"个："+vP3D4_item.get(j,0)+","+vP3D4_item.get(j,1)+","+vP3D4_item.get(j,2));
        }
        vP3D4.add(vP3D4_item);
        vP3D4_item = new Matrix(num, 3);
        int nGood3 = CheckRT(R1, t2, intrinsic, PoSelected1, PoSelected2, (float) (4*mSigma2), vbTriangulated3);
        Log.v(TAG,"++++++++++++++nGood3++++++++++++++++"+nGood3+"  parallax3:  "+ParallaxList.get(2));
        for(int j=0; j<vP3D4_item.getRowDimension(); j++){
            out.println("第"+j+"个："+vP3D4_item.get(j,0)+","+vP3D4_item.get(j,1)+","+vP3D4_item.get(j,2));
        }
        vP3D4.add(vP3D4_item);
        vP3D4_item = new Matrix(num, 3);
        int nGood4 = CheckRT(R2, t2, intrinsic, PoSelected1, PoSelected2, (float) (4*mSigma2), vbTriangulated4);
        Log.v(TAG,"++++++++++++++nGood4++++++++++++++++"+nGood4+"  parallax4:  "+ParallaxList.get(3));
        for(int j=0; j<vP3D4_item.getRowDimension(); j++){
            out.println("第"+j+"个："+vP3D4_item.get(j,0)+","+vP3D4_item.get(j,1)+","+vP3D4_item.get(j,2));
        }
        vP3D4.add(vP3D4_item);
        int maxGood = max(nGood1,max(nGood2,max(nGood3,nGood4)));
/*        Matrix R = new Matrix(3, 3);
        Matrix t21 = new Matrix(3, 1);*/
        // minTriangulated为可以三角化恢复三维点的个数
        int nMinGood = (int) max(0.1*num, minTriangulated);//
        int nsimilar = 0;
        if(nGood1>0.7*maxGood)
            nsimilar++;
        if(nGood2>0.7*maxGood)
            nsimilar++;
        if(nGood3>0.7*maxGood)
            nsimilar++;
        if(nGood4>0.7*maxGood)
            nsimilar++;
        // If there is not a clear winner or not enough triangulated points reject initialization
        if(maxGood<nMinGood || nsimilar>1)
        {
            Log.v(TAG, "maxGood<nMinGood??::"+ (maxGood < nMinGood) + ", nsimilar>1"+(nsimilar>1));
            Log.v(TAG,"HERE");
            return false;
        }
        // If best reconstruction has enough parallax initialize
//        Vector<Boolean> vbTriangulated;
        // Matrix vP3D;
        Log.v(TAG,"Have Triangulated..........");
        if(maxGood == nGood1){
            if(ParallaxList.get(0)>minParallax){
                X_3D = Point2D3DToHomogeneous(vP3D4.get(0).getArray(),1);//n*4
                vbTriangulatedF = vbTriangulated1;
                R = R1;
                t21 = t1;
                return true;
            }
        }else if(maxGood == nGood2){
            if(ParallaxList.get(1)>minParallax)
            {
                X_3D = Point2D3DToHomogeneous(vP3D4.get(1).getArray(),1);
                vbTriangulatedF = vbTriangulated2;
                R = R2;
                t21 = t1;
                return true;
            }
        }else if(maxGood == nGood3){
            if(ParallaxList.get(2)>minParallax)
            {
                X_3D = Point2D3DToHomogeneous(vP3D4.get(2).getArray(),1);
                vbTriangulatedF = vbTriangulated3;
                R = R1;
                t21 = t2;
                return true;
            }
        }else if(maxGood==nGood4)
        {
            if(ParallaxList.get(3)>minParallax)
            {
                X_3D = Point2D3DToHomogeneous(vP3D4.get(3).getArray(),1);
                vbTriangulatedF = vbTriangulated4;
                R = R2;
                t21 = t2;
                return true;
            }
        }
        return false;
    }

    /**
     * 从单应矩阵 H 中恢复相机位姿和三维点坐标
     * @param H
     * @param minParallax
     * @param minTriangulated
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean ReconstructH(Matrix H, float minParallax, int minTriangulated){
        Log.v("ReconstructH","Here enter the ReconstructH fun>>>>>>>>>>>>>>>");
        Matrix A = intrinsic.inverse().times(H.times(intrinsic));
        SingularValueDecomposition a = A.svd();
        Matrix Sa = a.getS();
        Matrix Ua = a.getU();
        Matrix Va = a.getV();
        double s = Ua.det()*(Va.transpose()).det();
        double d1 = Sa.get(0,0);
        double d2 = Sa.get(1,1);
        double d3 = Sa.get(2,2);
        if(d1/d2<1.00001 || d2/d3<1.00001)
        {
            return false;
        }
        ArrayList<Matrix> vR = new ArrayList<>();
        ArrayList<Matrix> vt = new ArrayList<>();
        ArrayList<Matrix> vn = new ArrayList<>();;
        double aux1 = sqrt((d1*d1-d2*d2)/(d1*d1-d3*d3));
        double aux3 = sqrt((d2*d2-d3*d3)/(d1*d1-d3*d3));
        double[] x1 = {aux1,aux1,-aux1,-aux1};
        double[] x3 = {aux3,-aux3,aux3,-aux3};
        //case d'=d2
        double aux_stheta = sqrt((d1*d1-d2*d2)*(d2*d2-d3*d3))/((d1+d3)*d2);
        double ctheta = (d2*d2+d1*d3)/((d1+d3)*d2);
        double[] stheta = {aux_stheta, -aux_stheta, -aux_stheta, aux_stheta};
        for(int i=0; i<4; i++){
            Matrix Rp = new Matrix(new double[][]{{1,0,0},{0,1,0},{0,0,1}});
            Rp.set(0,0, ctheta);
            Rp.set(0,2, -stheta[i]);
            Rp.set(2,0, stheta[i]);
            Rp.set(2,2, ctheta);
            Matrix R = Ua.times(Rp.times(Va.transpose())).times(s);
            vR.add(R);
            Matrix tp = new Matrix(3,1);
            tp.set(0,0, x1[i]);
            tp.set(1,0, 0);
            tp.set(2,0, -x3[i]);
            tp = tp.times(d1-d3);
            Matrix t = Ua.times(tp);
            vt.add(t.times(1/t.normF()));
            Matrix np = new Matrix(3,1);
            np.set(0,0, x1[i]);
            np.set(1,0, 0);
            np.set(2,0, x3[i]);
            Matrix n = Va.transpose().times(np);
            if(n.get(2,0)<0)  n = n.times(-1);
            vn.add(n);
        }
        //case d'=-d2
        double aux_sphi = sqrt((d1*d1-d2*d2)*(d2*d2-d3*d3))/((d1-d3)*d2);
        double cphi = (d1*d3-d2*d2)/((d1-d3)*d2);
        double[] sphi = {aux_sphi, -aux_sphi, -aux_sphi, aux_sphi};
        for(int i=0; i<4; i++){
            Matrix Rp = new Matrix(new double[][]{{1,0,0},{0,1,0},{0,0,1}});
            Rp.set(0,0, cphi);
            Rp.set(0,2, -sphi[i]);
            Rp.set(1,1, -1);
            Rp.set(2,0, sphi[i]);
            Rp.set(2,2, -cphi);
            Matrix R = Ua.times(Rp.times(Va.transpose())).times(s);
            vR.add(R);
            Matrix tp = new Matrix(3,1);
            tp.set(0,0, x1[i]);
            tp.set(1,0, 0);
            tp.set(2,0, x3[i]);
            tp = tp.times(d1+d3);
            Matrix t = Ua.times(tp);
            vt.add(t.times(1/t.normF()));
            Matrix np = new Matrix(3,1);
            np.set(0,0, x1[i]);
            np.set(1,0, 0);
            np.set(2,0, x3[i]);
            Matrix n = Va.transpose().times(np);
            if(n.get(2,0)<0)  n = n.times(-1);
            vn.add(n);
        }
        int bestGood = 0;
        int secondBestGood = 0;
        int bestSolutionIdx = -1;
        float bestParallax = -1;
        //==================================================================//
        // Instead of applying the visibility constraints proposed in the Faugeras' paper (which could fail for points seen with low parallax)
        // We reconstruct all hypotheses and check in terms of triangulated points and parallax
        int num = PoSelected1.getRowDimension();
        Log.v(TAG,"++++++++++++num+++++++++++++"+num);
        Matrix best_vP3D = new Matrix(num, 3);
        Vector<Boolean> best_vbTriangulated = new Vector<>(num);
        for(int i=0; i<8; i++){
            float parallaxi = 0;
            Matrix vP3D_i = new Matrix(num, 3);
            Vector<Boolean> vbTriangulated_i = new Vector<>(num);
            int nGood = CheckRT(vR.get(i), vt.get(2).transpose(), intrinsic, PoSelected1, PoSelected2, (float) (4*mSigma2), vbTriangulated_i);
            if(nGood > bestGood){
                secondBestGood = bestGood;
                bestGood = nGood;
                bestSolutionIdx = i;
                bestParallax = parallaxi;
                best_vP3D = vP3D_i;
                best_vbTriangulated = vbTriangulated_i;
            }else if(nGood > secondBestGood){
                secondBestGood = nGood;
            }
        }
        if(secondBestGood < 0.75*bestGood && bestParallax >= minParallax && bestGood > minTriangulated && bestGood > 0.1*num){
            R = vR.get(bestSolutionIdx);
            t21 = vt.get(bestSolutionIdx);
            X_3D = best_vP3D;
            vbTriangulatedH = best_vbTriangulated;
            return true;
        }
        return false;
    }



    private Matrix ArrayListToMatrix(ArrayList<double[]> vp3d){
        double[][] array = new double[vp3d.size()][];
        for(int i=0; i<vp3d.size(); i++){
            array[i] = vp3d.get(i);
        }
        return new Matrix(array);
    }





    /**
     * 三角化求一对点的三维对应点坐标，单个点（与SLAM做法相同）
     *  |P13*x1 -P11 |
     *  |P13*y1 -P12 ||X| = 0
     *  |P23*x2 -P21 |
     *  |P23*y2 -P22 |
     * @param Po_1 第一幅图中点的坐标 数组格式为1*3
     * @param Po_2 第二幅图中点的坐标  数组格式为1*3
     * @param P1 第一个角度的投影矩阵  [[1, 0, 0, 0], [0, 1, 0, 0], [0, 0, 0, 1]]
     * @param P2 第二个角度下的投影矩阵
     * @return 三维点坐标 格式为1*4
     */
    public Matrix TriangulatePoints(Matrix Po_1, Matrix Po_2, Matrix P1, Matrix P2){
        ///////////
//        Matrix test = (P2.getMatrix(2,2,0,3).times(Po_2.get(0,1)));
//        out.println("==============矩阵测试的值为==================");
//        out.println(Po_2.get(0,1));
//        for(int i=0; i<4; i++){
//
//                out.println("第"+i+"个点的值：" + test.get(0, i));
//        }

        Matrix M = new Matrix(new double[4][4]);
        M.setMatrix(0,0,0,3, (P1.getMatrix(2,2,0,3).times(Po_1.get(0,0))).minus(P1.getMatrix(0,0,0,3)));
        M.setMatrix(1,1,0,3, (P1.getMatrix(2,2,0,3).times(Po_1.get(0,1))).minus(P1.getMatrix(1,1,0,3)));
        M.setMatrix(2,2,0,3, (P2.getMatrix(2,2,0,3).times(Po_2.get(0,0))).minus(P2.getMatrix(0,0,0,3)));
        M.setMatrix(3,3,0,3, (P2.getMatrix(2,2,0,3).times(Po_2.get(0,1))).minus(P2.getMatrix(1,1,0,3)));
/*        out.println("==============矩阵M的值为==================");
        for(int i=0; i<4; i++){
            for(int j=0; j<4; j++){
                out.println("M的第("+i+","+j+")个点的值：" + M.get(i,j));
            }
        }*/
        SingularValueDecomposition m = M.svd();
        Matrix X = m.getV().getMatrix(0,3,3,3);//V取最后一列
        out.println("============================"+M.times(X).get(0,0)+","+M.times(X).get(1,0)+","+M.times(X).get(2,0));
//        out.println(">>>>>>>>>>齐次坐标未归一时X的值：" + X.get(0,0)+","+X.get(1,0)+","+X.get(2,0)+","+X.get(3,0));
        X = X.times(1/(X.get(3,0)));
        out.println("!!!!!!!!!!!X的值：" + X.get(0,0)+","+X.get(1,0)+","+X.get(2,0)+","+X.get(3,0));
//        out.println("@@@@@@@@@@@@@@@@@@@@@@@"+M.times(X).get(0,0));
//        out.println("=======================cond:"+ M.cond());
        return X.transpose();
    }




    /**
     * 多个点的三角剖分
     * @param PoList_1 第一幅图中的匹配点 n*3
     * @param PoList_2 第二幅图中的匹配点 n*3
     * @param P1 第一个角度的投影矩阵
     * @param P2 第二个角度的投影矩阵
     * @return 4*n 的三维点坐标矩阵
     */
    public Matrix TriangulateMultiPoints(Matrix PoList_1, Matrix PoList_2, Matrix P1, Matrix P2){
        int point_num = PoList_1.getRowDimension();
        if(PoList_2.getRowDimension() != point_num){
            throw new IllegalArgumentException("Number of points don't match.");
        }
        double[][] XArray = new double[point_num][4];
        for(int i=0; i<point_num; i++){
            Log.d("TRIANGULATEmulti","第"+i+"个点坐标"+PoList_1.getMatrix(i,i,0,2).get(0,0)+","+PoList_1.getMatrix(i,i,0,2).get(0,1));
            Log.d("TRIANGULATEmulti","第"+i+"个点坐标"+PoList_2.getMatrix(i,i,0,2).get(0,0)+","+PoList_2.getMatrix(i,i,0,2).get(0,1));
            XArray[i] = TriangulatePoints(PoList_1.getMatrix(i,i,0,2),PoList_2.getMatrix(i,i,0,2),P1,P2).getRowPackedCopy();
        }
        return new Matrix(XArray).transpose();
    }


    /**
     * 求找出的特征点的三位坐标
     * @param FeaturePoList_1 特征匹配点
     * @param FeaturePoList_2 特征匹配点
     * @return FeaturePoints3D
     */
    public ArrayList<ImageMarker> CalculateFeaturePoints3D(ArrayList<ImageMarker> FeaturePoList_1, ArrayList<ImageMarker> FeaturePoList_2){
        Matrix PoList_1 = Point2D3DToHomogeneous(MarkerListToArray(FeaturePoList_1), 0);//n*3矩阵
        Matrix PoList_2 = Point2D3DToHomogeneous(MarkerListToArray(FeaturePoList_2), 0);//n*3矩阵
        Matrix X = TriangulateMultiPoints(PoList_1,PoList_2,P1,this.P2_Selected);//4*n
        FeaturePoints3D = ArrayToMarkerList(X.transpose(),OpticalCenter[0],OpticalCenter[1]);
        return FeaturePoints3D;
    }

    //To be continue..

    /**
     * 计算投影点和真实投影点的误差
     * @param Point3D 求得的3D坐标点
     * @param poList1 真实匹配点
     * @param poList2 真实匹配点
     * @return 各点的误差值
     */
    public void CalculateError(Matrix Point3D, double[][] poList1, double[][] poList2){
//        out.println("==============投影矩阵P的值为==================");
//        for(int i=0; i<3; i++){
//            for(int j=0; j<4; j++){
//                out.println("P2第"+i+","+j+"个点的值：" + P2_Selected.get(i,j));
//            }
//        }
        for (int i = 0; i < X_3D.getRowDimension(); i++) {
            out.println("****************" + X_3D.get(i, 0) + "," + X_3D.get(i, 1) + "," + X_3D.get(i, 2) + "," + X_3D.get(i, 3));
        }
        out.println("==============投影回二维的点坐标==================");
        Matrix PointOrigin1 = Point2D3DToHomogeneous(poList1,0);
        Matrix PointOrigin2 = Point2D3DToHomogeneous(poList2,0);
        /////////////////////////////////////////////////
        ///加上偏移量
//        Matrix OffsetXY = new Matrix(Point3D.getRowDimension(),3);
//        for(int i=0; i<Point3D.getRowDimension(); i++){
//            OffsetXY.set(i,0, OpticalCenter[0]);
//            OffsetXY.set(i,1, OpticalCenter[1]);
//            OffsetXY.set(i,2, 0);
//        }
        ///////////////////////////////////////////////////
        Matrix Point2DComputed2 = QiCiTo1(P2_Selected.times(Point3D.transpose()).transpose());//(3*4)*(4*n).transpose()
        Matrix Point2DComputed1 = QiCiTo1(P1.times(Point3D.transpose()).transpose());
        Matrix error1 = Point2DComputed1.minus(PointOrigin1);
        Matrix error2 = Point2DComputed2.minus(PointOrigin2);
        double[] ErrorVal1 = new double[Point3D.getRowDimension()];
        double[] ErrorVal2 = new double[Point3D.getRowDimension()];
        for(int p=0; p<Point3D.getRowDimension(); p++){
            ErrorVal1[p] = sqrt(pow(error1.get(p,0),2) + pow(error1.get(p,1),2) + pow(error1.get(p,2),2));
            out.println("第"+p+"个点的误差值为(Image1)：=======" + ErrorVal1[p]);
            out.println("第"+p+"个点的真实坐标为：" + PointOrigin1.get(p,0)+","+PointOrigin1.get(p,1)+","+ PointOrigin1.get(p,2));
            out.println("第"+p+"个点的坐标为：" + Point2DComputed1.get(p,0)+","+Point2DComputed1.get(p,1)+","+ Point2DComputed1.get(p,2));
            ErrorVal2[p] = sqrt(pow(error2.get(p,0),2) + pow(error2.get(p,1),2) + pow(error2.get(p,2),2));
            out.println("第"+p+"个点的误差值为(Image2)：=======" + ErrorVal2[p]);
            out.println("第"+p+"个点的真实坐标为：" + PointOrigin2.get(p,0)+","+PointOrigin2.get(p,1)+","+ PointOrigin2.get(p,2));
            out.println("第"+p+"个点的坐标为：" + Point2DComputed2.get(p,0)+","+Point2DComputed2.get(p,1)+","+ Point2DComputed2.get(p,2));
        }
    }

    public Matrix QiCiTo1(Matrix PointList){
        for(int p=0; p<PointList.getRowDimension(); p++){
            for(int i=0; i<PointList.getColumnDimension(); i++){
                PointList.set(p,i,PointList.get(p,i)/PointList.get(p,PointList.getColumnDimension()-1));
            }
        }
        return PointList;
    }



    /**
     * 三维坐标反投影
     * @param Point3D
     */
    public void Point3DTo2D(Matrix Point3D){
        Matrix Point2DComputed1 = QiCiTo1(P1.times(Point3D.transpose()).transpose());//n*3
//        Matrix Point2DComputed1 = QiCiTo1(P1.times(Point3D).transpose());//n*3
        Point3Dto2D1 = ArrayToMarkerList(Point2DComputed1,OpticalCenter[0], OpticalCenter[1]);
        Matrix Point2DComputed2 = QiCiTo1(P2_Selected.times(Point3D.transpose()).transpose());//n*3
/*        out.println("==============P2_Selected 2的值为==================");
        for (int ii=0; ii<3; ii++){
            for (int j=0; j<4; j++){
                out.println("点("+ii+","+j+")的值：" + P2_Selected.get(ii,j));
            }
        }*/
//        Matrix Point2DComputed2 = QiCiTo1(P2_Selected.times(Point3D).transpose());//n*3
        Point3Dto2D2 = ArrayToMarkerList(Point2DComputed2,OpticalCenter[0], OpticalCenter[1]);

    }


    /**
     * 将MarkerList类转换为二维数组
     * @param markerList
     * @return
     */
    public static double[][] MarkerListToArray (ArrayList<ImageMarker> markerList) {
        double[][] array_markerList = new double[markerList.size()][2];
        for (int i = 0; i < markerList.size(); i++) {
            array_markerList[i][0] = markerList.get(i).x;
            array_markerList[i][1] = markerList.get(i).y;
        }
        return array_markerList;
    }

    /**
     * 将点转化为 MarkerList
     * @param X
     * @return
     */

    public static ArrayList<ImageMarker> ArrayToMarkerList (Matrix X, double offset_x, double offset_y) {
        ArrayList<ImageMarker> MarkerList = new ArrayList<>(X.getRowDimension());
        for (int i = 0; i < X.getRowDimension(); i++) {
            ImageMarker temp = new ImageMarker( X.get(i,0)+offset_x, X.get(i,1)+offset_y, X.get(i,2));
            temp.type = 2;

//            out.println("//"+temp.x+","+temp.y);
//    public static ArrayList<ImageMarker> ArrayToMarkerList (Matrix X, int MarkerColor) {
//        ArrayList<ImageMarker> MarkerList = new ArrayList<>(X.getRowDimension());
//        for (int i = 0; i < X.getRowDimension(); i++) {
//           // ImageMarker temp = new ImageMarker((float) X.get(i,0),(float) X.get(i,1),(float) X.get(i,2));
//            ImageMarker temp = new ImageMarker((float) X.get(i,0),(float) X.get(i,1),0);
//            temp.type = MarkerColor;

            MarkerList.add(temp);
        }
        return MarkerList;
    }

    public static ArrayList<ImageMarker> ArrayToMarkerList (Matrix X, double offset_x, double offset_y, double offset_z, int Type) {
        ArrayList<ImageMarker> MarkerList = new ArrayList<>(X.getRowDimension());
        for (int i = 0; i < X.getRowDimension(); i++) {
            ImageMarker temp = new ImageMarker( X.get(i,0)+offset_x, X.get(i,1)+offset_y, X.get(i,2) + offset_z);
            temp.type = Type;

//            out.println("//"+temp.x+","+temp.y);
//    public static ArrayList<ImageMarker> ArrayToMarkerList (Matrix X, int MarkerColor) {
//        ArrayList<ImageMarker> MarkerList = new ArrayList<>(X.getRowDimension());
//        for (int i = 0; i < X.getRowDimension(); i++) {
//           // ImageMarker temp = new ImageMarker((float) X.get(i,0),(float) X.get(i,1),(float) X.get(i,2));
//            ImageMarker temp = new ImageMarker((float) X.get(i,0),(float) X.get(i,1),0);
//            temp.type = MarkerColor;

            MarkerList.add(temp);
        }
        return MarkerList;
    }


    /**
     * 计算外极点
     * @param F 基本矩阵
     * @return 返回外极点坐标，行向量
     */
    public Matrix compute_epipole(Matrix F) {
        /*SingularValueDecomposition svd = F.svd();
        Matrix vt = svd.getV();
        Matrix e = vt.getMatrix(2,2,0,2);///????????????????????用的最后一行（已改）//
        e = e.times(1/e.get(0,2));
        out.println("*********************e**************"+e.get(0,0)+","+e.get(0,1)+","+e.get(0,2));*/

        SingularValueDecomposition svd = F.transpose().svd(); //+ Arrays.toString(e.getArray())
        Matrix u = svd.getV();
        Matrix e = u.getMatrix(0,2,2,2);
        e = e.transpose();
        e.print(1,3);
        e = e.times(1/e.get(0,2));
        Log.v("Convert2DTo3D","e :" );
        e.print(1,3);
        return e;
    }

    /**
     * 获取三维向量的叉积矩阵
     * @param point 三维向量坐标
     * @return 叉积矩阵
     */
    private Matrix getPointCrossMatrix(Matrix point) {
        int col = point.getColumnDimension();
        int row = point.getRowDimension();
        if (col < row) {
            point = point.transpose();
        }
        if (row > 1) {
            throw new IllegalArgumentException("Input is not a vector!");
        }
        double[][] array = new double[3][3];
        array[0][1] = -point.get(0,2);
        array[0][2] = point.get(0,1);
        array[1][0] = point.get(0,2);
        array[1][2] = -point.get(0,0);
        array[2][0] = -point.get(0,1);
        array[2][1] = point.get(0,0);
        Matrix crossMatrix = new Matrix(array);
        return crossMatrix;
    }

    /**
     * 相机自标定
     * @param F 相机基本矩阵
     * @param e 外极点
     * @return  [f1,f2,a]
     */
    public double[] cameraSelfCalibrate(Matrix F, Matrix e) {
        double[] result;

        Matrix A = new Matrix(6,3);

        A.set(0,0,(pow(F.get(0,0),2)+pow(F.get(0,1),2)));
        A.set(0,1,-pow(e.get(0,2),2));
        A.set(0,2,-pow(e.get(0,1),2));

        A.set(1,0,(F.get(0,0)*F.get(1,0)+F.get(0,1)*F.get(1,1)));
        A.set(1,2,e.get(0,0)*e.get(0,1));

        A.set(2,0,F.get(0,0)*F.get(2,0)+F.get(0,1)*F.get(2,1));
        A.set(2,1,e.get(0,0)*e.get(0,2));

        A.set(3,0,pow(F.get(1,0),2)+pow(F.get(1,1),2));
        A.set(3,1,-pow(e.get(0,2),2));
        A.set(3,2,-pow(e.get(0,0),2));

        A.set(4,0,F.get(1,0)*F.get(2,0)+F.get(1,1)*F.get(2,1));
        A.set(4,1,e.get(0,1)*e.get(0,2));

        A.set(5,0,pow(F.get(2,0),2)+pow(F.get(2,1),2));
        A.set(5,1,-pow(e.get(0,0),2)-pow(e.get(0,1),2));

        Matrix b = new Matrix(6,1);

        b.set(0,0,-pow(F.get(0,2),2));
        b.set(1,0,-F.get(0,2)*F.get(1,2));
        b.set(2,0,-F.get(0,2)*F.get(2,2));

        b.set(3,0,-pow(F.get(1,2),2));
        b.set(4,0,-F.get(1,2)*F.get(2,2));
        b.set(5,0,-pow(F.get(2,2),2));

        //SVD解方程组
//        Matrix Ab = new Matrix(6,4);
//        Ab.setMatrix(0,5,0,2,A);
//        Ab.setMatrix(0,5,3,3,b);
//
//        SingularValueDecomposition q = Ab.svd();
//        Matrix VT = q.getV();
//        result = VT.getMatrix(0,2,3,3).times(1/VT.get(3,3)).getColumnPackedCopy();
//        out.println("SVD解方程");
//        out.println("result[0] = "+result[0]);
//        out.println("result[1] = "+result[1]);
//        out.println("result[2] = "+result[2]);


        Matrix ATA = A.transpose().times(A);
        Matrix ATb = A.transpose().times(b);

        //3*3非齐次线性方程组求解
        result = linerFunction3_3(ATA,ATb);

        if (result[0] < 0 || result[1] < 0 || result[2] <0) {
            return null;
        }

        //换算为f1,f2,a
        result[2] = sqrt(result[2]);
        result[1] = sqrt(result[1]) / result[2];
        result[0] = sqrt(result[0]);

        return result;
    }

    /**
     * 求解3*3线性方程组
     * @param ATA 系数矩阵
     * @param ATb 常量矩阵
     * @return [x1 x2 x3]
     */
    private double [] linerFunction3_3(Matrix ATA, Matrix ATb) {
        double [] result = new double[3];
        //3*3非齐次线性方程组求解，列主元法
        double k;
        int index;
        Matrix row;
        double brow;
        //化为上三角矩阵
        for (int i = 0; i < 3; i++) {
            //找主元
            k = 0;
            index = i;
            for (int j = i; j < 3; j++) {
                if (abs(k) < abs(ATA.get(j,i))) {
                    k = ATA.get(j,i);
                    index = j;
                }
            }
            if (k == 0) {
                continue;
            }
            out.println("k = "+k);

            row = ATA.getMatrix(index,index,0,2).times(1/k);
            brow = ATb.get(index,0) / k;
            //主元行交换
            if (index != i) {
                ATA.setMatrix(index,index,0,2,ATA.getMatrix(i,i,0,2));
                ATb.set(index,0,ATb.get(i,0));
                ATA.setMatrix(i,i,0,2,row);
                ATb.set(i,0,brow);
            } else {
                ATA.setMatrix(index,index,0,2,row);
                ATb.set(index,0,brow);
            }
            //行运算
            for (int j = i+1; j < 3; j++) {
                ATb.set(j,0,(ATb.get(j,0)-brow*ATA.get(j,i)));
                ATA.setMatrix(j,j,0,2,ATA.getMatrix(j,j,0,2).minus(row.times(ATA.get(j,i))));
            }
        }

        //解上三角矩阵
        result[2] = ATb.get(2,0);
        result[1] = ATb.get(1,0) - ATA.get(1,2) * result[2];
        result[0] = ATb.get(0,0) - ATA.get(0,2) * result[2] - ATA.get(0,1) * result[1];

        out.println("result[0] = "+result[0]);
        out.println("result[1] = "+result[1]);
        out.println("result[2] = "+result[2]);
        return result;
    }

    public Matrix buildIntrinsicMatrix(double f) {
        double[][] array = {{f,0,0},{0,f,0},{0,0,1}};
        return new Matrix(array);
    }

    /**
     * 计算极线以及画极线
     * @param PoListHo1
     * @param PoListHo2
     * @param F
     */
    public void ComputeCorrespondEpiLines(Matrix PoListHo1, Matrix PoListHo2, Matrix F){
//        this.EpiLines2_Para = QiCiTo1(F.times(PoListHo1.transpose()).transpose()).transpose().getArray();//3*n
//        this.EpiLines1_Para = QiCiTo1((F.transpose()).times(PoListHo2.transpose()).transpose()).transpose().getArray();
        this.EpiLines2_Para = F.times(PoListHo1.transpose()).getArray();//3*n

        Matrix EPara2 = new Matrix(EpiLines2_Para);
        SingularValueDecomposition para2 = EPara2.transpose().svd();
        Matrix e2 = QiCiTo1(para2.getV().getMatrix(2,2,0,2));
//        Matrix e2 = para2.getV().getMatrix(0,2,2,2);
        out.println("=============e2==******&&&&&&&&&&&&&&^^^^^^^^^^^^=====");
        e2.print(1,3);
        Matrix re = EPara2.transpose().times(e2.transpose());
        re.transpose().print(1,EPara2.getRowDimension());

        this.EpiLines1_Para = F.transpose().times(PoListHo2.transpose()).getArray();
        Matrix EPara1 = new Matrix(EpiLines1_Para);
        SingularValueDecomposition para1 = EPara1.transpose().svd();
        Matrix e1 = QiCiTo1(para1.getV().getMatrix(2,2,0,2));
//        Matrix e2 = para2.getV().getMatrix(0,2,2,2);
        out.println("=============e2==******&&&&&&&&&&&&&&^^^^^^^^^^^^=====");
        e1.print(1,3);

        out.println("==============点到极线的距离P1==================");
        distance1 = DistancePointToLine(EpiLines1_Para, PoListHo1.getArray());
        out.println("==============点到极线的距离P2==================");
        distance2 = DistancePointToLine(EpiLines2_Para, PoListHo2.getArray());
    }

    public Bitmap DrawLine(Bitmap ori_img, double[][] PointParam){ //3*n
        System.out.println("Starting drawing EpiLines.....");
        Bitmap bitmap_new = ori_img.copy(Bitmap.Config.ARGB_8888,true);
        Canvas canvas = new Canvas(bitmap_new);
        Paint p = new Paint();
        p.setColor(Color.CYAN);//设置画笔颜色
        p.setAntiAlias(false);//设置画笔为无锯齿
        p.setStrokeWidth((float) 5.0);//线宽
        int PoNum = this.EpiLines1_Para[0].length;
        double[][] StartEnd = new double[PoNum][2];
        //获取起点和终点坐标
        for(int i=0; i<PoNum; i++){
//            double left = -0.5*ori_img.getWidth();
//            double right = 0.5*ori_img.getWidth();
//            StartEnd[i][0] = -(PointParam[2][i]+left*PointParam[0][i])/PointParam[1][i];
//            StartEnd[i][1] = -(PointParam[2][i]+right*PointParam[0][i])/PointParam[1][i];
//            System.out.println("Start:("+left+","+StartEnd[i][0]+"),End:("+right+","+StartEnd[i][1]+")");
//            canvas.drawLine((float) (-0.5*ori_img.getWidth()), (float) StartEnd[i][0], (float) (0.5*ori_img.getWidth()), (float) StartEnd[i][1], p);
            // double left = -0.5*ori_img.getWidth();
            //  double right = 0.5*ori_img.getWidth();
            //直线发生平移
            StartEnd[i][0] = (-PointParam[2][i]+OpticalCenter[0]*PointParam[0][i]+OpticalCenter[1]*PointParam[1][i])/PointParam[1][i];
            StartEnd[i][1] = (-PointParam[2][i]+(OpticalCenter[0]-(ori_img.getWidth()-1))*PointParam[0][i]+OpticalCenter[1]*PointParam[1][i])/PointParam[1][i];

//            StartEnd[i][0] = -(PointParam[2][i]-OpticalCenter[0]*PointParam[0][i]-OpticalCenter[1]*PointParam[1][i])/PointParam[1][i];
//            StartEnd[i][1] = -(PointParam[2][i]-(OpticalCenter[0]+ori_img.getWidth()-1)*PointParam[0][i]-OpticalCenter[1]*PointParam[1][i])/PointParam[1][i];
            //System.out.println("Start:("+left+","+StartEnd[i][0]+"),End:("+right+","+StartEnd[i][1]+")");
            canvas.drawLine(0, (float) StartEnd[i][0], (float) (ori_img.getWidth()-1), (float) StartEnd[i][1], p);
        }
        return bitmap_new;
    }

    public ArrayList<Double> DistancePointToLine(double[][] EpiLines_Para, double[][] PoList){
        int num = PoList.length;
        PriorityQueue<Double> dist = new PriorityQueue<Double>(num, (o1, o2) -> (o1 - o2)>0?1:-1);
        ArrayList<Double> distance = new ArrayList<>(num);
        double sum = 0;
        for(int i=0; i<num; i++){
//            double dis = abs(EpiLines_Para[0][i]*PoList[i][0] + EpiLines_Para[1][i]*PoList[i][1] + (EpiLines_Para[2][i]-(OpticalCenter[0]*EpiLines_Para[0][i]+OpticalCenter[1]*EpiLines_Para[1][i]))) / sqrt(pow(EpiLines_Para[0][i],2)+pow(EpiLines_Para[1][i],2));
            double dis = abs(EpiLines_Para[0][i]*PoList[i][0] + EpiLines_Para[1][i]*PoList[i][1] + EpiLines_Para[2][i]) / sqrt(pow(EpiLines_Para[0][i],2)+pow(EpiLines_Para[1][i],2));
            distance.add(dis);
            sum += dis;
            System.out.println("*************第"+i+"处点和极线的距离为***************："+dis);
            dist.add(dis);
        }
        out.println("==============点到极线的距离误差中值和均值为==================");
        out.println("距离误差中值为:"+ dist.toArray()[round(num/2)]+",均值为："+sum/num);
        return distance;
    }

    public double[] getMinMaxXYZ3D() {
        double[] minmaxXYZ = new double[6];
        double[] tempInt;
        for (int i = 0; i < 3; i++) {
            double[] tempDouble = X_3D.getMatrix(0,X_3D.getRowDimension()-1,i,i).getColumnPackedCopy();
            tempInt = getArrayMinMax(tempDouble);
            minmaxXYZ[2*i] = tempInt[0];
            minmaxXYZ[2*i+1] = tempInt[1];
        }
        return minmaxXYZ;
    }

    public double[] getArrayMinMax(double[] array) {
        double[] result = new double[2];
        Arrays.sort(array); //升序排列
        result[0] = array[0];
        result[1] = array[array.length-1];
        return result;
    }

    public void getMinusMin3DPoints(double[] minmaxXYZ) {
        if (FeaturePoints3D != null) {
            FeaturePoints3D.clear();
        }
        FeaturePoints3D = ArrayToMarkerList(X_3D,-minmaxXYZ[0],-minmaxXYZ[2],-minmaxXYZ[4],4);
        maxXYZ[0] = (int) (minmaxXYZ[1] - minmaxXYZ[0]);
        maxXYZ[1] = (int) (minmaxXYZ[3] - minmaxXYZ[2]);
        maxXYZ[2] = (int) (minmaxXYZ[5] - minmaxXYZ[4]);
    }

}
