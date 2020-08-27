package com.example.si.IMG_PROCESSING.Reconstruction3D;


import com.example.si.IMG_PROCESSING.CornerDetection.ImageMarker;

import java.util.ArrayList;
import java.util.Objects;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

import static java.lang.StrictMath.round;
import static java.lang.StrictMath.sqrt;
import static java.lang.System.out;


public class Convert2DTo3D {
    public enum MobileModel { MIX2, HUAWEI };//枚举手机型号
//    private static Matrix I_Matrix = new Matrix(new double[][] {{1,0,0},{0,1,0},{0,0,1}});
    public MobileModel MyMobileModel;
    public Matrix X_3D = null;
    public Matrix P2_Selected;

    /**
     * 2D-3D主函数
     * @param PoList_1 n*2格式储存的点数组
     * @param PoList_2 n*2格式储存
     * @return 格式为n*4
     */
    public boolean Convert2DTo3D_Fun(double[][] PoList_1, double[][] PoList_2){
        Matrix PoListHo_1 = CaculateKPoList(PoList_1,this.MyMobileModel);
        Matrix PoListHo_2 = CaculateKPoList(PoList_2,this.MyMobileModel);//inv(K)*x，矩阵格式为n*3
       // Matrix E = ComputeFundamental(PoListHo_1, PoListHo_2);//计算本质矩阵E
        Matrix E = compute_fundamental_normalized(PoListHo_1, PoListHo_2);
        ArrayList<Matrix> P2 = ComputePFromEssential(E);//计算第二角度可能的投影矩阵
        Matrix P1 = new Matrix(new double[][]{{1, 0, 0, 0}, {0, 1, 0, 0}, {0, 0, 0, 1}});//3*4矩阵
        X_3D = SelectPFrom4P(PoListHo_1,PoListHo_2,P1,P2);//恢复的三位坐标矩阵
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
                Intrinsic_matrix = new Matrix(new double[][] {{2090.7,0.0,0.0},{0.0,2096.9,0.0},{962.5,1290.9,1.0}});
                break;
            case HUAWEI:
                Intrinsic_matrix = new Matrix(new double[][] {{0.0,0.0,0.0},{0.0,0.0,0.0},{0.0,0.0,1.0}});
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
        for(int i=0; i<List_len-1; i++){
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
        Matrix F = matrixReshape(V.getMatrix(8,8,0,8).getArray() ,3,3);
        //进行奇异值分解
        SingularValueDecomposition f = F.svd();
        Matrix Uf = f.getU();
        Matrix Sf = f.getS();//
        Matrix Vf = f.getV();
        Sf.set(2,2,0);
        F = Uf.times(Sf.times(Vf));
        return F;
    }


    public Matrix compute_fundamental_normalized(Matrix PoList_1, Matrix PoList_2){
        int Po_num = PoList_1.getRowDimension();//获取匹配点对数
        if(PoList_2.getRowDimension() != Po_num){
            throw new IllegalArgumentException("Number of points don't match.");
        }
        double[] po1mean = AvgValue(PoList_1);
        double[] po2mean = AvgValue(PoList_2);//计算均值
        double std1 = Objects.requireNonNull(getWholeMeanStdValue(PoList_1.getMatrix(0, Po_num, 0, 2).getArray()))[1];
        double std2 = Objects.requireNonNull(getWholeMeanStdValue(PoList_2.getMatrix(0, Po_num, 0, 2).getArray()))[1];
        double S1 = sqrt(2)/std1;
        double S2 = sqrt(2)/std2;
        double[][] temp1 = {{S1,0,-S1*po1mean[0]}, {0,S1,-S1*po1mean[1]}, {0,0,1}};
        Matrix T1 = new Matrix(temp1);
        PoList_1 = T1.times(PoList_1);
        double[][] temp2 = {{S2,0,-S2*po2mean[0]}, {0,S2,-S2*po2mean[1]}, {0,0,1}};
        Matrix T2 = new Matrix(temp1);
        PoList_2 = T2.times(PoList_2);
        Matrix F = ComputeFundamental(PoList_1,PoList_2);
        F = (T1.inverse()).times(F.times(T2));
        return F.times(1/F.get(2,2));
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
                    std += Math.pow(Po_list[j][i] - meanStd[0], 2);
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
    public Matrix CaculateKPoList(double[][] Po_list, MobileModel mm){
        Matrix K = get_IntrinsicMatrix(mm);//3*3矩阵
        Matrix Po = Point2DToHomogeneous(Po_list);//n*3矩阵
        Po = K.inverse().times(Po.transpose());//3*n矩阵
        return Po.transpose();
    }

    /**
     * 利用本质矩阵求解投影矩阵，共有四种可能，需要后续排除
     * @param E 本质矩阵
     * @return 四种可能的 P 矩阵 3*4
     */
    public ArrayList<Matrix> ComputePFromEssential(Matrix E) {
        //进行奇异值分解
        SingularValueDecomposition e = E.svd();
        Matrix Ve = e.getV();
        if ((e.getU().times(e.getV())).det() < 0) {
            Ve = e.getV().times(-1);
        }
//        I_Matrix.set(2, 2, 0);
//        E = e.getU().times(I_Matrix.times(e.getV()));
        Matrix W = new Matrix(new double[][]{{0, -1, 0}, {1, 0, 0}, {0, 0, 1}});
        ArrayList<Matrix> P2 = new ArrayList<>(4);
        //double[][] temp_array = new double[3][4];
        Matrix Ue = e.getU();
        Matrix UWV = (Ue.times(W.times(Ve)));
        Matrix UWtV = (Ue.times((W.transpose()).times(Ve)));
        Matrix U2 = Ue.getMatrix(0,2,2,2);
        for(int i=0; i<4; i++){
            switch (i){
                case 0:
                    Matrix item = new Matrix(3,4);
                    item.setMatrix(0,2,0,2,UWV);
                    item.setMatrix(0,2,3,3,U2);
                    P2.add(item);
                    break;
                case 1:
                    Matrix item1 = new Matrix(3,4);
                    item1.setMatrix(0,2,0,2,UWV);
                    item1.setMatrix(0,2,3,3,U2.times(-1));
                    P2.add(item1);
                    break;
                case 2:
                    Matrix item2 = new Matrix(3,4);
                    item2.setMatrix(0,2,0,2,UWtV);
                    item2.setMatrix(0,2,3,3,U2);
                    P2.add(item2);
                    break;
                case 3:
                    Matrix item3 = new Matrix(3,4);
                    item3.setMatrix(0,2,0,2,UWtV);
                    item3.setMatrix(0,2,3,3,U2.times(-1));
                    P2.add(item3);
                    break;
            }
        }
        return P2;
    }

    /**
     * 三角化求一对点的三维对应点坐标，单个点
     *  |P1 -x1 0 |
     *  |P2  0 -x2|
     * @param Po_1 第一幅图中点的坐标 数组格式为1*3
     * @param Po_2 第二幅图中点的坐标  数组格式为1*3
     * @param P1 第一个角度的投影矩阵  [[1, 0, 0, 0], [0, 1, 0, 0], [0, 0, 0, 1]]
     * @param P2 第二个角度下的投影矩阵
     * @return 三维点坐标 格式为1*4
     */
    public Matrix TriangulatePoints(Matrix Po_1, Matrix Po_2, Matrix P1, Matrix P2){
        double[][] ZeroArray = new double[6][6];
        Matrix M = new Matrix(ZeroArray);
        M.setMatrix(0,2,0,3,P1);
        M.setMatrix(3,5,0,3,P2);
        M.setMatrix(0,2,4,4,Po_1.times(-1).transpose());
        M.setMatrix(3,5,5,5,Po_2.times(-1).transpose());
        SingularValueDecomposition m = M.svd();
        Matrix X = m.getV().getMatrix(5,5,0,3);
        X.times(1/X.get(0,3));
        return X;
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
     *
     * @param PoList_1 第一幅图的点对
     * @param PoList_2 第二幅图的点对
     * @param P1 第一个角度的投影矩阵
     * @param P2List 4个可能的投影矩阵
     * @return 保留的正确的三维点 n*4格式
     */
    public Matrix SelectPFrom4P(Matrix PoList_1, Matrix PoList_2, Matrix P1, ArrayList<Matrix> P2List){
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
            for(int j=0; j<d1.length; j++){
                if(d1[j]>0) sum += d1[j];
                if(d2[j]>0) sum += d2[j];
                f[j] = d1[j] > 0 && d2[j] > 0;
            }
            if(sum > maxres){
                maxres = sum;
                ind = i;
                Flag = f;
            }
        }
        int count = 0;
        assert Flag != null;
        for (boolean b : Flag) {
            if (!b) {
                count++;
            }
        }
        this.P2_Selected = P2List.get(ind);
        X = TriangulateMultiPoints(PoList_1,PoList_2,P1,this.P2_Selected);//4*n
        System.out.println(X.getRowDimension()+","+X.getColumnDimension());
        System.out.println("Flag.length:"+Flag.length);
        System.out.println("count:"+count);
        double[][] XSelected = new double[count][4];
        int pos=0;
        for(int k = 0; k<Flag.length; k++){
            if(!Flag[k]){
                XSelected[pos] = X.getMatrix(0,3,k,k).getColumnPackedCopy();//此处有问题
                pos++;
            }
        }
        this.X_3D = new Matrix(XSelected);
        return this.X_3D;
    }

    //To be continue..

    /**将MarkerList类转换为二维数组
     *
     * @param markerList
     * @return
     */
    private static int[][] MarkerListToArray (ArrayList<ImageMarker> markerList) {
        int[][] array_markerList = new int[markerList.size()][2];
        for (int i = 0; i < markerList.size(); i++) {
            array_markerList[i][0] = round(markerList.get(i).x);
            array_markerList[i][1] = round(markerList.get(i).y);
        }
        return array_markerList;
    }

    private static ArrayList<ImageMarker> ArrayToMarkerList (Matrix X_3D) {
        ArrayList<ImageMarker> MarkerList = new ArrayList<>(X_3D.getRowDimension());
        for (int i = 0; i < X_3D.getRowDimension(); i++) {
            array_markerList[i][0] = round(markerList.get(i).x);
            array_markerList[i][1] = round(markerList.get(i).y);
        }
        return array_markerList;
    }
//    double[][] polist1 = new double[][]{{710, 617}, {662, 472}, {101, 146}, {719, 548}, {8, 717}, {649, 567}, {438, 91}, {431, 80}, {310, 231}, {679, 477}};
//    double[][] polist2 = new double[][]{{9.47, 12.87}, { 7.98, 10.85}, {1.81, 2.46}, {8.96, 12.18}, {5.91, 8.04}, {8.68, 11.80}, {3.46, 4.71}, {3.33, 4.52}, {3.81096705, 5.18}, {8.13, 11.05}};
//    Matrix E_set = new Matrix(new double[][]{{9.2657E-04, -3.0478E-01, 1.0417E-02},{-5.5426E-02, -3.2049E-01, 6.2389E-01},{7.2953E-04, -6.4118E-01, 2.9404E-02}});
//    Matrix K = get_IntrinsicMatrix(MobileModel.MIX2);//3*3矩阵
//    Matrix F_set = (K.inverse().transpose()).times(E_set.times(K.inverse()));
//    Matrix Po = Point2DToHomogeneous(polist1);//n*3矩阵



}
