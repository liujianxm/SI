package com.example.si.IMG_PROCESSING.Reconstruction3D;

import Jama.Matrix;

import static java.lang.StrictMath.pow;

public class BundleAdjustment_LM {
    public static void LM(Matrix Po1, Matrix Po2, Matrix P1, Matrix P2, Matrix Po3D){
        int PoNumber = Po1.getRowDimension();
        int Iters = 200;//set the number of iterations for the LM algorithm
        float lamda = 0.01f;//initial the value of damping factor
        int updateJ = 1;
        int Ndata = 4 * PoNumber;
        int Nparams = 25;
        Matrix I = CreateEyeMatrix(Nparams);
        Matrix J = null;
        Matrix H = null;
        double error = 0;
        double error_im;
        Matrix d = new Matrix(Ndata, 1);
        double[] dp;
        Matrix ComputePo1 = P1.times(Po3D.transpose()).transpose();//此处未除以齐次坐标位
        Matrix ComputePo2 = P2.times(Po3D.transpose()).transpose();
        for(int i=1; i<=Iters; i++){
            if(updateJ == 1){
                J = new Matrix(Ndata, Nparams);
                for(int j=0; j<PoNumber; j++){
                    /////////此处计算J的值/////////
                    Matrix PoSingle = Po3D.getMatrix(j,j,0,2);
                    J.setMatrix(4*j, 4*j+3,0,24,CreateJacobiMatrix(P1, P2, PoSingle, 4, 25));
                    //////////////////////////////
                    d.set(4*j, 0, Po1.minus(ComputePo1).get(j,0));
                    d.set(4*j+1, 0, Po1.minus(ComputePo1).get(j,1));
                    d.set(4*j+2, 0, Po2.minus(ComputePo2).get(j,0));
                    d.set(4*j+3, 0, Po2.minus(ComputePo2).get(j,1));
                }
                // compute the approximated Hessian matrix
                H = (J.transpose()).times(J);
                // the first iteration : compute the initial total error
                if(i == 1){
                    error = d.transpose().times(d).get(0,0);
                    System.out.println("Error_init:"+error);
                }
            }
            // Apply the damping factor to the Hessian matrix
            Matrix H_im = H.plus(I.times(lamda));
            //Compute the updated parameters
            dp = (H_im.inverse().times(J.transpose().times(d))).times(-1).getColumnPackedCopy();
            Matrix P1_im = new Matrix(3,4), P2_im = new Matrix(3,4);
            for(int ii=0; ii<3; ii++){
                for(int jj=0; jj<4; jj++){
                    P1_im.set(ii, jj, P1.get(ii, jj)+dp[4*ii+jj]);
                    P2_im.set(ii, jj, P2.get(ii, jj)+dp[11+4*ii+jj]);
                }
            }
            Matrix Po3D_im = Po3D.copy(); //拷贝可能会出问题
            for(int iii=0; iii<Po1.getRowDimension(); iii++){
                Po3D_im.set(iii, 0, Po3D.get(iii, 0)+dp[22]);
                Po3D_im.set(iii, 1, Po3D.get(iii, 1)+dp[23]);
                Po3D_im.set(iii, 2, Po3D.get(iii, 2)+dp[24]);
            }
            // Evaluate the total geometric distance at the updated parameters
            Matrix ComputePo1_im = P1_im.times(Po3D_im.transpose()).transpose();//此处未除以齐次坐标位
            Matrix ComputePo2_im = P2_im.times(Po3D_im.transpose()).transpose();
            Matrix d_im = new Matrix(Ndata, 1);
            for(int j=0; j<PoNumber; j++){
                d_im.set(4*j, 0, Po1.minus(ComputePo1_im).get(j,0));
                d_im.set(4*j+1, 0, Po1.minus(ComputePo1_im).get(j,1));
                d_im.set(4*j+2, 0, Po2.minus(ComputePo2_im).get(j,0));
                d_im.set(4*j+3, 0, Po2.minus(ComputePo2_im).get(j,1));
            }
            error_im = d_im.transpose().times(d_im).get(0,0);
            System.out.println("迭代"+i+"次后的Error_im:"+error_im);
            if(error_im<error){
                lamda /= 10;
                P1 = P1_im;
                P2 = P2_im;
                Po3D = Po3D_im;
                error = error_im;
                updateJ = 1;
                System.out.println("更新error***迭代"+i+"次后的Error:"+error_im);
            }else {
                updateJ = 0;
                lamda *= 10;
            }
        }

    }

    /**
     * 构造单位矩阵
     * @param dimension 单位矩阵维度
     * @return 单位矩阵
     */
    public static Matrix CreateEyeMatrix(int dimension){
        Matrix Eye = new Matrix(dimension,dimension);
        for(int i=0; i<dimension; i++){
            Eye.set(i,i,1);
        }
        return Eye;
    }

