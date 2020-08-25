package com.example.si.IMG_PROCESSING.CircleDetect;

import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.example.si.BuildConfig;
import com.example.si.IMG_PROCESSING.ImgObj_Para;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

public class HoughCircle {
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static ArrayList<Point> HoughCircle_Fun(Bitmap blur_bitmap) throws Exception {
        ////////////////////////////////////
        ImgObj_Para imobj = new ImgObj_Para(blur_bitmap);//创建图像处理对象
        double dRationHigh=0.88,dRationLow=0.67;///canny边缘检测高低阈值比例
        EdgeDetect_fun.Canny_edge(imobj,blur_bitmap,dRationHigh,dRationLow);//边缘检测
        System.out.println("Enter here(the Edgedetect has been finished)");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //HoughCircleGradient(imobj,1,380,60,320,37,imobj.circles,imobj.circle_max);
            HoughCircleGradient(imobj,1,380,100,280,38,imobj.circles,imobj.circle_max);//参数影响特别大
            Log.d("HoughCirle:","输出圆心个数:"+imobj.circles.size());
            System.out.println("Enter here(the main fun of Houghcircle has been finished)");
        }
        //return imobj.line;
        return imobj.circles;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void HoughCircleGradient(ImgObj_Para imobj, float dp, float min_dist, int min_radius, int max_radius, int acc_threshold, ArrayList<Point> circles, int circle_max){
        int img_width = imobj.width;//输入图像宽(bitmap)
        int img_height = imobj.height;//输入图像高
        //为了提高运算精度，定义一个数值的位移量
        final int SHIFT = 10, ONE = 1 << SHIFT, R_THRESH = 0;//R_THRESH是起始值，赋给max_count,后续会被覆盖
        int[][] edges = imobj.tar_img[1];//图像边界信息
        Log.v("HoughCircle","edges:"+edges[40][234]);
        int[][] accum;//定义累加器矩阵
        //定义排序向量,最大堆优先队列
        PriorityQueue<Point> sort_buf = new PriorityQueue<Point>(new Comparator<Point>() {
            @Override
            public int compare(Point o1, Point o2) {
                return o2.accu_value - o1.accu_value;
            }
        });
        //创建半径距离矩阵,最大堆优先队列
        PriorityQueue<Integer> dist_buf = new PriorityQueue<Integer>(new Comparator<Integer>(){
            @Override
            public int compare(Integer i1, Integer i2) {
                return i2 - i1;
            }
        });
        /////定义存储/////
        int x, y, i, j, k, center_count, nz_count;
        //事先计算好最小半径和最大半径的平方
        int min_radius2 = min_radius*min_radius;
        int max_radius2 = max_radius*max_radius;
        int arows,acols,astep;
        int[] adata, ddata;
        ArrayList<Point> nz = new ArrayList<>();
        ArrayList<Point> centers = new ArrayList<>();//nz表示圆周点序列，centers表示圆心序列
        float idp,dr;//inv_dp，dp的倒数
        //确保累加器矩阵的分辨率不小于1
        if(dp < 1.0f) dp = 1.0f;
        //分辨率倒数
        idp = 1.0f/dp;
        //根据分辨率，创建累加器矩阵
        accum = new int[(int) (img_height*idp+2)][(int) (img_width*idp+2)];
        adata = new int[(int)((img_height*idp+2)*(img_width*idp+2))];///!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        //定义累加器的高，宽，步长
        arows = accum.length - 2;//宽
        acols = accum[0].length - 2;//高
        astep = accum.length;  //用于跨行移动，例如第一行p元素，p+astep则移动到第二行p正下方元素
        //GradSobel(edges,imobj.edgeGradX,imobj.edgeGradY,imobj.edgeGradXY,img_width,img_height);//Sobel求边缘图像梯度值
        int[][] dx = new int[img_height][img_width];
        int[][] dy = new int[img_height][img_width];
        int[][] edgeGradXY = new int[img_height][img_width];
        //ImgGrad(edges,imobj.edgeGradX,imobj.edgeGradY,img_height,img_width);
        ImgGrad(edges,dx,dy,img_height,img_width);
//        int[][] dx = imobj.edgeGradX;//水平梯度矩阵
//        int[][] dy = imobj.edgeGradY;//垂直梯度矩阵
        //int[][] dxy = imobj.edgeGradXY;
        //对边缘图像计算累加和
        //int flag = 0;
        for(x=0;x<img_height;x++){
            for(y=0;y<img_width;y++) {
                int vx, vy, x0, y0, x1, y1, r;
                float sx, sy;
                vx = dx[x][y];//当前的水平梯度和垂直梯度值
                vy = dy[x][y];
                //如果当前像素不是边缘点或水平垂直梯度都为0，则不会是圆周上的点，继续循环
                if (edges[x][y] == 0 || (vx == 0 && vy == 0)) continue;
                //计算当前点的梯度值
                int mag = (int) Math.sqrt(vx*vx+vy*vy);
                if (BuildConfig.DEBUG && !(mag >= 1)) {
                    throw new AssertionError("Assertion failed");
                }
                //定义水平和垂直的位移量
                sx = (vx*idp)*ONE/mag;
                sy = (vy*idp)*ONE/mag;
                //把当前点的坐标定位到累加器相应位置
                x0 = (int)(x*idp)*ONE;
                y0 = (int)(y*idp)*ONE;
                //在梯度的两个方向上进行位移并对累加器进行投票累计
                for(k=0;k<2;k++){
                    x1 = (int) (x0 + min_radius*sx);
                    y1 = (int) (y0 + min_radius*sy);
                    for(r=min_radius;r<=max_radius;x1+=sx,y1+=sy,r++){
                        int x2 = x1 >> SHIFT,y2 = y1 >> SHIFT;//变回真实坐标
                        if(x2<=0 || y2<=0 || x2>=arows || y2>=acols) break;//change
                        adata[y2*astep+x2]++;//累加器相应位置加一
                        accum[x2][y2]++;
/*
                        //================
                        if(flag>2550&&flag<2700) {
                            Point poi = new Point();
                            poi.x = x2;
                            poi.y = y2;
                            imobj.line.add(poi);
                        }
                        //================
*/

                    }
                    sx = -sx;//位移量设置为反方向
                    sy = -sy;
                }

                //================
                //flag += 1;
                //================

                //把输入图像中的当前点（即圆周上的点）的坐标压入序列圆周序列nz中
                Point nz_temp = new Point();
                nz_temp.x = x;
                nz_temp.y = y;
                nz.add(nz_temp);
            }
        }
        /////////////////////////
        nz_count = nz.size();
        Log.v("HoughCircle","nz_count:"+nz_count);
        if(nz_count==0) return;
        //遍历累加器矩阵找到可能的圆心
        for(x=1;x<arows-1;x++){
            for(y=1;y<acols-1;y++){
                //if(edges[x][y]==255) continue;///////test
                int base = y*(arows+2) + x;
                //如果当前的值大于阈值，并且在4邻域内是最大值，则认为是圆心
                if((adata[base]>acc_threshold) && (adata[base]>adata[base-1]) && (adata[base]>adata[base+1]) && (adata[base]>adata[base-arows-2]) && (adata[base]>adata[base+arows+2])){
                    //压入圆心序列
                    Point po_temp = new Point();
                    po_temp.x = x;
                    po_temp.y = y;
                    po_temp.accu_value = accum[x][y];
                    sort_buf.add(po_temp);
                }
            }
        }
        //将可能的圆心点按照累积值降序重新排列，再存入centers
        while(!sort_buf.isEmpty()){
            Point po_sorted = sort_buf.poll();
            //circles.add(po_sorted);
            centers.add(po_sorted);
        }
        center_count = centers.size();
        System.out.println("center_count:"+center_count);
        if(center_count==0) return;
        dr = dp;//定义圆半径的距离分辨率
        //重新定义圆心之间的最小距离
        min_dist = Math.max(min_dist,dp);
        //最小距离的平方
        min_dist *= min_dist;
        // For each found possible center
        // Estimate radius and check support
        //按照由大到小的顺序遍历整个圆心序列
        for(i=0;i<center_count;i++){
            //提取出圆心，得到该点在累加器矩阵中的偏移量
            int center_x = centers.get(i).x;
            int center_y = centers.get(i).y;
            //计算圆心在输入图像中的坐标位置
            float cx = (center_x+0.5f)*dp, cy = (center_y+0.5f)*dp;
            float start_dist,dist_sum;
            float r_best = 0;
            int max_count = R_THRESH;
            //判断当前的圆心与之前确定作为输出的圆心是否为同一个圆心
            for(j=0;j<circles.size();j++){
                Point p = circles.get(j);
                //计算当前圆心与提取出的圆心之间的距离，
                // 如果两者距离小于所设的阈值，则认为两个圆心是同一个圆心，退出循环
                if((p.x-cx)*(p.x-cx)+(p.y-cy)*(p.y-cy)<min_dist) break;
            }
            //如果j < circles->total，说明当前的圆心已被认为与之前确定作为输出的圆心是同一个圆心，
            // 则抛弃该圆心，返回上面的for循环
            if(j<circles.size()) continue;
            //第二阶段
            ddata = new int[nz_count];
            for(j=0;j<nz_count;j++){
                Point pt = nz.get(j);
                float _dx,_dy,_r2;
                _dx = cx - pt.x;
                _dy = cy - pt.y;
                //计算圆周上的点和当前圆心的距离，即半径
                _r2 = _dx*_dx + _dy*_dy;
                //如果半径在所设置的最大半径和最小半径之间
                if(min_radius2<=_r2 && _r2<=max_radius2){
                    //把半径放在dist_buf内
                    dist_buf.add((int)Math.sqrt(_r2));
                }
            }
            //对圆半径进行排序
            k=0;
            while(!dist_buf.isEmpty()){
                ddata[k] = dist_buf.poll();
                k++;
            }
            //k表示一共有多少圆周上的点
            int nz_count1 = k,start_idx = nz_count1 - 1;
            //nz_count1等于0也就是k等于0，说明当前的圆心没有所对应的圆，
            // 意味着当前圆心不是真正的圆心，所以抛弃该圆心，返回上面的for循环
            if(nz_count1 == 0) continue;
            dist_sum = start_dist = ddata[start_idx];
            for(j=start_idx-2;j>=0;j--){
                float d = ddata[j];
                if(d > max_radius) break;
                //d表示当前半径值，start_dist表示上一次通过下面if语句更新后的半径值，dr表示半径距离分辨率，如果这两个半径距离之差大于距离分辨率，
                // 说明这两个半径一定不属于同一个圆，而两次满足if语句条件之间的那些半径值可以认为是相等的，即是属于同一个圆
                if(d-start_dist>dr){
                    float r_cur = ddata[(int)(j+start_idx)/2];
                    ////////////////////////////
                    if((start_idx-j)*r_best>=max_count*r_cur||(r_best<1.192092896e-07f && start_idx-j>=max_count)){
                        r_best = r_cur;//把当前半径值作为最佳半径值
                        max_count = start_idx - j;//更新最大值
                    }
                    //更新半径距离和序号
                    start_dist = d;
                    start_idx = j;
                    dist_sum = 0;
                }
                dist_sum += d;//？？？作用
            }
            //最终确定输出
            if(max_count>acc_threshold){
                Log.d("HoughCirle:","圆心："+cx +"," + cy);
                Log.d("HoughCirle:","输出r_best:"+r_best);
                Point po_circle = new Point();
                po_circle.x = (int)cx;
                po_circle.y = (int)cy;
                po_circle.radius = r_best;
                Log.d("HoughCirle:","accumulation:"+accum[po_circle.x][po_circle.y]);
                circles.add(po_circle);
                if(circles.size()>=circle_max) return;
            }
        }
        Log.d("HoughCirle:","圆心个数:"+circles.size());

    }
/*
    public static void templet(int[][] ori_img,int[][] tar_img,int iwidth,int iheight,double[] templet,int tmpsize){
        int i,j,pos;
        int row,col;
        int half_tmpSize = tmpsize/2;
        double sum;
        for(i = 0;i<iheight;i++){
            for(j = 0;j<iwidth;j++){
                pos = 0;//记录模板位置
                sum = 0.0;//记录该点与模板加权微分结果
                for(row=-half_tmpSize;row<=half_tmpSize;row++){
                    for(col=-half_tmpSize;col<=half_tmpSize;col++){
                        if((i+row>=0)&&(i+row<iheight)&&(j+col>=0)&&(j+col<iwidth)){
                            sum += ori_img[i+row][j+col]*templet[pos];
                        }
                        pos++;
                    }//end for(col)
                }//end for(row)
                tar_img[i][j] = (int)Math.abs(sum+0.5);
            }//end for(j)
        }//end for(i)
    }

    public static void GradSobel(int[][] ori_img,int[][] gradimageX,int[][] gradimageY,int[][] gradimageXY,int iwidth,int iheight){
        int i,j;
        double t;
        double[] tmpX = {-1.0,0.0,1.0,-1.0,0.0,1.0,-1.0,0.0,1.0};
        double[] tmpY = {1.0,1.0,1.0,0.0,0.0,0.0,-1.0,-1.0,-1.0};
        //double[] tmpX = {-1.0,0.0,1.0,-2.0,0.0,2.0,-1.0,0.0,1.0};
        //double[] tmpY = {1.0,2.0,1.0,0.0,0.0,0.0,-1.0,-2.0,-1.0};
        int tmpSize = 3;
        //计算水平x方向加权微分，结果存在proimageX中
        templet(ori_img,gradimageX,iwidth,iheight,tmpX,tmpSize);
        //计算水平y方向加权微分，结果存在proimageY中
        templet(ori_img,gradimageY,iwidth,iheight,tmpY,tmpSize);
        //求各点梯度值
        for(i=0;i<iheight;i++){
            for(j=0;j<iwidth;j++){
                t = Math.sqrt(1.0*gradimageX[i][j]*gradimageX[i][j]+gradimageY[i][j]*gradimageY[i][j])+0.5;
                //if(t>255.0) t = 255;
                gradimageXY[i][j] = (int)t;
            }
        }
    }

 */

