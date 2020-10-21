package com.example.si.IMG_PROCESSING.Reconstruction3D;

import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import Jama.Matrix;

import static java.lang.StrictMath.PI;
import static java.lang.StrictMath.cos;
import static java.lang.StrictMath.sin;

public class DataBuilder {
    //    private Matrix Eye = new Matrix(new double[][]{{1, 0, 0},{0, 1, 0},{0, 0, 1}});
    private Matrix K0 = new Matrix(new double[][]{{3200, 0, 0}, {0, 3200, 0}, {0, 0, 1}});
    private Matrix R;//3*3
    private Matrix t;//1*3
    private Matrix P1;
    private Matrix P2;
    public Matrix E;
    public Matrix F;
    public static Matrix X;
    public double[][] PoList1;
    public double[][] PoList2;
    public double[][] PoList1_noise;
    public double[][] PoList2_noise;
    public double[][] PoList1_orderrefined;
    public double[][] PoList2_orderrefined;

    private static Random random = new Random();
    // randPerm(N,K) returns a vector of K unique values. This is sometimes
    // referred to as a K-permutation of 1:N or as sampling without replacement.
    private static Set<Integer> randPerm(int N, int K) {
        Set<Integer> res = new LinkedHashSet<>(); // unsorted set.
        while (res.size() < K) { res.add(random.nextInt(N)); // [0, number-1]
        }
        return res;
    }


    public DataBuilder(int num, boolean flag){
        P1 = K0.times(new Matrix(new double[][]{{1, 0, 0, 0}, {0, 1, 0, 0}, {0, 0, 1, 0}}));
        GenerateRt(-10,5,5);
        P2 = new Matrix(3,4);
//        P2.setMatrix(0,2,0,2, R.transpose());
//        P2.setMatrix(0,2,3,3,R.transpose().times(t.transpose().times(-1)));
        P2.setMatrix(0,2,0,2, R);
        P2.setMatrix(0,2,3,3,t.transpose());
        P2 = K0.times(P2);
        GenerateRandom3DPoints(num, 50,400);//50--450
        PoList1 = QiCiTo1(P1.times(X.transpose()).transpose()).getArray();//n*3
        PoList2 = QiCiTo1(P2.times(X.transpose()).transpose()).getArray();
        if(flag){
            AddGuassNoise(0,0.8f);
        }
        Matrix tx = new Matrix(new double[][]{{0, -t.get(0,2), t.get(0,1)},{t.get(0,2), 0, -t.get(0,0)},{-t.get(0,1), t.get(0,0), 0}});
        E = tx.times(R);
        F = K0.inverse().transpose().times(E.times(K0.inverse()));
    }

    /**
     * 生成随机三维点坐标
     * @param num 左生成的三维点数目
     * @param rangel 左边界
     * @param ranger +rangel = 右边界
     */
    public static void GenerateRandom3DPoints(int num, int rangel, int ranger){
        long startTime = System.currentTimeMillis(); //开始测试时间
        Random random = new Random();
        double[][] Array3D = new double[num][4];
        for(int i=0; i<num; i++){
            Array3D[i] = new double[]{random.nextDouble()*ranger+rangel, random.nextDouble()*ranger+rangel, random.nextDouble()*ranger+rangel, 1};
            for(int j = 0; j < i; j++){
                while (Array3D[i] == Array3D[j]){
                    i--;
                }
            }
        }
        long endTime=System.currentTimeMillis(); //获取结束时间
        System.out.println("代码运行时间： "+(endTime-startTime)+"ms");
        for(int i=0; i<num; i++){
            System.out.println("随机生成的点坐标为： x:: "+Array3D[i][0]+",y:: "+Array3D[i][1]+",z:: "+Array3D[i][2]+",q:: "+Array3D[i][3]);
        }
        X = new Matrix(Array3D);//n*4
    }

    /**
     * 合成旋转矩阵和平移向量
     * @param thetax x旋转
     * @param thetay y旋转
     * @param thetaz z旋转
     */
    private void GenerateRt(float thetax, float thetay, float thetaz){
        Matrix Rx = new Matrix(new double[][]{{1, 0, 0}, {0, cos(thetax * PI / 180), -sin(thetax * PI / 180)}, {0, sin(thetax * PI / 180), cos(thetax * PI / 180)}});
        Matrix Ry = new Matrix(new double[][]{{cos(thetay / 180 * PI), 0, sin(thetay / 180 * PI)}, {0, 1, 0}, {-sin(thetay / 180 * PI), 0, cos(thetay / 180 * PI)}});
        Matrix Rz = new Matrix(new double[][]{{cos(thetaz * PI / 180), -sin(thetaz * PI / 180), 0}, {sin(thetaz * PI / 180), cos(thetaz * PI / 180), 0}, {0, 0, 1}});
        R = Rx.times(Ry.times(Rz));
        t = new Matrix(new double[][]{{9, -12, 15}});//1*3
        t = t.times(R);
    }

    public void ChangeMatchOrder(float wrongRatio){
        int total_num = PoList1.length;
        int numTochange = (int)(total_num * wrongRatio);
        System.out.println("错误匹配的对数为：" + numTochange);
        Set<Integer> idxTochange = randPerm(total_num, numTochange);
        System.out.println("++++++++++++++非误匹配下标+++++++++++++");

/*        for(int i=0; i<total_num; i++){
            if(!idxTochange.contains(i)){
                System.out.println(i);
            }
        }*/
        PoList1_orderrefined = PoList1_noise;
        PoList2_orderrefined = PoList2_noise;
        if(numTochange != 0){
            Object[] idxArray = idxTochange.toArray();
            for(int i=0; i<numTochange-1; i++){
                PoList2_orderrefined[(int) idxArray[i]] = PoList2_orderrefined[(int) idxArray[i+1]];
            }
            PoList2_orderrefined[(int) idxArray[numTochange - 1]] = PoList2_orderrefined[(int) idxArray[0]];
        }
    }

    /**
     * 给生成的二维点添加高斯噪声
     * @param mu 均值
     * @param std 标准差
     */
    public void AddGuassNoise(int mu, float std){
        int num = PoList1.length;
        PoList1_noise = new double[num][3];
        PoList2_noise = new double[num][3];
        Random random = new Random();
        for(int i=0; i<num; i++){
            double noise1 = random.nextGaussian()*std + mu;
            double noise2 = random.nextGaussian()*std + mu;
            PoList1_noise[i][0] = PoList1[i][0] + noise1;
            PoList1_noise[i][1] = PoList1[i][1] + noise1;
//            PoList1_noise[i][2] = PoList1[i][2];
            PoList2_noise[i][0] = PoList2[i][0] + noise2;
            PoList2_noise[i][1] = PoList2[i][1] + noise2;
//            PoList2_noise[i][2] = PoList2[i][2];
//            PoList2[i][0] += noise2;
//            PoList2[i][1] += noise2;
        }
    }

    /**
     * 齐次坐标位化为1
     * @param PointList
     * @return
     */
    public Matrix QiCiTo1(Matrix PointList){
        for(int p=0; p<PointList.getRowDimension(); p++){
            for(int i=0; i<PointList.getColumnDimension(); i++){
                PointList.set(p,i,PointList.get(p,i)/PointList.get(p,PointList.getColumnDimension()-1));
            }
        }
        return PointList;
    }
}
