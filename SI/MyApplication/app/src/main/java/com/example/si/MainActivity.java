package com.example.si;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.loader.content.CursorLoader;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
//import android.media.ExifInterface;
import android.opengl.GLSurfaceView;
import android.os.Build;
import androidx.exifinterface.media.ExifInterface;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.net.Uri;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.example.si.IMG_PROCESSING.CircleDetect.EdgeDetect_fun;
import com.example.si.IMG_PROCESSING.FourAreaLabel;
import com.example.si.IMG_PROCESSING.CircleDetect.HoughCircle;
import com.example.si.IMG_PROCESSING.HessianMatrixLine;
import com.example.si.IMG_PROCESSING.CircleDetect.ImageFilter;
import com.example.si.IMG_PROCESSING.ImgObj_Para;
import com.example.si.IMG_PROCESSING.CircleDetect.Point;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.core.BasePopupView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public class MainActivity extends AppCompatActivity {
    private Button testbutton;
    private Button img_switch;
    private Button select_points;
    private Button finished;
    private Button loadImage;
    private Button Process;
    private ImageView imageView;
    private Dialog dialog_pic;
    private static final int TAKE_PHOTO = 0;
    private static final int CHOOSE_PHOTO = 1;
    private static final int EdgeDetect = 2;
    private Uri photoURI;
    private String currentPhotoPath;
    private String currentPicturePath;
    private Boolean ImageOpened = false;
    private Boolean ImageOpened2 = false;
    private ScaleGestureDetector mScaleGestureDetector = null;
    private static MyGLSurfaceView myGLSurfaceView;
    private static MyRenderer myrenderer;
    private static Context context;
    private BasePopupView popupView;
    private boolean isMutiImg;
    private boolean isP1 = true;
    private boolean isSelectPoints = false;
    private boolean isFinished = false;
    boolean isFinished1 = false;
    boolean isFinished2 = false;
    Bitmap img1 = null;
    Bitmap img2 = null;

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA};
    private static final int REQUEST_PERMISSION_CODE = 1;

    //子线程中实现UI更新
    @SuppressLint("HandlerLeak")
    private Handler uiHandler = new Handler(){
        // 覆写这个方法，接收并处理消息。
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 1:
                    Log.v("filesocket_send: ", "Connect with Server successfully");
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    System.out.println("------ Upload file successfully!!! -------");
                    popupView.dismiss();
                    break;
            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        imageView = (ImageView) findViewById(R.id.imageview3);

        //MultiDex.install(this);
        myrenderer = new MyRenderer();
        myGLSurfaceView = new MyGLSurfaceView(this);
        setContentView(myGLSurfaceView);
        popupView = new XPopup.Builder(this)
                .dismissOnTouchOutside(false)
                .asLoading("Processing......");
        context = getApplicationContext();




        /*
        testbutton = (Button) findViewById(R.id.testbutton);////用于测试
        testbutton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                Log.d("TAG","Click test Button");
                try {
                    Fun_Test();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
         */


//        mScaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.OnScaleGestureListener() {
//            private  float scale;
//            private float preScale = 1;//默认前一次缩放比例为1
//
//            @Override
//            public boolean onScale(ScaleGestureDetector detector) {
//                float previousSpan = detector.getPreviousSpan();
//                float currentSpan = detector.getCurrentSpan();
//                if(currentSpan<previousSpan){
//                    //缩小
//                    scale = preScale - (previousSpan-currentSpan)/1000;
//                }else{
//                    //放大
//                    scale = preScale + (currentSpan-previousSpan)/1000;
//                }
//                //缩放view
//                ViewHelper.setScaleX(imageView,scale);// x方向上缩小
//                ViewHelper.setScaleY(imageView,scale);// y方向上缩小
//                return false;
//            }
//
//            @Override
//            public boolean onScaleBegin(ScaleGestureDetector detector) {
//                return true;//返回true才会进入onScale()这个函数
//            }
//
//            @Override
//            public void onScaleEnd(ScaleGestureDetector detector) {
//                preScale = scale;//记录本次缩放比例
//            }
//        });


        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_PERMISSION_CODE);
            }
        }

    }


    /**
     * called when request permission
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case REQUEST_PERMISSION_CODE: {
                for (int i = 0; i < permissions.length; i++) {
                    Log.i("MainActivity", "申请的权限为：" + permissions[i] + ",申请结果：" + grantResults[i]);
                }
                break;
            }
        }

    }

    public boolean onTouchEvent(MotionEvent event){
        return mScaleGestureDetector.onTouchEvent(event);
    }

    //================新添menu部分======================//
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.main,menu);
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.openimage:
                Toast.makeText(this, "open an image..", Toast.LENGTH_LONG).show();
                openPictureSelectDialog();
                break;
            case R.id.function:
                if(!ImageOpened){
                    Toast.makeText(this, "Please load an image first!", Toast.LENGTH_LONG).show();
                }
                else {
                    chooseFunctionDialog();
                }
                break;
            //case R.id.
        }
        return true;
    }

    void chooseFunctionDialog(){
        Context dialogContext = new ContextThemeWrapper(MainActivity.this,android.R.style.Theme_Light);
        String[] Items = new String[4];
        Items[0] = "Edge Detection";
        Items[1] = "FourAreaLable";
        Items[2] = "HoughCircle";
        Items[3] = "On building..";
        ListAdapter adapter = new ArrayAdapter<String>(dialogContext,android.R.layout.simple_list_item_1,Items);
        AlertDialog.Builder builder = new AlertDialog.Builder(dialogContext);
        builder.setTitle("Functions");
        builder.setSingleChoiceItems(adapter,-1,new DialogInterface.OnClickListener(){
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(DialogInterface dialog, int which){
                switch (which){
                    case 0:
                        Toast.makeText(MainActivity.this, "edge function..", Toast.LENGTH_LONG).show();
                        Log.d("TAG", "Click EdgeDetect Button");
                        EdgeDetect_Test();
                        break;
                    case 1:
                        Toast.makeText(MainActivity.this, "FourAreaLable function..", Toast.LENGTH_LONG).show();
                        Log.d("TAG", "Click FourAreaLable Button");
                        try {
                            FourAreaLable_Fun();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case 2:
                        Toast.makeText(MainActivity.this, "HoughCircle function..", Toast.LENGTH_LONG).show();
                        try {
                            Circle_Fun();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case 3:
                        Toast.makeText(MainActivity.this,"Test Function..",Toast.LENGTH_LONG).show();

                        Log.d("TAG", "Click Test Button");
                        try {
                            Test_Fun();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        break;
                }
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    //用于子线程内toast
    public void Toast_in_Thread(final String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message,Toast.LENGTH_SHORT).show();
            }
        });
    }

    //======================================================//
    void openPictureSelectDialog() {
        Context dialogContext = new ContextThemeWrapper(MainActivity.this, android.R.style.Theme_Light);
        String[] choiceItems = new String[3];
        choiceItems[0] = "camera";
        choiceItems[1] = "album";
        choiceItems[2] = "3D-Reconstruction";
        ListAdapter adapter = new ArrayAdapter<String>(dialogContext, android.R.layout.simple_list_item_1, choiceItems);
        AlertDialog.Builder builder = new AlertDialog.Builder(dialogContext);
        builder.setTitle("Add picture");
        builder.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        checkPermission();
                        ImageOpened = true;
                        break;
                    case 1:
                        PickPhotoFromGallery();
                        ImageOpened = true;
                        break;
                    case 2:
                        Reconstruction3D();
                        //ImageOpened = true;
                        break;
                }
                dialog.dismiss();
            }
        });
        builder.create().show();
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void Circle_Fun() throws Exception {
        Bitmap bitmap = myrenderer.GetBitmap();//从myrenderer获取bitmap
        if(bitmap == null){
            Log.v("Test_Fun", "Please load img first!");
            if (Looper.myLooper() == null) {
                Looper.prepare();
            }
            Toast.makeText(getContext(), "Please load image first!", Toast.LENGTH_LONG).show();
            Looper.loop();
            return;
        }
        Bitmap blurbitmap = ImageFilter.blurBitmap(MainActivity.this,bitmap);//高斯滤波
        System.out.println("Enter here(the gauss finished!!)");
        ArrayList<Point> circles = new ArrayList<>();//存放找到的圆
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            circles =  HoughCircle.HoughCircle_Fun(blurbitmap);
            System.out.println("The number of circles:"+circles.size());
            Toast.makeText(this, "Circles number:"+circles.size(), Toast.LENGTH_LONG).show();
            System.out.println("Enter here(the circles have been found!!)");
        }
        Bitmap new_bitmap = Point.DrawCircle(circles,bitmap);
        System.out.println("Enter here(the circles have been drawn!!)");
        imageView.setImageBitmap(new_bitmap);
        System.out.println("Enter here(the bitmap have been updated!!)");
        bitmap.recycle();//回收bitmap
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void FourAreaLable_Fun() throws Exception {
        Bitmap bitmap = myrenderer.GetBitmap();//从myrenderer获取bitmap
        if(bitmap == null){
            Log.v("Test_Fun", "Please load img first!");
            if (Looper.myLooper() == null) {
                Looper.prepare();
            }
            Toast.makeText(getContext(), "Please load image first!", Toast.LENGTH_LONG).show();
            Looper.loop();
            return;
        }
        /////////////用于函数测试/////////////
        Bitmap blur_bitmap = ImageFilter.blurBitmap(MainActivity.this,bitmap);
        System.out.println("Enter here(the function)");
        //ImgObj_Para iobj = new ImgObj_Para(blur_bitmap);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            bitmap = FourAreaLabel.AreaLabel(blur_bitmap);
        }
        System.out.println("Enter here1");
        myrenderer.ResetImage(bitmap);
        myGLSurfaceView.requestRender();
        Toast.makeText(getContext(), "Have been shown on the screen.", Toast.LENGTH_SHORT).show();
        //imageView.setImageBitmap(bitmap);
        System.out.println("Enter here2");
        bitmap.recycle(); //回收bitmap

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void Test_Fun() throws Exception {

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        popupView.show();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = myrenderer.GetBitmap();//从myrenderer获取bitmap
                if(bitmap == null){
                    Log.v("Test_Fun", "Please load img first!");
                    if (Looper.myLooper() == null) {
                        Looper.prepare();
                    }
                    Toast_in_Thread("Please load image first!");
                    Looper.loop();
                    return;
                }

                /////////////用于函数测试/////////////
                //Bitmap blur_bitmap = ImageFilter.blurBitmap(MainActivity.this,bitmap);blur_
                System.out.println("Enter here(the function)");
                //ImgObj_Para iobj = new ImgObj_Para(blur_bitmap);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    try {
                        bitmap = HessianMatrixLine.HessianLine(bitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                System.out.println("Enter here1");
                myrenderer.ResetImage(bitmap);
                myGLSurfaceView.requestRender();
                Toast_in_Thread("Have been shown on the screen.");
                //imageView.setImageBitmap(bitmap);
                //ImageTools.savePhotoToSDCard(bitmap, Environment.getExternalStorageDirectory().getAbsolutePath() + "/SI_Photo", String.valueOf(System.currentTimeMillis()));
                System.out.println("Enter here2");
                //bitmap.recycle(); //回收bitmap

                uiHandler.sendEmptyMessage(1);

            }
        });

        thread.start();

    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void EdgeDetect_Test(){
        try {
            Bitmap bitmap = myrenderer.GetBitmap();//从myrenderer获取bitmap
            if(bitmap == null){
                Log.v("Test_Fun", "Please load img first!");
                if (Looper.myLooper() == null) {
                    Looper.prepare();
                }
                Toast.makeText(getContext(), "Please load image first!", Toast.LENGTH_LONG).show();
                Looper.loop();
                return;
            }
            Bitmap blur_bitmap = ImageFilter.blurBitmap(MainActivity.this,bitmap);
            System.out.println("Enter here(the function)");
            ImgObj_Para iobj = new ImgObj_Para(blur_bitmap);
            double dRationHigh=0.9,dRationLow=0.78;///可调
            //double dRationHigh=0.85,dRationLow=0.5;///可调
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                EdgeDetect_fun.Canny_edge(iobj,blur_bitmap,dRationHigh,dRationLow);
            }
            System.out.println("Enter here1");
            imageView.setImageBitmap(iobj.EdgeImage);
            System.out.println("Enter here2");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case TAKE_PHOTO:
                    String status = Environment.getExternalStorageState();//读取SD卡状态
                    if (status.equals(Environment.MEDIA_MOUNTED)) {

                        myrenderer.SetPath(currentPhotoPath);
                        //System.out.println(showPic.getAbsolutePath());
                        myGLSurfaceView.requestRender();


                        //Bitmap bitmap = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory() + "/image.jpg");
//                        try {
//                            Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(photoURI));
//                            int degree = getBitmapDegree(currentPhotoPath);
//                            System.out.println(degree);
//                            bitmap = rotateBitmapByDegree(bitmap, degree);
//                            Bitmap newBitmap = ImageTools.zoomBitmap(bitmap, bitmap.getWidth() / 5, bitmap.getHeight() / 5);
//                            imageView.setImageBitmap(newBitmap);
//                            ImageTools.savePhotoToSDCard(bitmap, Environment.getExternalStorageDirectory().getAbsolutePath() + "/SI_Photo", String.valueOf(System.currentTimeMillis()));
//                            bitmap.recycle();
//                        }catch (FileNotFoundException e){
//                            e.printStackTrace();
//                        }
                    }
                    break;
                case CHOOSE_PHOTO:
                    ContentResolver resolver = getContentResolver();
                    Uri originalUri = data.getData();
                    //currentPicturePath = getRealPathFromUri(this,originalUri);

                    String path = originalUri.toString();
                    myrenderer.SetPath(path);
                    myGLSurfaceView.requestRender();

                    /*
                    try {
                        Bitmap photo = MediaStore.Images.Media.getBitmap(resolver, originalUri);
                        int degree = getBitmapDegree(currentPicturePath);
                        if (photo != null) {
                            Bitmap smallBitmap = ImageTools.zoomBitmap(photo, photo.getWidth() / 5, photo.getHeight() / 5);
                            smallBitmap = rotateBitmapByDegree(smallBitmap, degree);
                            photo.recycle();
                            imageView.setImageBitmap(smallBitmap);
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }*/
                    break;
                default:
                    break;
            }
        }
    }

    private void TakePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (intent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            File photoFile = null;
            //            String photoFilePath = null;
            try {
                photoFile = createImageFile();
                File showPic = photoFile;
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            currentPhotoPath = photoFile.getAbsolutePath();
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(MainActivity.this,
                        "com.example.si.provider",
                        photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(intent, TAKE_PHOTO);
                Log.v("Camera", "Here we are");
            }
        }
    }


    private void PickPhotoFromGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, CHOOSE_PHOTO);
    }

    private void Reconstruction3D() {


        img_switch = new Button(this);
        img_switch.setText("P1");
        img_switch.setTextColor(Color.RED);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(120, 120);
        params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        params.setMargins(0,0,160,20);
        this.addContentView(img_switch,params);//
        img_switch.setVisibility(View.VISIBLE);

        loadImage = new Button(this);
        loadImage.setText("Load");
        loadImage.setTextColor(Color.RED);
        params.setMargins(160,0,320,20);
        this.addContentView(loadImage,params);//
        loadImage.setVisibility(View.VISIBLE);

        select_points = new Button(this);
        select_points.setText("Points");
        select_points.setTextColor(Color.RED);
        params.setMargins(320,0,480,20);
        this.addContentView(select_points,params);//
        select_points.setVisibility(View.VISIBLE);

        finished = new Button(this);
        finished.setText("Finish");
        finished.setTextColor(Color.RED);
        params.setMargins(480,0,640,20);
        this.addContentView(finished,params);//
        finished.setVisibility(View.VISIBLE);

        Process = new Button(this);
        Process.setText("Process");
        Process.setTextColor(Color.RED);
        params.gravity = Gravity.BOTTOM | Gravity.LEFT;
        params.setMargins(160,0,0,20);
        this.addContentView(Process,params);//
        Process.setVisibility(View.GONE);

        img_switch.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                myrenderer.switchImg();

                if (isP1){
                    img_switch.setText("P2");
                    functionMenu(isFinished2);
                    if (ImageOpened2) {
                        myrenderer.ResetImage(img2);
                        myGLSurfaceView.requestRender();
                    }

                    //display points

                } else {
                    img_switch.setText("P1");
                    functionMenu(isFinished1);
                    if (ImageOpened) {
                        myrenderer.ResetImage(img1);
                        myGLSurfaceView.requestRender();
                    }

                    //display points

                }
            }
        });

        select_points.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                //myrenderer.switchImg();

                if (isP1){
                    if (!myrenderer.ifImageLoaded()){
                        Toast.makeText(context, "Please load a image first", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    //wait for touch

                    //getpoints  (x,y)

                    //获取点周围小图像块
                    //Bitmap newbitmap = getSmallImageBlock(Bitmap img1,x,y)

                    //haar 角点检测获得点集
                    if (myrenderer.if2dImageLoaded()){
                        myrenderer.corner_detection();
                        myGLSurfaceView.requestRender();
                    } else {
                        Toast.makeText(getContext(), "Please load a 2d image first", Toast.LENGTH_SHORT).show();
                    }

                    //点集中选取最优点

                    //display points


                } else {

                    //wait for touch

                    //getpoints

                    //haar 角点检测修正点

                    //display points

                }
            }
        });

        //loadimage
        loadImage.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                //myrenderer.switchImg();
                if (isP1){
//                    img_switch.setText("P2");
                    functionMenu(isFinished1);
                    if(!ImageOpened){
                        Log.v("loadImage", "Please load img first!");
                        if (Looper.myLooper() == null) {
                            Looper.prepare();
                        }
                        Toast.makeText(getContext(), "Please load image first!", Toast.LENGTH_LONG).show();
                        Looper.loop();
                        return;
                    }
                    PickPhotoFromGallery();
                    ImageOpened = true;
                    img1 = myrenderer.GetBitmap();

                } else {
//                    img_switch.setText("P1");
                    functionMenu(isFinished1);
                    if(!ImageOpened2){
                        Log.v("loadImage", "Please load img first!");
                        if (Looper.myLooper() == null) {
                            Looper.prepare();
                        }
                        Toast.makeText(getContext(), "Please load image first!", Toast.LENGTH_LONG).show();
                        Looper.loop();
                        return;
                    }
                    PickPhotoFromGallery();
                    ImageOpened2 = true;
                    img2 = myrenderer.GetBitmap();

                }
            }
        });

        //finished
        finished.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isP1){
                    isFinished1 = true;
                } else {
                    isFinished2 = true;
                }
                functionMenu(isFinished1);
                functionMenu(isFinished2);
                //isFinished = isFinished1 & isFinished2;
                if (isFinished1 & isFinished2) {
                    Process.setVisibility(View.VISIBLE);
                }
            }
        });

        //img_switch.setVisibility(View.GONE);


    }

    private void functionMenu (boolean isfiished) {
        if (isfiished) {
            select_points.setVisibility(View.GONE);
            finished.setVisibility(View.GONE);
        } else {
            select_points.setVisibility(View.VISIBLE);
            finished.setVisibility(View.VISIBLE);
        }
    }

    //获取以（x,y）为中心x轴半径为30，y轴半径为40的图像块区域图像块
    private Bitmap getSmallImageBlock(Bitmap image, int x, int y) {
        int Rx = 30;
        int Ry = 40;
        int xfrom = max(0,x-Rx);
        int xto = min(image.getWidth(),x+Rx);
        int yfrom = max(0,y-Ry);
        int yto = min(image.getHeight(),y+Ry);
        int xlen = xto - xfrom + 1;
        int ylen = yto - yfrom + 1;

//        int color;
        Bitmap myBitmap = null;
        if (isP1) {
            myBitmap = Bitmap.createBitmap( xlen, ylen, Bitmap.Config.ARGB_8888 );
            for (int w = 0; w < xlen; w++) {
                for (int h = 0; h < ylen; h++) {
//                    color = image.getPixel(w,h);
                    myBitmap.setPixel(w, h, image.getPixel(xfrom+w,yfrom+h));
                }
            }
        }
        return myBitmap;
    }
    //org.gradle.java.home=C\:\\MySoft\\Java
