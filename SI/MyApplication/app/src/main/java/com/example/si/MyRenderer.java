package com.example.si;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.opengl.GLES10;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.example.si.IMG_PROCESSING.CornerDetection.ByteTranslate;

import com.example.si.IMG_PROCESSING.CornerDetection.HarrisCornerDetector;
import com.example.si.IMG_PROCESSING.CornerDetection.HarrisMatrix;
import com.example.si.IMG_PROCESSING.CornerDetection.ImageMarker;
import com.example.si.IMG_PROCESSING.CornerDetection.XYZ;
import com.example.si.RENDER.Axises3D;
import com.example.si.RENDER.ImageUtil;
import com.example.si.RENDER.MyDraw;
import com.example.si.RENDER.MyPattern2D;

import org.apache.commons.io.IOUtils;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;


import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import java.io.IOException;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.example.si.RENDER.BitmapRotation.getBitmapDegree;
import static com.example.si.RENDER.BitmapRotation.getOrientation;
import static com.example.si.RENDER.BitmapRotation.rotateBitmapByDegree;
import static com.example.si.MainActivity.getContext;
import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.lang.Math.sqrt;
import static java.lang.System.in;
import static java.lang.System.out;
import static javax.microedition.khronos.opengles.GL10.GL_ALPHA_TEST;
import static javax.microedition.khronos.opengles.GL10.GL_BLEND;
import static javax.microedition.khronos.opengles.GL10.GL_ONE_MINUS_SRC_ALPHA;
import static javax.microedition.khronos.opengles.GL10.GL_SRC_ALPHA;

//import android.graphics.Matrix;
//import org.apache.commons.io.IOUtils;
//import org.opencv.android.Utils;
//import org.opencv.core.CvType;
//import org.opencv.core.Mat;
//import org.opencv.core.MatOfPoint;
//import org.opencv.core.Point;
//import org.opencv.imgproc.Imgproc;


//@android.support.annotation.RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
public class MyRenderer implements GLSurfaceView.Renderer {
    private int UNDO_LIMIT = 20;
    private int curUndo = 0;

    private enum Operate {DRAWCURVE, DELETECURVE, DRAWMARKER, DELETEMARKER, CHANGELINETYPE, SPLIT}

    public static final String OUTOFMEM_MESSAGE = "OutOfMemory";
    public static final String FILE_SUPPORT_ERROR = "FileSupportError";
    public static final String FILE_PATH = "Myrender_FILEPATH";
    public static final String LOCAL_FILE_PATH = "LOCAL_FILEPATH";
    public static final String Time_out = "Myrender_Timeout";

    private MyPattern2D myPattern2D;

    private ByteBuffer imageBuffer;
    private byte[] image2D;
    private Bitmap bitmap2D;
    private Bitmap bitmap2D_backup;
    private MyDraw myDraw;
    private int mProgram;
    private boolean bitmap_reset = false;
    private boolean isAddPoint = false;

    private float angle = 0f;
    private float angleX = 0.0f;
    private float angleY = 0.0f;
    private float angleZ = 0.0f;
    private int mTextureId;
    int[] R = new int[2];  //图像长宽/100

    private int vol_w;
    private int vol_h;
    private int vol_d;
    private int markernum;
    private int[] sz = new int[3];
    private float[] mz = new float[3];
    private float[] mz_neuron = new float[3];
    private float[] mz_block = new float[6];

    //private boolean multiImgFlag = false;
    private boolean ifRefine = false;

    private int[] texture = new int[1]; //生成纹理id

    // vPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] scratch = new float[16];
    private final float[] vPMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] rotationMatrix = new float[16];
    private final float[] rotationXMatrix = new float[16];
    private final float[] rotationYMatrix = new float[16];
    private final float[] rotationZMatrix = new float[16];
    private final float[] translateMatrix = new float[16];//平移矩阵
    private final float[] translateAfterMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] RTMatrix = new float[16];
    private final float[] ZRTMatrix = new float[16];
    private final float[] mMVP2DMatrix = new float[16];
    private float[] ArotationMatrix = new float[16];


    private final float[] zoomMatrix = new float[16];//缩放矩阵
    private final float[] zoomAfterMatrix = new float[16];
    private final float[] finalMatrix = new float[16];//缩放矩阵
    private float[] linePoints = {

    };

    private ArrayList<ArrayList> curDrawed = new ArrayList<>();
    private ArrayList<ImageMarker> MarkerList = new ArrayList<ImageMarker>();

    private ArrayList<Float> splitPoints = new ArrayList<Float>();
    private int splitType;

    private ArrayList<ArrayList<Float>> lineDrawed = new ArrayList<ArrayList<Float>>();

    private ArrayList<Float> markerDrawed = new ArrayList<Float>();

    private ArrayList<Float> eswcDrawed = new ArrayList<Float>();

    private ArrayList<Float> apoDrawed = new ArrayList<Float>();

    private ArrayList<Float> swcDrawed = new ArrayList<Float>();

    private boolean isAddLine = false;
    private boolean isAddLine2 = false;

    private int lastLineType = 2;
    private int lastMarkerType = 3;


    private String filepath = ""; //文件路径
    private InputStream is;
    private long length;

    private boolean ifPainting = false;

    private boolean ifDownSampling = false;
    private boolean ifNeedDownSample = true;
    private boolean ifNavigationLococation = false;

    private int screen_w;
    private int screen_h;
    private float cur_scale = 1.0f;
    private float[] minXYZ = new float[3];

    private byte[] grayscale;
    private int data_length;
    private boolean isBig;

    private FileType fileType;
    private ByteBuffer mCaptureBuffer;
    private Bitmap mBitmap;
    private Bitmap text_Bitmap;
    private boolean isTakePic = false;
    private String mCapturePath;
    private Axises3D mAxises3D;


    private boolean ifFileSupport = false;
    private boolean ifFileLoaded = false;
    private boolean ifLoadSWC = false;
    private boolean ifDraw3D = false;

    private boolean ifShowSWC = true;

    private Context context_myrenderer;

    private int degree = 0;

    public ArrayList<ImageMarker> getMarkerList() { return MarkerList; }
    //public void switchImg() { multiImgFlag = !multiImgFlag; }

    public Bitmap GetBitmap() { return bitmap2D; }
    public Bitmap GetBackupBitmap() { return bitmap2D_backup; }
    public int getMarkerNum() { return markernum; }
    public boolean getIfDraw3D() { return ifDraw3D; }

    public void setMarkerNum(int num) {
        markernum = num;
    }
    public void setIfRefine(boolean ifrefine) { ifRefine = ifrefine; }
    public void setIsAddPoint(boolean isaddpoint) { isAddPoint = isaddpoint; }

    //设置marker颜色
    //支持种类为1-8
    public void setLastMarkerType(int type) {
        lastMarkerType = type;
    }

    public void ResetImage(Bitmap new_image) {

        bitmap2D = new_image;

        sz[0] = bitmap2D.getWidth();
        sz[1] = bitmap2D.getHeight();
        sz[2] = Math.max(sz[0], sz[1]);
        Integer[] num = {sz[0], sz[1]};
        float max_dim = (float) Collections.max(Arrays.asList(num));
        Log.v("MyRenderer", Float.toString(max_dim));

        mz[0] = (float) sz[0] / max_dim;
        mz[1] = (float) sz[1] / max_dim;
        mz[2] = Math.max(mz[0], mz[1]);

        myPattern2D = null;
        ifFileSupport = true;
    }

    public void ResetIfDraw3D(boolean b, int[] maxXYZ) {
        ifDraw3D = b;

        sz = maxXYZ;
        int intMax = Math.max(Math.max(sz[0], sz[1]),sz[2]);
//        Integer[] num = {sz[0], sz[1], sz[2]};
//        float max_dim = (float) Collections.max(Arrays.asList(num));
//        Log.v("MyRenderer", Float.toString(max_dim));

        mz[0] = (float) sz[0] / intMax;
        mz[1] = (float) sz[1] / intMax;
        mz[2] = (float) sz[2] / intMax;
//        float floatMax = Math.max(Math.max(mz[0], mz[1]),mz[2]);
    }

    public void ResetImage(Bitmap show_image, Bitmap backup_image) {

        bitmap2D = show_image;
        bitmap2D_backup = backup_image;

        sz[0] = bitmap2D.getWidth();
        sz[1] = bitmap2D.getHeight();
        sz[2] = Math.max(sz[0], sz[1]);
        Integer[] num = {sz[0], sz[1]};
        float max_dim = (float) Collections.max(Arrays.asList(num));
        Log.v("MyRenderer", Float.toString(max_dim));

        mz[0] = (float) sz[0] / max_dim;
        mz[1] = (float) sz[1] / max_dim;
        mz[2] = Math.max(mz[0], mz[1]);

        if (myPattern2D != null)
            myPattern2D.setNeedRelease();

        ifFileSupport = true;
    }

    public void ResetMarkerlist(ArrayList<ImageMarker> markerlist) {
        //MarkerList.clear();
        MarkerList = markerlist;
    }

    //初次渲染画面
    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // Set the background frame color
        //深蓝
        GLES30.glClearColor(0.098f, 0.098f, 0.439f, 1.0f);

        Log.v("onSurfaceCreated:", "successfully");

        MyPattern2D.initProgram();
        mAxises3D = new Axises3D();

        Matrix.setIdentityM(translateMatrix, 0);//建立单位矩阵
        Matrix.setIdentityM(zoomMatrix, 0);//建立单位矩阵
        Matrix.setIdentityM(zoomAfterMatrix, 0);
        Matrix.setIdentityM(rotationMatrix, 0);
        Matrix.setRotateM(rotationMatrix, 0, 0, -1.0f, -1.0f, 0.0f);
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, -2, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

    }


    //画面大小发生改变后
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //设置视图窗口
        GLES30.glViewport(0, 0, width, height);


        screen_w = width;
        screen_h = height;

        System.out.println("----------------");
        System.out.println(screen_w);
        System.out.println(screen_h);

//        if (fileType == FileType.PNG || fileType == FileType.JPG)
//        myPattern2D = new MyPattern2D(bitmap2D, sz[0], sz[1], mz);

        mBitmap = Bitmap.createBitmap(screen_w,screen_h, Bitmap.Config.ARGB_8888);

        float ratio = (float) width / height;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method

        if (width > height) {
            Matrix.orthoM(projectionMatrix, 0, -ratio, ratio, -1, 1, 1, 100);
        } else {
            Matrix.orthoM(projectionMatrix, 0, -1, 1, -1 / ratio, 1 / ratio, 1, 100);
        }


//        if (fileType == FileType.PNG || fileType == FileType.JPG) {
//            if (width > height) {
//                Matrix.orthoM(projectionMatrix, 0, -ratio, ratio, -1, 1, 1, 100);
//            } else {
//                Matrix.orthoM(projectionMatrix, 0, -1, 1, -1 / ratio, 1 / ratio, 1, 100);
//            }
//        } else {
//
//            if (width > height) {
//                Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 2f, 100);
//            } else {
//                Matrix.frustumM(projectionMatrix, 0, -1, 1, -1 / ratio, 1 / ratio, 2f, 100);
//            }
//        }

    }


    //绘制画面
    @Override
    public void onDrawFrame(GL10 gl) {

//        GLES30.glClearColor(0.5f, 0.4f, 0.3f, 1.0f);
//        GLES30.glClearColor(1.0f, 0.5f, 0.0f, 1.0f);
        //淡黄
//        GLES30.glClearColor(1.0f, 0.89f, 0.51f, 1.0f);
        //深蓝
//        GLES30.glClearColor(0.098f, 0.098f, 0.439f, 1.0f);
        //西红柿
        GLES30.glClearColor(1f, 1f, 1f, 1.0f);
        //紫色
//        GLES30.glClearColor(0.192f, 0.105f, 0.572f, 1.0f);
        //浅蓝
//        GLES30.glClearColor(0.623f, 0.658f, 0.854f, 1.0f);
        //中蓝
//        GLES30.glClearColor(121f / 255f, 134f / 255f, 203f / 255f, 1.0f);
        //浅紫
//        GLES30.glClearColor(0.929f, 0.906f, 0.965f, 1.0f);

        if ((myPattern2D == null || myPattern2D.getNeedRelease()) && bitmap2D != null) {

            if (myPattern2D != null){
                myPattern2D.free();
                myPattern2D = null;
            }
            myPattern2D = new MyPattern2D(bitmap2D, sz[0], sz[1], mz);

//            ImageUtil imageUtil = new ImageUtil();
//            Bitmap text_bitmap = Bitmap.createBitmap(bitmap2D.getWidth(),bitmap2D.getHeight(),Bitmap.Config.ARGB_8888);
//            int color;
//            color = Color.argb(0, 0, 255, 255);
//            //GLES30.glClearColor(121f / 255f, 134f / 255f, 203f / 255f, 1.0f);//浅紫
//
//            text_bitmap.eraseColor(color);
//            Bitmap text_bitmap_new = imageUtil.drawTextToRightBottom(getContext(),bitmap2D, "1111",60, Color.BLUE,50,50);
//            myPattern2D.set_Bitmap(text_bitmap_new);
//
//            if (text_bitmap_new != null){
//                Log.i("MyRenderer","text_bitmap_new != null");
//            }

            if (myDraw == null)
                myDraw = new MyDraw();
        }


        if (bitmap_reset){
            Log.v("Myrenderer", "bitmap_reset");
            myPattern2D.set_Bitmap(bitmap2D);
            bitmap_reset = false;
        }


        //把颜色缓冲区设置为我们预设的颜色
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        GLES10.glEnable(GL_ALPHA_TEST);
        GLES30.glEnable(GL_BLEND);
        GLES30.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);


        setMatrix();


