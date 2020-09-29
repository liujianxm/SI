package com.example.si.IMG_PROCESSING.Reconstruction3D;

import android.util.Log;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import Jama.Matrix;

import static com.example.si.IMG_PROCESSING.Reconstruction3D.Convert2DTo3D_new.compute_Homography_normalized;
import static com.example.si.IMG_PROCESSING.Reconstruction3D.Convert2DTo3D_new.compute_fundamental_normalized;
import static java.lang.StrictMath.abs;
import static java.lang.StrictMath.max;


public class RANSAC {
    public static double[][] PoListHoSelected_1;
    public static double[][] PoListHoSelected_2;
    static List<Integer> BestInlierIdxH = new ArrayList<>();
    static List<Integer> BestInlierIdxF = new ArrayList<>();
    static float SH, SF;
    public static Matrix bestF;
    public static Matrix bestH;


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

    public static float InitializeRANSAC(Matrix PoListHo_1, Matrix PoListHo_2, int num, int iter) throws InterruptedException {
//        Thread threadF = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                Log.v("InitializeRANSAC","Here enter the thread F");
//                bestF = CheckFundamentalRansac(PoListHo_1, PoListHo_2, 9, 500);
//            }
//        });
//        Thread threadH = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                Log.v("InitializeRANSAC","Here enter the thread H");
//                bestH = CheckHomographyRansac(PoListHo_1, PoListHo_2, 9, 500);
//            }
//        });
////        threadF.start();
////        threadH.start();
//        threadF.join();
//        threadH.join();
        Log.v("InitializeRANSAC","Here enter the thread F");
        bestF = CheckFundamentalRansac(PoListHo_1, PoListHo_2, 9, 500);
        Log.v("InitializeRANSAC","Here enter the thread H");
        bestH = CheckHomographyRansac(PoListHo_1, PoListHo_2, 9, 500);
        return SH/(SH+SF);
    }

    private static float CheckHomography(Matrix PoListHo_1, Matrix PoListHo_2, Matrix H, List<Integer> inlierIdx){
        float sigma = 1.0f;
        float score = 0.0f;
        final float th = 5.991f;
        final int num = PoListHo_1.getRowDimension();
        float invSigmaSquare = 1.0f/(sigma*sigma);
        Matrix Hinv = H.inverse();
        for(int i=0; i<num; i++){
            Matrix po1 = PoListHo_1.getMatrix(i,i,0,2);
            Matrix po2 = PoListHo_2.getMatrix(i,i,0,2);
            boolean bIn = true;
            double u1 = po1.get(0,0);
            double v1 = po1.get(0,1);
            double u2 = po2.get(0,0);
            double v2 = po2.get(0,1);
            // Reprojection error in first image
            // x2in1 = H12*x2
            float w2in1inv = (float) (1.0f/(Hinv.get(2,0)*u2+Hinv.get(2,1)*v2+Hinv.get(2,2)));
            float u2in1 = (float) ((Hinv.get(0,0)*u2+Hinv.get(0,1)*v2+Hinv.get(0,2))*w2in1inv);
            float v2in1 = (float) ((Hinv.get(1,0)*u2+Hinv.get(1,1)*v2+Hinv.get(1,2))*w2in1inv);
            float squareDist1 = (float) ((u1-u2in1)*(u1-u2in1) + (v1 - v2in1)*(v1 - v2in1));
            float chiSquare1 = squareDist1*invSigmaSquare;
            if(chiSquare1>th) bIn = false;
            else score += th -chiSquare1;
            // Reprojection error in second image
            // x1in2 = H21*x1
            float w1in2inv = (float) (1.0f/(H.get(2,0)*u1+H.get(2,1)*v1+H.get(2,2)));
            float u1in2 = (float) ((H.get(0,0)*u1+H.get(0,1)*v1+H.get(0,2))*w1in2inv);
            float v1in2 = (float) ((H.get(1,0)*u1+H.get(1,1)*v1+H.get(1,2))*w1in2inv);
            float squareDist2 = (float) ((u2-u1in2)*(u2-u1in2) + (v2 - v1in2)*(v2 - v1in2));
            float chiSquare2 = squareDist2*invSigmaSquare;
            if(chiSquare2>th) bIn = false;
            else score += th - chiSquare2;

            if(bIn)
                inlierIdx.add(i);
        }
        return score;
    }

