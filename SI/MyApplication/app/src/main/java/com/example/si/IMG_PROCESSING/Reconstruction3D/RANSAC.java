package com.example.si.IMG_PROCESSING.Reconstruction3D;

import android.util.Log;

import com.example.si.IMG_PROCESSING.GSDT.HeapElem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

import Jama.Matrix;

import static com.example.si.IMG_PROCESSING.Reconstruction3D.Convert2DTo3D.compute_fundamental_normalized;
import static java.lang.StrictMath.abs;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.min;
import static java.lang.System.out;


public class RANSAC {
    public static double[][] PoListHoSelected_1;
    public static double[][] PoListHoSelected_2;
    final static String TAG = "RANSAC";
    private static Random random = new Random();
    // randPerm(N,K) returns a vector of K unique values. This is sometimes
    // referred to as a K-permutation of 1:N or as sampling without replacement.
    private static Set<Integer> randPerm(int N, int K) {
        Set<Integer> res = new LinkedHashSet<>(); // unsorted set.
        while (res.size() < K) { res.add(random.nextInt(N)); // [0, number-1]
        }
        return res;
    }

    private static double norm(List<Double> vec) {
        return Math.sqrt(Math.pow(vec.get(0), 2) + Math.pow(vec.get(1), 2));
    }

    // 存放距离小于阈值的点对
    private static List<Integer> findLessThan(List<Double> distance, double threshDist) {
        List<Integer> res = new ArrayList<>();
        // Log.d(TAG, "dist&&&&&&&&&&&&&&&&&&&&&&");
        for (int i = 0; i < distance.size(); i++) {
            double dist = distance.get(i);
            //     Log.d(TAG, "dist"+i+"为："+dist);

            if (Math.abs(dist) <= threshDist) {
                res.add(i);
            }
        }
        return res;
    }

    /**
     * 八点法结合 RANSAC 稳健估计基础矩阵 F
     * @param PoListHo_1 齐次坐标点对
     * @param PoListHo_2 齐次坐标点对
     * @param num 每次随机选取的点对数 12
     * @param iter 迭代次数 2000
     * @param threshDist 归为内点的阈值
     * @param InlierRatio 内点比例
     * @return 基础矩阵 F
     */
    public static Matrix MyRansac(Matrix PoListHo_1, Matrix PoListHo_2, int num, int iter, double threshDist, double InlierRatio){
        int number = PoListHo_1.getRowDimension();
//        Log.d(TAG, "KKKKKKKKKKKKKKKKKKKKKK PoList++++++++++++++++++");
//        for (int i = 0; i < number; i++) {
//            out.println("****************" + PoListHo_1.get(i, 0) + "," + PoListHo_1.get(i, 1) + "," + PoListHo_1.get(i, 2));
//        }
        if(num>=number) num = max(9,number-2);
        int bestInNum = 0;
        Matrix bestF = new Matrix(3,3);
        List<Integer> BestInlierIdx = null;
        for(int i=0; i<iter; i++){
            Set<Integer> idx = randPerm(number, num);
            Matrix Po1Sample = new Matrix(num,3);
            Matrix Po2Sample = new Matrix(num,3);
            int count = 0;
            for(Integer idxSample: idx){
                Po1Sample.setMatrix(count,count,0,2,PoListHo_1.getMatrix(idxSample,idxSample,0,2));
                Po2Sample.setMatrix(count,count,0,2,PoListHo_2.getMatrix(idxSample,idxSample,0,2));
                count++;

            }
            Matrix FEstimate = compute_fundamental_normalized(Po1Sample, Po2Sample);
            List<Double> distance = new ArrayList<>();
            for (int j = 0; j < number; j++) {
                double distTmp = abs(PoListHo_1.getMatrix(j,j,0,2).times(FEstimate.times(PoListHo_2.getMatrix(j,j,0,2).transpose())).get(0,0));
                distance.add(distTmp);
            }
            //另一种误差判断，。。
//            for(int ii = 0; ii < count; ii++ ){
//                double[] F = FEstimate.getRowPackedCopy();
//                double[] f = PoListHo_1.getMatrix(ii,ii,0,2).getRowPackedCopy();
//                double[] t = PoListHo_2.getMatrix(ii,ii,0,2).getRowPackedCopy();
//                double a = F[0]*f[0] + F[1]*f[1] + F[ 2]*f[2] + F[3] - t[0];
//                double b = F[4]*f[0] + F[5]*f[1] + F[ 6]*f[2] + F[7] - t[1];
//                double c = F[8]*f[0] + F[9]*f[1] + F[10]*f[2] + F[11] - t[2];
//                double distTmp = (float)(a*a + b*b + c*c);
//                distance.add(distTmp);
//            }

            List<Integer> inlierIdx = findLessThan(distance, threshDist);
            int inlierNum = inlierIdx.size();
            if ((inlierNum >= Math.round(InlierRatio * number)) && (inlierNum > bestInNum)) {
                bestInNum = inlierNum;
                bestF = FEstimate;
                BestInlierIdx = inlierIdx;
                Log.d(TAG, "迭代"+i+"次时的内点个数为："+inlierNum);
//                for(int k=0; k<inlierNum; k++){
//                    Log.d(TAG, "第"+i+"内点下标是："+inlierIdx.get(k));
//                }
            }
        }
        int c = 0;
        Log.d(TAG, "Best InNum："+bestInNum);
        Log.d(TAG, "最终的内点个数为："+BestInlierIdx.size());
        PoListHoSelected_1 = new double[bestInNum][3];
        PoListHoSelected_2 = new double[bestInNum][3];
        Log.d(TAG, "dist&&&&&&&&&&&&&&&&&&&&&&");
        for(Integer id : BestInlierIdx){
            // Log.d(TAG, "选取的id："+id);
            PoListHoSelected_1[c] = PoListHo_1.getMatrix(id, id, 0,2).getRowPackedCopy();
            Log.d(TAG, "PoListHoSelected_1："+PoListHoSelected_1[c][0]+","+PoListHoSelected_1[c][1]+","+PoListHoSelected_1[c][2]);
            PoListHoSelected_2[c] = PoListHo_2.getMatrix(id, id, 0,2).getRowPackedCopy();
            Log.d(TAG, "PoListHoSelected_2："+PoListHoSelected_2[c][0]+","+PoListHoSelected_2[c][1]+","+PoListHoSelected_2[c][2]);
            double distTmp = abs(PoListHo_1.getMatrix(id, id, 0,2).times(bestF.times(PoListHo_2.getMatrix(id, id, 0,2).transpose())).get(0,0));
            Log.d(TAG, "dist为："+distTmp);
            c++;
        }
        //  Log.d(TAG, "KKKKKKKKKKKKKKKKKKKKKK："+c);

        return bestF;
    }

