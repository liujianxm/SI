package com.example.si;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.loader.content.CursorLoader;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
//import android.media.ExifInterface;
import android.os.Build;
import androidx.exifinterface.media.ExifInterface;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.content.Intent;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

import com.example.si.IMG_PROCESSING.EdgeDetect_fun;
import com.example.si.IMG_PROCESSING.ImageFilter;
import com.example.si.IMG_PROCESSING.ImgObj_Para;
import com.example.si.IMG_PROCESSING.RoberEdgeDetect;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private Button button;
    private Button Ed;
    private Button testbutton;
    private ImageView imageView;
    private Dialog dialog_pic;
    private static final int TAKE_PHOTO = 0;
    private static final int CHOOSE_PHOTO = 1;
    private static final int EdgeDetect = 2;
    private Uri photoURI;
    private String currentPhotoPath;
    private String currentPicturePath;
    private File showPic;
    private String imgPath = "";
    private String imgName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = (ImageView) findViewById(R.id.imageview3);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPictureDialog();
            }
        });

        button = (Button) findViewById(R.id.image);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPictureSelectDialog();
            }
        });

        Ed = (Button) findViewById(R.id.edgedetect);
        Ed.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                Log.d("TAG","Click EdgeDetect Button");
                EdgeDetect_Test();
            }
        });
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


    }

    void openPictureSelectDialog() {
        Context dialogContext = new ContextThemeWrapper(MainActivity.this, android.R.style.Theme_Light);
        String[] choiceItems = new String[2];
        choiceItems[0] = "camera";
        choiceItems[1] = "album";
        ListAdapter adapter = new ArrayAdapter<String>(dialogContext, android.R.layout.simple_list_item_1, choiceItems);
        AlertDialog.Builder builder = new AlertDialog.Builder(dialogContext);
        builder.setTitle("Add picture");
        builder.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        checkPermission();
                        //TakePhoto();
                        break;
                    case 1:
                        PickPhotoFromGallery();
                        break;
                }
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    void openPictureDialog() {
//////////////////////////////notice/////////////////////////////////////////////
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void Fun_Test() throws Exception {
        Bitmap bitmap = ImageTools.getBitmapfromimageView(imageView);//从imageView获取bitmap
        /////////////用于函数测试/////////////
        Bitmap blur_bitmap = ImageFilter.blurBitmap(MainActivity.this,bitmap);
        System.out.println("Enter here(the function)");
        ImgObj_Para iobj = new ImgObj_Para(blur_bitmap);
        double dRationHigh=0.83,dRationLow=0.5;///可调
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            EdgeDetect_fun.Canny_edge(iobj,blur_bitmap,dRationHigh,dRationLow);
        }
        System.out.println("Enter here1");
        imageView.setImageBitmap(iobj.EdgeImage);
        System.out.println("Enter here2");

    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void EdgeDetect_Test(){
        try {
            Bitmap bitmap = ImageTools.getBitmapfromimageView(imageView);
            Bitmap new_bitmap = ImageFilter.blurBitmap(MainActivity.this,bitmap);
            RoberEdgeDetect ro = new RoberEdgeDetect(5);
/*
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            //int sumpixel = width*height;
            int[][][] gray_img = colorToGray2D(bitmap);
            gray_img[1][2][3] = 255;
           // int[][] otsu_img = myOTSU(gray_img[1],width,height,256);
            int[][][] newgray_img = new int[2][width][height];
            newgray_img[0] = gray_img[0];
            newgray_img[1] = gray_img[1];
            Bitmap newimg = gray2DToBitmap(newgray_img,width,height);
            System.out.println("Enter the Fun....");
            imageView.setImageBitmap(newimg);
 */

            EdgeDetect_fun.EdgeDetect(ro, new_bitmap);
            imageView.setImageBitmap(new_bitmap);
            System.out.println("Enter the EdgeDetectFun....");
            imageView.setImageBitmap(ro.EdgeImage);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case TAKE_PHOTO:
                    String status = Environment.getExternalStorageState();//读取SD卡状态
                    if (status.equals(Environment.MEDIA_MOUNTED)) {
                        //Bitmap bitmap = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory() + "/image.jpg");
                       try {
                           Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(photoURI));
                           int degree = getBitmapDegree(currentPhotoPath);
                           System.out.println(degree);
                           bitmap = rotateBitmapByDegree(bitmap, degree);
                           Bitmap newBitmap = ImageTools.zoomBitmap(bitmap, bitmap.getWidth() / 5, bitmap.getHeight() / 5);
                           imageView.setImageBitmap(newBitmap);
                           ImageTools.savePhotoToSDCard(bitmap, Environment.getExternalStorageDirectory().getAbsolutePath() + "/SI_Photo", String.valueOf(System.currentTimeMillis()));
                           bitmap.recycle();
                       }catch (FileNotFoundException e){
                           e.printStackTrace();
                       }
                    }
                    break;
                case CHOOSE_PHOTO:
                    ContentResolver resolver = getContentResolver();
                    Uri originalUri = data.getData();
                    currentPicturePath = getRealPathFromUri(this,originalUri);

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
                    }
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
                showPic = photoFile;
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
    private int[][] myOTSU(int[][] image, int width, int height, int grayscale) {
        int[] pixelCount = new int[grayscale];
        float[] pixelPro = new float[grayscale];
        int i, j, pixelSum = width * height, threshold = 0;
        float w0, w1, u0tmp, u1tmp, u0, u1, deltaTmp, deltaMax = 0;

        //统计每个灰度级中像素的个数
        for(i = 0; i < height; i++) {
            for(j = 0;j < width;j++) {
                pixelCount[image[i][j]]++;
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
        for(i = 0; i < height; i++) {
            for(j = 0;j < width;j++) {
                if (image[width][height] < threshold) {
                    image[width][height] = 0;  //忘记哪个是零哪个是一
                } else {
                    image[width][height] = 1;
                }
            }
        }

        return image;

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

}