//        if (fileType == FileType.JPG || fileType == FileType.PNG)
        if (myPattern2D != null && !ifDraw3D){
            myPattern2D.draw(finalMatrix);
//            myPattern2D.draw(finalMatrix, 1);

        }
//        if (textmyPattern2D != null){
//            textmyPattern2D.draw(finalMatrix, 0);
////            myPattern2D.draw(finalMatrix, 1);
//
//        }


        GLES30.glDisable(GL_BLEND);
        GLES30.glDisable(GL_ALPHA_TEST);
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);

//        System.out.println("MarkerList.size() = "+MarkerList.size());
        //画三维坐标轴
        if (ifDraw3D) {
            mAxises3D.drawAxises3D(finalMatrix);
        }

        //现画的marker
        if (MarkerList.size() > 0) {
            float radius = 0.015f;
            if (fileType == FileType.JPG || fileType == FileType.PNG)
                radius = 0.01f;
            for (int i = 0; i < MarkerList.size(); i++) {
//                System.out.println("start draw marker---------------------");
                ImageMarker imageMarker = MarkerList.get(i);
                float[] markerModel = VolumetoModel(new float[]{(float) imageMarker.x, (float) imageMarker.y, (float) imageMarker.z});
                if (imageMarker.radius == 5) {
//
                    if (ifDraw3D) {
                        myDraw.drawMarker(finalMatrix, modelMatrix, markerModel[0], markerModel[1], markerModel[2], imageMarker.type, 0.01f);
                    } else {
                        myDraw.drawRing2D(finalMatrix, imageMarker.type, markerModel[0], markerModel[1], 0.5f, 0.01f);
                    }
                } else {
//
                    if (ifDraw3D) {
                        myDraw.drawMarker(finalMatrix, modelMatrix, markerModel[0], markerModel[1], markerModel[2], imageMarker.type, radius);
                    } else {
                        myDraw.drawRing2D(finalMatrix, imageMarker.type, markerModel[0], markerModel[1], 0.5f,  radius);
                    }
                }
//                Log.v("onDrawFrame: ", "(" + markerDrawed.get(i) + ", " + markerDrawed.get(i+1) + ", " + markerDrawed.get(i+2) + ")");

            }
        }





    }

    private void updateShowBitmap() {

        bitmap_reset = true;
        Log.v("MyRenderer","updateShowBitmap");
        ImageMarker imageMarker;
        ImageUtil imageUtil = new ImageUtil();
        int fontSize = (int) (Math.ceil(Math.max(bitmap2D.getWidth(), bitmap2D.getHeight()) / 1000f)) * 15;

//        Log.v("MyRenderer","MarkerList.size() = " + MarkerList.size());
        if (isAddPoint) {
            //增加marker直接添加即可
            Log.v("MyRenderer","Update bitmap for adding marker.");
            imageMarker = MarkerList.get(MarkerList.size()-1);
            bitmap2D = imageUtil.drawTextToLeftTop(getContext(),bitmap2D, Integer.toString(MarkerList.size()),fontSize, Color.BLUE,(int)imageMarker.x,(int)(imageMarker.y));
            isAddPoint = false;
        } else {
            //删除marker需要重新绘制
            Log.v("MyRenderer","Update bitmap for deleting marker.");
            bitmap2D = bitmap2D_backup.copy(Bitmap.Config.ARGB_8888,true);
            for (int index = 0; index < MarkerList.size(); index++) {
                imageMarker = MarkerList.get(index);
                //index--;
                bitmap2D = imageUtil.drawTextToLeftTop(getContext(),bitmap2D, Integer.toString(index+1),fontSize, Color.BLUE,(int)imageMarker.x,(int)(imageMarker.y));
//            bitmap2D = imageUtil.drawTextToLeftTop(getContext(),bitmap2D, Integer.toString(index+1),20, Color.BLUE,100,100);
            }
        }


//        Bitmap bitmap = Bitmap.createBitmap(bitmap2D.getWidth(),bitmap2D.getHeight(),Bitmap.Config.ARGB_8888);
//        int color = Color.argb(100,255,100,255);
//
//        bitmap.eraseColor(color);

    }

    //图像小块分割进行角点检测，保留一个最大角点
    public ArrayList<ImageMarker> globalImageCornerDetect() {

        int[] resultint = new int[3];
        float[] result = new float[3];
        int[] R = new int[2];
        ArrayList<ImageMarker> corner = new ArrayList<ImageMarker>();



//        R[0] = 1;
//        R[1] = 1;
//        // 7 这个参数需要斟酌
//        if ((bitmap2D.getWidth() / stepx > 7) || (bitmap2D.getHeight() / stepy > 7)) {
//            //保证后面获取图像块不会过小
//            while (bitmap2D.getWidth() % stepx >= R[0]/2) {
//
//            }
//            while (bitmap2D.getHeight() % stepy >= R[1]/2) {
//
//            }
//        } else {
//            Toast.makeText(getContext(), "The resolution of this image is insufficient.", Toast.LENGTH_SHORT).show();
//            return null;
//        }

//        System.out.println("R[0] = "+R[0]);
//        System.out.println("R[1] = "+R[1]);

//        int count = 0;
        //另存工作区原数据
        Bitmap tempBitmap = bitmap2D.copy(Bitmap.Config.ARGB_8888,true);
        Bitmap tempBitmapBackup = bitmap2D_backup.copy(Bitmap.Config.ARGB_8888,false);
        int k = (int)Math.ceil(min(bitmap2D.getHeight(),bitmap2D.getWidth()) / 2000f);
        System.out.println("k = "+k);
        if (k <= 1) {
            bitmap2D_backup = bitmap2D.copy(Bitmap.Config.ARGB_8888,true);
        } else {
            bitmap2D_backup = ImageTools.zoomBitmap(bitmap2D, bitmap2D.getWidth() / k, bitmap2D.getHeight() / k);
        }
        System.out.println("width = "+ bitmap2D_backup.getWidth());
        System.out.println("height = "+bitmap2D_backup.getHeight());

        //获取合适的分割倍数
        float stepx, stepy;
        stepx = 20f;
        stepy = 20f;
        R[0] = round(bitmap2D_backup.getWidth() / stepx);
        R[1] = round(bitmap2D_backup.getHeight() / stepy);
        System.out.println("R[0] = "+R[0]);
        System.out.println("R[1] = "+R[1]);

        ArrayList<ImageMarker> MarkerListTemp = new ArrayList<ImageMarker>(MarkerList);

        //区域中心定位图像块
        for (int center_x = R[0]; center_x < bitmap2D_backup.getWidth(); center_x += 2*R[0]+1) {
            for (int center_y = R[1]; center_y < bitmap2D_backup.getHeight(); center_y += 2*R[1]+1) {

                System.out.println("center_x = "+center_x);
                System.out.println("center_y = "+center_y);

                //工作区准备
                MarkerList.clear();
                bitmap2D = getSmallImageBlock(center_x, center_y, R[0], R[1]);
                if (bitmap2D.getWidth() < 7 || bitmap2D.getHeight() < 7) {
                    continue;
                }
//                count++;

                //harris角点检测
                if (if2dImageLoaded()){
                    //返回最大响应点
                    resultint = corner_detection();
//                        myGLSurfaceView.requestRender();
                } else {
                    Toast.makeText(getContext(), "Please load a 2d image first", Toast.LENGTH_SHORT).show();
                }

//              System.out.println(MarkerList.isEmpty());
//                System.out.println("resultint[0] = "+resultint[0]);
//                System.out.println("resultint[1] = "+resultint[1]);
                //角点矫正
                if (!MarkerList.isEmpty()) {
//                    result = getMostPossiblePoint(result, MarkerList,R); //以最近角点校正
                    //只保留最大点
//                    result[0] = center_x + (float)resultint[0] - R[0]; //附近存在角点则矫正为最大响应
//                    result[1] = center_y + (float)resultint[1] - R[1];
//                    ImageMarker imageMarker = new ImageMarker();
//                    imageMarker.x = result[0] * k;
//                    imageMarker.y = result[1] * k;
//                    imageMarker.type = lastMarkerType;
//                    corner.add(imageMarker); //若存在角点则保留最大角点

//                    System.out.println("----------------------");
//                    System.out.println("result[0] = "+result[0]);
//                    System.out.println("result[1] = "+result[1]);

                    //最多保留最大的三个点
                    for (int i = 0; i < MarkerList.size(); i++) {
                        ImageMarker imageMarker = new ImageMarker();
                        imageMarker.x = (MarkerList.get(i).x + center_x - R[0]) * k;
                        imageMarker.y = (MarkerList.get(i).y + center_y - R[1]) * k;
//                        System.out.println("MarkerList.get(i).x :"+MarkerList.get(i).x);
//                        System.out.println("MarkerList.get(i).y :"+MarkerList.get(i).y);
//                        System.out.println("-----------------------------");
//                        System.out.println("imageMarker.x :"+imageMarker.x);
//                        System.out.println("imageMarker.y :"+imageMarker.y);
                        imageMarker.type = lastMarkerType;
                        corner.add(imageMarker); //若存在角点则保留最大角点
                    }
//                    if (MarkerList.size() > 3) {
//                        for (int i = 0; i < 3; i++) {
//                            ImageMarker imageMarker = new ImageMarker();
//                            imageMarker.x = MarkerList.get(i).x + center_x - R[0];
//                            imageMarker.y = MarkerList.get(i).y + center_y - R[1];
//                            System.out.println("MarkerList.get(i).x :"+MarkerList.get(i).x);
//                            System.out.println("MarkerList.get(i).y :"+MarkerList.get(i).y);
//                            System.out.println("-----------------------------");
//                            System.out.println("imageMarker.x :"+imageMarker.x);
//                            System.out.println("imageMarker.y :"+imageMarker.y);
//                            imageMarker.type = lastMarkerType;
//                            corner.add(imageMarker); //若存在角点则保留最大角点
//                        }
//                    } else {
//                        for (int i = 0; i < MarkerList.size(); i++) {
//                            ImageMarker imageMarker = new ImageMarker();
//                            imageMarker.x = MarkerList.get(i).x + center_x - R[0];
//                            imageMarker.y = MarkerList.get(i).y + center_y - R[1];
//                            System.out.println("MarkerList.get(i).x :"+MarkerList.get(i).x);
//                            System.out.println("MarkerList.get(i).y :"+MarkerList.get(i).y);
//                            System.out.println("-----------------------------");
//                            System.out.println("imageMarker.x :"+imageMarker.x);
//                            System.out.println("imageMarker.y :"+imageMarker.y);
//                            imageMarker.type = lastMarkerType;
//                            corner.add(imageMarker); //若存在角点则保留最大角点
//                        }
////                        corner.addAll(MarkerList);
//                    }



                }// else {
//                附近无角点则返回坐标（-1，-1）
//                    Toast.makeText(getContext(), "There is no corner point at that position!", Toast.LENGTH_SHORT).show();
//                    result[0] = -1;
//                    result[1] = -1;
//                }

            }
        }

        //恢复工作区原数据
        MarkerList = MarkerListTemp;
        bitmap2D = tempBitmap.copy(Bitmap.Config.ARGB_8888,true);
        bitmap2D_backup = tempBitmapBackup.copy(Bitmap.Config.ARGB_8888,false);
        tempBitmap.recycle();
        tempBitmapBackup.recycle();

        //返回特征点
//        System.out.println("count = "+count);
        System.out.println("corner.size() = "+corner.size());
        return corner;

    }

    public ArrayList<ImageMarker> globalImageCornerDetect2() {
        int[] resultint = new int[3];
        //float[] result = new float[3];
        int[] R = new int[2];
        ArrayList<ImageMarker> corner = new ArrayList<ImageMarker>();

        //另存工作区原数据
        Bitmap tempBitmap = bitmap2D.copy(Bitmap.Config.ARGB_8888,true);
        Bitmap tempBitmapBackup = bitmap2D_backup.copy(Bitmap.Config.ARGB_8888,false);
        int k = (int)Math.ceil(max(bitmap2D.getHeight(),bitmap2D.getWidth()) / 1000f);
        System.out.println("k = "+k);

        if (k > 1) {
            bitmap2D = ImageTools.zoomBitmap(bitmap2D, bitmap2D.getWidth() / k, bitmap2D.getHeight() / k);
        }
        out.println("width = "+bitmap2D.getWidth());
        out.println("height = "+bitmap2D.getHeight());

        /*if (k <= 1) {
            bitmap2D_backup = bitmap2D.copy(Bitmap.Config.ARGB_8888,true);
        } else {
            bitmap2D_backup = ImageTools.zoomBitmap(bitmap2D, bitmap2D.getWidth() / k, bitmap2D.getHeight() / k);
        }
        System.out.println("width = "+ bitmap2D_backup.getWidth());
        System.out.println("height = "+bitmap2D_backup.getHeight());*/

        //获取合适的分割倍数
        float stepx, stepy;
        stepx = 35f;
        stepy = 35f;
        R[0] = round(bitmap2D.getWidth() / stepx);
        R[1] = round(bitmap2D.getHeight() / stepy);
        System.out.println("R[0] = "+R[0]);
        System.out.println("R[1] = "+R[1]);
        //bitmap2D = bitmap2D_backup;

        ArrayList<ImageMarker> MarkerListTemp = new ArrayList<ImageMarker>();

        //工作区准备
        MarkerList.clear();

        //harris角点检测
        if (if2dImageLoaded()){
            //返回最大响应点
            resultint = corner_detection2();
        } else {
            Toast.makeText(getContext(), "Please load a 2d image first", Toast.LENGTH_SHORT).show();
        }

        if (resultint == null) {
            return corner;
        }

        //区域中心定位图像块
        int [] tempPointIndexList;
        for (int center_x = R[0]; center_x < bitmap2D.getWidth(); center_x += 2*R[0]+1) {
            for (int center_y = R[1]; center_y < bitmap2D.getHeight(); center_y += 2*R[1]+1) {
//                System.out.println("center_x = "+center_x);
//                System.out.println("center_y = "+center_y);
                //获取区域内的点(无边界问题)
                tempPointIndexList = findPointsInTheRegion(center_x, center_y, R, MarkerList);
//                out.println("tempPointIndexList.length = "+tempPointIndexList.length);
//                for (int l = 0; l < tempPointIndexList.length; l++) {
//                    out.println("(x,y): ("+MarkerList.get(tempPointIndexList[l]).x+","+MarkerList.get(tempPointIndexList[l]).y+")");
//                }
                //对区域内点按照响应值排序，保留最大的三个响应点
                int[] new_corner_x_y = findThreeMaxResponse(hmlist, tempPointIndexList);
                if (new_corner_x_y != null) {
                    for (int i = 0; i < new_corner_x_y.length / 2; i++) {
                        ImageMarker imageMarker = new ImageMarker(new_corner_x_y[2*i+1] * k, new_corner_x_y[2*i] * k, 0, 3);
                        corner.add(imageMarker);
                    }
                }


            }
        }









        //恢复工作区原数据
        MarkerList = MarkerListTemp;
        bitmap2D = tempBitmap.copy(Bitmap.Config.ARGB_8888,true);
        bitmap2D_backup = tempBitmapBackup.copy(Bitmap.Config.ARGB_8888,false);
        tempBitmap.recycle();
        tempBitmapBackup.recycle();
        MarkerListTemp.clear();

        //返回特征点
//        System.out.println("count = "+count);
        System.out.println("corner.size() = "+corner.size());
        return corner;
    }

    /**
     * 获取投影点所在小区域内点的索引
     * @param x 投影点x坐标
     * @param y 投影点y坐标
     * @param R 区域半径（长宽半径分别对应于数组的0，1索引的元素）
     * @param list 待查找点集
     * @return 返回对应于点集的区域内点的索引数组
     */
    private int [] findPointsInTheRegion(int x, int y, int[] R, ArrayList<ImageMarker> list) {
        ArrayList<Integer> pointList = new ArrayList<Integer>();
        int count = 0;
        for (ImageMarker imageMarker:list) {
            if (imageMarker.x >= x - R[0] && imageMarker.x <= x + R[0] && imageMarker.y >= y - R[1] && imageMarker.y <= y + R[1]) {
                int index = count;
                pointList.add(index);
            }
            count++;
        }

        int[] indexList = new int[pointList.size()];
        for (int i = 0; i < pointList.size(); i++) {
            indexList[i] = pointList.get(i);
        }
        return indexList;
    }

    private int[] findThreeMaxResponse(ArrayList<HarrisMatrix> hmlist, int[] pointList) {
        ArrayList<HarrisMatrix> tempHMList = new ArrayList<HarrisMatrix>();
        for (int i = 0; i < pointList.length; i++) {
            HarrisMatrix hm = new HarrisMatrix();
            hm = hmlist.get(pointList[i]);
            tempHMList.add(hm);
        }

        //响应降序排列
//        System.out.println("-------------------------------------------------------");

        Collections.sort(tempHMList, new Comparator<HarrisMatrix>() {

            @Override
            public int compare(HarrisMatrix h1, HarrisMatrix h2) {
                //降序
                if (h1.getR()-h2.getR() > 0) {
                    return -1;
                } else if (h1.getR()-h2.getR() < 0) {
                    return 1;
                } else {
                    return 0;
                }

                //return h1.compareTo(h2);
            }

        });

        if (tempHMList.size() > 3) {
            int[] new_corner_index = new int[6];
            for (int i = 0; i < 3; i++) {
                new_corner_index[2*i + 1] = tempHMList.get(i).getX();
                new_corner_index[2*i] = tempHMList.get(i).getY();
//                System.out.println(i+" "+temp.get(i).getX()+","+temp.get(i).getY());
            }

            return new_corner_index;
        } else if (tempHMList.size() >0) {
            int[] new_corner_x_y = new int[2*tempHMList.size()];
            for (int i = 0; i < tempHMList.size(); i++) {
                new_corner_x_y[2*i + 1] = tempHMList.get(i).getX();
                new_corner_x_y[2*i] = tempHMList.get(i).getY();
//                System.out.println(i+" "+temp.get(i).getX()+","+temp.get(i).getY());
            }
            return new_corner_x_y;
        } else {
            return null;
        }

    }

    private float[] VolumetoModel(float[] input){
        if (input == null)
            return null;

        float[] result = new float[3];
        result[0] = (sz[0] - input[0]) / sz[0] * mz[0];
        result[1] = (sz[1] - input[1]) / sz[1] * mz[1];
        result[2] = input[2] / sz[2] * mz[2];

        return result;
    }

    private static String insertImageToSystem(Context context, String imagePath) {
        String url = "";
        String filename = imagePath.substring(imagePath.lastIndexOf("/") + 1);
        try {
            url = MediaStore.Images.Media.insertImage(context.getContentResolver(), imagePath, filename, "ScreenShot from C3");
        } catch (FileNotFoundException e) {
            System.out.println("SSSSSSSSSSSS");
            e.printStackTrace();
        }
        System.out.println("Filename: " + filename);
        System.out.println("Url: " + url);
        return url;
    }


    private void setMatrix() {

        // Calculate the projection and view transformation
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        Matrix.multiplyMM(mMVP2DMatrix, 0, vPMatrix, 0, zoomMatrix, 0);
        // Set the Rotation matrix
//        Matrix.setRotateM(rotationMatrix, 0, angle, 0.0f, 1.0f, 0.0f);
//        Matrix.setRotateM(rotationXMatrix, 0, angleX, 1.0f, 0.0f, 0.0f);
//        Matrix.setRotateM(rotationYMatrix, 0, angleY, 0.0f, 1.0f, 0.0f);

//        Log.v("roatation",Arrays.toString(rotationMatrix));

        Matrix.setIdentityM(translateMatrix, 0);//建立单位矩阵


        if (!ifNavigationLococation) {
            Matrix.translateM(translateMatrix, 0, -0.5f * mz[0], -0.5f * mz[1], -0.5f * mz[2]);
        } else {
            Matrix.translateM(translateMatrix, 0, -0.5f * mz_neuron[0], -0.5f * mz_neuron[1], -0.5f * mz_neuron[2]);
        }
//        Matrix.multiplyMM(translateMatrix, 0, zoomMatrix, 0, translateMatrix, 0);
        Matrix.setIdentityM(translateAfterMatrix, 0);

        Matrix.translateM(translateAfterMatrix, 0, 0, 0, cur_scale);
//        Matrix.translateM(translateAfterMatrix, 0, 0, 0, -cur_scale);

        // Combine the rotation matrix with the projection and camera view
        // Note that the vPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
//        Matrix.multiplyMM(rotationMatrix, 0, rotationYMatrix, 0, rotationXMatrix, 0);
//        Matrix.multiplyMM(rotationMatrix, 0, zoomMatrix, 0, rotationMatrix, 0);
        Matrix.multiplyMM(modelMatrix, 0, rotationMatrix, 0, translateMatrix, 0);

        Matrix.multiplyMM(RTMatrix, 0, zoomMatrix, 0, modelMatrix, 0);

        Matrix.multiplyMM(ZRTMatrix, 0, translateAfterMatrix, 0, RTMatrix, 0);

        Matrix.multiplyMM(finalMatrix, 0, vPMatrix, 0, ZRTMatrix, 0);      //ZRTMatrix代表modelMatrix

//        Matrix.multiplyMM(finalMatrix, 0, zoomMatrix, 0, scratch, 0);

//        Matrix.setIdentityM(translateAfterMatrix, 0);
//        Matrix.translateM(translateAfterMatrix, 0, 0.0f, 0.0f, -0.1f);
//        Matrix.multiplyMM(translateAfterMatrix, 0, zoomAfterMatrix, 0, translateAfterMatrix, 0);
    }


    //int转byte
    private static byte[] intToByteArray(int i) {
        byte[] result = new byte[4];
        result[0] = (byte) ((i >> 24) & 0xFF);
        result[1] = (byte) ((i >> 16) & 0xFF);
        result[2] = (byte) ((i >> 8) & 0xFF);
        result[3] = (byte) (i & 0xFF);
        return result;
    }



    //设置文件路径
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void SetPath(String message) throws IOException {

        filepath = message;
        cur_scale = 1.0f;

        loadImage2D();
        ifFileLoaded = true;
        ifFileSupport = true;
        myPattern2D = null;

//        if (fileType == FileType.PNG || fileType == FileType.JPG) {
//            loadImage2D();
//            ifFileLoaded = true;
//            ifFileSupport = true;
//        }

        Log.v("SetPath", Arrays.toString(mz));

        Matrix.setIdentityM(translateMatrix, 0);//建立单位矩阵

        Matrix.setIdentityM(zoomMatrix, 0);//建立单位矩阵
        Matrix.setIdentityM(zoomAfterMatrix, 0);
        Matrix.setIdentityM(rotationMatrix, 0);
        Matrix.setRotateM(rotationMatrix, 0, 0, -1.0f, -1.0f, 0.0f);
//        Matrix.setIdentityM(translateAfterMatrix, 0);
        // Set the camera position (View matrix)
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, -2, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        zoom_in();

    }

    //根据目标图片大小来计算Sample图片大小
    public static int CaculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight){
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if(height > reqHeight || width >reqWidth){
            final int halfHeight = height/2;
            final int halfWidth = width/2;
            while ((halfHeight/inSampleSize)>reqHeight&&(halfWidth/inSampleSize)>reqWidth){
                inSampleSize *= 2;
            }
        }
        Log.d("MyRender","inSampleSize"+inSampleSize);
        return inSampleSize;
    }
    //解码压缩后的图片
    public static Bitmap decodeSampleBitmapFromResource(InputStream is,int reqWidth,int reqHeight){
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Rect outPadding = new Rect(-1,-1,-1,-1);
        BitmapFactory.decodeStream(is, null,options);
        options.inSampleSize = CaculateInSampleSize(options,reqWidth,reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeStream(is,null, options);
    }

    //    @RequiresApi(api = Build.VERSION_CODES.N)
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void loadImage2D() throws IOException {
        File file = new File(filepath);
        long length = 0;
        InputStream is = null;
        FileDescriptor fd = null;
        if (file.exists()) {
            try {
                length = file.length();
                is = new FileInputStream(file);
                Log.v("loadImage2D: ", filepath);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            Uri uri = Uri.parse(filepath);
            Uri uri_degree = Uri.parse(filepath);


            try {
                ParcelFileDescriptor parcelFileDescriptor =
                        getContext().getContentResolver().openFileDescriptor(uri, "r");

                ParcelFileDescriptor parcelFileDescriptor_degree =
                        getContext().getContentResolver().openFileDescriptor(uri, "r");

                fd = parcelFileDescriptor_degree.getFileDescriptor();
                is = new ParcelFileDescriptor.AutoCloseInputStream(parcelFileDescriptor);

                length = (int) parcelFileDescriptor.getStatSize();

                Log.v("MyPattern", "Successfully load intensity");

            } catch (Exception e) {
                Log.v("MyPattern", "Some problems in the MyPattern when load intensity");
            }
        }



        //图片过大压缩
//        if(bitmap2D.getWidth()>2000&&bitmap2D.getHeight()>2000){
//            bitmap2D = ImageTools.zoomBitmap(bitmap2D, bitmap2D.getWidth() / 4, bitmap2D.getHeight() / 4);
//        }
        if(file.exists()){
            degree = getBitmapDegree(filepath);
            System.out.println("filepath:"+degree);
        }else{
            Log.i("MyRenderer",fd.toString());
            degree = getBitmapDegree(fd);
            System.out.println("fd:"+degree);
        }
//        ByteArrayOutputStream st = new ByteArrayOutputStream();

        final BitmapFactory.Options options = new BitmapFactory.Options();
        bitmap2D = BitmapFactory.decodeStream(is, null,options);

        System.out.println("Myrenderer bitmap");

        if (bitmap2D != null) {

//            int degree = 0;
//            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
//                degree = getBitmapDegree(is);
//            }
//            else {
//                degree = getBitmapDegree(filepath);
//            }
            System.out.println("degree: " + degree);


            bitmap2D = rotateBitmapByDegree(bitmap2D, degree);
//            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(bitmap.getByteCount());
            sz[0] = bitmap2D.getWidth();
            sz[1] = bitmap2D.getHeight();
            System.out.println("width:" + sz[0]);
            System.out.println("height:" + sz[1]);
            sz[2] = Math.max(sz[0], sz[1]);
            Integer[] num = {sz[0], sz[1]};
            float max_dim = (float) Collections.max(Arrays.asList(num));
            Log.v("MyRenderer", Float.toString(max_dim));

            mz[0] = (float) sz[0] / max_dim;
            mz[1] = (float) sz[1] / max_dim;
            mz[2] = Math.max(mz[0], mz[1]);

        }
        bitmap2D_backup = bitmap2D.copy(Bitmap.Config.ARGB_8888,false);
        is.close();
    }


    public void rotate(float dx, float dy) {

        angleX = dy * 70;
        angleY = dx * 70;
        Matrix.setRotateM(rotationXMatrix, 0, angleX, 1.0f, 0.0f, 0.0f);
        Matrix.setRotateM(rotationYMatrix, 0, angleY, 0.0f, 1.0f, 0.0f);
        float[] curRotationMatrix = new float[16];
        Matrix.multiplyMM(curRotationMatrix, 0, rotationXMatrix, 0, rotationYMatrix, 0);
        Matrix.multiplyMM(rotationMatrix, 0, curRotationMatrix, 0, rotationMatrix, 0);

//        Log.v("angleX = ", Float.toString(angleX));, float dis
//        Log.v("angleY = ", Float.toString(angleY));
    }

    public void rotate2f(float x1, float x2, float y1, float y2) {
        double value = (x1 * x2 + y1 * y2) / (Math.sqrt(x1 * x1 + y1 * y1) * Math.sqrt(x2 * x2 + y2 * y2));
        if (value > 1) {
            value = 1;
        }
        System.out.println(value);
//        angleZ = (float)Math.toDegrees(Math.acos(value));
        angleZ = (float) (Math.acos(value) / Math.PI * 180.0);
        System.out.println(angleZ);
        float axis = x2 * y1 - x1 * y2;
        if (axis != 0) {
//        float [] rotationZMatrix = new float[16];
            Matrix.setRotateM(rotationZMatrix, 0, angleZ, 0.0f, 0.0f, axis);
            Matrix.multiplyMM(rotationMatrix, 0, rotationZMatrix, 0, rotationMatrix, 0);
        }
    }


    public void zoom(float f) {

        if (cur_scale > 0.2 && cur_scale < 30) {
            Matrix.scaleM(zoomMatrix, 0, f, f, f);
            cur_scale *= f;
        } else if (cur_scale < 0.2 && f > 1) {
            Matrix.scaleM(zoomMatrix, 0, f, f, f);
            cur_scale *= f;
        } else if (cur_scale > 30 && f < 1) {
            Matrix.scaleM(zoomMatrix, 0, f, f, f);
            cur_scale *= f;
        }

    }


    public void zoom_in() {

        zoom(2f);

    }

    public void zoom_out() {

        zoom(0.6f);

    }

    //矩阵乘法
    private float[] multiplyMatrix(float[] m1, float[] m2) {
        float[] m = new float[9];
        for (int i = 0; i < 9; i++) {
            int r = i / 3;
            int c = i % 3;
            m[i] = 0;
            for (int j = 0; j < 3; j++) {
                m[i] += m1[r * 3 + j] * m2[j * 3 + c];
            }
        }
        return m;
    }


    private void CreateBuffer(byte[] data) {
        //分配内存空间,每个字节型占1字节空间
        imageBuffer = ByteBuffer.allocateDirect(data.length)
                .order(ByteOrder.nativeOrder());
        //传入指定的坐标数据
        imageBuffer.put(data);
        imageBuffer.position(0);
    }

    public void setLineDrawed(ArrayList<Float> lineDrawed) {
//        Float [] linePoints = lineDrawed.toArray(new Float[lineDrawed.size()]);

        linePoints = new float[lineDrawed.size()];
        for (int i = 0; i < lineDrawed.size(); i++) {
            linePoints[i] = lineDrawed.get(i);
        }
    }

    public void setIfPainting(boolean b) {

        ifPainting = b;
    }



    enum FileType {
        V3draw,
        SWC,
        ESWC,
        APO,
        ANO,
        TIF,
        JPG,
        PNG,
        V3dPBD,
        NotSupport
    }

    public void setTakePic(boolean takePic, Context contexts) {
        isTakePic = takePic;
        context_myrenderer = contexts;
    }

    public String getmCapturePath() {
        return mCapturePath;
    }

    public void resetCapturePath() {
        mCapturePath = null;
    }

    public void pencolorchange(int color) {
        lastLineType = color;


    }

    public void markercolorchange(int color) {
        lastMarkerType = color;
    }

    public int getLastLineType() {
        return lastLineType;
    }


    public void SetSwcLoaded() {
        ifLoadSWC = true;
    }

    public FileType getFileType() {
        return fileType;

    }

    public boolean getIfDownSampling() {
        return ifDownSampling;
    }

    public void setIfDownSampling(boolean b) {
        if (ifNeedDownSample)
            ifDownSampling = b;
        else
            ifDownSampling = false;
    }

    public boolean getIfNeedDownSample() {
        return ifNeedDownSample;
    }

    public void setIfNeedDownSample(boolean b) {
        ifNeedDownSample = b;
    }

    public boolean getIfFileSupport() {
        return ifFileSupport;
    }

    public boolean getIfFileLoaded() {
        return ifFileLoaded;
    }

    public boolean ifImageLoaded() {
        return !(bitmap2D == null);
    }

    public boolean if2dImageLoaded() {
        return !(bitmap2D == null);
    }

    public void setIfShowSWC(boolean b) {
        ifShowSWC = b;
    }

    public boolean getIfShowSWC() {
        return ifShowSWC;
    }

    public float[] solve2DMarker(float x, float y){
        if (ifIn2DImage(x, y)){
            System.out.println("innnnn");
            float i;
            float [] result = new float[3];
            for (i = -1; i < 1; i += 0.005){
                float [] invertfinalMatrix = new float[16];

                Matrix.invertM(invertfinalMatrix, 0, finalMatrix, 0);

                float [] temp = new float[4];
                Matrix.multiplyMV(temp, 0, invertfinalMatrix, 0, new float[]{x, y, i, 1}, 0);
                devideByw(temp);
                float dis = Math.abs(temp[2] - mz[2] / 2);
                if (dis < 0.1) {
                    System.out.println(temp[0]);
                    System.out.println(temp[1]);
                    result = new float[]{temp[0], temp[1], mz[2] / 2};
                    break;
                }
            }
            result = ModeltoVolume(result);

            System.out.println("result[0] = "+result[0]);
            System.out.println("result[1] = "+result[1]);

            //-------------------------
            if (ifRefine) {
                int[] resultint = new int[3];
                //获取小区域图像

                R[0] = round(bitmap2D.getWidth() / 50f);
                R[1] = round(bitmap2D.getHeight() / 50f);

//                System.out.println("R[0] = "+R[0]);
//                System.out.println("R[1] = "+R[1]);
                Bitmap tempBitmap = bitmap2D;
                bitmap2D = getSmallImageBlock(round(result[0]),round(result[1]),R[0],R[1]);

                //harris角点检测
                ArrayList<ImageMarker> MarkerListTemp = new ArrayList<ImageMarker>(MarkerList);

//                    MarkerList;MarkerListTemp =new ArrayList<ImageMarker>();
//            System.out.println(MarkerList.isEmpty());
                MarkerList.clear();
                if (if2dImageLoaded()){
                    //返回最大响应点
                    resultint = corner_detection();
//                        myGLSurfaceView.requestRender();
                } else {
                    Toast.makeText(getContext(), "Please load a 2d image first", Toast.LENGTH_SHORT).show();
                }

                if (!MarkerList.isEmpty()) {
                    //附近存在角点则矫正为最大响应
//                    result = getMostPossiblePoint(result, MarkerList,R);
                    result[0] += (float)resultint[0] - R[0];
                    result[1] += (float)resultint[1] - R[1];
                    System.out.println("----------------------");
                    System.out.println("result[0] = "+result[0]);
                    System.out.println("result[1] = "+result[1]);


                } else {
//                附近无角点则返回坐标（-1，-1）
                    Toast.makeText(getContext(), "There is no corner point at that position!", Toast.LENGTH_SHORT).show();
                    result[0] = -1;
                    result[1] = -1;
                }
                MarkerList = MarkerListTemp;
                bitmap2D = tempBitmap;
            }


            //----------------------------

            return result;
        }
        return null;
    }


    public void add2DMarker(float x, float y) throws CloneNotSupportedException {
        isAddPoint = true;
        float [] new_marker = solve2DMarker(x, y);
//        System.out.println();
//        System.out.println("----------------");
//        System.out.println("markernum = "+markernum);
//        System.out.println("MarkerList.size() = "+MarkerList.size());
//        System.out.println("----------------");
//        System.out.println();

        if (new_marker == null){
            System.out.println("outtttt");
            Toast.makeText(getContext(), "Please make sure the point is in the image", Toast.LENGTH_SHORT).show();
            return;
        }else {
            ImageMarker imageMarker_drawed = new ImageMarker(new_marker[0],
                    new_marker[1],
                    new_marker[2]);
            imageMarker_drawed.type = lastMarkerType;
            System.out.println("set type to 3");

//            ArrayList<ImageMarker> tempMarkerList = (ArrayList<ImageMarker>)MarkerList.clone();
//            V_NeuronSWC_list tempCurveList = curSwcList.clone();

//            if (curUndo < UNDO_LIMIT){
//                curUndo += 1;
//                undoMarkerList.add(tempMarkerList);
//                undoCurveList.add(tempCurveList);
//            } else {
//                undoMarkerList.remove(0);
//                undoCurveList.remove(0);
//                undoMarkerList.add(tempMarkerList);
//                undoCurveList.add(tempCurveList);
//            }
            //若检测到的坐标为（-1，-1）,则不添加新marker
            if (!(imageMarker_drawed.x == -1 && imageMarker_drawed.y ==-1)) {
                MarkerList.add(imageMarker_drawed);
                updateShowBitmap();
            }


//            if (process.size() < UNDO_LIMIT){
//                process.add(Operate.DRAWMARKER);
//                undoDrawMarkerList.add(imageMarker_drawed);
//            } else {
//                Operate first = process.firstElement();
//                process.remove(0);
//                process.add(Operate.DRAWMARKER);
//                removeFirstUndo(first);
//                undoDrawMarkerList.add(imageMarker_drawed);
//            }
        }
    }

    public void delete2DMarker(float x, float y) throws CloneNotSupportedException {
        //isAddPoint = false;
        float [] result = new float[3];
        float [] center = solve2DMarker(x, y);
//        center[0] = x;
//        center[1] = y;
        //选取最近的角点
        if (!MarkerList.isEmpty()) {
            //附近存在角点则矫正
            result = getMostNearMarker(MarkerList,center);

//            System.out.println("----------------------");
//            System.out.println("center[0] = "+center[0]);
//            System.out.println("center[1] = "+center[1]);
//            System.out.println("result[0] = "+result[0]);
//            System.out.println("result[1] = "+result[1]);
            int offset = (int) ceil(max(bitmap2D.getWidth(),bitmap2D.getHeight())/1000) * 80;
            offset *= offset;

            //最近marker距离小于offset，则认为选中该marker,一次只删除一个
            if ((pow(result[0]-center[0],2)+pow(result[1]-center[1],2)) < offset) {
                MarkerList.remove((int)result[2]);
                updateShowBitmap();
                Log.v("delete2DMarker", "Delete success!!!");
//                for (ImageMarker imagemarker:MarkerList) {
//                    if (imagemarker.x == result[0] && imagemarker.y == result[1]) {
//                        MarkerList.remove(imagemarker);
//                        updateShowBitmap();
//                        Log.v("delete2DMarker", "Delete success!!!");
//                        break;
//                    }
//                }
            }


        } else {
            //无marker可删
            Toast.makeText(getContext(), "There is no markers in this view!", Toast.LENGTH_SHORT).show();
        }
    }

    public int delete2DMarkerPairs(float x, float y) throws CloneNotSupportedException {
        //isAddPoint = false;
        float [] result = new float[3];
        float [] center = solve2DMarker(x, y);
//        center[0] = x;
//        center[1] = y;
        //选取最近的角点
        if (!MarkerList.isEmpty()) {
            //附近存在角点则矫正
            result = getMostNearMarker(MarkerList,center);

//            System.out.println("----------------------");
//            System.out.println("center[0] = "+center[0]);
//            System.out.println("center[1] = "+center[1]);
//            System.out.println("result[0] = "+result[0]);
//            System.out.println("result[1] = "+result[1]);
            int offset = (int) ceil(max(bitmap2D.getWidth(),bitmap2D.getHeight())/1000) * 80;
            offset *= offset;

            //最近marker距离小于offset，则认为选中该marker,一次只删除一个
            if ((pow(result[0]-center[0],2)+pow(result[1]-center[1],2)) < offset) {
                MarkerList.remove((int)result[2]);
                //updateShowBitmap();
                Log.v("delete2DMarker", "Delete success!!!");
//                for (ImageMarker imagemarker:MarkerList) {
//                    if (imagemarker.x == result[0] && imagemarker.y == result[1]) {
//                        MarkerList.remove(imagemarker);
//                        updateShowBitmap();
//                        Log.v("delete2DMarker", "Delete success!!!");
//                        break;
//                    }
//                }
            }


        } else {
            //无marker可删
            Toast.makeText(getContext(), "There is no markers in this view!", Toast.LENGTH_SHORT).show();
        }

        return (int)result[2];
    }

    // add the marker drawed into markerlist
    public void setMarkerDrawed(float x, float y) throws CloneNotSupportedException {

        if(solveMarkerCenter(x, y) != null) {
            float[] new_marker = solveMarkerCenter(x, y);

            ImageMarker imageMarker_drawed = new ImageMarker(new_marker[0],
                    new_marker[1],
                    new_marker[2]);
            imageMarker_drawed.type = lastMarkerType;
            System.out.println("set type to 3");

//            ArrayList<ImageMarker> tempMarkerList = (ArrayList<ImageMarker>)MarkerList.clone();
//            V_NeuronSWC_list tempCurveList = curSwcList.clone();
//
//            if (curUndo < UNDO_LIMIT){
//                curUndo += 1;
//                undoMarkerList.add(tempMarkerList);
//                undoCurveList.add(tempCurveList);
//            } else {
//                undoMarkerList.remove(0);
//                undoCurveList.remove(0);
//                undoMarkerList.add(tempMarkerList);
//                undoCurveList.add(tempCurveList);
//            }

            MarkerList.add(imageMarker_drawed);

//            if (process.size() < UNDO_LIMIT){
//                process.add(Operate.DRAWMARKER);
//                undoDrawMarkerList.add(imageMarker_drawed);
//            } else {
//                Operate first = process.firstElement();
//                process.remove(0);
//                process.add(Operate.DRAWMARKER);
//                removeFirstUndo(first);
//                undoDrawMarkerList.add(imageMarker_drawed);
//            }
        }
    }

    //寻找marker点的位置~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public float[] solveMarkerCenter(float x, float y){

//        float [] result = new float[3];
        float [] loc1 = new float[3];
        float [] loc2 = new float[3];

//        get_NearFar_Marker(x, y, loc1, loc2);
        get_NearFar_Marker_2(x, y, loc1, loc2);

        Log.v("loc1",Arrays.toString(loc1));
        Log.v("loc2",Arrays.toString(loc2));

        float steps = 512;
        float [] step = devide(minus(loc1, loc2), steps);
        Log.v("step",Arrays.toString(step));


        if(make_Point_near(loc1, loc2)){
//            Log.v("loc1",Arrays.toString(loc1));
//            Log.v("loc2",Arrays.toString(loc2));

            float [] Marker = getCenterOfLineProfile(loc1, loc2);
//            float [] Marker = {60.1f, 63.2f, 63.6f};
            if (Marker == null){
                return null;
            }

            Log.v("Marker",Arrays.toString(Marker));

//            float intensity = Sample3d(Marker[0], Marker[1], Marker[2]);
//            Log.v("intensity",Float.toString(intensity));

            return Marker;
        }else {
            Log.v("solveMarkerCenter","please make sure the point inside the bounding box");
//            Looper.prepare();
            Toast.makeText(getContext(), "please make sure the point inside the bounding box", Toast.LENGTH_SHORT).show();
            return null;
        }


    }

    //获取以（x,y）为中心x轴半径为Rx，y轴半径为Ry的图像块区域图像块
    private Bitmap getSmallImageBlock(int x, int y,int Rx, int Ry) {
        int xfrom = max(0,x-Rx);
        int xto = min(bitmap2D_backup.getWidth()-1,x+Rx);
        int yfrom = max(0,y-Ry);
        int yto = min(bitmap2D_backup.getHeight()-1,y+Ry);
        int xlen = xto - xfrom + 1;
        int ylen = yto - yfrom + 1;
//        System.out.println("xfrom = "+xfrom);
//        System.out.println("xto = "+xto);
//        System.out.println("yfrom = "+yfrom);
//        System.out.println("yto = "+yto);

//        int color;
        Bitmap myBitmap = null;
        myBitmap = Bitmap.createBitmap( xlen, ylen, Bitmap.Config.ARGB_8888 );
        for (int w = 0; w < xlen; w++) {
            for (int h = 0; h < ylen; h++) {
//                    color = image.getPixel(w,h);
                myBitmap.setPixel(w, h, bitmap2D_backup.getPixel(xfrom+w,yfrom+h));
            }
        }
        return myBitmap;
    }

    /**
     * 从点集中获取与特定点最近的点
     * @param Marker 中心点绝对坐标
     * @param MarkerList 相对坐标点集
     * @param R 中心点相对坐标
     * @return
     */
    private float[] getMostPossiblePoint(float[] Marker, ArrayList<ImageMarker> MarkerList, int[] R) {
        double mindst = Integer.MAX_VALUE;
        double dst;
        int index = 0;
        ImageMarker value;
        for (int i = 0; i < MarkerList.size(); i++) {
            value = MarkerList.get(i);
            dst = sqrt((pow(value.x-R[0],2) + pow(value.y-R[1],2)));
            if (mindst > dst) {
                mindst = dst;
                index = i;
            }
        }

        value = MarkerList.get(index);
//        System.out.println("+++++++++++++++++++++");
//        System.out.println("value.x = "+value.x);
//        System.out.println("value.y = "+value.y);
        Marker[0] += value.x - R[0]; // - 1 - 1
        Marker[1] += value.y - R[1];
        return Marker;
    }

    private float[] getMostNearMarker(ArrayList<ImageMarker> MarkerList, float[] R) {
        float[] Marker = new float[3];
        double mindst = Integer.MAX_VALUE;
        double dst;
        int index = 0;
        ImageMarker value;
        for (int i = 0; i < MarkerList.size(); i++) {
            value = MarkerList.get(i);
            dst = sqrt((pow(value.x-R[0],2) + pow(value.y-R[1],2)));
            if (mindst > dst) {
                mindst = dst;
                index = i;
            }
        }

        value = MarkerList.get(index);
//        System.out.println("+++++++++++++++++++++");
//        System.out.println("value.x = "+value.x);
//        System.out.println("value.y = "+value.y);
        Marker[0] = (float) value.x;
        Marker[1] = (float) value.y;
        Marker[2] = index; //二维图像中z坐标无效，暂时用于存储索引值
        return Marker;
    }

    //用于透视投影中获取近平面和远平面的焦点
    private void get_NearFar_Marker_2(float x, float y, float [] res1, float [] res2){

        //mvp矩阵的逆矩阵
        float [] invertfinalMatrix = new float[16];

        Matrix.invertM(invertfinalMatrix, 0, finalMatrix, 0);
//        Log.v("invert_rotation",Arrays.toString(invertfinalMatrix));

        float [] near = new float[4];
        float [] far = new float[4];

        Matrix.multiplyMV(near, 0, invertfinalMatrix, 0, new float [] {x, y, -1, 1}, 0);
        Matrix.multiplyMV(far, 0, invertfinalMatrix, 0, new float [] {x, y, 1, 1}, 0);

        devideByw(near);
        devideByw(far);

//        Log.v("near",Arrays.toString(near));
//        Log.v("far",Arrays.toString(far));

        for(int i=0; i<3; i++){
            res1[i] = near[i];
            res2[i] = far[i];
        }

    }

    //找到靠近boundingbox的两处端点
    private boolean make_Point_near(float[] loc1, float[] loc2){

        float steps = 512;
        float [] near = loc1;
        float [] far = loc2;
        float [] step = devide(minus(near, far), steps);

        float[][] dim = new float[3][2];
        for(int i=0; i<3; i++){
            dim[i][0]= 0;
            dim[i][1]= mz[i];
        }

        int num = 0;
        while(num<steps && !IsInBoundingBox(near, dim)){
            near = minus(near, step);
            num++;
        }
        if(num == steps)
            return false;


        while(!IsInBoundingBox(far, dim)){
            far = plus(far, step);
        }

        near = plus(near, step);
        far = minus(far, step);

        for(int i=0; i<3; i++){
            loc1[i] = near[i];
            loc2[i] = far[i];
        }

//        Log.v("make_point_near","here we are");
        return true;

    }

    //类似于光线投射，找直线上强度最大的一点
    // in Image space (model space)
    private float[] getCenterOfLineProfile(float[] loc1, float[] loc2){

        float[] result = new float[3];
        float[] loc1_index = new float[3];
        float[] loc2_index = new float[3];
        boolean isInBoundingBox = false;

//        for(int i=0; i<3; i++){
//            loc1_index[i] = loc1[i] * sz[i];
//            loc2_index[i] = loc2[i] * sz[i];
//        }

        loc1_index = ModeltoVolume(loc1);
        loc2_index = ModeltoVolume(loc2);

//        float f = 0.8f;

        float[] d = minus(loc1_index, loc2_index);
        normalize(d);

        float[][] dim = new float[3][2];
        for(int i=0; i<3; i++){
            dim[i][0] = 0;
            dim[i][1] = sz[i] - 1;
        }



//        for(int i=0; i<2; i++){
//            loc1_index[i] = (1.0f - loc1[i]) * sz[2 - i];
//            loc2_index[i] = (1.0f - loc2[i]) * sz[2 - i];
//        }
//
//
//        loc1_index[2] = loc1[2] * sz[0];
//        loc2_index[2] = loc2[2] * sz[0];


        result = devide(plus(loc1_index, loc2_index), 2);

        float max_value = 0f;

        //单位向量
//        float[] d = minus(loc1_index, loc2_index);
//        normalize(d);

        Log.v("getCenterOfLineProfile:", "step: " + Arrays.toString(d));

        //判断是不是一个像素
        float length = distance(loc1_index, loc2_index);
        if(length < 0.5)
            return result;

        int nstep = (int)(length+0.5);
        float one_step = length/nstep;

        Log.v("getCenterOfLineProfile", Float.toString(one_step));

//            float[][] dim = new float[3][2];
//            for(int i=0; i<3; i++){
//                dim[i][0] = 0;
//                dim[i][1] = sz[i] - 1;
//            }


//            float[] sum_loc = {0, 0, 0};
//            float sum = 0;
        float[] poc;
        for (int i = 0; i <= nstep; i++) {

            float value;

            poc = minus(loc1_index, multiply(d, one_step * i));
//            poc = multiply(d, one_step);

//            Log.v("getCenterOfLineProfile:", "update the max");

//            Log.v("getCenterOfLineProfile", "(" + poc[0] + "," + poc[1] + "," + poc[2] + ")");


            if (IsInBoundingBox(poc, dim)) {

                value = Sample3d(poc[0], poc[1], poc[2]);
//                    sum_loc[0] += poc[0] * value;
//                    sum_loc[1] += poc[1] * value;
//                    sum_loc[2] += poc[2] * value;
//                    sum += value;
                isInBoundingBox = true;
                if(value > max_value){
//                    Log.v("getCenterOfLineProfile", "(" + poc[0] + "," + poc[1] + "," + poc[2] + "): " +value);
//                    Log.v("getCenterOfLineProfile:", "update the max");
                    max_value = value;
                    for (int j = 0; j < 3; j++){
                        result[j] = poc[j];
                    }
                    isInBoundingBox = true;
                }
            }
        }

//            if (sum != 0) {
//                result[0] = sum_loc[0] / sum;
//                result[1] = sum_loc[1] / sum;
//                result[2] = sum_loc[2] / sum;
//            }else{
//                break;
//            }

//            for (int k = 0; k < 3; k++){
//                loc1_index[k] = result[k] + d[k] * (length * f / 2);
//                loc2_index[k] = result[k] - d[k] * (length * f / 2);
//            }


        if(!isInBoundingBox){
            Toast.makeText(getContext(), "please make sure the point inside the bounding box", Toast.LENGTH_SHORT).show();
            return null;
        }

        return result;
    }

    //判断是否在图像内部了
    private boolean IsInBoundingBox(float[] x, float[][] dim){
        int length = x.length;

        for(int i=0; i<length; i++){
//            Log.v("IsInBoundingBox", Float.toString(x[i]));
            if(x[i]>=dim[i][1] || x[i]<=dim[i][0])
                return false;
        }
//        Log.v("IsInBoundingBox", Arrays.toString(x));
//        Log.v("IsInBoundingBox", Arrays.toString(dim));
        return true;
    }

    public boolean ifIn2DImage(float x, float y){
        float [] x1 = new float[]{0 ,0, mz[2] / 2, 1};
        float [] x2 = new float[]{mz[0], 0, mz[2] / 2, 1};
        float [] x3 = new float[]{0, mz[1], mz[2] / 2, 1};
        float [] x4 = new float[]{mz[0], mz[1], mz[2] / 2, 1};
        float [] x1r = new float[4];
        float [] x2r = new float[4];
        float [] x3r = new float[4];
        float [] x4r = new float[4];

        Matrix.multiplyMV(x1r, 0, finalMatrix, 0, x1, 0);
        Matrix.multiplyMV(x2r, 0, finalMatrix, 0, x2, 0);
        Matrix.multiplyMV(x3r, 0, finalMatrix, 0, x3, 0);
        Matrix.multiplyMV(x4r, 0, finalMatrix, 0, x4, 0);

        devideByw(x1r);
        devideByw(x2r);
        devideByw(x3r);
        devideByw(x4r);

        float signOfTrig = (x2r[0] - x1r[0]) * (x3r[1] - x1r[1]) - (x2r[1] - x1r[1]) * (x3r[0] - x1r[0]);
        float signOfAB = (x2r[0] - x1r[0]) * (y - x1r[1]) - (x2r[1] - x1r[1]) * (x - x1r[0]);
        float signOfCA = (x1r[0] - x3r[0]) * (y - x3r[1]) - (x1r[1] - x3r[1]) * (x - x3r[0]);
        float signOfBC = (x3r[0] - x2r[0]) * (y - x3r[1]) - (x3r[1] - x2r[1]) * (x - x3r[0]);

        boolean d1 = (signOfAB * signOfTrig > 0);
        boolean d2 = (signOfCA * signOfTrig > 0);
        boolean d3 = (signOfBC * signOfTrig > 0);

        boolean b1 =  d1 && d2 && d3;

        float signOfTrig2 = (x3r[0] - x2r[0]) * (x4r[1] - x2r[1]) - (x3r[1] - x2r[1]) * (x4r[0] - x2r[0]);
        float signOfCB = (x3r[0] - x2r[0]) * (y - x2r[1]) - (x3r[1] - x2r[1]) * (x - x2r[0]);
        float signOfDB = (x2r[0] - x4r[0]) * (y - x4r[1]) - (x2r[1] - x4r[1]) * (x - x4r[0]);
        float signOfDC = (x4r[0] - x3r[0]) * (y - x4r[1]) - (x4r[1] - x3r[1]) * (x - x4r[0]);

        boolean d4 = (signOfCB * signOfTrig2 > 0);
        boolean d5 = (signOfDB * signOfTrig2 > 0);
        boolean d6 = (signOfDC * signOfTrig2 > 0);

        boolean b2 = d4 && d5 && d6;

        return b1 || b2;
    }

    float Sample3d(float x, float y, float z){
        int x0, x1, y0, y1, z0, z1;
        x0 = (int) Math.floor(x);         x1 = (int) Math.ceil(x);
        y0 = (int) Math.floor(y);         y1 = (int) Math.ceil(y);
        z0 = (int) Math.floor(z);         z1 = (int) Math.ceil(z);

        float xf, yf, zf;
        xf = x-x0;
        yf = y-y0;
        zf = z-z0;

        float [][][] is = new float[2][2][2];
        is[0][0][0] = grayData(x0, y0, z0);
        is[0][0][1] = grayData(x0, y0, z1);
        is[0][1][0] = grayData(x0, y1, z0);
        is[0][1][1] = grayData(x0, y1, z1);
        is[1][0][0] = grayData(x1, y0, z0);
        is[1][0][1] = grayData(x1, y0, z1);
        is[1][1][0] = grayData(x1, y1, z0);
        is[1][1][1] = grayData(x1, y1, z1);

        float [][][] sf = new float[2][2][2];
        sf[0][0][0] = (1-xf)*(1-yf)*(1-zf);
        sf[0][0][1] = (1-xf)*(1-yf)*(  zf);
        sf[0][1][0] = (1-xf)*(  yf)*(1-zf);
        sf[0][1][1] = (1-xf)*(  yf)*(  zf);
        sf[1][0][0] = (  xf)*(1-yf)*(1-zf);
        sf[1][0][1] = (  xf)*(1-yf)*(  zf);
        sf[1][1][0] = (  xf)*(  yf)*(1-zf);
        sf[1][1][1] = (  xf)*(  yf)*(  zf);

        float result = 0f;

        for(int i=0; i<2; i++)
            for(int j=0; j<2; j++)
                for(int k=0; k<2; k++)
                    result +=  is[i][j][k] * sf[i][j][k];

//        for(int i=0; i<2; i++)
//            for(int j=0; j<2; j++)
//                for(int k=0; k<2; k++)
//                    Log.v("Sample3d", Float.toString(is[i][j][k]));

        return result;
    }

    private int grayData(int x, int y, int z){
        int result = 0;
        if (data_length == 1){
            byte b = grayscale[z * sz[0] * sz[1] + y * sz[0] + x];
            result = ByteTranslate.byte1ToInt(b);
        }else if (data_length == 2){
            byte [] b = new byte[2];
            b[0] = grayscale[(z * sz[0] * sz[1] + y * sz[0] + x) * 2];
            b[1] = grayscale[(z * sz[0] * sz[1] + y * sz[0] + x) * 2 + 1];
            result = ByteTranslate.byte2ToInt(b, isBig);
        }else if (data_length == 4){
            byte [] b = new byte[4];
            b[0] = grayscale[(z * sz[0] * sz[1] + y * sz[0] + x) * 4];
            b[1] = grayscale[(z * sz[0] * sz[1] + y * sz[0] + x) * 4 + 1];
            b[2] = grayscale[(z * sz[0] * sz[1] + y * sz[0] + x) * 4 + 2];
            b[3] = grayscale[(z * sz[0] * sz[1] + y * sz[0] + x) * 4 + 3];
            result = ByteTranslate.byte2ToInt(b, isBig);
        }
        return result;
    }

    private float distance(float[] x, float[] y){
        int length = x.length;
        float sum = 0;

        for(int i=0; i<length; i++){
            sum += Math.pow(x[i]-y[i], 2);
        }
        return (float)Math.sqrt(sum);
    }

    private void normalize(float[] x){
        int length = x.length;
        float sum = 0;

        for(int i=0; i<length; i++)
            sum += Math.pow(x[i], 2);

        for(int i=0; i<length; i++)
            x[i] = x[i] / (float)Math.sqrt(sum);
    }

    //加法运算
    private float [] plus(float[] x, float[] y){
        if(x.length != y.length){
            Log.v("plus","length is not the same!");
            return null;
        }

        int length = x.length;
        float [] result = new float[length];

        for (int i=0; i<length; i++)
            result[i] = x[i] + y[i];
        return result;
    }

    //减法运算
    private float [] minus(float[] x, float[] y){
        if(x.length != y.length){
            Log.v("minus","length is not the same!");
            return null;
        }

        int length = x.length;
        float [] result = new float[length];

        for (int i=0; i<length; i++)
            result[i] = x[i] - y[i];
        return result;
    }



    //除法运算
    private float [] devide(float[] x, float num){
        if(num == 0){
            Log.v("devide","can not be devided by 0");
        }

        int length = x.length;
        float [] result = new float[length];

        for(int i=0; i<length; i++)
            result[i] = x[i]/num;

        return result;
    }

    //除法运算
    private void devideByw(float[] x){
        if(Math.abs(x[3]) < 0.000001f){
            Log.v("devideByw","can not be devided by 0");
            return;
        }

        for(int i=0; i<3; i++)
            x[i] = x[i]/x[3];

    }

    //乘法运算
    private float [] multiply(float[] x, float num){
        if(num == 0){
            Log.v("multiply","can not be multiply by 0");
        }

        int length = x.length;
        float [] result = new float[length];

        for(int i=0; i<length; i++)
            result[i] = x[i] * num;

        return result;
    }

    private float[] ModeltoVolume(float[] input){
        if (input == null)
            return null;

        float[] result = new float[3];
        result[0] = (1.0f - input[0] / mz[0]) * sz[0];
        result[1] = (1.0f - input[1] / mz[1]) * sz[1];
        result[2] = input[2] / mz[2] * sz[2];

        return result;
    }

    public int[] corner_detection() {

//        if (bitmap2D == null)
//            return;
//
//        Toast.makeText(getContext(), "Please load a 2d image first", Toast.LENGTH_SHORT).show();

        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        // PC端一定要有这句话，但是android端一定不能有这句话，否则报错

//        Mat src = new Mat();
//
//        Mat temp = new Mat();
//
//        Mat dst = new Mat();


//        final int maxCorners = 40, blockSize = 3; //blockSize表示窗口大小，越大那么里面的像素点越多，选取梯度和方向变化最大的像素点作为角点，这样总的角点数肯定变少，而且也可能错过一些角点

//        final double qualityLevel = 0.05, minDistance = 23.0, k = 0.04;

        //qualityLevel：检测到的角点的质量等级，角点特征值小于qualityLevel*最大特征值的点将被舍弃；
        //minDistance：两个角点间最小间距，以像素为单位；

//        final boolean useHarrisDetector = false;

//        MatOfPoint corners = new MatOfPoint();

        /*File file = new File(filepath);
        System.out.println(filepath);
        long length = 0;
        InputStream is1 = null;
        if (file.exists()) {
            try {
                length = file.length();
                is1 = new FileInputStream(file);
//                grayscale =  rr.run(length, is);


                Log.v("getIntensity_3d", filepath);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            Uri uri = Uri.parse(filepath);

            try {
                ParcelFileDescriptor parcelFileDescriptor =
                        getContext().getContentResolver().openFileDescriptor(uri, "r");

                is1 = new ParcelFileDescriptor.AutoCloseInputStream(parcelFileDescriptor);

                length = (int) parcelFileDescriptor.getStatSize();


            } catch (Exception e) {
                Log.v("MyPattern", "Successfully load intensity");

                Log.v("MyPattern", "Some problems in the MyPattern when load intensity");
            }


        }


        BitmapFactory.Options options1 = new BitmapFactory.Options();
        //设置inJustDecodeBounds为true表示只获取大小，不生成Btimap
        options1.inJustDecodeBounds = true;
        //解析图片大小
        //InputStream stream = getContentResolver().openInputStream(uri);
        BitmapFactory.decodeStream(is1, null, options1);
        if (is1 != null)
            System.out.println("isnnnnnn");
        IOUtils.closeQuietly(is1); // 关闭流
        // is.close();
        int width = options1.outWidth;
        int height = options1.outHeight;
        int ratio = 0;
        //如果宽度大于高度，交换宽度和高度
        if (width > height) {
            int temp2 = width;
            width = height;
            height = temp2;
        }
        //计算取样比例
        int sampleRatio = 1;
        if (width < 500 || height < 500)
            sampleRatio = 1;
        else{
            int s1 = 2;
            int s2 = 2;
            while ((width / s1) > 500){
                s1 *= 2;
            }
            while ((height / s2) > 900){
                s2 *= 2;
            }
            sampleRatio = Math.max(s1, s2);
        }
        System.out.println(width);
        System.out.println(height);
        //定义图片解码选项
        BitmapFactory.Options options2 = new BitmapFactory.Options();
        options2.inSampleSize = sampleRatio;


        //读取图片，并将图片缩放到指定的目标大小
        // InputStream stream = getContentResolver().openInputStream(uri);
        File file2 = new File(filepath);
        long length2 = 0;
        InputStream is2 = null;
        if (file2.exists()) {
            try {
                length2 = file2.length();
                is2 = new FileInputStream(file2);
//                grayscale =  rr.run(length, is);


                Log.v("getIntensity_3d", filepath);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            Uri uri = Uri.parse(filepath);

            try {
                ParcelFileDescriptor parcelFileDescriptor =
                        getContext().getContentResolver().openFileDescriptor(uri, "r");

                is2 = new ParcelFileDescriptor.AutoCloseInputStream(parcelFileDescriptor);

                length2 = (int) parcelFileDescriptor.getStatSize();

                Log.v("MyPattern", "Successfully load intensity");

            } catch (Exception e) {
                Log.v("MyPattern", "Some problems in the MyPattern when load intensity");
            }


        }


        Bitmap image = BitmapFactory.decodeStream(is2, null, options2);
        if (image == null) {
            System.out.println("nnnnnn");
        }

        image = rotateBitmapByDegree(image, degree);

//        System.out.println(image.getWidth());
//        System.out.println(image.getHeight());

//        System.out.println("ssssss");
//        System.out.println(options2.inSampleSize);
        IOUtils.closeQuietly(is2);
        //is.close();


        // Bitmap image = bitmap2D;//从bitmap中加载进来的图像有时候有四个通道，所以有时候需要多加一个转化
        // Bitmap image = BitmapFactory.decodeResource(this.getResources(),R.drawable.cube);

//        Utils.bitmapToMat(image, src);//把image转化为Mat

//        dst = src.clone();

//        Imgproc.cvtColor(src, temp, Imgproc.COLOR_BGR2GRAY);//这里由于使用的是Imgproc这个模块所有这里要这么写
//
//        Log.i("CV", "image type:" + (temp.type() == CvType.CV_8UC3));
//        Imgproc.goodFeaturesToTrack(temp, corners, maxCorners, qualityLevel, minDistance,
//
//                new Mat(), blockSize, useHarrisDetector, k);
//        Point[] pCorners = corners.toArray();*/

//        Bitmap destImage;
        Bitmap sourceImage = bitmap2D;
//        Bitmap sourceImage = image;
        /*HarrisCornerDetector filter = new HarrisCornerDetector();
        destImage = filter.filter(sourceImage, null);

        int[] corner_x_y = filter.corner_xy;*/
        int[] corner_x_y = myHarrisdemo();
        int[] corner_x = new int[corner_x_y.length/2];
        int[] corner_y = new int[corner_x_y.length/2];
//        int[] maxResponseLocation = filter.globalMaxLocation;
        int[] maxResponseLocation = new int[2];
        maxResponseLocation[0] = corner_x_y[1];
        maxResponseLocation[1] = corner_x_y[0];

//        System.out.println("++++++++++");
//        System.out.println("corner_x_y.length = "+corner_x_y.length);
//        System.out.println("++++++++++");
        for (int n=0;n<corner_x_y.length/2;n++)
        {
            corner_x[n]=corner_x_y[2*n+1];
            corner_y[n]=corner_x_y[2*n]; 
            //System.out.println(corner_x[n]);
            //System.out.println(corner_y[n]);
        }


//        System.out.println(pCorners.length);


//        int power = (int) (Math.log((double) sampleRatio) / Math.log(2));
//        int actual_ratio = (int) Math.pow(2, power);
//        if (actual_ratio>2){
//            actual_ratio+=2;
//        }

//        int actual_ratio = options2.inSampleSize;

//        System.out.println("aaaaaaaaa");
//        System.out.println(actual_ratio);
        for (int i = 0; i < corner_x_y.length/2; i++) {
            if (corner_x[i]==0&&corner_y[i]==0);
            else{
//                ImageMarker imageMarker_drawed = new ImageMarker((float) corner_x[i] * actual_ratio,
//                        (float) corner_y[i] * actual_ratio,
//                        sz[2] / 2);
                ImageMarker imageMarker_drawed = new ImageMarker((float) corner_x[i],
                        (float) corner_y[i],
                        sz[2] / 2);
                imageMarker_drawed.type = lastMarkerType;
//            System.out.println("set type to 3");

                MarkerList.add(imageMarker_drawed);}
//            Imgproc.circle(dst, pCorners[i], (width+height)/(350*sampleRatio), new Scalar(255,255,0),2);

        }
//        System.out.println(pCorners.length);

        // Imgproc.cvtColor(temp,dst,Imgproc.COLOR_GRAY2BGR);

//        Utils.matToBitmap(dst,image);//把mat转化为bitmap
//        bitmap2D = image;

//        System.out.println(image.getWidth());
//        System.out.println(mz[0]);
//        myPattern2D = new MyPattern2D(bitmap2D, image.getWidth(), image.getHeight(), mz);

        //ImageView imageView = findViewById(R.id.text_view);

        //imageView.setImageBitmap(image);

        //release

//        src.release();

//        temp.release();

//        dst.release();
//        ImageMarker maxloc = new ImageMarker(maxResponseLocation[0],maxResponseLocation[1],maxResponseLocation[2]);
//        maxloc.type = lastMarkerType;
        return maxResponseLocation;

    }

    private int[] myHarrisdemo() {

        // 定义阈值T//初始化各种Mat对象
        int threshold = 100;
        Mat src = new Mat();
        Mat gray = new Mat();
        Mat response = new Mat();
        Mat response_norm = new Mat();

        Utils.bitmapToMat(bitmap2D, src);//把image转化为Mat
        Imgproc.cvtColor(src,gray, Imgproc.COLOR_RGBA2GRAY);

        // 角点检测
        Imgproc.cornerHarris(gray, response, 2, 3, 0.04);
        Core.normalize(response, response_norm, 0, 255, Core.NORM_MINMAX, CvType.CV_32F);//归一化
//        Core.convertScaleAbs(response_norm, corners);

//        FeatureDetector detector = FeatureDetector.create(FeatureDetector.GRID_ORB);

        // 筛选角点
//        dst.create(src.size(), src.type());
//        src.copyTo(dst);
        ArrayList<HarrisMatrix> hmlist =  new ArrayList<HarrisMatrix>();
        float[] data = new float[1];//！！！！！！！！！！
        for(int j=0; j<response_norm.rows(); j++ )
        {
            for(int i=0; i<response_norm.cols(); i++ )
            {
                response_norm.get(j, i, data);
                if((int)data[0] > 100)
                {
//                    Imgproc.circle(dst, new Point(i, j), 5,　new Scalar(0, 0, 255),
//                            2, 8, 0);
                    HarrisMatrix hm = new HarrisMatrix();
                    hm.setR(data[0]);
                    hm.setX(i);
                    hm.setY(j);
                    hmlist.add(hm);
                    Log.i("Harris Corner", "find corner point……");
                }
            }
        }

        src.release();
        gray.release();
        response.release();
        response_norm.release();

        //响应降序排列
        System.out.println("-------------------------------------------------------");

        Collections.sort(hmlist, new Comparator<HarrisMatrix>() {

            @Override
            public int compare(HarrisMatrix h1, HarrisMatrix h2) {
                //降序
                if (h1.getR()-h2.getR() > 0) {
                    return -1;
                } else if (h1.getR()-h2.getR() < 0) {
                    return 1;
                } else {
                    return 0;
                }

                //return h1.compareTo(h2);
            }

        });

        gray.release();
        response.release();

//        for (HarrisMatrix harrisMatrix:hmlist) {
//            if ()
//        }

        if (hmlist.size() > 3) {
            int[] new_corner_x_y = new int[6];
            for (int i = 0; i < 3; i++) {
                new_corner_x_y[2*i + 1] = hmlist.get(i).getX();
                new_corner_x_y[2*i] = hmlist.get(i).getY();
//                System.out.println(i+" "+temp.get(i).getX()+","+temp.get(i).getY());
            }

            return new_corner_x_y;
        } else if (hmlist.size() >0) {
            int[] new_corner_x_y = new int[2*hmlist.size()];
            for (int i = 0; i < hmlist.size(); i++) {
                new_corner_x_y[2*i + 1] = hmlist.get(i).getX();
                new_corner_x_y[2*i] = hmlist.get(i).getY();
//                System.out.println(i+" "+temp.get(i).getX()+","+temp.get(i).getY());
            }
            return new_corner_x_y;
        } else {
            return new int[2];
        }

    }

    public int[] corner_detection2() {
        int[] corner_x_y = myHarrisdemo2();

        if (corner_x_y == null) {
            return null;
        }

        int[] corner_x = new int[corner_x_y.length/2];
        int[] corner_y = new int[corner_x_y.length/2];
        int[] maxResponseLocation = new int[2];
        maxResponseLocation[0] = corner_x_y[1];
        maxResponseLocation[1] = corner_x_y[0];

        for (int n=0;n<corner_x_y.length/2;n++)
        {
            corner_x[n]=corner_x_y[2*n+1];
            corner_y[n]=corner_x_y[2*n];
            //System.out.println(corner_x[n]);
            //System.out.println(corner_y[n]);
        }

        for (int i = 0; i < corner_x_y.length/2; i++) {
            if (corner_x[i]==0&&corner_y[i]==0);
            else{
                ImageMarker imageMarker_drawed = new ImageMarker((float) corner_x[i],
                        (float) corner_y[i],
                        sz[2] / 2);
                imageMarker_drawed.type = lastMarkerType;
//            System.out.println("set type to 3");

                MarkerList.add(imageMarker_drawed);}

        }

        return maxResponseLocation;

    }

    ArrayList<HarrisMatrix> hmlist =  new ArrayList<HarrisMatrix>();
    private int[] myHarrisdemo2() {

        if (hmlist.size() > 0) {
            hmlist.clear();
        }
        // 定义阈值T//初始化各种Mat对象
        double threshold;
        Mat src = new Mat();
        Mat gray = new Mat();
        Mat response = new Mat();
        Mat response_norm = new Mat();
        float[] data = new float[1];//！！！！！！！！！！ = 100


        Utils.bitmapToMat(bitmap2D, src);//把image转化为Mat
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY);

        // 角点检测
        Imgproc.cornerHarris(gray, response, 2, 3, 0.04);
        Core.normalize(response, response_norm, 0, 255, Core.NORM_MINMAX, CvType.CV_32F);//归一化

        //获取响应数组和最大最小响应值
        float maxR = 0;
        float minR = Float.MAX_VALUE;//，最小
        double[] sortArray = new double[response_norm.rows()*response_norm.cols()];
        for(int j=0; j<response_norm.rows(); j++ )
        {
            for(int i=0; i<response_norm.cols(); i++ )
            {
                response_norm.get(j, i, data);
                sortArray[j*response_norm.cols()+i] = data[0];
                if (maxR < data[0]) {
                    maxR = data[0];
                } else if (minR > data[0]) {
                    minR = data[0];
                }
            }
        }
//        out.println("maxR = "+maxR);
//        float step = (maxR - 100) / 10;
        Arrays.sort(sortArray); //升序排列

        //迭代求取阈值
        int saveNum = 10000;  //有待调整
        int[] tempThre = new int[4];
        int count = 0;
        tempThre[3] = sortArray.length - 1;
        System.out.println("*************************************************************************");
        while (tempThre[1] < saveNum) {
            //step = (maxR - minR) / Math.pow(10, count);
            tempThre = indexOfThreshold(sortArray, saveNum, tempThre[1], tempThre[2], tempThre[3]);
            count++;
            System.out.println(count+" tempThre[1] = "+tempThre[1]);
            System.out.println("tempThre[2] = "+tempThre[2]);
            System.out.println("tempThre[3] = "+tempThre[3]);
        }
        System.out.println("*************************************************************************");
        //计算阈值
        threshold = sortArray[tempThre[3]];
        System.out.println("threshold = "+threshold);
        System.out.println("sortArray[tempThre[3]] = "+sortArray[tempThre[2]]);



        /*// 0: 101+8*step-maxR; 1: 101+6*step--100+8*step; 2: 101+4*step--100+6*step; 3: 101+3*step--100+4*step; 4: 101+2*step--100+3*step; 5: 101+step--100+2*step; 6: 101--100+step
        //int[] count = new int[7];


        for(int j=0; j<response_norm.rows(); j++ )
        {
            for(int i=0; i<response_norm.cols(); i++ )
            {
                response_norm.get(j, i, data);
                if ((int)data[0] > 100+8*step) {
                    count[0]++;
                } else if ((int)data[0] > 100+6*step) {
                    count[1]++;
                } else if ((int)data[0] > 100+4*step) {
                    count[2]++;
                } else if ((int)data[0] > 100+3*step) {
                    count[3]++;
                } else if ((int)data[0] > 100+2*step) {
                    count[4]++;
                } else if ((int)data[0] > 100+step) {
                    count[5]++;
                } else if ((int)data[0] > 100) {
                    count[6]++;
                }
            }
        }

        int num = 0;
        int index = 0;
        for (int i = 0; i < count.length; i++) {
            if (num+count[i] < 400) {
                num += count[i];
            } else {
                index = i;
            }
        }

        if (index > 2) {
            threshold = maxR - 6 * step - (index - 2) *step - (400 - num) / count[index] * step;
        } else {
            threshold = maxR - (index + 1) * 2 * step - (400 - num) / count[index] * step;
        }*/




        for(int j=0; j<response_norm.rows(); j++ )
        {
            for(int i=0; i<response_norm.cols(); i++ )
            {
                response_norm.get(j, i, data);
                if((int)data[0] > threshold)
                {
//                    Imgproc.circle(dst, new Point(i, j), 5,　new Scalar(0, 0, 255),
//                            2, 8, 0);
                    HarrisMatrix hm = new HarrisMatrix();
                    hm.setR(data[0]);
                    hm.setX(i);
                    hm.setY(j);
                    hmlist.add(hm);
                    Log.i("Harris Corner", "find corner point……");
                }
            }
        }

        src.release();
        gray.release();
        response.release();
        response_norm.release();


        int[] new_corner_x_y = new int[hmlist.size()*2];
        for (int i = 0; i < hmlist.size(); i++) {
            new_corner_x_y[2*i + 1] = hmlist.get(i).getX();
            new_corner_x_y[2*i] = hmlist.get(i).getY();
//                System.out.println(i+" "+temp.get(i).getX()+","+temp.get(i).getY());
        }

        return new_corner_x_y;

//        for (HarrisMatrix harrisMatrix:hmlist) {
//
//        }

        //响应降序排列
        /*System.out.println("-------------------------------------------------------");

        Collections.sort(hmlist, new Comparator<HarrisMatrix>() {

            @Override
            public int compare(HarrisMatrix h1, HarrisMatrix h2) {
                //降序
                if (h1.getR()-h2.getR() > 0) {
                    return -1;
                } else if (h1.getR()-h2.getR() < 0) {
                    return 1;
                } else {
                    return 0;
                }

                //return h1.compareTo(h2);
            }

        });


//        for (HarrisMatrix harrisMatrix:hmlist) {
//            if ()
//        }

        if (hmlist.size() > 3) {
            int[] new_corner_x_y = new int[6];
            for (int i = 0; i < 3; i++) {
                new_corner_x_y[2*i + 1] = hmlist.get(i).getX();
                new_corner_x_y[2*i] = hmlist.get(i).getY();
//                System.out.println(i+" "+temp.get(i).getX()+","+temp.get(i).getY());
            }

            return new_corner_x_y;
        } else if (hmlist.size() >0) {
            int[] new_corner_x_y = new int[2*hmlist.size()];
            for (int i = 0; i < hmlist.size(); i++) {
                new_corner_x_y[2*i + 1] = hmlist.get(i).getX();
                new_corner_x_y[2*i] = hmlist.get(i).getY();
//                System.out.println(i+" "+temp.get(i).getX()+","+temp.get(i).getY());
            }
            return new_corner_x_y;
        } else {
            return new int[2];
        }*/



    }

    /**
     * 区间内十等分逼近数量分割阈值
     * @param sortArray 有序数组，升序
     * @param saveNum 需要保留的较大数的数量
     * @param toltalNum 当前已经保留的数量
     * @param fromIndex 区间起点
     * @param toIndex 区间终点
     * @return 阈值所在的索引值、本次迭代后保留的数量、下一次迭代的区间起点索引值、下一次迭代的区间终点的索引值
     */
    private int[] indexOfThreshold(double[] sortArray, int saveNum, int toltalNum, int fromIndex, int toIndex) {
        float step = (float)(sortArray[toIndex] - sortArray[fromIndex]) / 10;
        double minNum = sortArray[fromIndex];
        int[] result = new int[4];

        out.println("step = "+step);
        out.println("sortArray[toIndex] = "+sortArray[toIndex]);
        out.println("sortArray[fromIndex] = "+sortArray[fromIndex]);

        int[] count = new int[10];
        int[] fromIndexes = new int[10];
        //int[] toIndexes = new int[10];

        for (int i = toIndex; i >= fromIndex; i--) {
            if (sortArray[i] > minNum+9*step) {
                count[0]++;
                if (count[0] == 1) {
                    fromIndexes[0] = i;
                }
            } else if (sortArray[i] > minNum+8*step) {
                count[1]++;
                if (count[1] == 1) {
                    fromIndexes[1] = i;
                }
            } else if (sortArray[i] > minNum+7*step) {
                count[2]++;
                if (count[2] == 1) {
                    fromIndexes[2] = i;
                }
            } else if (sortArray[i] > minNum+6*step) {
                count[3]++;
                if (count[3] == 1) {
                    fromIndexes[3] = i;
                }
            } else if (sortArray[i] > minNum+5*step) {
                count[4]++;
                if (count[4] == 1) {
                    fromIndexes[4] = i;
                }
            } else if (sortArray[i] > minNum+4*step) {
                count[5]++;
                if (count[5] == 1) {
                    fromIndexes[5] = i;
                }
            } else if (sortArray[i] > minNum+3*step) {
                count[6]++;
                if (count[6] == 1) {
                    fromIndexes[6] = i;
                }
            } else if (sortArray[i] > minNum+2*step) {
                count[7]++;
                if (count[7] == 1) {
                    fromIndexes[7] = i;
                }
            } else if (sortArray[i] > minNum+1*step) {
                count[8]++;
                if (count[8] == 1) {
                    fromIndexes[8] = i;
                }
            } else {
                count[9]++;
                if (count[9] == 1) {
                    fromIndexes[9] = i;
                }
            }
        }
        out.println("sortArray.length = "+sortArray.length);
        System.out.println("count = "+Arrays.toString(count));
        System.out.println("fromIndexes = "+Arrays.toString(fromIndexes));

        //int num = 0;
        int index = 0;
        //boolean flag = true; && flag//flag = false;
        /*while (toltalNum+count[index] < saveNum) {
            toltalNum += count[index];
            System.out.println("toltalNum = "+toltalNum);
            index++;
        }*/

        for (int i = 0; i < 10; i++) {
            if (toltalNum+count[i] < saveNum) {
                toltalNum += count[i];
                System.out.println("toltalNum = "+toltalNum);
            } else {
                index = i;
                //System.out.println("toltalNum = "+toltalNum);
                break;
            }
        }
        System.out.println("index = "+index);

        //达到总点数不大于1.5倍目标点数时返回当前点数大于目标点数，否则小于目标点数并准备下次迭代
        if (toltalNum + count[index] - saveNum < saveNum / 2) {
            result[0] = index;
            result[1] = toltalNum + count[index];

        } else {
            result[0] = index - 1;
            result[1] = toltalNum;
//            result[2] = fromIndexes[index] + 1;
//            result[3] = fromIndexes[index-1];
        }
        if (index == 9) {
            result[2] = 0;
        } else {
            result[2] = fromIndexes[index + 1] + 1;
        }

        result[3] = fromIndexes[index];

        System.out.println("result = "+Arrays.toString(result));
        return result;







        /*for(int j=0; j<response_norm.rows(); j++ )
        {
            for(int i=0; i<response_norm.cols(); i++ )
            {
                response_norm.get(j, i, data);
                if ((int)data[0] > 100+8*step) {
                    count[0]++;
                } else if ((int)data[0] > 100+6*step) {
                    count[1]++;
                } else if ((int)data[0] > 100+4*step) {
                    count[2]++;
                } else if ((int)data[0] > 100+3*step) {
                    count[3]++;
                } else if ((int)data[0] > 100+2*step) {
                    count[4]++;
                } else if ((int)data[0] > 100+step) {
                    count[5]++;
                } else if ((int)data[0] > 100) {
                    count[6]++;
                }
            }
        }*/
    }


}