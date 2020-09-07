package com.example.si.IMG_PROCESSING.Reconstruction3D;



import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.example.si.IMG_PROCESSING.CornerDetection.ImageMarker;

import java.util.ArrayList;
import java.util.Objects;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

import static java.lang.StrictMath.abs;
import static java.lang.StrictMath.pow;
import static java.lang.StrictMath.round;
import static java.lang.StrictMath.sqrt;
import static java.lang.System.out;


public class Convert2DTo3D {
    public enum MobileModel { MIX2, HUAWEI };//枚举手机型号
    //    private static Matrix I_Matrix = new Matrix(new double[][] {{1,0,0},{0,1,0},{0,0,1}});
    public MobileModel MyMobileModel;
    public Matrix X_3D = null;//用来存放恢复匹配点的三维坐标
    public Matrix P1 = new Matrix(new double[][]{{1, 0, 0, 0}, {0, 1, 0, 0}, {0, 0, 0, 1}});//3*4矩阵
    public Matrix P2_Selected;//筛选出的角度二的投影矩阵
    public ArrayList<ImageMarker> FeaturePoints3D;//特征点对应的三维点坐标
    public ArrayList<ImageMarker> Point3Dto2D1;//特征点对应的三维点坐标
    public ArrayList<ImageMarker> Point3Dto2D2;//特征点对应的三维点坐标
    public int[] OpticalCenter = new int[2];
    public Matrix intrinsic;
    ArrayList<Double> distance1;
    ArrayList<Double> distance2;

    public double[][] EpiLines1_Para = null;
    public double[][] EpiLines2_Para = null;

