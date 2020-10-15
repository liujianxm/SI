package com.example.si.RENDER;

import android.opengl.GLES20;
import android.opengl.GLES30;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;


public class Axis {

    private final String vertexShaderCode =
            "#version 300 es\n" +
                    "layout (location = 0) in vec4 vPosition;" +
                    "layout (location = 1) out vec4 vColor;" +
                    "uniform mat4 uMVPMatrix;" +
                    "in vec4 aColor;" +
                    "void main() {" +
                    "gl_Position = uMVPMatrix * vPosition;" +
//                    "  gl_Position = vPosition;" +
//                    "  gl_PointSize = 10.0;" +
                    "  vColor = aColor;" +
                    "}";

    private final String fragmentShaderCode =
            "#version 300 es\n" +
                    "precision mediump float;"+
                    "out vec4 fragColor;"+
                    "in vec4 vColor;" +
                    "void main() {"+
//                    "fragColor = vec4(1.0,1.0,1.0,1.0);"+
                    "fragColor = vColor;"+
                    "}";


    private FloatBuffer xyzVertexData;
    private FloatBuffer colorBuffer;
    private final ByteBuffer XFacetsBuffer, YFacetsBuffer, ZFacetsBuffer;
    private final int mProgram_Axis;
    private float[] mMVPMatrix = new float[16];

    //定义XYZ坐标
    float xyzVertices[] = new float[]{
            0f, 0f, 0f,//0 x起点，画坐标轴的
            0.6f, 0f, 0f,//1 X轴的终点
            0.58f,0.02f,0f,//2 X轴箭头1
            0.58f,-0.02f,0f,//3 X轴箭头2

            0f, 0f, 0f,//4 Y轴起点
            0f, 0.6f, 0f,//5 Y轴终点
            0.02f ,0.58f ,0f,//6 Y轴箭头1
            -0.02f ,0.58f ,0f,//7 Y轴箭头2

            0f, 0f, 0f,//8 Z轴起点
            0f, 0f, 0.6f,//9 Z轴终点
            0f ,0.02f ,0.58f,//10 Z轴箭头1
            0f ,-0.02f ,0.58f,//11 Z轴箭头2

            0.8f,0f,0f,//12 绘制字X
            0.82f,0.02f,0f,//13
            0.78f,0.02f,0f,//14
            0.78f,-0.02f,0f,//15
            0.82f,-0.02f,0f,//16

            0f,0.7f,0f,//17 绘制字Y
            0f,0.68f,0f,//18
            0.02f,0.72f,0f,//19
            -0.02f,0.72f,0f,//20

            -0.02f ,0.02f ,0.7f,//21  绘制字Z
            0.02f,0.02f,0.7f,//22
            -0.02f,-0.02f,0.7f,//23
            0.02f,-0.02f,0.7f,//24
    };

    private float[] mColorPoints = {
            1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.5f, 0.5f, 0.5f,
            0.5f, 0.5f, 0.5f,
            0.5f, 0.5f, 0.5f,
            0.5f, 0.5f, 0.5f,
            0.5f, 0.5f, 0.5f,
            0.5f, 0.5f, 0.5f,
            0.5f, 0.5f, 0.5f,
            0.5f, 0.5f, 0.5f,
            0.5f, 0.5f, 0.5f,
            0.5f, 0.5f, 0.5f,
            0.5f, 0.5f, 0.5f,
            0.5f, 0.5f, 0.5f,
    };

    byte[] XFacets = new byte[]{
//起终点
            0,1,
//箭头
            1,2,
            1,3,
//X
            12,13,
            12,14,
            12,15,
            12,16
    };

    byte[] YFacets = new byte[]{
//起终点
            4,5,
//箭头
            5,6,
            5,7,
//字Y
            17,18,
            17,19,
            17,20
    };

    byte[] ZFacets = new byte[]{
//起终点
            8,9,
//箭头
            9,10,
            9,11,
//字Z
            21,22,
            22,23,
            23,24
    };

    public Axis(){
        mProgram_Axis = initProgram(vertexShaderCode, fragmentShaderCode);

        //分配内存空间,每个浮点型占4字节空间
        xyzVertexData = ByteBuffer
                .allocateDirect(xyzVertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        //传入指定的坐标数据
        xyzVertexData.put(xyzVertices);
        xyzVertexData.position(0);

        colorBuffer = ByteBuffer.allocateDirect(mColorPoints.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        colorBuffer.put(mColorPoints);
        colorBuffer.position(0);

        // 将直线的数组包装成ByteBuffer
        XFacetsBuffer = ByteBuffer.wrap(XFacets);
        YFacetsBuffer = ByteBuffer.wrap(YFacets);
        ZFacetsBuffer = ByteBuffer.wrap(ZFacets);
    }

    public void free(){

        xyzVertexData.clear();
        xyzVertexData = null;

        colorBuffer.clear();
        colorBuffer = null;

        XFacetsBuffer.clear();

        YFacetsBuffer.clear();
        ZFacetsBuffer.clear();

    }

    public void draw(float[] finalMatrix) {

        mMVPMatrix = finalMatrix;
        GLES30.glUseProgram(mProgram_Axis);

        //准备坐标数据
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, xyzVertexData);
        //启用顶点的句柄
        GLES30.glEnableVertexAttribArray(0);

        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 0, colorBuffer);
        GLES30.glEnableVertexAttribArray(1);

        int glHMatrix = GLES30.glGetUniformLocation(mProgram_Axis, "uMVPMatrix");
        GLES30.glUniformMatrix4fv(glHMatrix,1,false, mMVPMatrix,0);

        //绘制坐标系
        GLES30.glLineWidth(5.0f);//直线宽度

        GLES30.glDrawElements(GLES30.GL_LINES, XFacetsBuffer.remaining(), GLES30.GL_UNSIGNED_BYTE, XFacetsBuffer);
        GLES30.glDrawElements(GLES30.GL_LINES, YFacetsBuffer.remaining(), GLES30.GL_UNSIGNED_BYTE, YFacetsBuffer);
        GLES30.glDrawElements(GLES30.GL_LINES, ZFacetsBuffer.remaining(), GLES30.GL_UNSIGNED_BYTE, ZFacetsBuffer);



        GLES30.glDisableVertexAttribArray(0);
        GLES30.glDisableVertexAttribArray(1);
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

    private int initProgram(String vertexShaderCode, String fragmentShaderCode) {
        //加载着色器
        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode);

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
