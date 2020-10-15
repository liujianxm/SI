package com.example.si.RENDER;

import android.opengl.GLES20;
import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Axises3D {
//无mvp矩阵
//    private final String vertexShaderCode =
//            "#version 300 es\n" +
//                    "layout (location = 0) in vec4 vPosition;" +
////                    "layout (location = 1) out vec4 vColor;" +
////                    "in vec4 aColor;" +
//                    "void main() {" +
//                    "  gl_Position = vPosition;" +
////                    "  gl_PointSize = 10.0;" +
////                    "  vColor = aColor;" +
//                    "}";
//
//    private final String fragmentShaderCode =
//            "#version 300 es\n" +
//                    "precision mediump float;"+
//                    "uniform vec4 vColor;" +
//                    "out vec4 fragColor;"+
//                    "void main() {"+
////                    "fragColor = vec4(1.0,1.0,1.0,1.0);"+
//                    "fragColor = vColor;"+
//                    "}";

    //有mvp矩阵
    private final String vertexShaderCode =
            "#version 300 es\n" +
                    "layout (location = 0) in vec4 vPosition;" +
//                    "layout (location = 1) out vec4 vColor;" +
//                    "in vec4 aColor;" +
                    "uniform mat4 uMVPMatrix;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
//                    "  gl_PointSize = 10.0;" +
//                    "  vColor = aColor;" +
                    "}";

    private final String fragmentShaderCode =
            "#version 300 es\n" +
                    "precision mediump float;"+
                    "uniform vec4 vColor;" +
                    "out vec4 fragColor;"+
                    "void main() {"+
//                    "fragColor = vec4(1.0,1.0,1.0,1.0);"+
                    "fragColor = vColor;"+
                    "}";

    private final int mProgram;

    FloatBuffer xyzVertexData;
    FloatBuffer xyzColorBuffer;

    ByteBuffer XFacetsBuffer;
    ByteBuffer YFacetsBuffer;
    ByteBuffer ZFacetsBuffer;

    private int mPositionHandle = 0;
    private int mColorHandle;
    private int mMVPMatrixHandle;

//    float[] verticesData = new float[] {
//
//    };

    float[] xyzVertices = new float[]{
            0f, 0f, 0f,//0 x起点，画坐标轴的
            0.6f, 0f, 0f,//1 X轴的终点
            0.5f, 0.1f, 0f,//2 X轴箭头1
            0.5f, -0.1f, 0f,//3 X轴箭头2

            0f, 0f, 0f,//Y轴起点
            0f, 0.6f, 0f,//Y轴终点
            0.1f ,0.5f ,0f,//Y轴箭头1
            -0.1f ,0.5f ,0f,//Y轴箭头2

            0f, 0f, 0f,//Z轴起点
            0f, 0f, 0.6f,//Z轴终点
            0f ,0.1f ,0.5f,//Z轴箭头1
            0f ,-0.1f ,0.5f//Z轴箭头2,

//            1.3f,0f,0f,//12 绘制字X
//            1.35f,0.1f,0f,//13
//            1.25f,0.1f,0f,//14
//            1.25f,-0.1f,0f,//15
//            1.35f,-0.1f,0f,//16
//
//            0f,1.4f,0f,//17 绘制字Y
//            0f,1.3f,0f,//18
//            0.05f,1.5f,0f,//19
//            -0.05f,1.5f,0f,//20
//
//            -0.05f ,0.05f ,1.25f,//21  绘制字Z
//            0.05f,0.05f,1.25f,//22
//            -0.05f,-0.05f,1.25f,//23
//            0.05f,-0.05f,1.25f,//24

    };

    //X坐标及其箭头
    byte[] XFacets = new byte[] {
            //起终点
            0,1,
            //箭头
            1,2,
            1,3//,
            //X
//            12,13,
//            12,14,
//            12,15,
//            12,16,

    };
    //Y坐标及其箭头
    byte[] YFacets = new byte[] {
            //起终点
            4,5,
            //箭头
            5,6,
            5,7//,
//            //字Y
//            17,18,
//            17,19,
//            17,20,


    };
    //Z坐标及其箭头
    byte[] ZFacets = new byte[] {
            //起终点
            8,9,
            //箭头
            9,10,
            9,11//,
//            //字Z
//            21,22,
//            22,23,
//            23,24,
    };

    // Set color with red, green, blue and alpha (opacity) values
    float[][] color = new float[][]{
            {0.0f, 1.0f, 0.0f, 1.0f},
            {1.0f, 0.0f, 0.0f, 1.0f},
            {0.0f, 0.0f, 1.0f, 1.0f}
    };

    float[] xyzColor = new float[]{
            0.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 1.0f, 0.0f, 1.0f,

            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,

            0.0f, 0.0f, 1.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 1.0f
    };

    public Axises3D() {
        xyzVertexData = ByteBuffer // initialize vertex byte buffer for shape coordinates
                .allocateDirect(xyzVertices.length * 4) // (number of coordinate values * 4 bytes per float)
                .order(ByteOrder.nativeOrder()) // create a floating point buffer from the ByteBuffer
                .asFloatBuffer(); // add the coordinates to the FloatBuffer

        xyzVertexData.put(xyzVertices).position(0);

//        FloatBuffer localColorBuffer = ByteBuffer.allocateDirect(color.length * 4)
//                .order(ByteOrder.nativeOrder()).asFloatBuffer();
//        localColorBuffer.put(color).position(0);

//        XFacetsBuffer = ByteBuffer.allocateDirect(XFacets.length)
//                .order(ByteOrder.nativeOrder()).asShortBuffer();
//        XFacetsBuffer.put(XFacets).position(0);
//
//        YFacetsBuffer = ByteBuffer.allocateDirect(YFacets.length * 2)
//                .order(ByteOrder.nativeOrder()).asShortBuffer();
//        YFacetsBuffer.put(YFacets).position(0);
//
//        ZFacetsBuffer = ByteBuffer.allocateDirect(ZFacets.length * 2)
//                .order(ByteOrder.nativeOrder()).asShortBuffer();
//        ZFacetsBuffer.put(ZFacets).position(0);

        XFacetsBuffer = ByteBuffer.wrap(XFacets);
        YFacetsBuffer = ByteBuffer.wrap(YFacets);
        ZFacetsBuffer = ByteBuffer.wrap(ZFacets);


        mProgram = initProgram(vertexShaderCode, fragmentShaderCode);
    }

    public void drawAxises3D(float[] mvpMatrix) { // pass in the calculated transformation matrix
        // Add program to OpenGL ES environment
        GLES30.glUseProgram(mProgram);

        // Enable a handle to the triangle vertices
        GLES30.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES30.glVertexAttribPointer(mPositionHandle, 3,
                GLES20.GL_FLOAT, false,
                0, xyzVertexData);

        mColorHandle = GLES30.glGetUniformLocation(mProgram, "vColor");

        // get handle to vertex shader's uMVPMatrix member
        mMVPMatrixHandle = GLES30.glGetUniformLocation(mProgram,"uMVPMatrix");

        // Pass the projection and view transformation to the shader
        GLES30.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

        GLES30.glEnableVertexAttribArray(mColorHandle);
        GLES30.glLineWidth(10);

        // X
        GLES30.glUniform4fv(mColorHandle, 1, color[0], 0);
        GLES30.glDrawElements(GLES30.GL_LINES, XFacets.length,
                GLES30.GL_UNSIGNED_BYTE, XFacetsBuffer);

        // Y
        GLES30.glUniform4fv(mColorHandle, 1, color[1], 0);
        GLES30.glDrawElements(GLES30.GL_LINES, YFacets.length,
                GLES30.GL_UNSIGNED_BYTE, YFacetsBuffer);

        // Z ZFacetsBuffer.remaining()
        GLES30.glUniform4fv(mColorHandle, 1, color[2], 0);
        GLES30.glDrawElements(GLES30.GL_LINES, ZFacets.length,
                GLES30.GL_UNSIGNED_BYTE, ZFacetsBuffer);

        // 绘制结束
        GLES30.glDisableVertexAttribArray(mPositionHandle);
        GLES30.glDisableVertexAttribArray(mColorHandle);
    }

    public static int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

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

}
