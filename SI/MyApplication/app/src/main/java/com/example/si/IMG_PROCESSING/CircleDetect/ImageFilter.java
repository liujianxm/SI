package com.example.si.IMG_PROCESSING.CircleDetect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

public class ImageFilter {
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static Bitmap blurBitmap(@NonNull Context context, Bitmap bitmap) {
        //用需要创建高斯模糊bitmap创建一个空的bitmap
        Bitmap outBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        // 初始化Renderscript，该类提供了RenderScript context，创建其他RS类之前必须先创建这个类，其控制RenderScript的初始化，资源管理及释放
        RenderScript rs = RenderScript.create(context);
        // 创建高斯模糊对象
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        // 创建Allocations，此类是将数据传递给RenderScript内核的主要方 法，并制定一个后备类型存储给定类型
        Allocation allIn = Allocation.createFromBitmap(rs, bitmap);
        Allocation allOut = Allocation.createFromBitmap(rs, outBitmap);
        //设定模糊度(注：Radius最大只能设置25.f)
        blurScript.setRadius(10.0f);
        // Perform the Renderscript
        blurScript.setInput(allIn);
        blurScript.forEach(allOut);
        // Copy the final bitmap created by the out Allocation to the outBitmap
        allOut.copyTo(outBitmap);
        // recycle the original bitmap
        // bitmap.recycle();
        // After finishing everything, we destroy the Renderscript.
        rs.destroy();
        Log.d("EdgeDetect","hhhhhhhhhhhhhhhhhhh");
        return outBitmap;
    }

    //////////////双边滤波/////////////
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Bitmap bilateralFilter(Bitmap bitmap, int d, double sigma_color, double sigma_space){
        Bitmap outBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        int i,j,k,maxk,radius;
        if(sigma_color<=0) sigma_color = 1;
        if(sigma_space<=0) sigma_space = 1;
        //计算颜色域和空间域的权重的高斯核系数，均值μ=0；exp(-1/(2*sigma^2))
        double guass_color_coeff = -0.5/(sigma_color*sigma_color);
        double guass_space_coeff = -0.5/(sigma_space*sigma_space);
        //radius为空间域的大小：其值是window_size的一半
        if(d<=0){
            radius = (int) Math.round(sigma_space*1.5);
        }else{
            radius = d/2;
        }
        radius = Math.max(radius,1);
        d = radius*2+1;
        Log.d("ImageFilter","radius:"+radius);
        Log.d("ImageFilter","d:"+d);
        ///////////////////
        int cn = 3;
        Bitmap temp = copyMakeBorder(bitmap,radius,radius,radius,radius);//复制补边
        //重新构建数组
        int[] tempArray = new int[cn*(bitmap.getWidth()+2*radius)*(bitmap.getHeight()+2*radius)];
        int tempWidth = bitmap.getWidth()+2*radius;
        int tempHeight = bitmap.getHeight()+2*radius;
        for(i=0;i<tempHeight;i++){
            for(j=0;j<tempWidth;j++){
                int color = temp.getPixel(j,i);
                //Log.d("ImageFilter", "cur_pixel(" + j + "," + i + "):" + color);
                int i1 = i * cn * tempWidth + cn * j;
                tempArray[i1] = Color.red(color);
                tempArray[i1+1] = Color.green(color);
                tempArray[i1+2] = Color.blue(color);
            }
        }

        float[] color_weight = new float[cn*256];
        float[] space_weight = new float[d*d];
        int[] space_ofs = new int[d*d];
        //初始化颜色相关的滤波器系数exp(-1*x^2/(2*sigma^2))
        Log.d("ImageFilter","开始初始化颜色相关滤波器系数");
        for(i=0;i<256*cn;i++){
            color_weight[i] = (float)Math.exp(i*i*guass_color_coeff);
            //Log.d("ImageFilter","color_weight["+i+"]"+color_weight[i]);
        }
        Log.d("ImageFilter","开始初始化空间相关滤波器系数和offest");
        for(i=-radius,maxk=0;i<=radius;i++){
            for(j=-radius;j<=radius;j++){
                double r = Math.sqrt(i*i+j*j);
                //Log.d("ImageFilter","r:("+i+","+j+")_"+r);
                if(r>radius) continue;
                space_weight[maxk] = (float)Math.exp(r*r*guass_space_coeff);
                space_ofs[maxk] = (i*temp.getWidth()+j)*cn;//核内位置偏移
                // Log.d("ImageFilter","space_ofs["+maxk+"]:"+space_ofs[maxk]+",,,"+"space_weight["+maxk+"]:"+space_weight[maxk]);
                maxk++;
            }
        }
        Log.d("ImageFilter","开始计算滤波后的像素值");
        //int count= 0;
        for(i=0;i<bitmap.getHeight();i++){
            for(j=0;j<bitmap.getWidth();j++){
                int cur_pixel = ((i+radius)*tempWidth+(radius+j))*cn;
                //count++;
                float sum_r=0, sum_g=0, sum_b=0, wsum=0;
                int r0 = tempArray[cur_pixel];
                int g0 = tempArray[cur_pixel+1];
                int b0 = tempArray[cur_pixel+2];
                for(k=0;k<maxk;k++){
                    int sptr_k = cur_pixel+space_ofs[k];
                    int r = tempArray[sptr_k];
                    int g = tempArray[sptr_k+1];
                    int b = tempArray[sptr_k+2];
                    float w = space_weight[k]*color_weight[Math.abs(r-r0)+Math.abs(g-g0)+Math.abs(b-b0)];
                    sum_b += b*w;
                    sum_g += g*w;
                    sum_r += r*w;
                    wsum += w;
                }
                wsum = 1.0f/wsum;
                r0 = Math.round(sum_r*wsum);
                g0 = Math.round(sum_g*wsum);
                b0 = Math.round(sum_b*wsum);
                int color_tar = Color.argb(Color.alpha(bitmap.getPixel(j,i)),r0,g0,b0);
                //Log.d("ImageFilter"," color_tar:"+color_tar);
                outBitmap.setPixel(j,i,color_tar);
            }
        }
        return outBitmap;
    }

