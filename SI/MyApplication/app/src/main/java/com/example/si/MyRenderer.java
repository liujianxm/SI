package com.example.si;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.opengl.GLES10;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.example.si.IMG_PROCESSING.CornerDetection.HarrisCornerDetector;
import com.example.si.IMG_PROCESSING.CornerDetection.ImageMarker;
import com.example.si.RENDER.MyPattern2D;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.example.si.RENDER.BitmapRotation.getBitmapDegree;
import static com.example.si.RENDER.BitmapRotation.rotateBitmapByDegree;
import static com.example.si.MainActivity.getContext;
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

    private int mProgram;

    //    private boolean ispause = false;
    private float angle = 0f;
    private float angleX = 0.0f;
    private float angleY = 0.0f;
    private float angleZ = 0.0f;
    private int mTextureId;

    private int vol_w;
    private int vol_h;
    private int vol_d;
    private int[] sz = new int[3];
    private float[] mz = new float[3];
    private float[] mz_neuron = new float[3];
    private float[] mz_block = new float[6];

    private boolean multiImgFlag = false;

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

    private byte[] grayscale;
    private int data_length;
    private boolean isBig;

    private FileType fileType;
    private ByteBuffer mCaptureBuffer;
    private Bitmap mBitmap;
    private boolean isTakePic = false;
    private String mCapturePath;


    private boolean ifFileSupport = false;
    private boolean ifFileLoaded = false;
    private boolean ifLoadSWC = false;

    private boolean ifShowSWC = true;

    private Context context_myrenderer;

    private int degree = 0;

    public void switchImg() { multiImgFlag = !multiImgFlag; }

    public Bitmap GetBitmap() { return bitmap2D; }

    public void ResetImage(Bitmap new_image) {
        bitmap2D = new_image;
        myPattern2D = null;

        ifFileSupport = true;
    }

    //初次渲染画面
    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // Set the background frame color
        //深蓝
        GLES30.glClearColor(0.098f, 0.098f, 0.439f, 1.0f);

        Log.v("onSurfaceCreated:", "successfully");

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
//        GLES30.glClearColor(1f, 1f, 1f, 1.0f);
        //紫色
//        GLES30.glClearColor(0.192f, 0.105f, 0.572f, 1.0f);
        //浅蓝
//        GLES30.glClearColor(0.623f, 0.658f, 0.854f, 1.0f);
        //中蓝
        GLES30.glClearColor(121f / 255f, 134f / 255f, 203f / 255f, 1.0f);
        //浅紫
//        GLES30.glClearColor(0.929f, 0.906f, 0.965f, 1.0f);

        if (myPattern2D == null) {
            myPattern2D = new MyPattern2D(bitmap2D, sz[0], sz[1], mz);
        }

        //把颜色缓冲区设置为我们预设的颜色
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        GLES10.glEnable(GL_ALPHA_TEST);
        GLES30.glEnable(GL_BLEND);
        GLES30.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);


        setMatrix();


