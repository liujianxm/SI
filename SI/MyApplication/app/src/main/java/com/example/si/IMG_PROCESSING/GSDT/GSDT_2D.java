package com.example.si.IMG_PROCESSING.GSDT;

import android.graphics.Bitmap;
import android.util.Log;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import static java.lang.System.out;

public class GSDT_2D {
    enum Type {ALIVE,TRIAL,FAR};

    public static float[][] GSDT_Fun(GSDT_Para p, int[][] gray_img){
        int Height = gray_img.length;//图像高
        int Width = gray_img[0].length;//图像宽（反）
        double[] meanStd = getMeanStdValue(gray_img);
        double imgAve = meanStd[0];
        double imgStd = meanStd[1];
        double td= (imgStd<10)? 10: imgStd;
        p.bkg_thresh = (int) (imgAve +0.5*td) ;
        p.phi = new float[Height][Width];
        Log.d("GSDT_Fun","p.bkg_thresh:"+p.bkg_thresh);
        gsdt_FM(gray_img,p.phi,p.cnn_type,p.bkg_thresh);
       // Log.d("GSDT_Fun","phi[0][0]:"+p.phi[0][0]);
       // Log.d("GSDT_Fun","phi[50][100]:"+p.phi[50][100]);
        return p.phi;
    }


