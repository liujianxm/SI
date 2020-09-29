package com.example.si.IMG_PROCESSING.Reconstruction3D;



import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.example.si.IMG_PROCESSING.CornerDetection.ImageMarker;

import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Vector;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

import static java.lang.Double.isFinite;
import static java.lang.StrictMath.PI;
import static java.lang.StrictMath.abs;
import static java.lang.StrictMath.acos;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.min;
import static java.lang.StrictMath.pow;
import static java.lang.StrictMath.round;
import static java.lang.StrictMath.sqrt;
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
    ArrayList<Double> distance1;
    ArrayList<Double> distance2;
    Matrix PoSelected1 = null;
    Matrix PoSelected2 = null;
    double sigma = 15.0f;
    double mSigma2 = sigma*sigma;
    Matrix R = new Matrix(3, 3);
    Matrix t21 = new Matrix(3, 1);
    Vector<Boolean> vbTriangulated;
    final String TAG = "Convert2DTo3D";
    Matrix vP3D4_item = null;
//    ArrayList<double[]> vP3D1 = new ArrayList<double[]>();


    /**
     * 2D-3D主函数, MobileModel mm
     * @param PoList_1 n*2格式储存的点数组
     * @param PoList_2 n*2格式储存
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean Convert2DTo3D_Fun_new(double[][] PoList_1, double[][] PoList_2, MobileModel mm){
//    public boolean Convert2DTo3D_Fun(double[][] PoList_1, double[][] PoList_2){
        ////////////////////此处经过修改！！！！！！！！
        Matrix K = get_IntrinsicMatrix(mm).transpose();//3*3矩阵///此处经过修改！！！！！！！！
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
        boolean Flag = ReconstructF(F, K,1,15);
        Log.d("Convert2DTo3D_new","Flag:"+Flag);
        P2_Selected = new Matrix(3,4);
        P2_Selected.setMatrix(0,2,0,2,R);
        P2_Selected.setMatrix(0,2,3,3,t21);
        P2_Selected = K.times(P2_Selected);
        Matrix e = compute_epipole(F.transpose());  //计算外极点
/*        Matrix E1 = K.transpose().times(F.times(K));
        out.println("==============本质矩阵E1的值为==================");
        for (int i=0; i<3; i++){
            for (int j=0; j<3; j++){
                out.println("点("+i+","+j+")的值：" + E1.get(i,j));
            }
        }
        ArrayList<Matrix> P2 = ComputePFromEssential(E1);//计算第二角度可能的投影矩阵
        //Matrix F = compute_fundamental_normalized(Po1, Po2);
        //Matrix F = compute_fundamental_normalized(PoSelected1, PoSelected2);*/
        ////////计算极线参数abc/////////
        ComputeCorrespondEpiLines(PoSelected1, PoSelected2, F);
        CalculateError(X_3D, PoSelected1.getArray(), PoSelected2.getArray());


//////////////////////////////内参自标定///////////////////////////////////
    /*    Matrix Po1 = Point2DToHomogeneous(PoList_1);//n*3矩阵齐次坐标
        Matrix Po2 = Point2DToHomogeneous(PoList_2);//n*3矩阵
          Matrix F = RANSAC.MyRansac(Po1,Po2,9,500,5*10E-3,0.7);
          out.println("==============F的值为==================");
          for (int i=0; i<3; i++){
              for (int j=0; j<3; j++){
                  out.println("点("+i+","+j+")的值：" + F.get(i,j));
              }
          }
          PoSelected1 = new Matrix(RANSAC.PoListHoSelected_1);
          PoSelected2 = new Matrix(RANSAC.PoListHoSelected_2);
        //计算内参矩阵
        Matrix e = compute_epipole(F);  //计算外极点

        out.println("==============本质矩阵E的值为==================");
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
        intrinsic = buildIntrinsicMatrix(paras[0]);
        F = compute_fundamental_normalized(PoSelected1, PoSelected2);
        ArrayList<Matrix> P2 = ComputePFromEssential(F);//计算第二角度可能的投影矩阵*/