//        if (fileType == FileType.JPG || fileType == FileType.PNG)
            myPattern2D.draw(finalMatrix);


        GLES30.glDisable(GL_BLEND);
        GLES30.glDisable(GL_ALPHA_TEST);
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);

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
    public void SetPath(String message) {

        filepath = message;
        cur_scale = 1.0f;

        loadImage2D();
        ifFileLoaded = true;
        ifFileSupport = true;
        myPattern2D = null;

        if (fileType == FileType.PNG || fileType == FileType.JPG) {
            loadImage2D();
            ifFileLoaded = true;
            ifFileSupport = true;
        }

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


    //    @RequiresApi(api = Build.VERSION_CODES.N)
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void loadImage2D() {
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

            try {
                ParcelFileDescriptor parcelFileDescriptor =
                        getContext().getContentResolver().openFileDescriptor(uri, "r");

                is = new ParcelFileDescriptor.AutoCloseInputStream(parcelFileDescriptor);

                length = (int) parcelFileDescriptor.getStatSize();

                fd = parcelFileDescriptor.getFileDescriptor();

                Log.v("MyPattern", "Successfully load intensity");

            } catch (Exception e) {
                Log.v("MyPattern", "Some problems in the MyPattern when load intensity");
            }
        }

        bitmap2D = BitmapFactory.decodeStream(is);

        if (file.exists()) {
            degree = getBitmapDegree(filepath);
        }else {
            degree = getBitmapDegree(fd);
        }
//        ByteArrayOutputStream st = new ByteArrayOutputStream();
        if (bitmap2D != null) {

//            int degree = 0;
//            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
//                degree = getBitmapDegree(is);
//            }
//            else {
//                degree = getBitmapDegree(filepath);
//            }
            System.out.println(degree);

            bitmap2D = rotateBitmapByDegree(bitmap2D, degree);
//            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(bitmap.getByteCount());
            sz[0] = bitmap2D.getWidth();
            sz[1] = bitmap2D.getHeight();
            sz[2] = Math.max(sz[0], sz[1]);

            Integer[] num = {sz[0], sz[1]};
            float max_dim = (float) Collections.max(Arrays.asList(num));
            Log.v("MyRenderer", Float.toString(max_dim));

            mz[0] = (float) sz[0] / max_dim;
            mz[1] = (float) sz[1] / max_dim;
            mz[2] = Math.max(mz[0], mz[1]);

        }

    }


    public void rotate(float dx, float dy, float dis) {

        angleX = dy * 30;
        angleY = dx * 30;
        Matrix.setRotateM(rotationXMatrix, 0, angleX, 1.0f, 0.0f, 0.0f);
        Matrix.setRotateM(rotationYMatrix, 0, angleY, 0.0f, 1.0f, 0.0f);
        float[] curRotationMatrix = new float[16];
        Matrix.multiplyMM(curRotationMatrix, 0, rotationXMatrix, 0, rotationYMatrix, 0);
        Matrix.multiplyMM(rotationMatrix, 0, curRotationMatrix, 0, rotationMatrix, 0);

//        Log.v("angleX = ", Float.toString(angleX));
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
            System.out.println(result[0]);
            System.out.println(result[1]);
            return result;
        }
        return null;
    }

    /*
    public void add2DMarker(float x, float y) throws CloneNotSupportedException {
        float [] new_marker = solve2DMarker(x, y);
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

            ArrayList<ImageMarker> tempMarkerList = (ArrayList<ImageMarker>)MarkerList.clone();
            V_NeuronSWC_list tempCurveList = curSwcList.clone();

            if (curUndo < UNDO_LIMIT){
                curUndo += 1;
                undoMarkerList.add(tempMarkerList);
                undoCurveList.add(tempCurveList);
            } else {
                undoMarkerList.remove(0);
                undoCurveList.remove(0);
                undoMarkerList.add(tempMarkerList);
                undoCurveList.add(tempCurveList);
            }

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

*/

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

    //除法运算
    private void devideByw(float[] x){
        if(Math.abs(x[3]) < 0.000001f){
            Log.v("devideByw","can not be devided by 0");
            return;
        }

        for(int i=0; i<3; i++)
            x[i] = x[i]/x[3];

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

    public void corner_detection() {

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


        File file = new File(filepath);
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

        System.out.println("ssssss");
        System.out.println(options2.inSampleSize);
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
//        Point[] pCorners = corners.toArray();

        Bitmap destImage;
//        Bitmap sourceImage = bitmap2D;
        Bitmap sourceImage = image;
        HarrisCornerDetector filter = new HarrisCornerDetector();
        destImage = filter.filter(sourceImage, null);

        int[] corner_x_y = filter.corner_xy;
        int[] corner_x = new int[corner_x_y.length/2];
        int[] corner_y = new int[corner_x_y.length/2];

        System.out.println("LLLLLLLLLLLLLL");
        System.out.println(corner_x_y.length);
        System.out.println("xyxyxyxyxy");
        for (int n=0;n<corner_x_y.length/2;n++)
        {
            corner_x[n]=corner_x_y[2*n+1];
            corner_y[n]=corner_x_y[2*n];
            System.out.println(corner_x[n]);
            System.out.println(corner_y[n]);
        }


//        System.out.println(pCorners.length);


//        int power = (int) (Math.log((double) sampleRatio) / Math.log(2));
//        int actual_ratio = (int) Math.pow(2, power);
//        if (actual_ratio>2){
//            actual_ratio+=2;
//        }

        int actual_ratio = options2.inSampleSize;

        System.out.println("aaaaaaaaa");
        System.out.println(actual_ratio);
        for (int i = 0; i < corner_x_y.length/2; i++) {

            if (corner_x[i]==0&&corner_y[i]==0);
            else{
                ImageMarker imageMarker_drawed = new ImageMarker((float) corner_x[i] * actual_ratio,
                        (float) corner_y[i] * actual_ratio,
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

    }

}