    //用来计算直线参数
    public static List<Double> perform(List<Double> data_Y, int num, int iter, double threshDist, double inlierRatio) {
        int number = data_Y.size();
        List<Integer> data_X = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            data_X.add(i + 1);
        }
        double bestInNum = 0;
        double bestParameter1 = 0, bestParameter2 = 0;
        for (int i = 0; i < iter; i++) {
            Set<Integer> idx = randPerm(number, num);
            List<Integer> sample_X = new ArrayList<>();
            List<Double> sample_Y = new ArrayList<>();
            for (Integer idxVal : idx) {
                sample_X.add(data_X.get(idxVal));
                sample_Y.add(data_Y.get(idxVal));
            }

            List<Double> kLine = new ArrayList<>();
            kLine.add((double) (sample_X.get(1) - sample_X.get(0)));
            kLine.add(sample_Y.get(1) - sample_Y.get(0));
            List<Double> kLineNorm = new ArrayList<>();
            double norm = norm(kLine);
            kLineNorm.add(kLine.get(0) / norm);
            kLineNorm.add(kLine.get(1) / norm);
            List<Double> normVector = new ArrayList<>();
            normVector.add(-kLineNorm.get(1));
            normVector.add(kLineNorm.get(0));

            List<Double> distance = new ArrayList<>();
            for (int j = 0; j < number; j++) {
                double distTmp = normVector.get(0) * (data_X.get(j) - sample_X.get(0));
                distTmp += normVector.get(1) * (data_Y.get(j) - sample_Y.get(0));
                distance.add(distTmp);
            }
            List<Integer> inlierIdx = findLessThan(distance, threshDist);
            int inlierNum = inlierIdx.size();
            double parameter1;
            double parameter2;
            if ((inlierNum >= Math.round(inlierRatio * number)) && (inlierNum > bestInNum)) {
                bestInNum = inlierNum;
                parameter1 = (sample_Y.get(1) - sample_Y.get(0)) / (sample_X.get(1) - sample_X.get(0));
                parameter2 = sample_Y.get(0) - parameter1 * sample_X.get(0);
                bestParameter1 = parameter1;
                bestParameter2 = parameter2;
            }
        }
        List<Double> res = new ArrayList<>();
        res.add(bestParameter1);
        res.add(bestParameter2);
        return res;
    }
}