package com.example.si.RENDER;

import android.opengl.GLES10;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import com.example.si.IMG_PROCESSING.CornerDetection.ImageMarker;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public class MyDraw {

    public final static float[][] colormap = {
            {0f,0f,0f},
            {1f,1f,1f},
            {1f,0f,0f},
            {0f,0f,1f},
            {0f,1f,0f},
            {1f,0f,1f},
            {1f,1f,0f}
    } ;

    float n = 100;
//    final float radius = 0.1f;
//    final float radius = 0.02f;
    final float splitRadius = 0.005f;

    private final int mProgram_marker;
    private final int mProgram_line;
    private final int mProgram_points;
    private final int mProgram_Cylinder;

    private FloatBuffer vertexBuffer_marker;
    private FloatBuffer normalizeBuffer_marker;
    private FloatBuffer normalizeBuffer_marker_small;
    private FloatBuffer vertexBuffer_line;
    private FloatBuffer vertexBuffer_circle;
    private FloatBuffer colorBuffer_marker;
    private FloatBuffer colorBuffer_line;
    private FloatBuffer colorBuffer_circle;
    private FloatBuffer vertexBuffer_points;
    private FloatBuffer circle1Buffer;
    private FloatBuffer circle2Buffer;
    private FloatBuffer cylinderSideBuffer;

    float[] normalMatrix = new float[16];
    float[] normalMatrix_before = new float[16];

    private float[] normalizePoints_marker;
    private float[] normalizePoints_marker_small;
    private float[] vertexPoints_marker;
    private float[] colorPoints_marker;

    private int vertexPoints_handle = 0;
    private int normalizePoints_handle = 1;
    private int colorPoints_handle = 2;


//    private float[] normalizePoints_marker = createNormlizes();

    private final String vertexShaderCode_line =
            // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            "#version 300 es\n" +
                    "layout (location = 0) in vec4 vPosition;" +
//                    "layout (location = 1) in vec3 aVertexNormal;" +
                    "layout (location = 2) in vec4 vColor;" +
                    "uniform mat4 uMVPMatrix;" +
                    "out vec4 vOutColor;" +
                    "void main() {" +
                    "    gl_Position = uMVPMatrix * vPosition;" +
//                    "  gl_Position = vec4((uMVPMatrix * vPosition).xy, -0.99999, 1.0);" +
                    "    vOutColor = vColor;" +
                    "}";



    private final String fragmentShaderCode_line =
            "#version 300 es\n" +
                    "precision mediump float;" +
                    "in vec4 vOutColor;" +
                    "out vec4 FragColor;" +
                    "void main() {" +
//                    "  FragColor = vec4(1.0, 0.0, 0.0, 1.0);" +
                    "  FragColor = vec4(vOutColor.rgb,1.0);" +
                    "}";




    private final String vertexShaderCode_marker =
            // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            "#version 300 es\n" +
                    "layout (location = 0) in vec4 vPosition;" +
                    "layout (location = 1) in vec3 aVertexNormal;" +
                    "layout (location = 2) in vec4 vColor;"+

                    "uniform mat4 uNormalMatrix;" +
                    "uniform mat4 uMVPMatrix;" +

                    "out vec3 vLighting;" +
                    "out vec4 vOutColor;" +

                    "void main() {" +
                    "    gl_Position = uMVPMatrix * vPosition;" +

                    "    vec3 uAmbientColor = vec3(0.2, 0.2, 0.2);\n" +
                    "    vec3 uDirectionalColor = vec3(0.8, 0.8, 0.8);\n" +
                    "    vec3 directionalVector = normalize(vec3(-1.0, 1.0, -1.0));\n" +
                    "    vec3 transformedNormal = (uNormalMatrix * vec4(aVertexNormal, 1.0)).xyz;\n" +
                    "    float directionalLightWeighting = max(dot(transformedNormal, directionalVector), 0.0);\n" +
                    "    vLighting = uAmbientColor + uDirectionalColor * directionalLightWeighting;" +
                    "    vOutColor = vColor;"+
                    "}";



    private final String fragmentShaderCode_maeker =
            "#version 300 es\n" +
                    "precision mediump float;" +

                    "in vec4 vOutColor;" +
                    "in vec3 vLighting;" +
                    "out vec4 FragColor;" +

                    "void main() {" +
//                    "  vec4 markerColor = vec4(0.29, 0.13, 0.36, 1.0);" +
//                    "  vec4 markerColor = vec4(1.0, 0.0, 0.0, 1.0);" +
                    "  vec4 markerColor = vOutColor;"+
                    "  FragColor = vec4(markerColor.rgb * vLighting, 1.0);" +
                    "}";

    //draw points
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private final String vertexShaderCode_points =
            "#version 300 es\n" +
                    "layout (location = 0) in vec4 vPosition;\n" +
                    "void main() {\n" +
                    "     gl_Position  = vPosition;\n" + //
                    "     gl_PointSize = 20.0;\n" +
                    "}\n";


    private final String fragmentShaderCode_points =
            "#version 300 es\n" +
                    "precision mediump float;\n" +
                    "out vec4 fragColor;\n" +
                    "void main() {\n" +
                    "if (length(gl_PointCoord  - vec2(0.5)) > 0.5) {\n" +
                    "        discard;\n" +
                    "    }" +
                    "     fragColor = vec4(1.0,1.0,1.0,1.0);\n" +
                    "}\n";

    //draw cylinders
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private final String vertexShaderCode_Cylinder =
            "#version 300 es\n" +
                    "layout (location = 0) in vec4 vPosition;" +
//                    "layout (location = 1) out vec4 vColor;" +
//                    "in vec4 aColor;" +
                    "uniform mat4 uMVPMatrix;" +
                    "uniform mat4 uLocalFinalMatrix;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "  gl_Position = uMVPMatrix * uLocalFinalMatrix * vPosition;" +
//                    "  gl_PointSize = 10.0;" +
//                    "  vColor = aColor;" +
                    "}";

    private final String fragmentShaderCode_Cylinder =
            "#version 300 es\n" +
                    "precision mediump float;"+
                    "uniform vec4 vColor;" +
                    "out vec4 fragColor;"+
                    "void main() {"+
//                    "fragColor = vec4(1.0,1.0,1.0,1.0);"+
                    "fragColor = vColor;"+
                    "}";


    public MyDraw(){

        //for the marker
        mProgram_marker = initProgram(vertexShaderCode_marker, fragmentShaderCode_maeker);
        Log.v("MyDraw", "init the mProgram_marker");

        //for the line
        mProgram_line = initProgram(vertexShaderCode_line, fragmentShaderCode_line);
        Log.v("MyDraw", "init the mProgram_line");

        //for the points
        mProgram_points = initProgram(vertexShaderCode_points, fragmentShaderCode_points);
        Log.v("MyDraw", "init the mProgram_points");

        mProgram_Cylinder = initProgram(vertexShaderCode_Cylinder, fragmentShaderCode_Cylinder);
        Log.v("MyDraw", "init the mProgram_Cylinder");

        BufferSet_Normalize();

    }

    private void BufferSet_Cylinder(cylinder mcylinder) {
        circle1Buffer = allocateFloatBuffer(mcylinder.circle1);
        circle2Buffer = allocateFloatBuffer(mcylinder.circle2);
        cylinderSideBuffer = allocateFloatBuffer(mcylinder.cylinderSide);
    }

    private void BufferSet_Marker(float x, float y, float z, int type, float r){

        vertexPoints_marker = createPositions(x, y, z, r);

        // for the marker
        //分配内存空间,每个浮点型占4字节空间
        vertexBuffer_marker = ByteBuffer.allocateDirect(vertexPoints_marker.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        //传入指定的坐标数据
        vertexBuffer_marker.put(vertexPoints_marker);
        vertexBuffer_marker.position(0);

        colorPoints_marker = new float[vertexPoints_marker.length];//colormap[type];
        for(int i=0; i<colorPoints_marker.length; i++){
            colorPoints_marker[i] = colormap[type%7][i%3];
        }
        colorBuffer_marker = ByteBuffer.allocateDirect(colorPoints_marker.length*4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        colorBuffer_marker.put(colorPoints_marker);
        colorBuffer_marker.position(0);

    }


    private void BufferSet_Normalize(){

        normalizePoints_marker = createNormlizes(4.0f);

        // for the marker
        //分配内存空间,每个浮点型占4字节空间
        normalizeBuffer_marker = ByteBuffer.allocateDirect(normalizePoints_marker.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        //传入指定的坐标数据
        normalizeBuffer_marker.put(normalizePoints_marker);
        normalizeBuffer_marker.position(0);
        normalizePoints_marker_small = createNormlizes(6.0f);

        // for the marker
        //分配内存空间,每个浮点型占4字节空间
        normalizeBuffer_marker_small = ByteBuffer.allocateDirect(normalizePoints_marker_small.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        //传入指定的坐标数据
        normalizeBuffer_marker_small.put(normalizePoints_marker_small);
        normalizeBuffer_marker_small.position(0);

    }

    private void BufferSet_Points(float [] vertexPoints_points){
        //分配内存空间,每个浮点型占4字节空间
        vertexBuffer_points = ByteBuffer.allocateDirect(vertexPoints_points.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        //传入指定的坐标数据
        vertexBuffer_points.put(vertexPoints_points);
        vertexBuffer_points.position(0);
    }


    private void BufferSet_Line(float [] line, int type){
        vertexBuffer_line = ByteBuffer.allocateDirect(line.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer_line.put(line);
        vertexBuffer_line.position(0);

        colorPoints_marker = new float[line.length];
        for(int i=0; i<colorPoints_marker.length; i++){
            colorPoints_marker[i] = colormap[type%7][i%3];
//            System.out.println(colorPoints_marker[i]);
        }
        colorBuffer_line = ByteBuffer.allocateDirect(colorPoints_marker.length* 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        colorBuffer_line.put(colorPoints_marker);
        colorBuffer_line.position(0);


    }





//    public void drawMarker(float[] mvpMatrix, float x, float y, float z){
//
//        BufferSet_Marker(x, y, z);
//
//        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
//
//        GLES30.glUseProgram(mProgram_marker);
//
////        Matrix.invertM(normalMatrix_before,0,mvpMatrix,0);
////
////        Matrix.transposeM(normalMatrix,0,normalMatrix_before,0);
//
//        //准备坐标数据
//        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer_marker);
//        //启用顶点的句柄
//        GLES30.glEnableVertexAttribArray(0);
//
////        //准备f法向量数据
////        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, normalizeBuffer_marker);
////        //启用顶点的句柄
////        GLES30.glEnableVertexAttribArray(1);
//
//        // get handle to vertex shader's uMVPMatrix member
//        int vPMatrixHandle_marker = GLES30.glGetUniformLocation(mProgram_marker,"uMVPMatrix");
//
//        // Pass the projection and view transformation to the shader
//        GLES30.glUniformMatrix4fv(vPMatrixHandle_marker, 1, false, mvpMatrix, 0);
//
//        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, vertexPoints_marker.length/3);
//
//        //禁止顶点数组的句柄
//        GLES30.glDisableVertexAttribArray(0);
//
//        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
//
//    }


    /**
     * 三维空间中画圆柱
     * @param mMVPMatrix 渲染变换矩阵
     * @param x1 圆柱空间起点x
     * @param y1 圆柱空间起点y
     * @param z1 圆柱空间起点z
     * @param x2 圆柱空间终点x
     * @param y2 圆柱空间终点y
     * @param z2 圆柱空间终点z
     * @param radius 圆柱半径
     */
    public void drawCylinder(float[] mMVPMatrix, float x1, float y1, float z1, float x2, float y2, float z2, int type, float radius) {
        //生成标准圆柱体
        float dis = (float) computeDis(x1, y1, z1, x2, y2, z2);
        cylinder mcylinder = new cylinder(dis/2f, -dis/2f, radius);
        BufferSet_Cylinder(mcylinder);
        float[] localFinalMatrix = getLocalFinalMatrix_Cylinder(x1, y1, z1, x2, y2, z2);
        float[] color = new float[4];
        color[0] = colormap[type%7][0];
        color[1] = colormap[type%7][1];
        color[2] = colormap[type%7][2];
        color[3] = 1f;

        GLES30.glUseProgram(mProgram_Cylinder);

        //设置投影矩阵
        int mMVPMatrixHandle = GLES30.glGetUniformLocation(mProgram_Cylinder, "uMVPMatrix");
        GLES30.glUniformMatrix4fv(mMVPMatrixHandle,1,false, mMVPMatrix,0);
        System.out.println("mMVPMatrixHandle = "+mMVPMatrixHandle);

        //设置旋转平移矩阵
        int mLocalFinalMatrixHandle = GLES30.glGetUniformLocation(mProgram_Cylinder, "uLocalFinalMatrix");
        GLES30.glUniformMatrix4fv(mLocalFinalMatrixHandle,1,false, localFinalMatrix,0);
        System.out.println("mLocalFinalMatrixHandle = "+mLocalFinalMatrixHandle);

        //设置颜色
        int mColorHandle = GLES30.glGetUniformLocation(mProgram_Cylinder, "vColor");
        GLES30.glEnableVertexAttribArray(mColorHandle);
        GLES30.glUniform4fv(mColorHandle, 1, color, 0);
        System.out.println("mColorHandle = "+mColorHandle);

        GLES30.glEnableVertexAttribArray(vertexPoints_handle);

        //画上圆，终点的圆，（x2,y2,z2）
        GLES30.glVertexAttribPointer(vertexPoints_handle, 3, GLES30.GL_FLOAT, false, 0, circle1Buffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, mcylinder.circle1.size()/3);
        //画柱面
        GLES30.glVertexAttribPointer(vertexPoints_handle, 3, GLES30.GL_FLOAT, false, 0, cylinderSideBuffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, mcylinder.cylinderSide.size()/3);
        //画下圆，起点的圆，（x1,y1,z1）
//        GLES30.glUniform4fv(mColorHandle, 1, color1, 0);
        GLES30.glVertexAttribPointer(vertexPoints_handle, 3, GLES30.GL_FLOAT, false, 0, circle2Buffer);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, mcylinder.circle2.size()/3);

        GLES30.glDisableVertexAttribArray(vertexPoints_handle);
        GLES30.glDisableVertexAttribArray(mColorHandle);
        GLES30.glDisableVertexAttribArray(mMVPMatrixHandle);
        GLES30.glDisableVertexAttribArray(mLocalFinalMatrixHandle);
    }

    public void drawMarker(float[] mvpMatrix, float[] modelMatrix, float x, float y, float z, int type, float radius){
//        System.out.println("set marker");



        BufferSet_Marker(x, y, z, type, radius);

//        System.out.println("set marker end");

        GLES30.glDisable(GLES30.GL_DEPTH_TEST);

        GLES30.glUseProgram(mProgram_marker);


        //准备坐标数据
        GLES30.glVertexAttribPointer(vertexPoints_handle, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer_marker);
        //启用顶点的句柄
        GLES30.glEnableVertexAttribArray(vertexPoints_handle);

        if (radius < 0.015f){
            //准备f法向量数据
            GLES30.glVertexAttribPointer(normalizePoints_handle, 3, GLES30.GL_FLOAT, false, 0, normalizeBuffer_marker_small);
        }else {
            //准备f法向量数据
            GLES30.glVertexAttribPointer(normalizePoints_handle, 3, GLES30.GL_FLOAT, false, 0, normalizeBuffer_marker);
        }
        //准备f法向量数据
       // GLES30.glVertexAttribPointer(normalizePoints_handle, 3, GLES30.GL_FLOAT, false, 0, normalizeBuffer_marker);
        //启用顶点的句柄
        GLES30.glEnableVertexAttribArray(normalizePoints_handle);

        //准备颜色数据
        GLES30.glVertexAttribPointer(colorPoints_handle,3,GLES30.GL_FLOAT, false, 0,colorBuffer_marker);
        GLES30.glEnableVertexAttribArray(colorPoints_handle);




        // get handle to vertex shader's uMVPMatrix member
        int vPMatrixHandle_marker = GLES30.glGetUniformLocation(mProgram_marker,"uMVPMatrix");

        // Pass the projection and view transformation to the shader
        GLES30.glUniformMatrix4fv(vPMatrixHandle_marker, 1, false, mvpMatrix, 0);


        // get handle to vertex shader's uMVPMatrix member
        int normalizeMatrixHandle_marker = GLES30.glGetUniformLocation(mProgram_marker,"uNormalMatrix");

        // Pass the projection and view transformation to the shader
        GLES30.glUniformMatrix4fv(normalizeMatrixHandle_marker, 1, false, modelMatrix, 0);


        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, vertexPoints_marker.length/3);

//        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, vertexPoints_marker.length/3);


        //禁止顶点数组的句柄
        GLES30.glDisableVertexAttribArray(vertexPoints_handle);
        GLES30.glDisableVertexAttribArray(normalizePoints_handle);

        GLES30.glDisableVertexAttribArray(colorPoints_handle);


        GLES30.glEnable(GLES30.GL_DEPTH_TEST);

    }

    ////////////////////////////////////////

    public void drawRing2D(float [] mvpMatrix, int type, float x, float y, float z, float radius) {

        float step1 = (float)(2*Math.PI/360);
        ArrayList<Float> circle = new ArrayList<Float>();
//        float step2 = 0.2f*radius/20;
//        for (float j = 0.8f*radius; j < radius; j += step2) {
//            for (int i = 0; i < 360; i += 3) {
//                circle.add ((float)(x + radius*Math.cos(i*step1)));
//                circle.add((float)(y + radius*Math.sin(i*step1)));
//                circle.add(z);//z为0
//            }
//
//            //画线的方式画圆
//            drawLine(mvpMatrix, circle, type);
//        }
        for (int i = 0; i < 360; i += 3) {
            circle.add ((float)(x + radius*Math.cos(i*step1)));
            circle.add((float)(y + radius*Math.sin(i*step1)));
            circle.add(z);//z为0
        }

        //画线的方式画圆
        drawLine(mvpMatrix, circle, type);

    }

    ////////////////////////////////////////

    ////////////////////////////////////////

    public void drawLine(float [] mvpMatrix, ArrayList<Float> lineDrawed, int type){
        float [] line = new float[lineDrawed.size()];
//        System.out.println("line size in 302: "+line.length +" "+ lineDrawed.size());
        for (int i = 0; i < lineDrawed.size(); i++){
            line[i] = lineDrawed.get(i);
        }
        BufferSet_Line(line, type);
//        System.out.println("set end-----------");

        GLES30.glDisable(GLES30.GL_DEPTH_TEST);

        GLES30.glUseProgram(mProgram_line);

        //准备坐标数据
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer_line);
        //启用顶点的句柄
        GLES30.glEnableVertexAttribArray(0);

        //准备颜色数据
        GLES30.glVertexAttribPointer(colorPoints_handle, 3, GLES30.GL_FLOAT, false, 0, colorBuffer_line);
        //启用颜色的句柄
        GLES30.glEnableVertexAttribArray(colorPoints_handle);

        // get handle to vertex shader's uMVPMatrix member
        int vPMatrixHandle_marker = GLES30.glGetUniformLocation(mProgram_line,"uMVPMatrix");

        // Pass the projection and view transformation to the shader
        GLES30.glUniformMatrix4fv(vPMatrixHandle_marker, 1, false, mvpMatrix, 0);

        GLES30.glLineWidth(4);

        GLES30.glDrawArrays(GLES30.GL_LINE_LOOP, 0, line.length/3);

        //禁止顶点数组的句柄
        GLES30.glDisableVertexAttribArray(0);

        //禁止颜色的句柄
        GLES30.glDisableVertexAttribArray(2);

        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
    }

    public void drawPoints(float []  linePoints, int num){
        GLES30.glUseProgram(mProgram_points);

        BufferSet_Points(linePoints);

        //准备坐标数据
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer_points);
        //启用顶点的句柄
        GLES30.glEnableVertexAttribArray(0);

        //绘制三个点
        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, num);

        //绘制直线
//        GLES30.glDrawArrays(GLES30.GL_LINE_STRIP, 0, 2);
//        GLES30.glLineWidth(10);

//        //绘制三角形
//        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3);

        //禁止顶点数组的句柄
        GLES30.glDisableVertexAttribArray(0);
    }

    public void drawSplitPoints(float [] mvpMatrix, float x, float y, float z, int type){
//        float [] line = new float[splitPoints.size()];
////        System.out.println("line size in 302: "+line.length +" "+ lineDrawed.size());
//        for (int i = 0; i < splitPoints.size(); i++){
//            line[i] = splitPoints.get(i);
//        }
        BufferSet_Marker(x, y, z, type, splitRadius);
//        System.out.println("set end-----------");

        GLES30.glDisable(GLES30.GL_DEPTH_TEST);

        GLES30.glDisable(GLES30.GL_DEPTH_TEST);

        GLES30.glUseProgram(mProgram_line);

        //准备坐标数据
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer_marker);
        //启用顶点的句柄
        GLES30.glEnableVertexAttribArray(0);

        //准备颜色数据
        GLES30.glVertexAttribPointer(colorPoints_handle, 3, GLES30.GL_FLOAT, false, 0, colorBuffer_marker);
        //启用颜色的句柄
        GLES30.glEnableVertexAttribArray(colorPoints_handle);

        // get handle to vertex shader's uMVPMatrix member
        int vPMatrixHandle_marker = GLES30.glGetUniformLocation(mProgram_line,"uMVPMatrix");

        // Pass the projection and view transformation to the shader
        GLES30.glUniformMatrix4fv(vPMatrixHandle_marker, 1, false, mvpMatrix, 0);

        GLES30.glLineWidth(3);

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, vertexPoints_marker.length/3);

        //禁止顶点数组的句柄
        GLES30.glDisableVertexAttribArray(0);

        //禁止颜色的句柄
        GLES30.glDisableVertexAttribArray(2);

        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
    }

//    public void drawEswc(float [] mvpMatrix, ArrayList<Float> lineDrawed){
//        float [] line = new float[lineDrawed.size()];
//        for (int i = 0; i < lineDrawed.size(); i++){
//            line[i] = lineDrawed.get(i);
//        }
//        BufferSet_Line(line);
//
//        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
//
//        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
//
//        GLES30.glUseProgram(mProgram_line);
//
//        //准备坐标数据
//        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer_line);
//        //启用顶点的句柄
//        GLES30.glEnableVertexAttribArray(0);
//
//        // get handle to vertex shader's uMVPMatrix member
//        int vPMatrixHandle_marker = GLES30.glGetUniformLocation(mProgram_line,"uMVPMatrix");
//
//        // Pass the projection and view transformation to the shader
//        GLES30.glUniformMatrix4fv(vPMatrixHandle_marker, 1, false, mvpMatrix, 0);
//
//        GLES30.glLineWidth(3);
//
//        GLES30.glDrawArrays(GLES30.GL_LINES, 0, line.length/3);
//
//        //禁止顶点数组的句柄
//        GLES30.glDisableVertexAttribArray(0);
//
//        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
//    }


    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //创建着色器程序
    private int initProgram(String vertShaderCode, String fragmShaderCode){

        //加载着色器
        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER,
                vertShaderCode);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER,
                fragmShaderCode);

        Log.v("vertexShader", Integer.toString(vertexShader));

        Log.v("fragmentShader", Integer.toString(fragmentShader));


        // create empty OpenGL ES Program
        int mProgram = GLES30.glCreateProgram();

        // add the vertex shader to program
        GLES30.glAttachShader(mProgram, vertexShader);

        // add the fragment shader to program
        GLES30.glAttachShader(mProgram, fragmentShader);

        // creates OpenGL ES program executables
        GLES30.glLinkProgram(mProgram);


        GLES30.glValidateProgram(mProgram); // 让OpenGL来验证一下我们的shader program，并获取验证的状态
        int[] status = new int[1];
        GLES30.glGetProgramiv(mProgram, GLES30.GL_VALIDATE_STATUS, status, 0);                                  // 获取验证的状态
        Log.d("Program:", "validate shader----program: " + GLES30.glGetProgramInfoLog(mProgram));

        return mProgram;

    }





    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //加载着色器
    private static int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES30.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES30.GL_FRAGMENT_SHADER)
        int shader = GLES30.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);

        int[] status = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0);// 获取验证的状态