///////////////////////////////////////////////////////////////////////

/*        ////////计算极线参数abc/////////
        ComputeCorrespondEpiLines(PoSelected1, PoSelected2, F);
        ///////////////////////////////
        SelectPFrom4P(PoSelected1, PoSelected2, P1, P2);//恢复的三位坐标矩阵
        CalculateError(X_3D, PoSelected1.getArray(), PoSelected2.getArray());*/
        return true;
    }

    /////////////////////////////////////////////


    /**
     * 根据手机型号获得对应的相机内参，通过 MATLAB 标定
     * @param mm 手机型号
     * @return Intrinsic_matrix
     */
    public Matrix get_IntrinsicMatrix(MobileModel mm){
        Matrix Intrinsic_matrix;
        switch (mm){
            case MIX2:
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
     * @return n*3的 Matrix 格式的齐次坐标
     */
    public Matrix Point2DToHomogeneous(double[][] Po_list){
        int List_len = Po_list.length;
        double[][] temp = new double[List_len][3];
        for(int i=0; i<List_len; i++){
            for(int j=0; j<=2; j++){
                if(j==2) temp[i][j] = 1;
                else temp[i][j] = Po_list[i][j];
            }
        }
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
            //此处有改动 0914
            A_array[i] = new double[]{PoList_1.get(i,0)*PoList_2.get(i,0), PoList_1.get(i,0)*PoList_2.get(i,1), PoList_1.get(i,0)*PoList_2.get(i,2),
                    PoList_1.get(i,1)*PoList_2.get(i,0), PoList_1.get(i,1)*PoList_2.get(i,1), PoList_1.get(i,1)*PoList_2.get(i,2),
                    PoList_1.get(i,2)*PoList_2.get(i,0), PoList_1.get(i,2)*PoList_2.get(i,1), PoList_1.get(i,2)*PoList_2.get(i,2)};
/*            final double u1 = PoList_1.get(i,0);
            final double v1 = PoList_1.get(i,1);
            final double u2 = PoList_2.get(i,0);
            final double v2 = PoList_2.get(i,1);
            A_array[i] = new double[]{u2*u1, u2*v1, u2, v2*u1, v2*v1, v2, u1,v1,1};*/
        }
        Matrix A = new Matrix(A_array);
        //  out.println("==AAAAAAAAAAAAAAAAAAAAAAAA=====cond:"+ A.cond());
        //进行奇异值分解
        SingularValueDecomposition s = A.svd();
        Matrix V = s.getV();//9*9
        //  Matrix F = matrixReshape(V.getMatrix(8,8,0,8).getArray() ,3,3);
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
     * 计算E
     */
    public static Matrix compute_fundamental_normalized(Matrix PoList_1, Matrix PoList_2){//n*3
        int Po_num = PoList_1.getRowDimension();//获取匹配点对数
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
        Matrix T1 = new Matrix(temp1);
        PoList_1 = T1.times(PoList_1.transpose()).transpose();
        double[][] temp2 = {{S2,0,-S2*po2mean[0]}, {0,S2,-S2*po2mean[1]}, {0,0,1}};
        Matrix T2 = new Matrix(temp2);
        PoList_2 = T2.times(PoList_2.transpose()).transpose();
        Matrix F = ComputeFundamental(PoList_1,PoList_2);
        F = (T1.transpose()).times(F.times(T2));
        return F.times(1/F.get(2,2));
        // return F;
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
            meanStd = new double[2];
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
        Matrix Po = Point2DToHomogeneous(Po_list);//n*3矩阵
        Po = (K.inverse()).times(Po.transpose());//3*n矩阵
        return Po.transpose();
    }




    /**
     * 从E中分解出旋转和平移矩阵
     * @param E
     * @param R1
     * @param R2
     * @param t
     */
    private void DecomposeE(Matrix E, Matrix R1, Matrix R2, Matrix t){
        SingularValueDecomposition e = E.svd();
        Matrix Ue = e.getU();
        Matrix VeT = e.getV().transpose();
        Matrix W = new Matrix(new double[][]{{0, -1, 0}, {1, 0, 0}, {0, 0, 1}});
        t = Ue.getMatrix(0,2,2,2);
        t = t.times(1/t.norm2());//3*1
        R1 = Ue.times(W.times(VeT));
        if(R1.det()<0) R1 = R1.times(-1);
        R2 = Ue.times((W.transpose()).times(VeT));
        if(R2.det()<0) R2 = R2.times(-1);

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
     * @param parallax
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private int CheckRT(Matrix R, Matrix t, Matrix K, Matrix Po_1, Matrix Po_2, float th2,
                        Vector<Boolean> vbGood, float parallax){
//        vP3D1.clear();
        int num = Po_1.getRowDimension();
        //    vP3D = new Matrix(num,3);
        final double fx = K.get(0,0);
        final double fy = K.get(1,1);
        final double cx = K.get(0,2);
        final double cy = K.get(1,2);
        Vector<Double> vCosParallax = new Vector<>(num);
        vbGood = new Vector<>(num);
        // Camera 1 Projection Matrix K[I|0]
        P1 = K.times(P1);
        Matrix O1 = new Matrix(new double[][]{{0, 0, 0}});//1*3
        out.println("*************"+O1.getRowDimension());
        // Camera 2 Projection Matrix K[R|t]
        Matrix P2 = new Matrix(3,4);
        P2.setMatrix(0,2,0,2,R);
        P2.setMatrix(0,2,3,3,t);
        P2 = K.times(P2);
        Matrix O2 = (R.transpose().times(t)).times(-1);//3*1



        int nGood = 0;
        for(int i=0; i<num; i++){
            double point1_x = Po_1.get(i,0);
            double point1_y = Po_1.get(i,1);
            double point2_x = Po_2.get(i,0);
            double point2_y = Po_2.get(i,1);
            Matrix p3dC1 = TriangulatePoints(Po_1, Po_2, P1, P2).getMatrix(0,0,0,2);//1*3
            if(!isFinite(p3dC1.get(0,0))||!isFinite(p3dC1.get(0,1))||!isFinite(p3dC1.get(0,2))){
                vbGood.set(i,false);
                continue;
            }
            // Check parallax
            Matrix normal1 = p3dC1.minus(O1);//1*3
            double dist1 = normal1.norm2();
            Matrix normal2 = p3dC1.minus(O2.transpose());
            double dist2 = normal2.norm2();
            double cosParallax = normal1.times(normal2.transpose()).get(0,0)/(dist1*dist2);
            // Check depth in front of first camera (only if enough parallax, as "infinite" points can easily go to negative depth)
            if(p3dC1.get(0,2)<=0 && cosParallax<0.99998) continue;
            // Check depth in front of second camera (only if enough parallax, as "infinite" points can easily go to negative depth)
            Matrix p3dC2 = R.times(p3dC1.transpose()).plus(t).transpose();//1*3
            if(p3dC2.get(0,2)<=0 && cosParallax<0.99998) continue;
            // Check reprojection error in first image
            double im1x, im1y;
            double invZ1 = 1.0/p3dC1.get(0,2);
            im1x = fx*p3dC1.get(0,0)*invZ1 + cx;
            im1y = fy*p3dC1.get(0,1)*invZ1 + cy;
            double squareError1 = pow(im1x-point1_x,2)+pow(im1y-point1_y,2);
            if(squareError1 > th2) continue;
            // Check reprojection error in second image
            double im2x, im2y;
            double invZ2 = 1.0/p3dC2.get(0,2);
            im2x = fx*p3dC2.get(0,0)*invZ2 + cx;
            im2y = fy*p3dC2.get(0,1)*invZ2 + cy;
            double squareError2 = pow(im2x-point2_x,2)+pow(im2y-point2_y,2);
            if(squareError2 > th2) continue;
            vCosParallax.add(cosParallax);
            vP3D4_item.setMatrix(i, i,0,2, p3dC1);

//            vP3D1.add(p3dC1.getRowPackedCopy());

            Log.v("CheckRT","Have added one 3d Point..++++++++++++++");
            nGood++;
            if(cosParallax<0.99998){
                vbGood.set(i, true);
            }
        }
        // 得到3D点中较大的视差角
        if(nGood>0){
            Log.v("CheckRT","Have enter here + nGood");
            //将视差角余弦有小到大排序, 取出第50个，或者最后那个也就是最大那个
            vCosParallax.sort(new Comparator<Double>() {
                @Override
                public int compare(Double o1, Double o2) {
                    return o1 - o2 > 0 ? 1:-1;
                }
            });
            int idx = min(50, vCosParallax.size()-1);
            parallax = (float) (acos(vCosParallax.get(idx))*180/PI);
        }else {
            parallax = 0;
        }
        return nGood;
    }

    /**
     * 从基础矩阵恢复位姿
     * @param F
     * @param K
     * @param minParallax
     * @param minTriangulated
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean ReconstructF(Matrix F, Matrix K, float minParallax, int minTriangulated){
        // Compute Essential Matrix from Fundamental Matrix
        Matrix E = K.transpose().times(F.times(K));
        Matrix R1 = new Matrix(3,3), R2 = new Matrix(3,3), t = new Matrix(3,1);
        // Recover the 4 motion hypotheses
        DecomposeE(E, R1, R2, t);
        Matrix t1 = t;
        Matrix t2 = t.times(-1);
        // Reconstruct with the 4 hyphoteses and check
        ArrayList<Matrix> vP3D4 = new ArrayList<>(4);
        int num = PoSelected1.getRowDimension();
        Vector<Boolean> vbTriangulated1 = new Vector<>(num);
        Vector<Boolean> vbTriangulated2 = new Vector<>(num);
        Vector<Boolean> vbTriangulated3 = new Vector<>(num);
        Vector<Boolean> vbTriangulated4 = new Vector<>(num);
        float parallax1 = 0,parallax2 = 0, parallax3 = 0, parallax4 = 0;
        vP3D4_item = new Matrix(num, 3);
        int nGood1 = CheckRT(R1, t1, K, PoSelected1, PoSelected2, (float) mSigma2, vbTriangulated1, parallax1);
//        Matrix vp3d = ArrayListToMatrix(vP3D1);
        Log.v(TAG,"++++++++++++++nGood1++++++++++++++++"+nGood1);
        for(int j=0; j<vP3D4_item .getRowDimension(); j++){
            out.println("第"+j+"个："+vP3D4_item .get(j,0)+","+vP3D4_item .get(j,1)+","+vP3D4_item.get(j,2));
        }
        vP3D4.add(vP3D4_item);
        vP3D4_item = new Matrix(num, 3);
        int nGood2 = CheckRT(R2, t1, K, PoSelected1, PoSelected2, (float) mSigma2, vbTriangulated2, parallax2);
        Log.v(TAG,"++++++++++++++nGood2++++++++++++++++"+nGood2);
        for(int j=0; j<vP3D4_item.getRowDimension(); j++){
            out.println("第"+j+"个："+vP3D4_item.get(j,0)+","+vP3D4_item.get(j,1)+","+vP3D4_item.get(j,2));
        }
        vP3D4.add(vP3D4_item);
        vP3D4_item = new Matrix(num, 3);
        int nGood3 = CheckRT(R1, t2, K, PoSelected1, PoSelected2, (float) mSigma2, vbTriangulated3, parallax3);
        Log.v(TAG,"++++++++++++++nGood3++++++++++++++++"+nGood3);
        for(int j=0; j<vP3D4_item.getRowDimension(); j++){
            out.println("第"+j+"个："+vP3D4_item.get(j,0)+","+vP3D4_item.get(j,1)+","+vP3D4_item.get(j,2));
        }
        vP3D4.add(vP3D4_item);
        vP3D4_item = new Matrix(num, 3);
        int nGood4 = CheckRT(R2, t2, K, PoSelected1, PoSelected2, (float) mSigma2, vbTriangulated4, parallax4);
        Log.v(TAG,"++++++++++++++nGood4++++++++++++++++"+nGood4);
        for(int j=0; j<vP3D4_item.getRowDimension(); j++){
            out.println("第"+j+"个："+vP3D4_item.get(j,0)+","+vP3D4_item.get(j,1)+","+vP3D4_item.get(j,2));
        }
        vP3D4.add(vP3D4_item);
        int maxGood = max(nGood1,max(nGood2,max(nGood3,nGood4)));
/*        Matrix R = new Matrix(3, 3);
        Matrix t21 = new Matrix(3, 1);*/
        // minTriangulated为可以三角化恢复三维点的个数
        int nMinGood = max((int)0.9*num, minTriangulated);
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
            Log.v(TAG,"HERE");
            return false;
        }
        // If best reconstruction has enough parallax initialize
//        Vector<Boolean> vbTriangulated;
        // Matrix vP3D;
        Log.v(TAG,"Have Triangulated..........");
        if(maxGood == nGood1){
            if(parallax1>minParallax){
                X_3D = vP3D4.get(0);
                vbTriangulated = vbTriangulated1;
                R = R1;
                t21 = t1;
                return true;
            }
        }else if(maxGood == nGood2){
            if(parallax2>minParallax)
            {
                X_3D = vP3D4.get(1);
                vbTriangulated = vbTriangulated2;
                R = R2;
                t21 = t1;
                return true;
            }
        }else if(maxGood == nGood3){
            if(parallax3>minParallax)
            {
                X_3D = vP3D4.get(2);
                vbTriangulated = vbTriangulated3;
                R = R1;
                t21 = t2;
                return true;
            }
        }else if(maxGood==nGood4)
        {
            if(parallax4>minParallax)
            {
                X_3D = vP3D4.get(3);
                vbTriangulated = vbTriangulated4;
                R = R2;
                t21 = t2;
                return true;
            }
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
     * 利用本质矩阵求解投影矩阵，共有四种可能，需要后续排除（应该是存在问题！！！！！！！！！！！！！！）
     * @param E 本质矩阵
     * @return 四种可能的 P 矩阵 3*4
     */
//    public ArrayList<Matrix> ComputePFromEssential(Matrix F) {
    public ArrayList<Matrix> ComputePFromEssential(Matrix E) {
//        Matrix E = (intrinsic.transpose()).times(F.times(intrinsic));
        //进行奇异值分解
        out.println("&&&&&&&&&&&&&&&&& rank(E):"+E.rank());
        SingularValueDecomposition e = E.svd();
        Matrix VeT = e.getV().transpose();
        if ((e.getU().times(VeT)).det() < 0) {
            VeT = VeT.times(-1);
        }
//        Matrix reE = e.getU().times(e.getS().times(VeT));
//        out.println("==============本质矩阵分解得到的USV恢复的E的值为==================");
//        for (int i=0; i<3; i++){
//            for (int j=0; j<3; j++){
//                out.println("点("+i+","+j+")的值U：" + reE.get(i,j));
//            }
//        }
//        I_Matrix.set(2, 2, 0);
//        E = e.getU().times(I_Matrix.times(e.getV()));
        Matrix W = new Matrix(new double[][]{{0, -1, 0}, {1, 0, 0}, {0, 0, 1}});
        ArrayList<Matrix> P2 = new ArrayList<>(4);
        //double[][] temp_array = new double[3][4];
        Matrix Ue = e.getU();
        Matrix UWV = (Ue.times(W.times(VeT)));
/*        out.println("==============UWV==================");
        for (int ii=0; ii<3; ii++){
            for (int j=0; j<3; j++){
                out.println("点("+ii+","+j+")的值：" + UWV.get(ii,j));
            }
        }*/
        Matrix UWtV = (Ue.times((W.transpose()).times(VeT)));
        Matrix U2 = Ue.getMatrix(0,2,2,2);
/*        out.println("==============Ut==================");
        for (int ii=0; ii<3; ii++){
            out.println("点("+ii+"的值：" + U2.get(ii,0));
        }*/
        for(int i=0; i<4; i++){
            switch (i){
                case 0:
                    Matrix item = new Matrix(3,4);
                    item.setMatrix(0,2,0,2,UWV);
                    item.setMatrix(0,2,3,3,U2);
                    P2.add(item);
/*                    out.println("==============P2 1的值为==================");
                    for (int ii=0; ii<3; ii++){
                        for (int j=0; j<4; j++){
                            out.println("点("+ii+","+j+")的值：" + item.get(ii,j));
                        }
                    }*/
                    break;
                case 1:
                    Matrix item1 = new Matrix(3,4);
                    item1.setMatrix(0,2,0,2,UWV);
                    item1.setMatrix(0,2,3,3,U2.times(-1));
                    P2.add(item1);
/*                    out.println("==============P2 2的值为==================");
                    for (int ii=0; ii<3; ii++){
                        for (int j=0; j<4; j++){
                            out.println("点("+ii+","+j+")的值：" + item1.get(ii,j));
                        }
                    }*/
                    break;
                case 2:
                    Matrix item2 = new Matrix(3,4);
                    item2.setMatrix(0,2,0,2,UWtV);
                    item2.setMatrix(0,2,3,3,U2);
                    P2.add(item2);
/*                    out.println("==============P2 3的值为==================");
                    for (int ii=0; ii<3; ii++){
                        for (int j=0; j<4; j++){
                            out.println("点("+ii+","+j+")的值：" + item2.get(ii,j));
                        }
                    }*/
                    break;
                case 3:
                    Matrix item3 = new Matrix(3,4);
                    item3.setMatrix(0,2,0,2,UWtV);
                    item3.setMatrix(0,2,3,3,U2.times(-1));
                    P2.add(item3);
/*                    out.println("==============P2 4的值为==================");
                    for (int ii=0; ii<3; ii++){
                        for (int j=0; j<4; j++){
                            out.println("点("+ii+","+j+")的值：" + item3.get(ii,j));
                        }
                    }*/
                    break;
            }
        }
        return P2;
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
//        out.println("==============矩阵M的值为==================");
//        for(int i=0; i<4; i++){
//            for(int j=0; j<4; j++){
//                out.println("M的第("+i+","+j+")个点的值：" + M.get(i,j));
//            }
//        }
        SingularValueDecomposition m = M.svd();
        Matrix X = m.getV().getMatrix(0,3,3,3);//V取最后一列
        X = X.times(1/(X.get(3,0)));
//        out.println("!!!!!!!!!!!X的值：" + X.get(0,0)+","+X.get(1,0)+","+X.get(2,0));
//        out.println("@@@@@@@@@@@@@@@@@@@@@@@"+M.times(X).get(0,0));
//        out.println("=======================cond:"+ M.cond());
        return X.transpose();
    }

/* //弃用，计算结果有问题，有待研究
    public Matrix TriangulatePoints(Matrix Po_1, Matrix Po_2, Matrix P1, Matrix P2){
        double[][] ZeroArray = new double[6][6];
        Matrix M = new Matrix(ZeroArray);
        M.setMatrix(0,2,0,3,P1);
        M.setMatrix(3,5,0,3,P2);
        M.setMatrix(0,2,4,4,Po_1.times(-1).transpose());
        M.setMatrix(3,5,5,5,Po_2.times(-1).transpose());
        SingularValueDecomposition m = M.svd();
        out.println("=======================cond:"+ M.cond());
        Matrix X = m.getV().getMatrix(5,5,0,3);
        X = X.times(1/(X.get(0,3)));
        return X;
    }*/



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
            XArray[i] = TriangulatePoints(PoList_1.getMatrix(i,i,0,2),PoList_2.getMatrix(i,i,0,2),P1,P2).getRowPackedCopy();
        }
        return new Matrix(XArray).transpose();
    }

    /**
     * 获得投影矩阵以及恢复的三维点坐标
     * @param PoList_1 第一幅图的点对
     * @param PoList_2 第二幅图的点对
     * @param P1 第一个角度的投影矩阵
     * @param P2List 4个可能的投影矩阵
     */
    public void SelectPFrom4P(Matrix PoList_1, Matrix PoList_2, Matrix P1, ArrayList<Matrix> P2List){
/*        P1 = K.times(P1);
        for(int i=0; i<4; i++){
            P2List.set(i, K.times(P2List.get(i)));
        }*/
        int ind = 0;
        double maxres = 0;
        Matrix X;
        boolean[] Flag = new boolean[PoList_1.getRowDimension()];//用来标记深度均为正的点
        for(int i=0; i<4; i++){
            X = TriangulateMultiPoints(PoList_1,PoList_2,P1,P2List.get(i));//4*n
//            out.println("#####===========P2( "+i+" )的值为============#####");
//            for (int ii=0; ii<3; ii++){
//                for (int j=0; j<4; j++){
//                    out.println("点("+ii+","+j+")的值：" + P2List.get(i).get(ii,j));
//                }
//            }
            double[] d1 = P1.times(X).getArray()[2];
            double[] d2 = P2List.get(i).times(X).getArray()[2];
            double sum = 0;
            boolean[] f = new boolean[d1.length];
            //////////此处需要重新改一下////////
            for(int j=0; j<d1.length; j++){
//                System.out.println("d1[j]:"+d1[j]+" " + "d2[j]:"+d2[j]);
//                if(d1[j]>0) sum += d1[j];
//                if(d2[j]>0) sum += d2[j];
                f[j] = d1[j] > 0 && d2[j] > 0;
                sum++;
            }
//            if(sum > maxres){
            if(sum > maxres){
                maxres = sum;
                ind = i;
                Flag = f;
            }
        }
        int count = 0;//可改成 maxres
        for (boolean b : Flag) {
            if (b) {
                //out.println(b);
                count++;
            }
        }
        this.P2_Selected = P2List.get(ind);
        out.println("==============P2 2的值为==================");
        for (int ii=0; ii<3; ii++){
            for (int j=0; j<4; j++){
                out.println("点("+ii+","+j+")的值：" + P2_Selected.get(ii,j));
            }
        }
        System.out.println("##############################################");
        X = TriangulateMultiPoints(PoList_1,PoList_2,P1,this.P2_Selected);//4*n
        System.out.println("==============所得三维点坐标==================");
        for (int j=0; j<X.getColumnDimension(); j++){
            out.println(j+"点的值：" + X.get(0,j)+","+X.get(1,j)+ ","+X.get(2,j)+ ","+X.get(3,j));
        }
        System.out.println(X.getRowDimension()+","+X.getColumnDimension());
        System.out.println("Flag.length:"+Flag.length);
        System.out.println("count:"+count);
        double[][] XSelected = new double[X.getColumnDimension()][4];
        //       double[][] XSelected = new double[9][4];//暂改
        //       int pos=0;
        for(int k = 0; k<Flag.length; k++){
//            if(!Flag[k]){
            XSelected[k] = X.getMatrix(0,3,k,k).getColumnPackedCopy();//此处有问题
            // XSelected[pos] = (X.getMatrix(0,3,k,k).times(1/X.get(3,k))).getColumnPackedCopy();//除以其次坐标
//                pos++;
//            }
        }
        this.X_3D = QiCiTo1(new Matrix(XSelected));
    }

    /**
     * 求找出的特征点的三位坐标
     * @param FeaturePoList_1 特征匹配点
     * @param FeaturePoList_2 特征匹配点
     * @return FeaturePoints3D
     */
    public ArrayList<ImageMarker> CalculateFeaturePoints3D(ArrayList<ImageMarker> FeaturePoList_1, ArrayList<ImageMarker> FeaturePoList_2){
        Matrix PoList_1 = Point2DToHomogeneous(MarkerListToArray(FeaturePoList_1));//n*3矩阵
        Matrix PoList_2 = Point2DToHomogeneous(MarkerListToArray(FeaturePoList_2));//n*3矩阵
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
        Matrix PointOrigin1 = Point2DToHomogeneous(poList1);
        Matrix PointOrigin2 = Point2DToHomogeneous(poList2);
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


//    public void Point3DTo2D(Matrix Point3D, Matrix P){
//        Matrix Point2DComputed1 = P1.times(Point3D.transpose()).transpose();//n*3
//        Point3Dto2D1 = ArrayToMarkerList(Point2DComputed1, OpticalCenter[0], OpticalCenter[1]);
//        Matrix Point2DComputed2 = P2_Selected.times(Point3D.transpose()).transpose();//n*3
//        Point3Dto2D2 = ArrayToMarkerList(Point2DComputed2, OpticalCenter[0], OpticalCenter[1]);

    /**
     * 三维坐标反投影
     * @param Point3D
     */
    public void Point3DTo2D(Matrix Point3D){
        Matrix Point2DComputed1 = QiCiTo1(P1.times(Point3D.transpose()).transpose());//n*3
        Point3Dto2D1 = ArrayToMarkerList(Point2DComputed1,OpticalCenter[0], OpticalCenter[1]);
        Matrix Point2DComputed2 = QiCiTo1(P2_Selected.times(Point3D.transpose()).transpose());//n*3
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
            array_markerList[i][0] = round(markerList.get(i).x);
            array_markerList[i][1] = round(markerList.get(i).y);
        }
        return array_markerList;
    }

    /**
     * 将点转化为 MarkerList
     * @param X
     * @return
     */

    public static ArrayList<ImageMarker> ArrayToMarkerList (Matrix X, int offset_x, int offset_y) {
        ArrayList<ImageMarker> MarkerList = new ArrayList<>(X.getRowDimension());
        for (int i = 0; i < X.getRowDimension(); i++) {
            ImageMarker temp = new ImageMarker((float) X.get(i,0)+offset_x,(float) X.get(i,1)+offset_y,(float) X.get(i,2));
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
//    double[][] polist1 = new double[][]{{326, 314}, {325, 369}, {332, 445}, {334, 506}, {377, 314}, {377, 370}, {388, 426}, {392, 478}, {438, 312}};
//    double[][] polist2 = new double[][]{{456, 402}, {471, 465}, {486, 511}, {506, 557}, {497, 385}, {515, 424}, {536, 469}, {548, 512}, {539, 361}};
//    Matrix E_set = new Matrix(new double[][]{{9.2657E-04, -3.0478E-01, 1.0417E-02},{-5.5426E-02, -3.2049E-01, 6.2389E-01},{7.2953E-04, -6.4118E-01, 2.9404E-02}});
//    Matrix K = get_IntrinsicMatrix(MobileModel.MIX2);//3*3矩阵
//    Matrix F_set = (K.inverse().transpose()).times(E_set.times(K.inverse()));
//    Matrix Po = Point2DToHomogeneous(polist1);//n*3矩阵

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
        this.EpiLines2_Para = F.transpose().times(PoListHo1.transpose()).getArray();//3*n

        Matrix EPara2 = new Matrix(EpiLines2_Para);
        SingularValueDecomposition para2 = EPara2.transpose().svd();
        Matrix e2 = QiCiTo1(para2.getV().getMatrix(2,2,0,2));
//        Matrix e2 = para2.getV().getMatrix(0,2,2,2);
        out.println("=============e2==******&&&&&&&&&&&&&&^^^^^^^^^^^^=====");
        e2.print(1,3);
        Matrix re = EPara2.transpose().times(e2.transpose());
        re.transpose().print(1,EPara2.getRowDimension());

        this.EpiLines1_Para = F.times(PoListHo2.transpose()).getArray();
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

}