    public static void ImgGrad(int[][] ori_img,int[][] gradimageX,int[][] gradimageY,int iwidth,int iheight){
        int[][] gradX = new int[iwidth][iheight];
        int[][] gradY = new int[iwidth][iheight];
        for(int i = 1;i<iwidth;i++){
            for(int j = 1;j<iheight;j++) {
                gradimageX[i-1][j-1] = Math.abs(ori_img[i][j]-ori_img[i-1][j]);
                gradX[i][j] = Math.abs(ori_img[i-1][j]-ori_img[i][j]);
                gradimageY[i-1][j-1] = Math.abs(ori_img[i][j]-ori_img[i][j-1]);
                gradY[i][j] = Math.abs(ori_img[i][j-1]-ori_img[i][j]);
            }
        }
        for(int k=0;k<iheight;k++) gradimageX[iwidth-1][k] = 0;
        for(int t=0;t<iwidth;t++) gradimageY[t][iheight-1] = 0;
        for(int k1=0;k1<iheight;k1++) gradX[0][k1] = 0;
        for(int t1=0;t1<iwidth;t1++) gradY[t1][0] = 0;
        for(int i1 = 0;i1<iwidth;i1++){
            for(int j1 = 0;j1<iheight;j1++) {
                if(gradimageX[i1][j1] != gradX[i1][j1]){
                    gradimageX[i1][j1] = 0;
                }
            }
        }
    }
}