//        if (status[0] == 0) {
//            String error = GLES30.glGetShaderInfoLog(shader);
//            GLES30.glDeleteShader(shader);
//            Log.v("Error:", "validate shader program: " + error);
//        }
        Log.v("Error:", "validate shader program: " + GLES30.glGetShaderInfoLog(shader));


        return shader;
    }



    private float[]  createPositions(float x, float y, float z, float r){

////        float x, y, z;
//        x = 0.5f;  y = 0.1f;  z = 0.2f;
/*
        float step = 6.0f;

        if(r > 0.011f){
            step = 2.0f;
        }
*/
//        Log.v("createPositions ", Float.toString(r));
        int step = 4;

        if(r <= 0.01f){
            step = 6;
        }

        Log.v("createPositions:", "step: " + step);

        int count = 0;
        ArrayList<Float> data=new ArrayList<>();
        float r1,r2;
        float h1,h2;
        float sin,cos;
        for(int i=-90;i<90+step;i+=step){
            r1 = (float)Math.cos(i * Math.PI / 180.0)          * r;
            r2 = (float)Math.cos((i + step) * Math.PI / 180.0) * r;
            h1 = (float)Math.sin(i * Math.PI / 180.0)          * r;
            h2 = (float)Math.sin((i + step) * Math.PI / 180.0) * r;
            // 固定纬度, 360 度旋转遍历一条纬线
            float step2=step*2;
            for (int j = 0; j < 360 + step; j +=step2 ) {
                cos = (float) Math.cos(j * Math.PI / 180.0);
                sin = -(float) Math.sin(j * Math.PI / 180.0);

                data.add(r2 * cos + x);
                data.add(h2       + y);
                data.add(r2 * sin + z);

                data.add(r1 * cos + x);
                data.add(h1       + y);
                data.add(r1 * sin + z);

                count++;
//                System.out.println("Count: " + count);
            }
        }
        float[] f=new float[data.size()];
        for(int i=0;i<f.length;i++){
            f[i]=data.get(i);
        }

        return f;
    }



    private float[]  createNormlizes(float step_input){

        float step = step_input;
        ArrayList<Float> normlizes=new ArrayList<>();
        float r1,r2;
        float h1,h2;
        float sin,cos;
        for(float i=-90;i<90+step;i+=step){
            r1 = (float)Math.cos(i * Math.PI / 180.0)         ;
            r2 = (float)Math.cos((i + step) * Math.PI / 180.0);
            h1 = (float)Math.sin(i * Math.PI / 180.0)         ;
            h2 = (float)Math.sin((i + step) * Math.PI / 180.0);
            // 固定纬度, 360 度旋转遍历一条纬线
            float step2=step*2;
            for (float j = 0.0f; j <360.0f+step; j +=step2 ) {
                cos = (float) Math.cos(j * Math.PI / 180.0);
                sin = -(float) Math.sin(j * Math.PI / 180.0);

                normlizes.add(r2 * cos + 0.5f);
                normlizes.add(h2       + 0.5f);
                normlizes.add(r2 * sin + 0.5f);

                normlizes.add(r1 * cos + 0.5f);
                normlizes.add(h1       + 0.5f);
                normlizes.add(r1 * sin + 0.5f);
            }
        }
        float[] f=new float[normlizes.size()];
        for(int i=0;i<f.length;i++){
            f[i]=normlizes.get(i);
        }

        return f;
    }

    private double computeDis(float x1, float y1, float z1, float x2, float y2, float z2) {
        return sqrt(pow((x2 - x1), 2) + pow((y2 - y1), 2) + pow((z2 - z1), 2));
    }



    private float[] getLocalFinalMatrix_Cylinder(float x1, float y1, float z1, float x2, float y2, float z2) {
        //先旋转
        float[] rotationMatrix = new float[16];
        //中轴向量 n = (nx, ny, nz)，旋转法向量 m = n x z0 = (nx, -ny, 0)
        float nx = x2 - x1;
        float ny = y2 - y1;
        float nz = z2 - z1;
        double norm = Math.sqrt(nx * nx + ny * ny + nz * nz);
        float angle = (float) (Math.acos(nz / norm) / Math.PI * 180);
//        System.out.println("angleZ = "+angle);
        Matrix.setRotateM(rotationMatrix, 0, angle, ny, -nx, 0f);

        float[] ZFromCenter = {0, 0, 1f, 1f};
        Matrix.multiplyMV(ZFromCenter, 0, rotationMatrix, 0, ZFromCenter, 0);
//        System.out.println("n : ("+nx+","+ny+","+nz+")");
//        System.out.println("ZFromCenter : "+ Arrays.toString(ZFromCenter));
        if (nx/ZFromCenter[0] != ny/ZFromCenter[1] || nx/ZFromCenter[0] != nz*ZFromCenter[2]) {
            Matrix.setRotateM(rotationMatrix, 0, -angle, ny, -nx, 0f);
        }


        //再平移
        float[] translateMatrix = new float[16];
        Matrix.setIdentityM(translateMatrix,0);
        Matrix.translateM(translateMatrix, 0, (x1 + x2) / 2, (y1 + y2) / 2, (z1 + z2) / 2);

        float[] localFinalMatrix = new float[16];
        Matrix.setIdentityM(localFinalMatrix,0);
        Matrix.multiplyMM(localFinalMatrix, 0, translateMatrix, 0, rotationMatrix, 0);

        return localFinalMatrix;
    }

    private FloatBuffer allocateFloatBuffer(ArrayList<Float> points) {
        float[] Points = new float[points.size()];
        for (int i = 0; i < points.size(); i++) {
            Points[i] = points.get(i);
        }

        FloatBuffer bb = ByteBuffer.allocateDirect(Points.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        bb.put(Points).position(0);

        return bb;
    }

    private class cylinder {
        ArrayList<Float> circle1 = new ArrayList<Float>();
        ArrayList<Float> circle2 = new ArrayList<Float>();
        ArrayList<Float> cylinderSide = new ArrayList<Float>();

        private void createCirclePoints(float z1, float z2, float radius) {
            //加入圆心点坐标
            circle1.add(0f);
            circle1.add(0f);
            circle1.add(z1);
            circle2.add(0f);
            circle2.add(0f);
            circle2.add(z2);
            //加入圆周点坐标
            int step = 2;
            float radianPerDegree = (float)(2*Math.PI/360);
            float x,y;
            for (int i = 0; i <= 360; i += step) {
                x = (float) (radius*Math.cos(i*radianPerDegree));
                y = (float) (radius*Math.sin(i*radianPerDegree));
                circle1.add(x);
                circle1.add(y);
                circle1.add(z1);

                circle2.add(x);
                circle2.add(y);
                circle2.add(z2);
            }
        }

        private void createCylinderSidePoints(float z1, float z2, float radius) {

            int step = 2;
            float radianPerDegree = (float)(2*Math.PI/360);
            float x,y;

            for (int i = 0; i <= 360; i += step) {
                x = (float) (radius*Math.cos(i*radianPerDegree));
                y = (float) (radius*Math.sin(i*radianPerDegree));

                cylinderSide.add(x);
                cylinderSide.add(y);
                cylinderSide.add(z1);

                cylinderSide.add(x);
                cylinderSide.add(y);
                cylinderSide.add(z2);
            }
        }

        public cylinder(float z1, float z2, float radius) {
            createCirclePoints(z1, z2, radius);
            createCirclePoints(z1, z2, radius);
            createCylinderSidePoints(z1, z2, radius);
        }
    }
}
