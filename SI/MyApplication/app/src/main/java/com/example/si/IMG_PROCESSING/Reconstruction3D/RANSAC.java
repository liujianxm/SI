package com.example.si.IMG_PROCESSING.Reconstruction3D;

import android.util.Log;

import com.example.si.BuildConfig;

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
import static java.lang.StrictMath.exp;
import static java.lang.StrictMath.log;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.min;
import static java.lang.StrictMath.pow;
import static java.lang.System.out;


public class RANSAC {
    private final String FundamentalRansac = "FundamentalMatrix";
    private final String HomographyRansac = "HomographyMatrix";

    public static double[][] PoListHoSelected_1;
    public static double[][] PoListHoSelected_2;
    static List<Integer> BestInlierIdxH = new ArrayList<>();
    static List<Integer> BestInlierIdxF = new ArrayList<>();
    public static float SH, SF;
    public static Matrix bestF;
    public static Matrix bestH;
    Matrix FPoSelected1 = null;
    Matrix FPoSelected2 = null;
    Matrix HPoSelected1 = null;
    Matrix HPoSelected2 = null;


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

    public static float InitializeRANSAC(Matrix PoListHo_1, Matrix PoListHo_2) throws InterruptedException {
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
        bestF = CheckFundamentalRansac(PoListHo_1, PoListHo_2, 10, 5000);
        Log.v("InitializeRANSAC","Here enter the thread H");
        bestH = CheckHomographyRansac(PoListHo_1, PoListHo_2, 9, 4000);
        return SH/(SH+SF);
    }

    public RANSAC(Matrix PoListHo_1, Matrix PoListHo_2){
/*        Thread threadF = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.v("InitializeRANSAC","Here enter the thread F");
                MyRansacModel(PoListHo_1, PoListHo_2,9, FundamentalRansac,0.01);
            }
        });*/

        Log.v("InitializeRANSAC","Here enter the thread F");
        MyRansacModel(PoListHo_1, PoListHo_2,9, FundamentalRansac,0.005);
//        Log.v("InitializeRANSAC","Here enter the thread H");
//        MyRansacModel(PoListHo_1, PoListHo_2,9, HomographyRansac,0.1);
    }