    /**
     * 构造 Jacobi
     * @param P1 第一幅图的投影矩阵
     * @param P2 第二幅图的投影矩阵
     * @param Po3D 计算获得的三维点坐标(单个点的坐标) 1*4
     * @return 当前参数下的坐标
     */
    public static Matrix CreateJacobiMatrix(Matrix P1, Matrix P2, Matrix Po3D, int row, int col){
        double[][] JacobiArray = new double[row][col];
        double X1 = Po3D.get(0,0);
        double X2 = Po3D.get(0,1);
        double X3 = Po3D.get(0,2);
        double P111 = P1.get(0,0);
        double P112 = P1.get(0,1);
        double P113 = P1.get(0,2);
        double P114 = P1.get(0,3);
        double P121 = P1.get(1,0);
        double P122 = P1.get(1,1);
        double P123 = P1.get(1,2);
        double P124 = P1.get(1,3);
        double P131 = P1.get(2,0);
        double P132 = P1.get(2,1);
        double P133 = P1.get(2,2);
        double P211 = P2.get(0,0);
        double P212 = P2.get(0,1);
        double P213 = P2.get(0,2);
        double P214 = P2.get(0,3);
        double P221 = P2.get(1,0);
        double P222 = P2.get(1,1);
        double P223 = P2.get(1,2);
        double P224 = P2.get(1,3);
        double P231 = P2.get(2,0);
        double P232 = P2.get(2,1);
        double P233 = P2.get(2,2);
        double v = P131 * X1 + P132 * X2 + P133 * X3 + 1;
        double v1 = P124 + P121 * X1 + P122 * X2 + P123 * X3;
        double v2 = P114 + P111 * X1 + P112 * X2 + P113 * X3;
        final double v3 = P231 * X1 + P232 * X2 + P233 * X3 + 1;
        double v4 = P214 + P211 * X1 + P212 * X2 + P213 * X3;
        final double v5 = P224 + P221 * X1 + P222 * X2 + P223 * X3;
        JacobiArray[0][0] = X1/ v - X1;
        JacobiArray[0][1] = X2/ v - X2;
        JacobiArray[0][2] = X3/ v - X3;
        JacobiArray[0][3] = 1/ v - 1;
        JacobiArray[1][4] = X1/ v - X1;
        JacobiArray[1][5] = X2/ v - X2;
        JacobiArray[1][6] = X3/ v - X3;
        JacobiArray[1][7] = 1/ v - 1;
        JacobiArray[1][8] = -(X1* v1)/pow(v,2) ;
        JacobiArray[0][8] = -(X1* v2)/pow(v,2);
        JacobiArray[1][9] = -(X2* v1)/pow(v,2) ;
        JacobiArray[0][9] = -(X2* v2)/pow(v,2);
        JacobiArray[1][10] = -(X3* v1)/pow(v,2) ;
        JacobiArray[0][10] = -(X3* v2)/pow(v,2);
        JacobiArray[2][11] = X1/ v3 - X1;
        JacobiArray[2][12] = X2/ v3 - X2;
        JacobiArray[2][13] = X3/ v3 - X3;
        JacobiArray[2][14] = 1/ v3 - 1;
        JacobiArray[3][15] = X1/ v3 - X1;
        JacobiArray[3][16] = X2/ v3 - X2;
        JacobiArray[3][17] = X3/ v3 - X3;
        JacobiArray[3][18] = 1/ v3 - 1;
        JacobiArray[2][19] = -(X1* v4)/pow(v3,2);
        JacobiArray[3][19] = -(X1* v5)/pow(v3,2);
        JacobiArray[2][20] = -(X2* v4)/pow(v3,2);
        JacobiArray[3][20] = -(X2* v5)/pow(v3,2);
        JacobiArray[2][21] = -(X3* v4)/pow(v3,2);
        JacobiArray[3][21] = -(X3* v5)/pow(v3,2);
        JacobiArray[0][22] = P111/ v - P111 - (P131* v2)/pow(v,2);
        JacobiArray[1][22] = P121/ v - P121 - (P131* v1)/pow(v,2);
        JacobiArray[2][22] = P211/ v3 - P211 - (P231* v4)/pow(v3,2);
        JacobiArray[3][22] = P221/ v3 - P221 - (P231* v5)/pow(v3,2);
        JacobiArray[0][23] = P112/ v - P112 - (P132* v2)/pow(v,2);
        JacobiArray[1][23] = P122/ v - P122 - (P132* v1)/pow(v,2);
        JacobiArray[2][23] = P212/ v3 - P212 - (P232* v4)/pow(v3,2);
        JacobiArray[3][23] = P222/ v3 - P222 - (P232* v5)/pow(v3,2);
        JacobiArray[0][24] = P113/ v - P113 - (P133* v2)/pow(v,2);
        JacobiArray[1][24] = P123/ v - P123 - (P133* v1)/pow(v,2);
        JacobiArray[2][24] = P213/ v3 - P213 - (P233* v4)/pow(v3,2);
        JacobiArray[3][24] = P223/ v3 - P223 - (P233* v5)/pow(v3,2);

        /////////////////////////////////////////////////
//        for(int i=0; i<row; i++){
//            for(int j=0; j<col; j++){
//                System.out.println(JacobiArray[i][j]);
//            }
//        }
        return new Matrix(JacobiArray);
    }
}