    //补边函数
    public static Bitmap copyMakeBorder(Bitmap ori_img,int top,int bottom,int left,int right){
        int ori_width = ori_img.getWidth();
        int ori_height = ori_img.getHeight();
        int tar_width = ori_width+left+right;
        int tar_height = ori_height+top+bottom;
        //先补上下，后补左右
        int tempsize_1 = ori_width*(ori_height+top+bottom);
        int[] tempArray_1 = new int[tempsize_1];
        int tempsize_2 = (ori_width+left+right)*(ori_height+top+bottom);
        int[] tempArray_2 = new int[tempsize_2];

        //补上下
        for(int j=0;j<ori_height+top+bottom;j++){
            for(int i=0;i<ori_width;i++){
                if(j<top){
                    tempArray_1[j*ori_width+i] = ori_img.getPixel(i,0);
                }else if(j>=(ori_height+top)){
                    tempArray_1[j*ori_width+i] =  ori_img.getPixel(i,ori_height-1);
                }else {
                    tempArray_1[j*ori_width+i] = ori_img.getPixel(i,j-top);
                }
            }
        }
        Bitmap temp_img = Bitmap.createBitmap(tempArray_1,ori_width,tar_height,Bitmap.Config.ARGB_8888);
        //补左右
        for(int i=0;i<ori_width+left+right;i++){
            for(int j=0;j<temp_img.getHeight();j++){
                final int i1 = j * (ori_width + left + right) + i;
                if(i<left){
                    tempArray_2[i1] = temp_img.getPixel(0,j);
                }else if(i>=(left+ori_width)){
                    tempArray_2[i1] = temp_img.getPixel(ori_width-1,j);
                }else{
                    tempArray_2[i1] = temp_img.getPixel(i-left,j);
                }
            }
        }
        temp_img.recycle();
        return Bitmap.createBitmap(tempArray_2,tar_width,tar_height,Bitmap.Config.ARGB_8888);
    }

    ////////////////灰度图的直方图均衡化函数////////////////
    public static int[][] EqualizeHist(int[][] gray_img){
        int img_width = gray_img.length;
        int img_height = gray_img[0].length;
        int[] nSumPix = new int[256];
        double[] nProDis = new double[256];
        double[] nSumProDis = new double[256];
        int[] EqualizeSumPix = new int[256];
        for(int j=0;j<img_height;j++){
            for(int i=0;i<img_width;i++){
                nSumPix[gray_img[i][j]]++;
            }
        }
        for(int i=0;i<256;i++){
            nProDis[i] = (double)nSumPix[i]/(img_height*img_width);
        }
        nSumProDis[0] = nProDis[0];
        for(int i=1;i<256;i++){
            nSumProDis[i] = nSumProDis[i-1]+nProDis[i];
        }
        for(int i=0;i<256;i++){
            EqualizeSumPix[i] = (int) Math.round(nSumProDis[i]*255);
        }
        int[][] img_out = new int[img_width][img_height];
        for(int j=0;j<img_height;j++){
            for(int i=0;i<img_width;i++){
                img_out[i][j] = EqualizeSumPix[gray_img[i][j]];
            }
        }
        return img_out;
    }


}