    private static float CheckHomography(Matrix PoListHo_1, Matrix PoListHo_2, Matrix H, List<Integer> inlierIdx){
        float sigma = 2f;
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
            if(currentScore > SH && inlierIdx.size()/(float)number > 0.3)//添加内点比例
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

    /**
     * // 计算 基础矩阵 得分
     * // 和卡方分布的对应值比较，由此判定该点是否为内点。累计内点的总得分
     * // p2转置 * F * p1 =  0
     *
     *  * p2 ------> p1
     *  *                    f11   f12    f13     u1
     *  *   (u2 v2 1)    *   f21   f22    f23  *  v1    = 0应该=0 不等于零的就是误差
     *  * 		             f31   f32    f33 	   1
     *  * 	a1 = f11*u2+f21*v2+f31;
     * 	    b1 = f12*u2+f22*v2+f32;
     * 	    c1 = f13*u2+f23*v2+f33;
     * 	num1 = a1*u1 + b1*v1+ c1;// 应该等0
     * 	num1*num1/(a1*a1+b1*b1);// 误差
     *
     * @param PoListHo_1
     * @param PoListHo_2
     * @param F
     * @param inlierIdx
     * @return
     */
    public static float CheckFundamental(Matrix PoListHo_1, Matrix PoListHo_2, Matrix F, List<Integer> inlierIdx){
        float sigma = 2f;
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
        if(num>=number) num = min(9,number-2);//匹配点对数小于9，可能到不了这里
        int bestInNum;
//        Matrix bestF = new Matrix(3,3);
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
            if(currentScore > SF && inlierIdx.size()/(float)number > 0.3 && inlierIdx.size()>BestInlierIdxH.size())
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
     * 八点法结合 RANSAC 稳健估计基础矩阵 F 或 H
     * @param PoListHo_1 匹配点对
     * @param PoListHo_2 匹配点对
     * @param S_num      抽样点数 （9）
     * @param matrixType 决定计算的变换矩阵类型
     * @param p_badxform 允许的错误概率，即允许RANSAC算法计算出的变换矩阵错误的概率，当前计算出的模型的错误概率小于p_badxform时迭代停止
     * @return
     */
    public void MyRansacModel(Matrix PoListHo_1, Matrix PoListHo_2, int S_num,
                                       String matrixType, double p_badxform) {
        SF = 0.0f;
        SH = 0.0f;
        //p：当前计算出的模型的错误概率，当p小于p_badxform时迭代停止
        //in_frac：内点数目占样本总数目的百分比
        double p, in_frac = 0.2;
        //number：输入的特征点数组中具有mtype类型匹配点的特征点个数
        //in：当前一致集中元素个数
        //in_min：一致集中元素个数允许的最小值，保证RANSAC最终计算出的转换矩阵错误的概率小于p_badxform所需的最小内点数目
        //in_max：当前最优一致集(最大一致集)中元素的个数
        //k：迭代次数，与计算当前模型的错误概率有关
        int number = PoListHo_1.getRowDimension();
        if (BuildConfig.DEBUG && number <= S_num) {
            throw new AssertionError("Assertion number > 8 failed!!");
        }
        int in, in_min, in_max = 0, k = 0;
        //计算保证RANSAC最终计算出的转换矩阵错误的概率小于p_badxform所需的最小内点数目
        in_min = calc_min_inliers(number, S_num, 0.2, p_badxform );
        Log.d(TAG, "in_min为++++++++++++++：" + in_min);
        //当前计算出的模型的错误概率,内点所占比例in_frac越大，错误概率越小；迭代次数k越大，错误概率越小
        //当前错误概率p的计算公式为：p=( 1 - in_frac^m)^k
        p = pow(1.0 - pow(in_frac, S_num), k);
        float currentScore;
        int MaxIterCount = 18000;
        while(p > p_badxform && k<=MaxIterCount){
            Set<Integer> idx = randPerm(number, S_num);
            Matrix Po1Sample = new Matrix(S_num, 3);
            Matrix Po2Sample = new Matrix(S_num, 3);
            int count = 0;
            for (Integer idxSample : idx) {
                Po1Sample.setMatrix(count, count, 0, 2, PoListHo_1.getMatrix(idxSample, idxSample, 0, 2));
                Po2Sample.setMatrix(count, count, 0, 2, PoListHo_2.getMatrix(idxSample, idxSample, 0, 2));
                count++;
            }

            Matrix MatrixEstimate;
            List<Integer> inlierIdx = new ArrayList<>();
            if(matrixType.equals(FundamentalRansac)){
                MatrixEstimate = compute_fundamental_normalized(Po1Sample, Po2Sample);
                currentScore = CheckFundamental(PoListHo_1, PoListHo_2, MatrixEstimate, inlierIdx);
                in = inlierIdx.size();
                in_max = BestInlierIdxF.size();
            }else {
                MatrixEstimate = compute_Homography_normalized(Po1Sample, Po2Sample);
                currentScore = CheckHomography(PoListHo_1, PoListHo_2, MatrixEstimate, inlierIdx);
                in = inlierIdx.size();
                in_max = BestInlierIdxH.size();
            }

/*            List<Integer> inlierIdx = new ArrayList<>();
            currentScore = CheckFundamental(PoListHo_1, PoListHo_2, MatrixEstimate, inlierIdx);
            in = inlierIdx.size();
            in_max = BestInlierIdxF.size();*/
//            if(currentScore > SF && in > in_max)
            if(in > in_max)
            {
                if(matrixType.equals(FundamentalRansac)){
                    SF = currentScore;
                    BestInlierIdxF = inlierIdx;
//                    bestF = MatrixEstimate;
                    Log.d(TAG, "F 目前的内点个数为："+BestInlierIdxF.size());
                }else {
                    SH = currentScore;
                    BestInlierIdxH = inlierIdx;
                    Log.d(TAG, "H 目前的内点个数为："+BestInlierIdxH.size());
                }
                in_max = in;
                in_frac = in_max/(float)number;
            }
            p = pow( 1.0 - pow( in_frac, S_num ), ++k );
//            Log.d(TAG, "此时的P为++++++++++++++：" + p);
        }
        Log.d(TAG, "迭代的次数k为：" + k);
        Log.d(TAG, "此时的内点比例为：" + in_frac);
        /* calculate final transform based on best consensus set */
        //若最优一致集中元素个数大于最低标准，即符合要求
        double[][] PoListHoSelected_1;
        double[][] PoListHoSelected_2;
        if(in_max >= in_min){
            if(matrixType.equals(FundamentalRansac)){
                PoListHoSelected_1 = new double[BestInlierIdxF.size()][3];
                PoListHoSelected_2 = new double[BestInlierIdxF.size()][3];
                int cc=0;
                for(Integer id : BestInlierIdxF){
                    // Log.d(TAG, "选取的id："+id);
                    PoListHoSelected_1[cc] = PoListHo_1.getMatrix(id, id, 0,2).getRowPackedCopy();
//                Log.d(TAG, "FFFFFPoListHoSelected_1："+PoListHoSelected_1[cc][0]+","+PoListHoSelected_1[cc][1]+","+PoListHoSelected_1[cc][2]);
                    PoListHoSelected_2[cc] = PoListHo_2.getMatrix(id, id, 0,2).getRowPackedCopy();
//                Log.d(TAG, "PoListHoSelected_2："+PoListHoSelected_2[cc][0]+","+PoListHoSelected_2[cc][1]+","+PoListHoSelected_2[cc][2]);
//            double distTmp = abs(PoListHo_1.getMatrix(id, id, 0,2).times(bestH.times(PoListHo_2.getMatrix(id, id, 0,2).transpose())).get(0,0));
//            Log.d(TAG, "dist为："+distTmp);
                    cc++;
                }
                FPoSelected1 = new Matrix(PoListHoSelected_1);
                FPoSelected2 = new Matrix(PoListHoSelected_2);
                bestF = compute_fundamental_normalized(FPoSelected1, FPoSelected2);
                BestInlierIdxF  = new ArrayList<>();
                SF = CheckFundamental(PoListHo_1, PoListHo_2, bestF, BestInlierIdxF);
                //////////再一次迭代计算///////////
                int finalInliersnum = BestInlierIdxF .size();
                PoListHoSelected_1 = new double[finalInliersnum][3];
                PoListHoSelected_2 = new double[finalInliersnum][3];
                int ccc=0;
                for(Integer id : BestInlierIdxF){
                    // Log.d(TAG, "选取的id："+id);
                    PoListHoSelected_1[ccc] = PoListHo_1.getMatrix(id, id, 0,2).getRowPackedCopy();
//                Log.d(TAG, "FFFFFPoListHoSelected_1："+PoListHoSelected_1[ccc][0]+","+PoListHoSelected_1[ccc][1]+","+PoListHoSelected_1[ccc][2]);
                    PoListHoSelected_2[ccc] = PoListHo_2.getMatrix(id, id, 0,2).getRowPackedCopy();
//                Log.d(TAG, "PoListHoSelected_2："+PoListHoSelected_2[ccc][0]+","+PoListHoSelected_2[ccc][1]+","+PoListHoSelected_2[ccc][2]);
//            double distTmp = abs(PoListHo_1.getMatrix(id, id, 0,2).times(bestH.times(PoListHo_2.getMatrix(id, id, 0,2).transpose())).get(0,0));
//            Log.d(TAG, "dist为："+distTmp);
                    ccc++;
                }
                FPoSelected1 = new Matrix(PoListHoSelected_1);
                FPoSelected2 = new Matrix(PoListHoSelected_2);
                bestF = compute_fundamental_normalized(FPoSelected1, FPoSelected2);
                Log.d(TAG, "最终的内点个数为："+BestInlierIdxF.size()+"此时的内点最终比例为：" + BestInlierIdxF.size()/(float)number);
                for(Integer integer :BestInlierIdxF){
                    out.println(integer);
                }
            }else {
                PoListHoSelected_1 = new double[BestInlierIdxH.size()][3];
                PoListHoSelected_2 = new double[BestInlierIdxH.size()][3];
                int cc=0;
                for(Integer id : BestInlierIdxH){
                    // Log.d(TAG, "选取的id："+id);
                    PoListHoSelected_1[cc] = PoListHo_1.getMatrix(id, id, 0,2).getRowPackedCopy();
//                Log.d(TAG, "FFFFFPoListHoSelected_1："+PoListHoSelected_1[cc][0]+","+PoListHoSelected_1[cc][1]+","+PoListHoSelected_1[cc][2]);
                    PoListHoSelected_2[cc] = PoListHo_2.getMatrix(id, id, 0,2).getRowPackedCopy();
//                Log.d(TAG, "PoListHoSelected_2："+PoListHoSelected_2[cc][0]+","+PoListHoSelected_2[cc][1]+","+PoListHoSelected_2[cc][2]);
//            double distTmp = abs(PoListHo_1.getMatrix(id, id, 0,2).times(bestH.times(PoListHo_2.getMatrix(id, id, 0,2).transpose())).get(0,0));
//            Log.d(TAG, "dist为："+distTmp);
                    cc++;
                }
                HPoSelected1 = new Matrix(PoListHoSelected_1);
                HPoSelected2 = new Matrix(PoListHoSelected_2);
                bestH = compute_Homography_normalized(HPoSelected1, HPoSelected2);
                BestInlierIdxH  = new ArrayList<>();
                SF = CheckFundamental(PoListHo_1, PoListHo_2, bestH, BestInlierIdxH);
                //////////再一次迭代计算///////////
                int finalInliersnum = BestInlierIdxH.size();
                PoListHoSelected_1 = new double[finalInliersnum][3];
                PoListHoSelected_2 = new double[finalInliersnum][3];
                int ccc=0;
                for(Integer id : BestInlierIdxH){
                    // Log.d(TAG, "选取的id："+id);
                    PoListHoSelected_1[ccc] = PoListHo_1.getMatrix(id, id, 0,2).getRowPackedCopy();
//                Log.d(TAG, "FFFFFPoListHoSelected_1："+PoListHoSelected_1[ccc][0]+","+PoListHoSelected_1[ccc][1]+","+PoListHoSelected_1[ccc][2]);
                    PoListHoSelected_2[ccc] = PoListHo_2.getMatrix(id, id, 0,2).getRowPackedCopy();
//                Log.d(TAG, "PoListHoSelected_2："+PoListHoSelected_2[ccc][0]+","+PoListHoSelected_2[ccc][1]+","+PoListHoSelected_2[ccc][2]);
//            double distTmp = abs(PoListHo_1.getMatrix(id, id, 0,2).times(bestH.times(PoListHo_2.getMatrix(id, id, 0,2).transpose())).get(0,0));
//            Log.d(TAG, "dist为："+distTmp);
                    ccc++;
                }
                HPoSelected1 = new Matrix(PoListHoSelected_1);
                HPoSelected2 = new Matrix(PoListHoSelected_2);
                bestH = compute_Homography_normalized(HPoSelected1, HPoSelected2);
                Log.d(TAG, "最终的内点个数为："+BestInlierIdxH.size()+"此时的内点最终比例为：" + BestInlierIdxH.size()/(float)number);
            }
        }else {
            FPoSelected1 = PoListHo_1;
            FPoSelected2 = PoListHo_2;
            HPoSelected1 = PoListHo_1;
            HPoSelected2 = PoListHo_2;
        }
    }


    private static int calc_min_inliers(int n, int m, double p_badsupp, double p_badxform ){
        double pi, sum;
        int i, j;
        for(j = m+1; j<=n; j++){
            sum = 0;
            for(i=j; i<=n; i++){
                pi = (i-m) * log(p_badsupp) + ( n-i+m ) * log( 1.0 - p_badsupp ) +
                        log_factorial( n - m ) - log_factorial( i - m ) -
                        log_factorial( n - i );
                sum += exp( pi );
            }
            if( sum < p_badxform )
                break;
        }
        return j;
    }

    private static double log_factorial(int n){
        double f = 0;
        int i;
        for( i = 1; i <= n; i++ ){
            f += log( i );
        }
        return f;
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