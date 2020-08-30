package com.example.si.IMG_PROCESSING.CircleDetect;

import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.example.si.BuildConfig;
import com.example.si.IMG_PROCESSING.GSDT.GSDT_Para;
import com.example.si.IMG_PROCESSING.ImgObj_Para;
import com.example.si.ImageTools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

import static com.example.si.IMG_PROCESSING.CircleDetect.HoughCircle.ImgGrad;
import static com.example.si.IMG_PROCESSING.GSDT.GSDT_2D.GSDT_Fun;

public class CircleDetect_new {
    private static final float PI = 3.1415f;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static Bitmap CircleDetect_Fun(Bitmap bitmap,Bitmap blur_bitmap) throws Exception {
        ImgObj_Para imobj = new ImgObj_Para(blur_bitmap);//创建图像处理对象
        imobj.colorToGray2D(blur_bitmap);
/*        ///双边滤波///
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            blur_bitmap = ImageFilter.bilateralFilter(blur_bitmap,9,100,100);
        }

 */


        ///////////GSDT部分///////////
        GSDT_Para p = new GSDT_Para();
        int[][] binay_img = ImageTools.myOTSU(imobj.gray_img,imobj.height,imobj.width,256);
        //p.phi = GSDT_Fun(p, imobj.gray_img);
        p.phi = GSDT_Fun(p, binay_img);//////////////////////////////
 //       binay_img = imobj.binaryReverse(binay_img);//////////////////
 //       p.phi_1 = GSDT_Fun(p, binay_img);////////////////////////////
        double dRationHigh=0.88,dRationLow=0.38;///canny边缘检测高低阈值比例
        EdgeDetect_fun.Canny_edge(imobj,blur_bitmap,dRationHigh,dRationLow);//边缘检测
        //System.out.println("Enter here(the Edgedetect has been finished)");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //HoughCircleGradient(imobj,1,217,58,200,28,imobj.circles,imobj.circle_max);
            int max_radius = Math.min(imobj.width/2,imobj.height/2);
            //Log.d("HoughCirle:","最大半径值:"+max_radius);
            CircleDetect_Main(imobj,p,1,60,30,40,5,imobj.circles,imobj.circle_max);
           // CircleDetect_Main(imobj,p,1,max_radius/2,20,max_radius,33,imobj.circles,imobj.circle_max);
            //Log.d("HoughCirle:","输出圆心个数:"+imobj.circles.size());
            //System.out.println("Enter here(the main fun of Houghcircle has been finished)");
        }
        Log.v("CircleDetect_Fun", "The number of the circles:"+imobj.circles.size());
        return Point.DrawCircle(imobj.circles,bitmap);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void CircleDetect_Main(ImgObj_Para imobj, GSDT_Para p, float dp, float min_dist, int min_radius, int max_radius, int acc_threshold, ArrayList<Point> circles, int circle_max){
        int img_width = imobj.width;//输入图像宽(bitmap)
        int img_height = imobj.height;//输入图像高
        //为了提高运算精度，定义一个数值的位移量
        final int SHIFT = 10, ONE = 1 << SHIFT, R_THRESH = 30;//R_THRESH是起始值，赋给max_count,后续会被覆盖
        int[][] edges = imobj.tar_img[1];//图像边界信息
        // Log.v("HoughCircle","edges:"+edges[40][234]);
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
        int[][] dx = new int[img_height][img_width];					//存放各点水平方向梯度值(edge)
        int[][] dy = new int[img_height][img_width];					//存放各点垂直方向梯度值(edge)
        //int[][] edgeGradXY = new int[img_height][img_width];					//存放各点XY方向的综合梯度值(edge)
        ImgGrad(edges,dx,dy,img_height,img_width);
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

        nz_count = nz.size();
        Log.v("HoughCircle","nz_count:"+nz_count);
        if(nz_count==0) return;

        ///////////////////////////GSDT//////////////////////////
        float[] GSDTMinMax = GetMaxMinValue(p.phi);
//        float[] GSDTMinMax_1 = GetMaxMinValue(p.phi_1);
        int[] AccumMinMax = GetMaxMinValue(accum);
        //////////计算加权累积值/////////////
        for(x=0;x<img_height;x++) {
            for (y = 0; y < img_width; y++) {
                p.phi[x][y] = (int) (255*(p.phi[x][y]-GSDTMinMax[0])/(GSDTMinMax[1]-GSDTMinMax[0]));
//                p.phi_1[x][y] = (int) (255*(p.phi_1[x][y]-GSDTMinMax_1[0])/(GSDTMinMax_1[1]-GSDTMinMax_1[0]));
               // p.phi[x][y] = normalization(p.phi[x][y],GSDTMinMax[0],GSDTMinMax[1],0,255);
                accum[x][y] = 255 * (accum[x][y] - AccumMinMax[0]) / (AccumMinMax[1] - AccumMinMax[0]);
               // accum[x][y] = normalization(accum[x][y],AccumMinMax[0],AccumMinMax[1],0,255);
                //Log.v("Circle==========","p.phi["+x+"]["+y+"]:"+p.phi[x][y]);
                //Log.v("Circle=>>>>>>>>>","accum["+x+"]["+y+"]:"+accum[x][y]);
                p.phi[x][y] = (float) (0.15*p.phi[x][y]+0.85*accum[x][y]);//////////////////
                //p.phi[x][y] = (float) (0*p.phi_1[x][y]+0.5*p.phi[x][y]+0.5*accum[x][y]);//////////////////
                //Log.v("Circle=>>>>>>>>>","phi["+x+"]["+y+"]:"+p.phi[x][y]);
            }
        }

        int threshold = 100;
        //遍历累加器矩阵找到可能的圆心
        for(x=1;x<arows-1;x++){
            for(y=1;y<acols-1;y++){
                //if(edges[x][y]==255) continue;///////test
                //int base = y*(arows+2) + x;
                int base = (int)p.phi[x][y];
                //如果当前的值大于阈值，并且在8邻域内是最大值，则认为是圆心
                if((base>threshold) && (base>p.phi[x-1][y]) && (base>p.phi[x+1][y]) && (base>p.phi[x][y+1]) && (base>p.phi[x][y-1])&&(base>p.phi[x+1][y+1])
                &&(base>p.phi[x-1][y-1])&&(base>p.phi[x-1][y+1])&&(base>p.phi[x+1][y-1])){
                    //压入圆心序列
                    Point po_temp = new Point();
                    po_temp.x = x;
                    po_temp.y = y;
                    po_temp.accu_value = (int)p.phi[x][y];
                    sort_buf.add(po_temp);
                    //Log.v("TEST========" ,po_temp.accu_value +",("+po_temp.y+","+po_temp.x+")");
//                    if(imobj.max_accum<accum[x][y]){
//                        imobj.max_accum = accum[x][y];
//                        imobj.max_accum_x = x;
//                        imobj.max_accum_y = y;
//                        Log.v("TEST========max", imobj.max_accum +",("+imobj.max_accum_y+","+imobj.max_accum_x+")");
//                    }
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
        ////////////////////////////
//        imobj.circles = centers;
        ///////////////////////////
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
                Point po = circles.get(j);
                //计算当前圆心与提取出的圆心之间的距离，
                // 如果两者距离小于所设的阈值，则认为两个圆心是同一个圆心，退出循环
                if((po.x-cx)*(po.x-cx)+(po.y-cy)*(po.y-cy)<min_dist) break;
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
            float nz_ratio = 0.0f;//目前圆周上点占完整圆周点的比例值
            if(nz_count1 == 0) continue;
            dist_sum = start_dist = ddata[start_idx];
            for(j=start_idx-2;j>=0;j--){
                float d = ddata[j];
                if(d > max_radius) break;
                //d表示当前半径值，start_dist表示上一次通过下面if语句更新后的半径值，dr表示半径距离分辨率，如果这两个半径距离之差大于距离分辨率，
                // 说明这两个半径一定不属于同一个圆，而两次满足if语句条件之间的那些半径值可以认为是相等的，即是属于同一个圆
                if(d-start_dist>dr){
                    float r_cur = ddata[(j+start_idx) /2];
                    float temp_ratio = (start_idx - j)/(r_cur*2*PI);
                    ////////////////////////////
                    if(temp_ratio>=nz_ratio && ((start_idx-j)*r_best>=max_count*r_cur||(r_best<1.192092896e-07f && start_idx-j>=max_count))){
                        r_best = r_cur;//把当前半径值作为最佳半径值
                        max_count = start_idx - j;//更新最大值
                        nz_ratio = temp_ratio;
                    }
                    //更新半径距离和序号
                    start_dist = d;
                    start_idx = j;
                    dist_sum = 0;
                }
                dist_sum += d;//？？？作用
            }
            //最终确定输出
            if(max_count>acc_threshold && r_best!=0){
                Log.d("HoughCirle:","圆心："+cx +"," + cy);
                Log.d("HoughCirle:","输出r_best:"+r_best);
                Point po_circle = new Point();
                po_circle.x = (int)cx;
                po_circle.y = (int)cy;
                po_circle.radius = r_best;
                Log.d("HoughCirle:","accumulation:"+p.phi[po_circle.x][po_circle.y]);
                circles.add(po_circle);
                if(circles.size()>=circle_max) return;
            }
        }

        Log.d("HoughCirle:","圆心个数:"+circles.size());
        //return accum;
        //return p.phi;
    }

   // NORMALIZATION 将数据x归一化到任意区间[ymin,ymax]范围的方法
    public static int normalization( float x, float xMin, float xMax, int yMin, int yMax){
        int y = (int) ((yMax-yMin)*(x-xMin)/(xMax-xMin) + yMin);
        return y;
    }
    //获取数组的最大值和最小值,out[0] = Min, out[1] = Max
    public static float[] GetMaxMinValue(float[][] Array){
        float[] out= {0.0f,0.0f};
        int Width = Array.length;
        int Height = Array[0].length;
        for(int i=0;i<Width;i++){
            for(int j=0;j<Height;j++){
                if(Array[i][j]<out[0]) out[0] = Array[i][j];
                if(Array[i][j]>out[1]) out[1] = Array[i][j];
            }
        }
        return out;
    }
    //获取数组的最大值和最小值,out[0] = Min, out[1] = Max
    public static int[] GetMaxMinValue(int[][] Array){
        int[] out= {0,0};
        int Width = Array.length;
        int Height = Array[0].length;
        for(int i=0;i<Width;i++){
            for(int j=0;j<Height;j++){
                if(Array[i][j]<out[0]) out[0] = Array[i][j];
                if(Array[i][j]>out[1]) out[1] = Array[i][j];
            }
        }
        return out;
    }

}