/*
    private String getImageFilePath() {
        String mCaptureDir = "/storage/emulated/0/C3/cameraPhoto";
        File dir = new File(mCaptureDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String mCapturePath = mCaptureDir + "/" + "Photo_" + System.currentTimeMillis() + ".jpg";
        return mCapturePath;
    }

 */

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        //currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    /**
     * 获取图片的旋转角度
     *
     * @param path 图片绝对路径
     * @return 图片的旋转角度
     */
    public static int getBitmapDegree(String path) {
        int degree = 0;
        try {
            // 从指定路径下读取图片，并获取其EXIF信息
            ExifInterface exifInterface = new ExifInterface(path);
            // 获取图片的旋转信息
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Log.v("TAG", "原图被旋转角度： ========== " + orientation );
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }
    /**
     * 将图片按照指定的角度进行旋转
     *
     * @param bitmap 需要旋转的图片
     * @param degree 指定的旋转角度
     * @return 旋转后的图片
     */
    public static Bitmap rotateBitmapByDegree(Bitmap bitmap, int degree) {
        // 根据旋转角度，生成旋转矩阵
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
        Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
        return newBitmap;
    }


    /////////////////////////////////////
    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        } else {
            TakePhoto();
        }
    }
    /**
     * 根据图片的Uri获取图片的绝对路径。@uri 图片的uri
     * @return 如果Uri对应的图片存在,那么返回该图片的绝对路径,否则返回null
     */
    public static String getRealPathFromUri(Context context, Uri uri) {
        if(context == null || uri == null) {
            return null;
        }
        if("file".equalsIgnoreCase(uri.getScheme())) {
            return getRealPathFromUri_Byfile(context,uri);
        } else if("content".equalsIgnoreCase(uri.getScheme())) {
            return getRealPathFromUri_Api11To18(context,uri);
        }
        return getRealPathFromUri_AboveApi19(context, uri);
    }

    //针对图片URI格式为Uri:: file:///storage/emulated/0/DCIM/Camera/IMG_20170613_132837.jpg
    private static String getRealPathFromUri_Byfile(Context context,Uri uri){
        String uri2Str = uri.toString();
        String filePath = uri2Str.substring(uri2Str.indexOf(":") + 3);
        return filePath;
    }

    /**
     * 适配api19以上,根据uri获取图片的绝对路径
     */
    @SuppressLint("NewApi")
    private static String getRealPathFromUri_AboveApi19(Context context, Uri uri) {
        String filePath = null;
        String wholeID = null;

        wholeID = DocumentsContract.getDocumentId(uri);

        // 使用':'分割
        String id = wholeID.split(":")[1];

        String[] projection = { MediaStore.Images.Media.DATA };
        String selection = MediaStore.Images.Media._ID + "=?";
        String[] selectionArgs = { id };

        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
                selection, selectionArgs, null);
        int columnIndex = cursor.getColumnIndex(projection[0]);

        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }
        cursor.close();
        return filePath;
    }

    /**
     * //适配api11-api18,根据uri获取图片的绝对路径。
     * 针对图片URI格式为Uri:: content://media/external/images/media/1028
     */
    private static String getRealPathFromUri_Api11To18(Context context, Uri uri) {
        String filePath = null;
        String[] projection = { MediaStore.Images.Media.DATA };

        CursorLoader loader = new CursorLoader(context, uri, projection, null,
                null, null);
        Cursor cursor = loader.loadInBackground();

        if (cursor != null) {
            cursor.moveToFirst();
            filePath = cursor.getString(cursor.getColumnIndex(projection[0]));
            cursor.close();
        }
        return filePath;
    }

    /**
     * 适配api11以下(不包括api11),根据uri获取图片的绝对路径
     */
    private static String getRealPathFromUri_BelowApi11(Context context, Uri uri) {
        String filePath = null;
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = context.getContentResolver().query(uri, projection,
                null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            filePath = cursor.getString(cursor.getColumnIndex(projection[0]));
            cursor.close();
        }
        return filePath;
    }


    private int[][][] colorToGray2D(Bitmap myBitmap) {
        //myimage[0][][]为透明度
        //myimage[1][][]为灰度
        int width = myBitmap.getWidth();
        int height = myBitmap.getHeight();
        int[][][] myimage = new int[2][width][height];  //存储透明度和灰度图像
        int color;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                color = myBitmap.getPixel(i,j);
                myimage[0][i][j] = Color.alpha(color);
                myimage[1][i][j] = (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114 + 500) / 1000;
            }
        }
        return myimage;
    }

    //OTSU二值化
    public int[][] myOTSU(int[][] grayimage, int width, int height, int grayscale) {
        int[] pixelCount = new int[grayscale];
        float[] pixelPro = new float[grayscale];
        int i, j, pixelSum = width * height, threshold = 0;
        float w0, w1, u0tmp, u1tmp, u0, u1, deltaTmp, deltaMax = 0;

        //统计每个灰度级中像素的个数
        for(i = 0; i < width; i++) {
            for(j = 0;j < height;j++) {
                pixelCount[grayimage[i][j]]++;
            }
        }

        //计算每个灰度级的像素数目占整幅图像的比例
        for(i = 0; i < grayscale; i++) {
            pixelPro[i] = (float)pixelCount[i] / pixelSum;
        }

        //遍历所有从0到255灰度级的阈值分割条件，测试哪一个的类间方差最大
        for(i = 0; i < grayscale; i++) {
            w0 = w1 = u0tmp = u1tmp = u0 = u1 = deltaTmp = 0;
            for(j = 0; j < grayscale; j++) {
                if(j <= i) {  //背景部分
                    w0 += pixelPro[j];
                    u0tmp += j * pixelPro[j];
                } else {  //前景部分
                    w1 += pixelPro[j];
                    u1tmp += j * pixelPro[j];
                }
            }
            u0 = u0tmp / w0;
            u1 = u1tmp / w1;
            deltaTmp = (float)(w0 *w1* Math.pow((u0 - u1), 2)) ;
            if(deltaTmp > deltaMax) {
                deltaMax = deltaTmp;
                threshold = i;
            }
        }

        //根据threshold对图像进行二值化
        for(i = 0; i < width; i++) {
            for(j = 0; j < height; j++) {
                if (grayimage[i][j] < threshold) {
                    grayimage[i][j] = 0;  //忘记哪个是零哪个是一
                } else {
                    grayimage[i][j] = grayscale - 1;
                }
            }
        }

        return grayimage;

    }

    //二值图腐蚀运算
    //structelement形如
    // {{0,1,0},
    //  {1,1,1},
    //  {0,1,0}}
    private int[][] myerode(int[][] binaryimage, int[][] structelement, int grayscale) {
        int width = binaryimage.length;
        int height = binaryimage[0].length;
        int sewidth = structelement.length;
        int seheight = structelement[0].length;
        int[][] newbinaryimage = new int[width][height];
        //newbinaryimage = binaryimage;//浅拷贝

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                newbinaryimage[i][j] = binaryimage[i][j];
            }
        }



        for (int i = sewidth / 2; i < width - sewidth / 2; i++) {
            for (int j = seheight / 2; j < height - seheight / 2; j++) {
                //结构单元运算
                if (binaryimage[i][j] == 0) {  //当前像素为黑色
                    int temp = 0;
                    for (int a = 0; a < sewidth; a++) {
                        for (int b = 0; b < seheight; b++) {
                            if (structelement[a][b] ==0) {
                                continue;
                            }
                            temp += binaryimage[i - sewidth / 2 + a][j -seheight / 2 + b];
                        }
                        if (temp / (grayscale - 1) > 0) {
                            newbinaryimage[i][j] = grayscale - 1;
                        }
                    }
                }
            }
        }

        return newbinaryimage;

    }

    //二值图膨胀运算
    private int[][] mydilate(int[][] binaryimage, int[][] structelement, int grayscale) {
        int width = binaryimage.length;
        int height = binaryimage[0].length;
        int sewidth = structelement.length;
        int seheight = structelement[0].length;
        int[][] newbinaryimage = new int[width][height];
        //newbinaryimage = binaryimage;//浅拷贝

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                newbinaryimage[i][j] = binaryimage[i][j];
            }
        }

        //结构元非零元素个数
        int senum = 0;
        for (int i = 0; i < sewidth; i++) {
            for (int j = 0; j < seheight; j++) {
                if (structelement[i][j] != 0) {
                    senum++;
                }
            }
        }

        for (int i = sewidth / 2; i < width - sewidth / 2; i++) {
            for (int j = seheight / 2; j < height - seheight / 2; j++) {
                //结构单元运算
                if (binaryimage[i][j] == (grayscale - 1)) {  //当前像素为白色
                    int temp = 0;
                    for (int a = 0; a < sewidth; a++) {
                        for (int b = 0; b < seheight; b++) {
                            if (structelement[a][b] == 0) {
                                continue;
                            }
                            if (binaryimage[i - sewidth / 2 + a][j - seheight / 2 + b] == 0) {
                                temp++;
                            }
                        }

                    }
                    System.out.println(temp);
                    if (temp > 0) {
                        newbinaryimage[i][j] = 0;
                    }
                }
            }
        }

        return newbinaryimage;

    }


    //灰度图转Bitmap
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private Bitmap gray2DToBitmap(int[][][] myimage, int width, int height) {
        Bitmap myBitmap = Bitmap.createBitmap( width, height, Bitmap.Config.ARGB_8888 );
        int color;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                color = Color.argb(myimage[0][i][j], myimage[1][i][j], myimage[1][i][j], myimage[1][i][j]);
                myBitmap.setPixel(i, j, color);
            }
        }
        return myBitmap;
    }


    public static Context getContext() {
        return context;
    }


    //opengl中的显示区域
    class MyGLSurfaceView extends GLSurfaceView {
        private float X, Y;
        private double dis_start;
        private float dis_x_start;
        private float dis_y_start;
        private boolean isZooming;
        private boolean isZoomingNotStop;
        private float x1_start;
        private float x0_start;
        private float y1_start;
        private float y0_start;


        public MyGLSurfaceView(Context context) {
            super(context);

            ActivityManager am = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
            ConfigurationInfo info = am.getDeviceConfigurationInfo();
            String v = info.getGlEsVersion(); //判断是否为3.0 ，一般4.4就开始支持3.0版本了。

            Log.v("MainActivity", "GLES-version: " + v);

            //设置一下opengl版本；
            setEGLContextClientVersion(3);


            setRenderer(myrenderer);


            //调用 onPause 的时候保存EGLContext
            setPreserveEGLContextOnPause(true);

            //当发生交互时重新执行渲染， 需要配合requestRender();
            setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
//            setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        }


        //触摸屏幕的事件
        @RequiresApi(api = Build.VERSION_CODES.N)
        @SuppressLint("ClickableViewAccessibility")
        public boolean onTouchEvent(MotionEvent motionEvent) {

            //ACTION_DOWN不return true，就无触发后面的各个事件
            if (motionEvent != null) {
                final float normalizedX = toOpenGLCoord(this, motionEvent.getX(), true);
                final float normalizedY = toOpenGLCoord(this, motionEvent.getY(), false);

                switch (motionEvent.getActionMasked()) {


                    case MotionEvent.ACTION_DOWN:
                        X=normalizedX;
                        Y=normalizedY;
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        isZooming=true;
                        float x1=toOpenGLCoord(this,motionEvent.getX(1),true);
                        float y1=toOpenGLCoord(this,motionEvent.getY(1),false);
                        dis_start=computeDis(normalizedX,x1,normalizedY,y1);

                        break;
                    case MotionEvent.ACTION_MOVE:
                        if(isZooming){
                            float x2=toOpenGLCoord(this,motionEvent.getX(1),true);
                            float y2=toOpenGLCoord(this,motionEvent.getY(1),false);
                            double dis=computeDis(normalizedX,x2,normalizedY,y2);
                            double scale=dis/dis_start;
                            myrenderer.zoom((float) scale);
                            requestRender();
                            dis_start=dis;
                        }else {
//                            move(normalizedX - X, normalizedY - Y);
                            X = normalizedX;
                            Y = normalizedY;
                        }
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        isZooming=false;
                        X = normalizedX;
                        Y = normalizedY;
                        break;
                    case MotionEvent.ACTION_UP:
                        break;
                    default:break;

                }
                return true;
            }
            return false;
        }


        //坐标系变换
        private float toOpenGLCoord(View view, float value, boolean isWidth) {
            if (isWidth) {
                return (value / (float) view.getWidth()) * 2 - 1;
            } else {
                return -((value / (float) view.getHeight()) * 2 - 1);
            }
        }


        //距离计算
        private double computeDis(float x1, float x2, float y1, float y2) {
            return sqrt(pow((x2 - x1), 2) + pow((y2 - y1), 2));
        }
    }


}