    public static boolean gsdt_FM(int[][] gray_img, float[][] phi, int cnn_type, int bkg_thresh){
        int Height = gray_img.length;//图像高
        int Width = gray_img[0].length;//图像宽（反）
        Log.d("GSDT_2D","Height:"+Height+", Width:"+Width);
        int total_pix = Height*Width;
        //phi = new float[Height][Width];
        if (phi == null) {
            System.out.println("phi is null");
            return false;
        }
        Type[][] state = new Type[Height][Width];
        int i,j;
        int forcounter = 0;
        int nbkg = 0;
        int bkg = 0;
        //Divide the initial background points according to the para bkg_thresh
        for(j=0;j<Height;j++){
            for(i=0;i<Width;i++){
                if(gray_img[j][i]<=bkg_thresh) {
                    phi[j][i] = gray_img[j][i];
                    state[j][i] = Type.ALIVE;
                    bkg++;
                }else{
                    phi[j][i] = Float.MAX_VALUE;
                    state[j][i] = Type.FAR;
                    forcounter++;
                }
            }
        }
        Log.d("GSDT_2D","前景数："+forcounter);
        Log.d("GSDT_2D","背景数："+bkg);
        Log.d("GSDT_2D","Enter here gsdt_FM0");
        //Create a priorityqueue to save temp Trival
        PriorityQueue<HeapElem> minHeap = new PriorityQueue<HeapElem>(total_pix, new Comparator<HeapElem>() {
            @Override
            public int compare(HeapElem o1, HeapElem o2) {
                return (o1.value - o2.value)>0?1:-1;
                //return (int) (o1.value - o2.value);
            }
        });
        Map<Integer,HeapElem> elems = new HashMap<Integer, HeapElem>();
        //Heap initialization
        for(j=0;j<Height;j++){
            for(i=0;i<Width;i++){
                if(state[j][i] == Type.ALIVE){
                    for(int dj=-1;dj<=1;dj++){
                        int j1 = j+dj;
                        if(j1<0 || j1>=Height) continue;
                        for(int di=-1;di<=1;di++){
                            int i1 = i+di;
                            if(i1<0 ||i1>=Width) continue;
                            int offset = abs(di)+abs(dj);
                            if(offset == 0 || offset > cnn_type) continue;
                            int[] min_ind = {j,i};
                            if(state[j1][i1] == Type.FAR){
                                if(phi[min_ind[0]][min_ind[1]]>0.0){
                                    for(int djj = -1;djj<=1;djj++){
                                        int j2 = j1+djj;
                                        if(j2<0 || j2>=Height) continue;
                                        for(int dii=-1;dii<=1;dii++){
                                            int i2 = i1+dii;
                                            if(i2<0 || i2>=Width) continue;
                                            int offset1 = abs(dii)+abs(djj);
                                            if(offset1 == 0||offset1>cnn_type) continue;
                                            if(state[j2][i2] == Type.ALIVE&&phi[j2][i2] < phi[min_ind[0]][min_ind[1]]){
                                                min_ind[0] = j2;
                                                min_ind[1] = i2;
                                            }
                                        }
                                    }
                                }
                                phi[j1][i1] = phi[min_ind[0]][min_ind[1]] + gray_img[j1][i1];
                                state[j1][i1] = Type.TRIAL;
                                HeapElem elem = new HeapElem(j1 * Width + i1, phi[j1][i1]);
                                minHeap.add(elem);
                                elems.put(j1 * Width + i1, elem);
                                nbkg++;
                                //System.out.println("new_TRIALtemp："+nbkg);
                            }
                        }
                    }
                }
            }
        }
        System.out.println("new_TRIAL："+nbkg);
        // System.out.println("elems.size() = "+elems.size());
        System.out.println("Enter here gsdt_FM1");

        //FM_gsdt
        while (!minHeap.isEmpty()){
            HeapElem trival_elem = minHeap.poll();
            elems.remove(trival_elem.img_index);
            int trival_elelindex = trival_elem.img_index;
            int tx = trival_elelindex % Width;
            int ty = (trival_elelindex/Width) % Height;
            state[ty][tx] = Type.ALIVE;
            int x0,y0;
            for(int jj=-1;jj<=1;jj++){
                y0 = ty+jj;
                if(y0<0 ||y0>=Height) continue;
                for(int ii=-1;ii<=1;ii++){
                    x0 = tx+ii;
                    if(x0<0 || x0>=Width) continue;
                    int offset = abs(ii)+abs(jj);
                    if(offset == 0 || offset>cnn_type) continue;
                    if(state[y0][x0] != Type.ALIVE){
                        float new_dis = (float) (phi[ty][tx]+gray_img[y0][x0]*sqrt((double)offset));
                        if(state[y0][x0] == Type.FAR){
                            phi[y0][x0] = new_dis;
                            HeapElem elem = new HeapElem(y0 * Width + x0, phi[y0][x0]);
                            minHeap.add(elem);
                            elems.put(y0 * Width + x0, elem);
                            state[y0][x0] = Type.TRIAL;
                        }else if(state[y0][x0]==Type.TRIAL){
                            if(phi[y0][x0]>new_dis){
                                phi[y0][x0] = new_dis;
                                HeapElem elem = elems.get(y0 * Width+ x0);
                                minHeap.remove(elem);
                                elem.value = phi[y0][x0];
                                minHeap.add(elem);
                                elems.remove( y0 * Width + x0);
                                elems.put(y0 * Width + x0,elem);
                            }
                        }
                    }
                }
            }
        }
        System.out.println("Enter here gsdt_FM2");
        return true;
    }

    public static double[] getMeanStdValue(int[][] gray_img){
        double[] meanStd = null;
        if(gray_img == null){
            out.println("out of data!");
            return  meanStd;
        }else{
            int Height = gray_img.length;//图像高
            int Width = gray_img[0].length;//图像宽（反）
            meanStd = new double[2];
            double mean = 0.0;
            double std = 0.0;
            int k,j,i;
            for (j = 0; j < Height; j++)
                for (i = 0; i < Width; i++) {
                    mean += gray_img[j][i];
                }
            mean /= (double)(Height*Width);
                for (j = 0; j < Height; j++) {
                    for (i = 0; i < Width; i++) {
                        std += Math.pow(gray_img[j][i] - meanStd[0], 2);
                    }
                }
            std /= (double)(Height*Width-1);
            std = Math.sqrt(std);

            meanStd[0] = mean;
            meanStd[1] = std;
            return meanStd;
        }
    }

}