    /**
     * 返回最佳 H 和对应的内点下标
     * @param PoListHo_1
     * @param PoListHo_2
     * @param num
     * @param iter
     * @return
     */
    public static Matrix CheckHomographyRansac(Matrix PoListHo_1, Matrix PoListHo_2, int num, int iter){
        SH=0.0f;
        int number = PoListHo_1.getRowDimension();
        if(num>=number) num = max(9,number-2);
        int bestInNum;
        Matrix bestH = new Matrix(3,3);
//        List<Integer> BestInlierIdx = new ArrayList<>();
        float currentScore;
        for(int i=0; i<iter; i++) {
            Set<Integer> idx = randPerm(number, num);
            Matrix Po1Sample = new Matrix(num, 3);
            Matrix Po2Sample = new Matrix(num, 3);
            int count = 0;
            for (Integer idxSample : idx) {
                Po1Sample.setMatrix(count, count, 0, 2, PoListHo_1.getMatrix(idxSample, idxSample, 0, 2));
                Po2Sample.setMatrix(count, count, 0, 2, PoListHo_2.getMatrix(idxSample, idxSample, 0, 2));
                count++;
            }
            Matrix HEstimate = compute_Homography_normalized(Po1Sample, Po2Sample);
            List<Integer> inlierIdx = new ArrayList<>();
            currentScore = CheckHomography(PoListHo_1, PoListHo_2, HEstimate, inlierIdx);
            if(currentScore > SH && inlierIdx.size()/(float)number > 0.4)//添加内点比例
            {
                bestH = HEstimate;
                BestInlierIdxH = inlierIdx;
                Log.d(TAG, "目前的内点个数为："+BestInlierIdxH.size());
                SH = currentScore;
            }
        }
        bestInNum = BestInlierIdxH.size();
        Log.d(TAG, "最终的内点个数为："+bestInNum);
/*        PoListHoSelected_1 = new double[bestInNum][3];
        PoListHoSelected_2 = new double[bestInNum][3];
        Log.d(TAG, "dist&&&&&&&&&&&&&&&&&&&&&&");
        int c = 0;
        for(Integer id : BestInlierIdx){
            // Log.d(TAG, "选取的id："+id);
            PoListHoSelected_1[c] = PoListHo_1.getMatrix(id, id, 0,2).getRowPackedCopy();
//            Log.d(TAG, "PoListHoSelected_1："+PoListHoSelected_1[c][0]+","+PoListHoSelected_1[c][1]+","+PoListHoSelected_1[c][2]);
            PoListHoSelected_2[c] = PoListHo_2.getMatrix(id, id, 0,2).getRowPackedCopy();
//            Log.d(TAG, "PoListHoSelected_2："+PoListHoSelected_2[c][0]+","+PoListHoSelected_2[c][1]+","+PoListHoSelected_2[c][2]);
//            double distTmp = abs(PoListHo_1.getMatrix(id, id, 0,2).times(bestH.times(PoListHo_2.getMatrix(id, id, 0,2).transpose())).get(0,0));
//            Log.d(TAG, "dist为："+distTmp);
            c++;
        }*/
        return bestH;
    }