    /**
     * 2D-3D主函数, MobileModel mm
     * @param PoList_1 n*2格式储存的点数组
     * @param PoList_2 n*2格式储存
     */
    public boolean Convert2DTo3D_Fun(double[][] PoList_1, double[][] PoList_2){
        ////////////////////此处经过修改！！！！！！！！
//        Matrix K = get_IntrinsicMatrix(mm).transpose();//3*3矩阵///此处经过修改！！！！！！！！
//        Matrix Po1 = Point2DToHomogeneous(PoList_1);//n*3矩阵
//        Matrix Po2 = Point2DToHomogeneous(PoList_2);//n*3矩阵
//        Matrix PoListHo_1 = K.inverse().times(Po1.transpose()).transpose();
//        Matrix PoListHo_2 = ((K.inverse()).times(Po2.transpose())).transpose();//3*n矩阵
//        Matrix E = compute_fundamental_normalized(PoListHo_1, PoListHo_2);
//        ArrayList<Matrix> P2 = ComputePFromEssential(E);//计算第二角度可能的投影矩阵

//////////////////////////////内参自标定///////////////////////////////////
        Matrix Po1 = Point2DToHomogeneous(PoList_1);//n*3矩阵齐次坐标
        Matrix Po2 = Point2DToHomogeneous(PoList_2);//n*3矩阵
        Matrix F = compute_fundamental_normalized(Po1, Po2);
        //计算内参矩阵
        Matrix e = compute_epipole(F);  //计算外极点

        out.println("==============本质矩阵F的值为==================");
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
        intrinsic = buildIntrinsicMatrix((paras[0]+paras[1])/2);
        ArrayList<Matrix> P2 = ComputePFromEssential(F);//计算第二角度可能的投影矩阵
///////////////////////////////////////////////////////////////////////

        ////////计算极线参数abc/////////
        ComputeCorrespondEpiLines(Po1, Po2, F);
        ///////////////////////////////



//        Matrix PoListHo_1 = CalculateKPoList(PoList_1,this.MyMobileModel);
//        Matrix PoListHo_2 = CalculateKPoList(PoList_2,this.MyMobileModel);//inv(K)*x，矩阵格式为n*3
//        Matrix E = ComputeFundamental(PoListHo_1, PoListHo_2);//计算本质矩阵E



        //Matrix P1 = new Matrix(new double[][]{{1, 0, 0, 0}, {0, 1, 0, 0}, {0, 0, 0, 1}});//3*4矩阵
        //齐次坐标
//        Matrix Po1 = Point2DToHomogeneous(PoList_1);//n*3矩阵
//        Matrix Po2 = Point2DToHomogeneous(PoList_2);//n*3矩阵
        //////////////////////////////////
   //     Matrix F = compute_fundamental_normalized(Po1, Po2);
//        out.println("==============本质矩阵F的值为==================");
//        for (int i=0; i<3; i++){
//            for (int j=0; j<3; j++){
//                out.println("点("+i+","+j+")的值：" + F.get(i,j));
//            }
//        }
        SelectPFrom4P(Po1,Po2,P1,P2);//恢复的三位坐标矩阵
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
    public Matrix ComputeFundamental(Matrix PoList_1, Matrix PoList_2){
        int Po_num = PoList_1.getRowDimension();//获取匹配点对数
        if(PoList_2.getRowDimension() != Po_num || Po_num <= 8){
            throw new IllegalArgumentException("Number of points don't match OR Need more than 8 matched points.");
        }
        double[][] A_array = new double[Po_num][9];
        for(int i=0; i<Po_num; i++){
            A_array[i] = new double[]{PoList_1.get(i,0)*PoList_2.get(i,0), PoList_1.get(i,0)*PoList_2.get(i,1), PoList_1.get(i,0)*PoList_2.get(i,2),
                    PoList_1.get(i,1)*PoList_2.get(i,0), PoList_1.get(i,1)*PoList_2.get(i,1), PoList_1.get(i,1)*PoList_2.get(i,2),
                    PoList_1.get(i,2)*PoList_2.get(i,0), PoList_1.get(i,2)*PoList_2.get(i,1), PoList_1.get(i,2)*PoList_2.get(i,2)};
        }
        Matrix A = new Matrix(A_array);
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
        Sf.set(2,0,0);//最后一行置零
        Sf.set(2,1,0);
        Sf.set(2,2,0);
        F = Uf.times(Sf.times(Vf.transpose()));
        return F;
    }

    /**
     * 计算E
     */
    public Matrix compute_fundamental_normalized(Matrix PoList_1, Matrix PoList_2){//n*3
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
    public double[] AvgValue(Matrix Po_list){
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
     * 利用本质矩阵求解投影矩阵，共有四种可能，需要后续排除
     * @param F 本质矩阵
     * @return 四种可能的 P 矩阵 3*4
     */
    public ArrayList<Matrix> ComputePFromEssential(Matrix F) {
        Matrix E = (intrinsic.transpose()).times(F.times(intrinsic));
        //进行奇异值分解
        SingularValueDecomposition e = E.svd();
        Matrix VeT = e.getV().transpose();
        if ((e.getU().times(VeT)).det() < 0) {
            VeT = VeT.times(-1);
        }
//        I_Matrix.set(2, 2, 0);
//        E = e.getU().times(I_Matrix.times(e.getV()));
        Matrix W = new Matrix(new double[][]{{0, -1, 0}, {1, 0, 0}, {0, 0, 1}});
        ArrayList<Matrix> P2 = new ArrayList<>(4);
        //double[][] temp_array = new double[3][4];
        Matrix Ue = e.getU();
        Matrix UWV = (Ue.times(W.times(VeT)));
        Matrix UWtV = (Ue.times((W.transpose()).times(VeT)));
        Matrix U2 = Ue.getMatrix(0,2,2,2);
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
     * 三角化求一对点的三维对应点坐标，单个点
     *  |P13*x1 -P11 |
     *  |P13*y1 -P12 ||X| = 0
     *  |P23*x2 -P11 |
     *  |P23*y1 -P22 |
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
//                out.println("M的第"+i+","+j+"个点的值：" + M.get(i,j));
//            }
//        }
        SingularValueDecomposition m = M.svd();
        Matrix X = m.getV().getMatrix(0,3,3,3);
        double para = 1/(X.get(3,0));
        X = X.times(para);
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
        int ind = 0;
        double maxres = 0;
        Matrix X;
        boolean[] Flag = null;//用来标记深度均为正的点
        for(int i=0; i<4; i++){
            X = TriangulateMultiPoints(PoList_1,PoList_2,P1,P2List.get(i));//4*n
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
        int count = 0;
        assert Flag != null;
        for (boolean b : Flag) {
            if (b) {
                //out.println(b);
                count++;
            }
        }
        this.P2_Selected = P2List.get(ind);
//        out.println("==============P2 2的值为==================");
//        for (int ii=0; ii<3; ii++){
//            for (int j=0; j<4; j++){
//                out.println("点("+ii+","+j+")的值：" + P2_Selected.get(ii,j));
//            }
//        }
        X = TriangulateMultiPoints(PoList_1,PoList_2,P1,this.P2_Selected);//4*n
//        System.out.println("==============所得三维点坐标==================");
//        for (int j=0; j<9; j++){
//            out.println(j+"点的值：" + X.get(0,j)+","+X.get(1,j)+ ","+X.get(2,j)+ ","+X.get(3,j));
//        }
        System.out.println(X.getRowDimension()+","+X.getColumnDimension());
        System.out.println("Flag.length:"+Flag.length);
        System.out.println("count:"+count);
//        double[][] XSelected = new double[count][4];
        double[][] XSelected = new double[PoList_1.getRowDimension()][4];//暂改
        int pos=0;
        for(int k = 0; k<Flag.length; k++){
//            if(!Flag[k]){
            XSelected[k] = X.getMatrix(0,3,k,k).getColumnPackedCopy();//此处有问题
            // XSelected[pos] = (X.getMatrix(0,3,k,k).times(1/X.get(3,k))).getColumnPackedCopy();//除以其次坐标
//                pos++;
//            }
        }
        this.X_3D = new Matrix(XSelected);
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
     * @param poList 真实匹配点
     * @return 各点的误差值
     */
    public double[] CalculateError(Matrix Point3D, double[][] poList){
//        out.println("==============投影矩阵P的值为==================");
//        for(int i=0; i<3; i++){
//            for(int j=0; j<4; j++){
//                out.println("P2第"+i+","+j+"个点的值：" + P2_Selected.get(i,j));
//            }
//        }
//        for (int i = 0; i < X_3D.getRowDimension(); i++) {
//            out.println("****************" + X_3D.get(i, 0) + "," + X_3D.get(i, 1) + "," + X_3D.get(i, 2) + "," + X_3D.get(i, 3));
//        }
        Matrix Point2DComputed = P2_Selected.times(Point3D.transpose()).transpose();//(3*4)*(4*n).transpose()
        out.println("==============投影回二维的点坐标==================");
        Matrix PointOrigin = Point2DToHomogeneous(poList);
        for(int p=0; p<Point3D.getRowDimension(); p++){
            //out.println("第"+p+"个点的齐次坐标为：" + Point2DComputed.get(p,2));
//            out.println("第"+p+"个点的原始坐标为：====" + Point2DComputed.get(p,0)+","+Point2DComputed.get(p,1)+","+ Point2DComputed.get(p,2));
//            Point2DComputed.set(p,0,Point2DComputed.get(p,0)/Point2DComputed.get(p,2));
//            Point2DComputed.set(p,1,Point2DComputed.get(p,1)/Point2DComputed.get(p,2));
//            Point2DComputed.set(p,2,1);
            out.println("第"+p+"个点的真实坐标为：" + PointOrigin.get(p,0)+","+PointOrigin.get(p,1)+","+ PointOrigin.get(p,2));

            out.println("第"+p+"个点的坐标为：" + Point2DComputed.get(p,0)/Point2DComputed.get(p,2)+","+Point2DComputed.get(p,1)/Point2DComputed.get(p,2)+","+ Point2DComputed.get(p,2)/Point2DComputed.get(p,2));
        }
//        Matrix PointOrigin = Point2DToHomogeneous(poList);
        Matrix error = Point2DComputed.minus(PointOrigin);
//        for(int p=0; p<Point3D.getRowDimension(); p++){
//            //out.println("第"+p+"个点的齐次坐标为：" + Point2DComputed.get(p,2));
//            out.println("第"+p+"个点的误差：====" + error.get(p,0)+","+error.get(p,1)+","+ error.get(p,2));
//         }

        double[] ErrorVal = new double[Point3D.getRowDimension()];
        for(int i=0; i<error.getRowDimension(); i++){
            for(int j=0; j<error.getColumnDimension(); j++){
                ErrorVal[i] += abs(error.get(i,j));
            }
            ErrorVal[i] /= 3.0f;
            out.println("第"+i+"个点的误差值为：" + ErrorVal[i]);
        }
        return ErrorVal;
    }

    public void Point3DTo2D(Matrix Point3D, Matrix P){
        Matrix Point2DComputed1 = P1.times(Point3D.transpose()).transpose();//n*3
        Point3Dto2D1 = ArrayToMarkerList(Point2DComputed1, OpticalCenter[0], OpticalCenter[1]);
        Matrix Point2DComputed2 = P2_Selected.times(Point3D.transpose()).transpose();//n*3
        Point3Dto2D2 = ArrayToMarkerList(Point2DComputed2, OpticalCenter[0], OpticalCenter[1]);
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
        SingularValueDecomposition svd = F.svd();
        Matrix vt = svd.getV();
        Matrix e = vt.getMatrix(2,2,0,2);
        e = e.times(1/e.get(0,2));
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
        //this.EpiLines1_Para = new double[PoListHo1.getRowDimension()][3];
        //this.EpiLines2_Para = new double[PoListHo2.getRowDimension()][3];
        this.EpiLines2_Para = F.times(PoListHo1.transpose()).getArray();//3*n
        this.EpiLines1_Para = (F.transpose()).times(PoListHo2.transpose()).getArray();
        out.println("==============点到极线的距离P1==================");
        distance1 = DistancePointToLine(EpiLines1_Para, PoListHo1.getArray());
        out.println("==============点到极线的距离P2==================");
        distance2 = DistancePointToLine(EpiLines2_Para, PoListHo2.getArray());
    }

//    public Bitmap DrawLine(Bitmap ori_img, double[][] PointParam){ //3*n
//        System.out.println("Starting drawing EpiLines.....");
//        Bitmap bitmap_new = ori_img.copy(Bitmap.Config.ARGB_8888,true);
//        Canvas canvas = new Canvas(bitmap_new);
//        Paint p = new Paint();
//        p.setColor(Color.CYAN);//设置画笔颜色
//        p.setAntiAlias(false);//设置画笔为无锯齿
//        p.setStrokeWidth((float) 5.0);//线宽
//        int PoNum = this.EpiLines1_Para[0].length;
//        double[][] StartEnd = new double[PoNum][2];
//        //获取起点和终点坐标
//        for(int i=0; i<PoNum; i++){
////            double left = -0.5*ori_img.getWidth();
////            double right = 0.5*ori_img.getWidth();
////            StartEnd[i][0] = -(PointParam[2][i]+left*PointParam[0][i])/PointParam[1][i];
////            StartEnd[i][1] = -(PointParam[2][i]+right*PointParam[0][i])/PointParam[1][i];
////            System.out.println("Start:("+left+","+StartEnd[i][0]+"),End:("+right+","+StartEnd[i][1]+")");
////            canvas.drawLine((float) (-0.5*ori_img.getWidth()), (float) StartEnd[i][0], (float) (0.5*ori_img.getWidth()), (float) StartEnd[i][1], p);
//           // double left = -0.5*ori_img.getWidth();
//          //  double right = 0.5*ori_img.getWidth();
//            StartEnd[i][0] = -(PointParam[2][i])/PointParam[1][i];
//            StartEnd[i][1] = -(PointParam[2][i]+(ori_img.getWidth()-1)*PointParam[0][i])/PointParam[1][i];
//            //System.out.println("Start:("+left+","+StartEnd[i][0]+"),End:("+right+","+StartEnd[i][1]+")");
//            canvas.drawLine((float) 0, (float) StartEnd[i][0], (float) (ori_img.getWidth()-1), (float) StartEnd[i][1], p);
//        }
//        return bitmap_new;
//    }

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
        ArrayList<Double> distance = new ArrayList<>(num);
        for(int i=0; i<num; i++){
//            double dis = abs(EpiLines_Para[0][i]*PoList[i][0] + EpiLines_Para[1][i]*PoList[i][1] + (EpiLines_Para[2][i]-(OpticalCenter[0]*EpiLines_Para[0][i]+OpticalCenter[1]*EpiLines_Para[1][i]))) / sqrt(pow(EpiLines_Para[0][i],2)+pow(EpiLines_Para[1][i],2));
            double dis = abs(EpiLines_Para[0][i]*PoList[i][0] + EpiLines_Para[1][i]*PoList[i][1] + EpiLines_Para[2][i]) / sqrt(pow(EpiLines_Para[0][i],2)+pow(EpiLines_Para[1][i],2));
            distance.add(dis);
            System.out.println("*************第"+i+"处点和极线的距离为***************："+dis);
        }
        return distance;
    }

}