    private static float CheckFundamental(Matrix PoListHo_1, Matrix PoListHo_2, Matrix F, List<Integer> inlierIdx){
        float sigma = 1.0f;
        final int num = PoListHo_1.getRowDimension();
        float score = 0.0f;
        float th = 3.841f;
//        float th = 5*10E-3f;
        float thScore = 5.991f;
        float invSigmaSquare = 1.0f/(sigma*sigma);
        for(int i=0; i<num; i++){
            Matrix po1 = PoListHo_1.getMatrix(i,i,0,2);
            Matrix po2 = PoListHo_2.getMatrix(i,i,0,2);
            boolean bIn = true;
            double[] EpiLines2_Para = F.times(po1.transpose()).getColumnPackedCopy();//3*1
            double[] EpiLines1_Para = F.transpose().times(po2.transpose()).getColumnPackedCopy();//3*1  这两行可能写反
            double num2 = EpiLines2_Para[0]*po2.get(0,0)+EpiLines2_Para[1]*po2.get(0,1)+EpiLines2_Para[2];
            double squareDist1 = num2*num2/(EpiLines2_Para[0]*EpiLines2_Para[0]+EpiLines2_Para[1]*EpiLines2_Para[1]);
            double chiSquare1 = squareDist1*invSigmaSquare;
            if(chiSquare1>th)
                bIn = false;
            else
                score += thScore - chiSquare1;
            double num1 = EpiLines1_Para[0]*po1.get(0,0)+EpiLines1_Para[1]*po1.get(0,1)+EpiLines1_Para[2];
            double squareDist2 = num1*num1/(EpiLines1_Para[0]*EpiLines1_Para[0]+EpiLines1_Para[1]*EpiLines1_Para[1]);
            double chiSquare2 = squareDist2*invSigmaSquare;
//            Log.d(TAG, "$$$$$$$$$$$$$$$："+chiSquare2+",,,"+th);
            if(chiSquare2>th)
                bIn = false;
            else
                score += thScore - chiSquare2;
            if(bIn)
                inlierIdx.add(i);
        }
        return score;
    }

    public static Matrix CheckFundamentalRansac(Matrix PoListHo_1, Matrix PoListHo_2, int num, int iter){
        SF=0.0f;
        int number = PoListHo_1.getRowDimension();
        if(num>=number) num = max(9,number-2);
        int bestInNum;
        Matrix bestF = new Matrix(3,3);
//        List<Integer> BestInlierIdx = new ArrayList<>();
        float currentScore;
        for(int i=0; i<iter; i++) {
            Set<Integer> idx = randPerm(number, num);
            Matrix Po1Sample = new Matrix(num, 3);
            Matrix Po2Sample = new Matrix(num, 3);
            int count = 0;
            for (Integer idxSample : idx) {
                Po1Sample.setMatrix(count, count, 0, 2, PoListHo_1.getMatrix(idxSample, idxSample, 0, 2));
                Po2Sample.setMatrix(count, count, 0, 2, PoListHo_2.getMatrix(idxSample, idxSample, 0, 2));
                count++;
            }
            Matrix FEstimate = compute_fundamental_normalized(Po1Sample, Po2Sample);
            List<Integer> inlierIdx = new ArrayList<>();
            currentScore = CheckFundamental(PoListHo_1, PoListHo_2, FEstimate, inlierIdx);
            if(currentScore > SF && inlierIdx.size()/(float)number > 0.4)
            {
                bestF = FEstimate;
                BestInlierIdxF = inlierIdx;
                Log.d(TAG, "目前的内点个数为："+BestInlierIdxF.size());
                SF = currentScore;
            }
        }
        bestInNum = BestInlierIdxF.size();
        Log.d(TAG, "最终的内点个数为："+bestInNum);

/*        int c = 0;
        PoListHoSelected_1 = new double[bestInNum][3];
        PoListHoSelected_2 = new double[bestInNum][3];
        Log.d(TAG, "dist&&&&&&&&&&&&&&&&&&&&&&");
        for(Integer id : BestInlierIdx){
            // Log.d(TAG, "选取的id："+id);
            PoListHoSelected_1[c] = PoListHo_1.getMatrix(id, id, 0,2).getRowPackedCopy();
//            Log.d(TAG, "PoListHoSelected_1："+PoListHoSelected_1[c][0]+","+PoListHoSelected_1[c][1]+","+PoListHoSelected_1[c][2]);
            PoListHoSelected_2[c] = PoListHo_2.getMatrix(id, id, 0,2).getRowPackedCopy();
//            Log.d(TAG, "PoListHoSelected_2："+PoListHoSelected_2[c][0]+","+PoListHoSelected_2[c][1]+","+PoListHoSelected_2[c][2]);
            double distTmp = abs(PoListHo_1.getMatrix(id, id, 0,2).times(bestF.times(PoListHo_2.getMatrix(id, id, 0,2).transpose())).get(0,0));
//            Log.d(TAG, "dist为："+distTmp);
            c++;
        }*/
        return bestF;
    }


    /**
     * 八点法结合 RANSAC 稳健估计基础矩阵 F
     * @param PoListHo_1 齐次坐标点对
     * @param PoListHo_2 齐次坐标点对
     * @param num 每次随机选取的点对数 9
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