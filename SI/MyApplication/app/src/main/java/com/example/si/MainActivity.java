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

import android.graphics.BitmapFactory;
import android.graphics.Color;
//import android.media.ExifInterface;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.os.Build;
import androidx.exifinterface.media.ExifInterface;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract;
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
import com.example.si.IMG_PROCESSING.CornerDetection.ImageMarker;

import com.example.si.IMG_PROCESSING.CircleDetect.HoughCircle;
import com.example.si.IMG_PROCESSING.GSDT.GSDT_Para;
import com.example.si.IMG_PROCESSING.HessianMatrixLine;
import com.example.si.IMG_PROCESSING.CircleDetect.ImageFilter;
import com.example.si.IMG_PROCESSING.ImgObj_Para;
import com.example.si.IMG_PROCESSING.CircleDetect.Point;
import com.example.si.IMG_PROCESSING.Reconstruction3D.BundleAdjustment_LM;
import com.example.si.IMG_PROCESSING.Reconstruction3D.Convert2DTo3D;
import com.example.si.IMG_PROCESSING.Reconstruction3D.Convert2DTo3D_new;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.core.BasePopupView;
import com.lxj.xpopup.interfaces.OnSelectListener;


import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;


import Jama.Matrix;
import Jama.SingularValueDecomposition;

import static com.example.si.IMG_PROCESSING.Reconstruction3D.Convert2DTo3D.ArrayToMarkerList;
import static com.example.si.IMG_PROCESSING.Reconstruction3D.Convert2DTo3D.MarkerListToArray;
import static java.lang.Math.max;
import static java.lang.Math.min;

import static com.example.si.IMG_PROCESSING.GSDT.GSDT_2D.GSDT_Fun;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.lang.Math.sqrt;

import static com.example.si.IMG_PROCESSING.CircleDetect.CircleDetect_new.CircleDetect_Fun;
import static java.lang.System.out;


public class MainActivity extends AppCompatActivity {
    private Button testbutton;
    private ImageView imageView;
    private Dialog dialog_pic;
    private static final int TAKE_PHOTO = 0;
    private static final int CHOOSE_PHOTO = 1;
    private static final int EdgeDetect = 2;
    private Uri photoURI;
    private String currentPhotoPath;
    private String currentPicturePath;
    private ScaleGestureDetector mScaleGestureDetector = null;
    private static MyGLSurfaceView myGLSurfaceView;
    private static MyRenderer myrenderer;
    private static Context context;
    private BasePopupView popupView;

    private Button img_switch;
    private Button select_points;
    private Button finished;
    private Button loadImage;
    private Button Process;
    private Boolean ImageOpened = false;
    private Boolean ImageOpened2 = false;
    private boolean isMutiImg = false;
    private boolean isP1 = true;
    private boolean ifPoint = false;
    private boolean ifDelete = false;
    private boolean isFinished1 = false;
    private boolean isFinished2 = false;
    private boolean isProcessed = false;
    private boolean isProcessed2 = false;

    private Bitmap showimg1 = null;
    private Bitmap showimg2 = null;
    private Bitmap img1 = null;
    private Bitmap img2 = null;
    ///////////////////////////////////
    double[][] polist1 = new double[][]{{1052,758,1},{1851,2190,1},{3900,1356,1},{3138,1886,1},{787,2938,1},{1703,1799,1},{3027,1798,1},{2673,642,1},{3023,2381,1},{775,1985,1},{195,1601,1},{1294,2205,1},{3515,1543,1},{3882,896,1},{751,344,1},{2161,1428,1},{2458,64,1},{3060,2765,1},{3014,2123,1},{2556,2961,1},{1767,570,1},{706,2732,1},{519,1925,1},{573,567,1},{753,319,1},{1593,2379,1},{1864,2509,1},{2971,541,1},{3313,2610,1},{491,2456,1},{802,408,1},{1932,485,1},{3505,1101,1},{3636,2540,1},{1458,930,1},{3869,1111,1},{2777,2428,1},{405,2572,1},{901,1930,1},{978,1573,1},{3055,2967,1},{841,1566,1},{3214,1343,1},{2794,1116,1},{858,922,1},{2387,1063,1},{1989,2693,1},{3230,632,1},{332,1348,1},{1959,1850,1}};
    double[][] polist2 = new double[][]{{1017.2,742.7,1},{1746.9,2183,1},{3825.9,1426.6,1},{3059,1799.4,1},{627.8,3078.1,1},{1597,1948.8,1},{2910.2,2032.8,1},{2604.4,929.9,1},{2880.9,2596.3,1},{665.9,2089.1,1},{122.3,1566.7,1},{1195.9,2145.5,1},{3444.4,1517.8,1},{3794.3,1238.7,1},{712,511,1},{2057,1699.1,1},{2425.3,288.3,1},{2928.5,2758.8,1},{2910.8,2135.1,1},{2410.2,2991.7,1},{1713.1,771,1},{558,2860.8,1},{394.9,2165.6,1},{535,645,1},{724.4,416.3,1},{1471.4,2436,1},{1743.6,2509.7,1},{2910.5,804.4,1},{3181.7,2658.9,1},{395.7,2282,1},{771.6,486.2,1},{1897.8,565.9,1},{3441.1,1186.1,1},{3484.6,2768.9,1},{1374.4,1225.8,1},{3789.9,1309.2,1},{2659.3,2438.6,1},{283.9,2553.2,1},{791.8,2055.6,1},{905,1551.4,1},{2900.1,3065.9,1},{747,1707.8,1},{3114,1615.6,1},{2736.6,1144.7,1},{791.8,1086.7,1},{2302.9,1315.1,1},{1843.7,2817.3,1},{3176,812.8,1},{243.8,1524.9,1},{1837.5,2100,1}};
//    double[][] polist1 = new double[][]{{758,1052,1},{2190,1851,1},{1356,3900,1},{1886,3138,1},{2938,787,1},{1799,1703,1},{1798,3027,1},{642,2673,1},{2381,3023,1},{1985,775,1},{1601,195,1},{2205,1294,1},{1543,3515,1},{896,3882,1},{344,751,1},{1428,2161,1},{64,2458,1},{2765,3060,1},{2123,3014,1},{2961,2556,1},{570,1767,1},{2732,706,1},{1925,519,1},{567,573,1},{319,753,1},{2379,1593,1},{2509,1864,1},{541,2971,1},{2610,3313,1},{2456,491,1},{408,802,1},{485,1932,1},{1101,3505,1},{2540,3636,1},{930,1458,1},{1111,3869,1},{2428,2777,1},{2572,405,1},{1930,901,1},{1573,978,1},{2967,3055,1},{1566,841,1},{1343,3214,1},{1116,2794,1},{922,858,1},{1063,2387,1},{2693,1989,1},{632,3230,1},{1348,332,1},{1850,1959,1}};

//    double[][] polist1 = new double[][]{{1274,415,1},{1537,1007,1},{354,2652,1},{2276,1344,1},{1029,2173,1},{2356,1119,1},{2999,960,1},{317,827,1},{2100,3982,1},{1179,704,1},{1476,1255,1},{1112,2470,1},{2827,357,1},{2570,3346,1},{1725,2966,1},{100,3985,1},{1302,1010,1},{1561,1677,1},{785,2355,1},{268,3941,1},{637,154,1},{435,724,1},{1909,1438,1},{2267,1451,1},{2001,2358,1},{185,1901,1},{1492,3856,1},{1792,2626,1},{1779,1117,1},{114,2567,1},{2520,2034,1},{2140,1985,1},{514,3112,1},{377,1094,1},{676,3874,1},{2554,1692,1},{2870,797,1},{1643,3447,1},{904,2698,1},{511,532,1},{276,3822,1},{1876,3750,1},{2131,304,1},{348,3235,1},{772,2021,1},{2341,1881,1},{1639,2777,1},{2658,1626,1},{293,2333,1},{2357,2986,1}};
//
//    double[][] polist2 = new double[][]{{1.23076104E3,5.90092851E2,1},{1.47187979E3,1.13355665E3,1},{2.33181897E2,2.60191686E3,1},{2.19760944E3,1.44998958E3,1},{8.97328699E2,2.38214152E3,1},{2.27887361E3,1.29769766E3,1},{2.93354165E3,1.10838301E3,1},{2.70210802E2,8.77281067E2,1},{1.92740782E3,3.84452219E3,1},{1.12452499E3,8.59329398E2,1},{1.42383670E3,1.19155330E3,1},{1.00490169E3,2.38246083E3,1},{2.78227287E3,5.66876808E2,1},{2.41912890E3,3.27540264E3,1},{1.57139467E3,3.05411534E3,1},{-6.11803886e+01,3.75622434E3,1},{1.25794155E3,9.73829896E2,1},{1.48176842E3,1.66665726E3,1},{6.82888070E2,2.27086740E3,1},{1.04816899E2,3.74388737E3,1},{6.24524517E2,1.88041404E2,1},{3.59050081E2,1.03543694E3,1},{1.80367908E3,1.71534575E3,1},{2.19942593E3,1.43501200E3,1},{1.88424515E3,2.38661728E3,1},{6.44204259e+01,2.12347716E3,1},{1.31005001E3,3.83541132E3,1},{1.67992247E3,2.52050159E3,1},{1.69674445E3,1.33490133E3,1},{-1.48708416e+01,2.60933437E3,1},{2.42930534E3,1.98224418E3,1},{2.05532724E3,1.90451125E3,1},{3.46414454E2,3.25209504E3,1}
//            ,{3.15868498E2,1.15656889E3,1},{4.97283656E2,3.82092025E3,1},{2.45446031E3,1.83309283E3,1},{2.80984547E3,9.64188685E2,1},{1.47506955E3,3.46881744E3,1},{8.01933758E2,2.48825687E3,1},{4.87662170E2,5.10827155E2,1},{9.87477712e+01,3.77616180E3,1},{1.73432217E3,3.46028967E3,1},{2.07616079E3,6.09724564E2,1},{2.02895909E2,3.15771106E3,1},{6.95163950E2,1.86541176E3,1},{2.23167649E3,2.02760987E3,1},{1.51169166E3,2.73271394E3,1},{2.54928051E3,1.86167629E3,1},{1.87057196E2,2.28556097E3,1},{2.20055924E3,3.08940293E3,1}};

//    double[][] polist1 = new double[][]{{414,26,1},{291,176,1},{313,331,1},{333,166,1},{326,77,1},{113,86,1},{27,376,1},{156,253,1},{145,501,1},{367,255,1},{128,118,1},{167,280,1},{516,212,1},{187,281,1},{44,409,1},{172,128,1},{216,109,1},{95,58,1},{437,554,1},{70,61,1},{536,407,1},{145,36,1},{150,135,1},{227,244,1},{224,355,1},{99,264,1},{35,173,1},{122,252,1},{93,228,1},{315,188,1},{311,323,1},{426,396,1},{86,460,1},{212,207,1},{156,115,1},{80,451,1},{434,403,1}
//            ,{211,342,1},{465,24,1},{15,284,1},{59,192,1},{198,83,1},{265,174,1},{270,61,1},{370,242,1},{378,132,1},{381,361,1},{87,38,1},{132,241,1},{101,190,1}};
//
//    double[][] polist2 = new double[][]{{-1.33779634E3,2.79879007E1,1},{-1.48352742E3,1.94448985E2,1},{-1.45483107E3,3.62460413E2,1},{-1.43328292E3,1.81719148E2,1},{-1.43960597E3,8.43534916E1,1},{-1.70478330E3,9.83061236E1,1},{-1.81545924E3,4.33694521E2,1},{-1.64854639E3,2.84774872E2,1},{-1.66428341E3,5.65636917E2,1},{-1.39155324E3,2.76861249E2,1},{-1.68569933E3,1.33417468E2,1},{-1.63408949E3,3.14729116E2,1},{-1.22300693E3,2.24507688E2,1},{-1.61034738E3,3.15094302E2,1},{-1.79463278E3,4.70188651E2,1},{-1.62810391E3,1.43566468E2,1},{-1.57394321E3,1.21479770E2,1},{-1.72580536E3,6.60407563E1,1},{-1.31193666E3,5.94785347E2,1},{-1.75881539E3,6.97028924E1,1},{-1.20063089E3,4.29387208E2,1},{-1.66263228E3,4.09631067E1,1},{-1.65539471E3,1.52368335E2,1},{-1.55979184E3,2.71971996E2,1},{-1.56446282E3,3.95109126E2,1},{-1.72059603E3,3.00840889E2,1},{-1.80567006E3,1.99413005E2,1},{-1.69339287E3,2.86017799E2,1},{-1.72919413E3,2.59705597E2,1},{-1.45380169E3,2.06230340E2,1},{-1.45775345E3,3.53719673E2,1},{-1.32538168E3,4.26090292E2,1},{-1.73968032E3,5.24321489E2,1},{-1.57814876E3,2.30782057E2,1},{-1.64789226E3,1.30261863E2,1},{-1.74761143E3,5.14391840E2,1},{-1.31458524E3,4.32686969E2,1},{-1.58048763E3,3.81591976E2,1},{-1.27887877E3,2.62969983E1,1},{-1.83170388E3,3.27629610E2,1},{-1.77278190E3,2.20231144E2,1},{-1.59811556E3,9.35460019E1,1},{-1.51412345E3,1.92105790E2,1},{-1.50721054E3,6.78950862E1,1},{-1.38823903E3,2.62689570E2,1},{-1.37968557E3,1.43615107E2,1},{-1.37545463E3,3.90674830E2,1},{-1.73727989E3,4.33323957E1,1},{-1.68047676E3,2.72737170E2,1},{-1.71789902E3,2.16062993E2,1}};

//    double[][] polist1 = new double[][]{{143,530,1},{426,274,1},{789,60,1},{149,395,1},{392,115,1},{866,866,1},{831,146,1},{265,751,1},{1286,68,1},{788,44,1},{495,1352,1},{619,69,1},{709,971,1},{819,433,1},{137,1160,1},{302,741,1},{448,543,1},{123,742,1},{183,806,1},{1312,714,1},{87,1259,1},{276,112,1},{307,353,1},{675,410,1},{669,143,1},{1334,279,1},{125,1244,1},{729,1046,1},{533,946,1},{952,416,1},{46,797,1},{382,116,1},{1020,98,1},{290,590,1},{162,718,1},{881,401,1},{552,91,1},{299,207,1},{643,203,1},{255,520,1},{311,72,1},{1252,540,1},{289,651,1},{285,75,1},{740,805,1},{1243,1249,1},{730,717,1},{757,381,1}
//            ,{334,259,1},{255,448,1}};
//
//    double[][] polist2 = new double[][]{{-1.66508060E3,5.97800394E2,1},{-1.32380770E3,2.94874798E2,1},{-9.30737018E2,6.10165344E1,1},{-1.65711652E3,4.44969031E2,1},{-1.36165324E3,1.24859736E2,1},{-8.52900159E2,8.66352064E2,1},{-8.86667684E2,1.46872615E2,1},{-1.51509981E3,8.29946153E2,1},{-4.58097396E2,6.44699006E1,1},{-9.30294127E2,4.50162070E1,1},{-1.24599630E3,1.43558404E3,1},{-1.10839056E3,7.27723654E1,1},{-1.01277341E3,9.95588520E2,1},{-8.99828827E2,4.36325600E2,1},{-1.67554186E3,1.31008596E3,1},{-1.46802492E3,8.12461810E2,1},{-1.29860737E3,5.81024679E2,1},{-1.69083533E3,8.39393083E2,1},{-1.61503002E3,9.02651956E2,1},{-4.35462800E2,6.67784369E2,1},{-1.73967171E3,1.43397899E3,1},{-1.50046367E3,1.23642203E2,1},{-1.46399478E3,3.87553392E2,1},{-1.04850928E3,4.22744155E2,1},{-1.05464107E3,1.48198053E2,1},{-4.16862242E2,2.60692250E2,1},{-1.68908683E3,1.40710160E3,1},{-9.92500282E2,1.06913628E3,1},{-1.20254565E3,9.97917583E2,1},{-7.66666798E2,4.10433984E2,1},{-1.79043594E3,9.13690051E2,1},{-1.37468115E3,1.25984164E2,1},{-7.01679368E2,9.57705821E1,1},{-1.48276952E3,6.48824925E2,1},{-1.64074997E3,8.07257093E2,1},{-8.36408259E2,4.00679106E2,1},{-1.18110403E3,9.57673583E1,1},{-1.47252930E3,2.27980029E2,1},{-1.08130906E3,2.10504396E2,1},{-1.52579268E3,5.75155246E2,1},{-1.45746587E3,7.90201450E1,1},{-4.88515656E2,5.10147595E2,1},{-1.48418003E3,7.16230678E2,1},{-1.49000220E3,8.27360563E1,1},{-9.79882237E2,8.20978191E2,1},{-4.96663830E2,1.18004740E3,1},{-9.89346634E2,7.32323510E2,1},{-9.62442239E2,3.88326475E2,1},{-1.43045481E3,2.83168219E2,1},{-1.52686250E3,4.96340551E2,1}};

    //        double[][] X = new double[][]{{1052,758,90,1},{1851,2190,389,1},{3900,1356,343,1},{3138,1886,89,1},{787,2938,943,1},{1703,1799,699,1},{3027,1798,869,1},{2673,642,767,1},{3023,2381,944,1},{775,1985,653,1},{195,1601,247,1},{1294,2205,279,1},{3515,1543,157,1},{3882,896,920,1},{751,344,463,1},{2161,1428,907,1},{2458,64,494,1},{3060,2765,476,1},{3014,2123,387,1},{2556,2961,624,1},{1767,570,564,1},{706,2732,874,1},{519,1925,986,1},{573,567,295,1},{753,319,285,1},{1593,2379,595,1},{1864,2509,475,1},{2971,541,676,1},{3313,2610,572,1},{491,2456,73,1},{802,408,255,1},{1932,485,244,1},{3505,1101,337,1},{3636,2540,993,1},{1458,930,884,1},{3869,1111,608,1},{2777,2428,455,1},{405,2572,484,1},{901,1930,691,1},{978,1573,249,1},{3055,2967,779,1},{841,1566,656,1},{3214,1343,861,1},{2794,1116,222,1},{858,922,576,1},{2387,1063,776,1},{1989,2693,816,1},{3230,632,483,1},{332,1348,712,1},{1959,1850,950,1}};
    private ArrayList<ImageMarker> MarkerList1 = new ArrayList<ImageMarker>();
    private ArrayList<ImageMarker> MarkerList2 = new ArrayList<ImageMarker>();
//    private ArrayList<ImageMarker> MarkerList1 = Convert2DTo3D.ArrayToMarkerList(new Matrix(polist1),0,0);
//    private ArrayList<ImageMarker> MarkerList2 = Convert2DTo3D.ArrayToMarkerList(new Matrix(polist2),0,0);
//    boolean tag2 = true;
//    boolean tag1 = true;
    boolean tag2 = false;
    boolean tag1 = false;

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

    /**
     * 动态加载 openCV4Android 的库
     */
    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.i("cv", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
        } else {
            Log.i("cv", "OpenCV library found inside package. Using it!");
        }
    }



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
                Toast.makeText(this, "Choosing work mode...", Toast.LENGTH_LONG).show();
                openPictureSelectDialog();
                break;
            case R.id.function:
                boolean flag;
                if (isMutiImg) {

                    if (isP1) {
                        flag = !ImageOpened;
                    } else {
                        flag = !ImageOpened2;
                    }

                } else {
                    flag = !ImageOpened;
                }


                if(flag){
                    Toast.makeText(this, "Please load an image first!", Toast.LENGTH_LONG).show();
                } else {
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

                            Hough_Circle_Fun();//HoughCircle
                           // Circle_Fun();
                            //Hessian_Fun();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case 3:
                        Toast.makeText(MainActivity.this,"Test Function..",Toast.LENGTH_LONG).show();
                        Log.d("TAG", "Click Test Button");
                        try {
//                            Circle_Fun();
                         //  Test_Fun();
                            testforcorner();
                           // Convert2Dto3D_Test();


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
            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        if (isMutiImg) {
                            clear3D_Reconstruction();
                        }
                        checkPermission();
                        ImageOpened = true;
                        break;
                    case 1:
                        if (isMutiImg) {
                            clear3D_Reconstruction();
                        }
                        PickPhotoFromGallery();
                        ImageOpened = true;
                        break;
                    case 2:
                        if (isMutiImg) {
                            clear3D_Reconstruction();
                        } else if (ImageOpened == true) {
                            ImageOpened = false;
                            clearPreImage();
                        }
                        Reconstruction3D();
                        isMutiImg = true;
                        //ImageOpened = true;
                        break;
                }
                dialog.dismiss();
            }
        });
        builder.create().show();
    }


    private void Hough_Circle_Fun() throws Exception {

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        popupView.show();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = myrenderer.GetBitmap();//从myrenderer获取bitmap
                if (bitmap == null) {
                    Log.v("Test_Fun", "Please load img first!");
                    if (Looper.myLooper() == null) {
                        Looper.prepare();
                    }
                    Toast_in_Thread("Please load image first!");
                    Looper.loop();
                    return;
                }
                Bitmap blurbitmap = ImageFilter.blurBitmap(MainActivity.this,bitmap);//高斯滤波
                System.out.println("Enter here(the gauss finished!!)");
                ArrayList<Point> circles = new ArrayList<>();//存放找到的圆
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    try {
                        circles =  HoughCircle.HoughCircle_Fun(blurbitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println("The number of circles:"+circles.size());
                    //Toast.makeText(this, "Circles number:"+circles.size(), Toast.LENGTH_LONG).show();
                    System.out.println("Enter here(the circles have been found!!)");
                }
                Bitmap new_bitmap = Point.DrawCircle(circles,bitmap);
                System.out.println("Enter here(the circles have been drawn!!)");
                //imageView.setImageBitmap(new_bitmap);
                myrenderer.ResetImage(new_bitmap);
                myGLSurfaceView.requestRender();
                System.out.println("Enter here(the bitmap have been updated!!)");
                ImageTools.recycle_fun(bitmap);
                ImageTools.recycle_fun(blurbitmap);
                uiHandler.sendEmptyMessage(1);
            }
        });

        thread.start();
    }



    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void Circle_Fun() throws Exception {

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        popupView.show();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = myrenderer.GetBitmap();//从myrenderer获取bitmap
                if (bitmap == null) {
                    Log.v("Test_Fun", "Please load img first!");
                    if (Looper.myLooper() == null) {
                        Looper.prepare();
                    }
                    Toast_in_Thread("Please load image first!");
                    Looper.loop();
                    return;
                }
            Bitmap blurbitmap = ImageFilter.blurBitmap(MainActivity.this,bitmap);//高斯滤波
            System.out.println("Enter here(the gauss finished!!)");
                /////////////CircleDetect_new/////////////////
                Bitmap new_bitmap = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    try {
                        new_bitmap = CircleDetect_Fun(bitmap,blurbitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                //imageView.setImageBitmap(new_bitmap);
            myrenderer.ResetImage(new_bitmap);
            myGLSurfaceView.requestRender();
            System.out.println("Enter here(the bitmap have been updated!!)");
            ImageTools.recycle_fun(bitmap);
            ImageTools.recycle_fun(blurbitmap);
            uiHandler.sendEmptyMessage(1);
            }
        });

        thread.start();
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
        //////////距离变换////////////
        ImgObj_Para imobj = new ImgObj_Para(bitmap);
        imobj.colorToGray2D(bitmap);
        GSDT_Para p = new GSDT_Para();
        int[][] binay_img = ImageTools.myOTSU(imobj.gray_img,imobj.height,imobj.width,256);
        //p.phi = GSDT_Fun(p, imobj.gray_img);
        p.phi = GSDT_Fun(p, binay_img);//////////////////////////////
        float max_value = p.phi[0][0];
        for(int i=0; i<p.phi.length; i++){
            for(int j=0; j<p.phi[0].length; j++){
                //imobj.tar_img[1][i][j] = (int) p.phi[i][j]/255;
                if(p.phi[i][j]>max_value) max_value = p.phi[i][j];
            }
        }
        for(int i=0; i<p.phi.length; i++){
            for(int j=0; j<p.phi[0].length; j++){
                imobj.tar_img[1][i][j] = (int) (p.phi[i][j]/100)*255;
            }
        }
        Bitmap new_bitmap = imobj.gray2DToBitmap(imobj.tar_img,imobj.width,imobj.height);
        myrenderer.ResetImage(new_bitmap);
        myGLSurfaceView.requestRender();
        /////////////用于函数测试/////////////
/*        Mat src = new Mat();//测试opencv
        Mat temp = new Mat();
        Mat dst = new Mat();
        Utils.bitmapToMat(bitmap,src);//把image转化为Mat
        Imgproc.cvtColor(src,temp,Imgproc.COLOR_BGR2GRAY);//这里由于使用的是Imgproc这个模块所有这里要这么写
        Log.i("CV", "image type:" + (temp.type() == CvType.CV_8UC3));
        Imgproc.cvtColor(temp,dst, Imgproc.COLOR_GRAY2BGR);
        Utils.matToBitmap(dst,bitmap);//把mat转化为bitmap
        myrenderer.ResetImage(bitmap);
        myGLSurfaceView.requestRender();
 */
/*        Bitmap blur_bitmap = ImageFilter.blurBitmap(MainActivity.this,bitmap);
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

 */
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
                if (bitmap == null) {
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
                /*
                Bitmap blurbitmap = ImageFilter.blurBitmap(MainActivity.this,bitmap);//高斯滤波
                /////////////CircleDetect_new/////////////////
                Bitmap new_bitmap = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                   new_bitmap = CircleDetect_Fun(bitmap,blurbitmap);
                }
                 */

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
            //System.out.println("Enter here(the function)");
            ImgObj_Para iobj = new ImgObj_Para(blur_bitmap);
            double dRationHigh=0.84,dRationLow=0.6;///可调
            //double dRationHigh=0.85,dRationLow=0.5;///可调
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                EdgeDetect_fun.Canny_edge(iobj,blur_bitmap,dRationHigh,dRationLow);
            }
            //System.out.println("Enter here1");
            ImageTools.recycle_fun(bitmap);
            ImageTools.recycle_fun(blur_bitmap);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                myrenderer.ResetImage(iobj.gray2DToBitmap(iobj.tar_img,iobj.width,iobj.height));
            }
            // myrenderer.ResetImg(iobj.EdgeImage);
            //imageView.setImageBitmap(iobj.EdgeImage);

            //myrenderer.ResetImg(blur_bitmap);
            //System.out.println("Enter here2");
            myGLSurfaceView.requestRender();

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
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            try {
                                myrenderer.SetPath(currentPhotoPath);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        //System.out.println(showPic.getAbsolutePath());


                        //清空工作区marker
                        ArrayList<ImageMarker> temp = myrenderer.getMarkerList();
                        if (temp.size() != 0) {
                            temp.clear();
                            myrenderer.ResetMarkerlist(temp);
                        }


                        myGLSurfaceView.requestRender();
                        if (isMutiImg) {
                            if (isP1) {
                                showimg1 = myrenderer.GetBitmap();
                                img1 = showimg1.copy(Bitmap.Config.ARGB_8888, false);
                                //System.out.println(img1 == null);
                            } else {
                                showimg2 = myrenderer.GetBitmap();
                                img2 = showimg2.copy(Bitmap.Config.ARGB_8888, false);
                                //System.out.println(img2 == null);
                            }
                        }

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
                    try {
                        myrenderer.SetPath(path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (isMutiImg) {
                        boolean flag1,flag2;
                        if (isP1 && ImageOpened) {
                            flag1 = false;
                        } else {
                            flag1 = ImageOpened;
                        }
                        if (isP1 && ImageOpened2) {
                            flag2 = false;
                        } else {
                            flag2 = ImageOpened2;
                        }
                        if (flag1 || flag2) {
                            Bitmap temp1 = myrenderer.GetBitmap();
                            Bitmap temp2;
                            if (isP1) {
                                temp2 = img2;
                            } else {
                                temp2 = img1;
                            }
                            if ((temp1.getWidth() != temp2.getWidth()) || (temp1.getHeight() != temp2.getHeight())) {
                                Toast.makeText(getContext(), "Please load images token by the same camera!", Toast.LENGTH_LONG).show();
                                if (isP1) {
                                    if (ImageOpened) {
                                        myrenderer.ResetImage(showimg1,img1);
                                        myrenderer.ResetMarkerlist(MarkerList1);
                                        myGLSurfaceView.requestRender();
                                    } else {
                                        clearPreImage();
                                    }

                                } else {
                                    if (ImageOpened2) {
                                        myrenderer.ResetImage(showimg2,img2);
                                        myrenderer.ResetMarkerlist(MarkerList2);
                                        myGLSurfaceView.requestRender();
                                    } else {
                                        clearPreImage();
                                    }

                                }

                                return;

                            }
                        }

                        if (isP1) {
                            Log.v("loadImage", "load Image1");
                            showimg1 = myrenderer.GetBitmap();
                            img1 = showimg1.copy(Bitmap.Config.ARGB_8888, false);
                            MarkerList1.clear();
                            ImageOpened = true;
                        } else {
                            Log.v("loadImage", "load Image2");
                            showimg2 = myrenderer.GetBitmap();
                            img2 = showimg2.copy(Bitmap.Config.ARGB_8888, false);
                            MarkerList2.clear();
                            ImageOpened2 = true;
                        }
                    } else {
                        //清空工作区marker
                        ArrayList<ImageMarker> temp = myrenderer.getMarkerList();
                        if (temp.size() != 0) {
                            temp.clear();
                            myrenderer.ResetMarkerlist(temp);
                        }
                        myGLSurfaceView.requestRender();
                    }




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

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void Reconstruction3D() {

        img_switch = new Button(this);
        img_switch.setText("P1");
        img_switch.setAllCaps(false);
        img_switch.setTextColor(Color.RED);
        FrameLayout.LayoutParams params_img_switch = new FrameLayout.LayoutParams(230, 120);
        params_img_switch.gravity = Gravity.TOP | Gravity.LEFT;
        params_img_switch.setMargins(50,20,0,0);
        this.addContentView(img_switch,params_img_switch);//
        img_switch.setVisibility(View.VISIBLE);

        loadImage = new Button(this);
        loadImage.setText("Load");
        loadImage.setAllCaps(false);
        loadImage.setTextColor(Color.BLACK);
        FrameLayout.LayoutParams params_loadImage = new FrameLayout.LayoutParams(230, 120);
        params_loadImage.gravity = Gravity.TOP | Gravity.LEFT;
        params_loadImage.setMargins(300,20,0,0);
        this.addContentView(loadImage,params_loadImage);//
        loadImage.setVisibility(View.VISIBLE);

        select_points = new Button(this);
        select_points.setText("Point");
        select_points.setAllCaps(false);
        select_points.setTextColor(Color.BLACK);
        FrameLayout.LayoutParams params_select_points = new FrameLayout.LayoutParams(230, 120);
        params_select_points.gravity = Gravity.TOP | Gravity.LEFT;
        params_select_points.setMargins(550,20,0,0);
        this.addContentView(select_points,params_select_points);//
        select_points.setVisibility(View.VISIBLE);

        finished = new Button(this);
        finished.setText("Finish");
        finished.setAllCaps(false);
        finished.setTextColor(Color.BLACK);
        FrameLayout.LayoutParams params_finished = new FrameLayout.LayoutParams(230, 120);
        params_finished.gravity = Gravity.TOP | Gravity.LEFT;
        params_finished.setMargins(800,20,0,0);
        this.addContentView(finished,params_finished);//
        finished.setVisibility(View.VISIBLE);

        Process = new Button(this);
        Process.setText("Process");
        Process.setAllCaps(false);
        Process.setTextColor(Color.RED);
        FrameLayout.LayoutParams params_Process = new FrameLayout.LayoutParams(230, 120);
        params_Process.gravity = Gravity.TOP | Gravity.RIGHT;
        params_Process.setMargins(0,20,50,00);
        this.addContentView(Process,params_Process);//
        Process.setVisibility(View.GONE);

        img_switch.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {

                clearSelect_points();
                if (isP1){
//                    MarkerList1 = myrenderer.getMarkerList();
                    ////////////////////////
                    if (tag1) {
                        for (int i = 0; i < polist1.length; i++) {
                            double temp = polist1[i][0];
                            polist1[i][0] = polist1[i][1];
                            polist1[i][1] = temp;
                            /*polist1[i][0] += img1.getWidth()/2;
                            polist1[i][1] += img1.getHeight()/2;*/
                        }
                        MarkerList1 = Convert2DTo3D.ArrayToMarkerList(new Matrix(polist1),0,0,3);
                        int a = 0;
                        for (ImageMarker imageMarker:MarkerList1) {
                            System.out.println(a+" "+imageMarker.x);
                            System.out.println(a+" "+imageMarker.y);
                            a++;
                        }
                        tag1 = false;
                    } else {
                        MarkerList1 = myrenderer.getMarkerList();
                    }
                    ////////////////////////
                    //img1 = myrenderer.GetBackupBitmap();
                    showimg1 = myrenderer.GetBitmap();
                    Log.v("img_switch", "Change to P2");
                    isP1 = !isP1;
                    img_switch.setText("P2");
                    functionMenu(isFinished2);

                    //display points
                    myrenderer.ResetMarkerlist(MarkerList2);
                    //System.out.println("MarkerList2 is empty: "+ MarkerList2.isEmpty());

                    if (ImageOpened2) {
                        myrenderer.ResetImage(showimg2,img2);


                        myGLSurfaceView.requestRender();
                    } else {
                        if (ImageOpened) {
                            clearPreImage();
                        }
                        //return;
                    }

                } else {
//                    MarkerList2 = myrenderer.getMarkerList();
                    ////////////////////////
                    if (tag2) {
                        for (int i = 0; i < polist2.length; i++) {
                            double temp = polist2[i][0];
                            polist2[i][0] = polist2[i][1];
                            polist2[i][1] = temp;
                            /*polist2[i][0] += img2.getWidth()/2;
                            polist2[i][1] += img2.getHeight()/2;*/
                        }
                        MarkerList2 = Convert2DTo3D.ArrayToMarkerList(new Matrix(polist2),0,0, 3);
                        int a = 0;
                        for (ImageMarker imageMarker:MarkerList2) {
                            System.out.println(a+" "+imageMarker.x);
                            System.out.println(a+" "+imageMarker.y);
                            a++;
                        }
                        tag2 = false;
                    } else {
                        MarkerList2 = myrenderer.getMarkerList();
                    }
                    ////////////////////////
                    //img2 = myrenderer.GetBackupBitmap();
                    showimg2 = myrenderer.GetBitmap();
                    Log.v("img_switch", "Change to P1");
                    isP1 = !isP1;
                    img_switch.setText("P1");
                    functionMenu(isFinished1);

                    //display points
                    myrenderer.ResetMarkerlist(MarkerList1);

                    if (ImageOpened) {
                        myrenderer.ResetImage(showimg1,img1);

                        myGLSurfaceView.requestRender();
                    } else {
                        if (ImageOpened2) {
                            clearPreImage();
                        }
                        //return;
                    }

                }
            }
        });

        //loadimage
        loadImage.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v("load image", "Image loading!");
                Loadimage(v);
            }
        });

        select_points.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                //测试解方程函数
//                double[][] array = {{0,2,7},{1,1,1},{2,5,3}};
//                double[][] array2 = {{32},{7},{24}};
//                System.out.println(array[0][0]+","+array[1][0]+","+array[2][0]);
//                System.out.println(array[0][1]+","+array[1][1]+","+array[2][1]);
//                System.out.println(array2[0][0]+","+array2[1][0]+","+array2[2][0]);
//                double[] result = Convert2DTo3D.linerFunction3_3(new Matrix(array),new Matrix(array2));
//
//                System.out.println("方程的解为：");
//                System.out.println(result[0]+","+result[1]+","+result[2]);
               // ORBTest(MarkerList1, MarkerList2);//ORB提取匹配点
                Log.v("Select_points", "Point selecting!");
                if (isP1) {
                    if (img1 == null) {
                        Toast.makeText(context, "Please load a image first", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    if (img2 == null) {
                        Toast.makeText(context, "Please load a image first", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                SelectPoint(v);


            }
        });

        //finished
        finished.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.v("Finished", "Finish point select of the image!");
                boolean flag = false;
                if (isP1) {
                    if (img1 == null) {
                        Toast.makeText(context, "Please load a image first", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    if (img2 == null) {
                        Toast.makeText(context, "Please load a image first", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                clearSelect_points();
             //   Mat m = new Mat();
//                ORBTest(MarkerList1, MarkerList2);//ORB提取匹配点

                if (isP1){
                    MarkerList1 = myrenderer.getMarkerList();
                    int pointnum1 = MarkerList1.size();
                    int pointnum2 = MarkerList2.size();
                    System.out.println("flag = "+flag);

                    if (!isProcessed) {
                        flag = pointnum1 > 8;
                    } else {
                        flag = pointnum1 > 0;
                    }
                    if (flag) {
                        if (isFinished2) {
                            if (pointnum1 == pointnum2) {
                                isFinished1 = true;
                                functionMenu(true);
                            } else {
                                Toast.makeText(getContext(), "Point number is insufficient, please add more!", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            if (pointnum1 >= pointnum2) {
                                isFinished1 = true;
                                functionMenu(true);
                                myrenderer.setMarkerNum(MarkerList1.size());
                            } else {
                                Toast.makeText(getContext(), "Point number is insufficient, please add more!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        if (!isProcessed) {
                            Toast.makeText(getContext(), "Point number must be more than 8!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Point number must be more than 0!", Toast.LENGTH_SHORT).show();
                        }

                    }


                } else {
                    MarkerList2 = myrenderer.getMarkerList();
                    int pointnum1 = MarkerList1.size();
                    int pointnum2 = MarkerList2.size();
                    System.out.println("flag = "+flag);
                    System.out.println("MarkerList1.isEmpty() = "+MarkerList1.isEmpty());
                    System.out.println("MarkerList2.isEmpty() = "+MarkerList2.isEmpty());

                    if (!isProcessed) {
                        flag = pointnum2 > 8;
                    } else {
                        flag = pointnum2 > 0;
                    }
                    if (flag) {
                        if (isFinished1) {
                            if (pointnum1 == pointnum2) {
                                isFinished2 = true;
                                functionMenu(true);
                            } else {
                                Toast.makeText(getContext(), "Point number is insufficient, please add more!", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            if (pointnum1 <= pointnum2) {
                                isFinished2 = true;
                                functionMenu(true);
                                myrenderer.setMarkerNum(MarkerList2.size());
                            } else {
                                Toast.makeText(getContext(), "Point number is insufficient, please add more!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        if (!isProcessed) {
                            Toast.makeText(getContext(), "Point number must be more than 8!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Point number must be more than 0!", Toast.LENGTH_SHORT).show();
                        }
                    }

                }


                if (isFinished1 & isFinished2) {
                    img_switch.setVisibility(View.GONE);
                    Process.setVisibility(View.VISIBLE);
                }
            }
        });

        //Process
        Process.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v("Process", "Do 2D to 3D reconstruction!");
                //图一点集为MarkerList1
                //图二点集为MarkerList2
                if (!isProcessed) {
                    isProcessed = true;
                    double[][] Po_list1 = MarkerListToArray(MarkerList1);
                    double[][] Po_list2 = MarkerListToArray(MarkerList2);

                    ///////////////////2Dto3D/////////////////////
                    //原点调整为图像中心
/*                    for (int i = 0; i < Po_list1.length;i++) {
                        Po_list1[i][0] -= img1.getWidth()/2f;
                        Po_list1[i][1] -= img1.getHeight()/2f;
                        Po_list2[i][0] -= img2.getWidth()/2f;
                        Po_list2[i][1] -= img2.getHeight()/2f;

                    }*/
//                    Convert2DTo3D_new p = new Convert2DTo3D_new();
                    Convert2DTo3D p = new Convert2DTo3D();
//                    p.MyMobileModel = Convert2DTo3D_new.MobileModel.MIX2;
                    p.MyMobileModel = Convert2DTo3D.MobileModel.MIX2;
 //                   p.Convert2DTo3D_Fun(Po_list1, Po_list2,p.MyMobileModel);
//                    p.Convert2DTo3D_Fun(Po_list1, Po_list2, p.MyMobileModel);
                    //计算相机外参矩阵
//                    p.OpticalCenter[0] = img1.getWidth()/2;
//                    p.OpticalCenter[1] = img1.getHeight()/2;
                    p.OpticalCenter[0] = 0;
                    p.OpticalCenter[1] = 0;
//                    boolean flag = p.Convert2DTo3D_Fun_new(Po_list1, Po_list2,p.MyMobileModel);//非自标定
                    boolean flag = p.Convert2DTo3D_Fun(Po_list1, Po_list2, p.MyMobileModel);//自标定
                    if (!flag) {
                        clear3D_Reconstruction();
                        clearPreImage();
                        return;
                    }

                    p.Point3DTo2D(p.X_3D);
                    MarkerList1.addAll(p.Point3Dto2D1);
                    MarkerList2.addAll(p.Point3Dto2D2);
                    myGLSurfaceView.requestRender();
                    ///////////////////////////////////


//                    ArrayList<ImageMarker> Point3D = ArrayToMarkerList(p.X_3D);//保存三维坐标
//                    for(ImageMarker im: Point3D){
//                        Log.d("TestConvert3DFun","=========x:"+im.x+","+"y:"+im.y+","+"z:"+im.z);
//                    }
 //                   p.CalculateError(p.X_3D, Po_list1, Po_list2);


                    //测试LM算法/////////////////////////////
//                    Matrix Po1 = p.Point2DToHomogeneous(Po_list1);//n*3矩阵
//                    Matrix Po2 = p.Point2DToHomogeneous(Po_list2);//n*3矩阵
//                    BundleAdjustment_LM.LM(Po1, Po2, p.P1, p.P2_Selected, p.X_3D);

                    ///////////////测试极线/////////////////////////

                    showimg2 = p.DrawLine(showimg2, p.EpiLines2_Para);
                    showimg1 = p.DrawLine(showimg1, p.EpiLines1_Para);

                    if (isP1) {
                        myrenderer.ResetImage(showimg1);
                    } else {
                        myrenderer.ResetImage(showimg2);
                    }
                    myGLSurfaceView.requestRender();
                    ///////////////////////////////////////////////

                    //单应矩阵
                    /*Convert2DTo3D p2 = new Convert2DTo3D();
                    p2.Convert2DTo2D_func(Po_list1, Po_list2);
                    MarkerList1.addAll(p2.Point2Dto2D1);
                    MarkerList2.addAll(p2.Point2Dto2D2);
                    myGLSurfaceView.requestRender();*/



                    //提示运行完成
                    Toast.makeText(getContext(), "The external parameter matrix of camera is calculated!", Toast.LENGTH_SHORT).show();
                    //初始化下一阶段界面按钮
                    initiateForReconstruction3D();
                } else {
                    isProcessed2 = !isProcessed2;
                    //目标点投射到三维
                    if (isP1) {
                        myrenderer.ResetMarkerlist(MarkerList1);
                    } else {
                        myrenderer.ResetMarkerlist(MarkerList2);
                    }
                    myGLSurfaceView.requestRender();
                }



                //改变marker颜色可以直接对相应ImageMarker对象的type属性进行修改，深蓝色为3号，type支持1-8
            }
        });

    }


    ///////////////////////////////
    private void testforcorner() {
        ArrayList<ImageMarker> temp = new ArrayList<>();
        temp = myrenderer.globalImageCornerDetect();
        System.out.println("temp.size() = "+temp.size());
        myrenderer.ResetMarkerlist(temp);
        myGLSurfaceView.requestRender();

        /*double[][] array = new double[][]{{7.5,6.2,3},{6,0,-3},{0,6,-1}};
        Matrix A = new Matrix(array);
        Matrix b = new Matrix(3,1);
        b.set(0,0,79.09);
        b.set(1,0,9.6);
        b.set(2,0,12.2);*/


        /*double[][] array = new double[][]{{5,-2,7,1},{3,1,-2,-3},{0,4,3,7.5},{2,3.4,0,-6.7}};
        Matrix A = new Matrix(array);
        Matrix b = new Matrix(4,1);
        b.set(0,0,50.3);
        b.set(1,0,-7.9);
        b.set(2,0,43.2);
        b.set(3,0,5.84);
        double[] result = new double[4];
        Matrix Ab = new Matrix(4,5);
        Ab.setMatrix(0,3,0,3,A);
        Ab.setMatrix(0,3,4,4,b.times(-1));*/

        /*double[][] array = new double[][]{{-4,0,0,1},{-4,-3,2,1},{0,-2,0,1},{-3,-2,1,1}};
        Matrix A = new Matrix(array);
        Matrix b = new Matrix(4,1);
        b.set(0,0,0);
        b.set(1,0,0);
        b.set(2,0,0);
        b.set(3,0,0);
        double[] result = new double[4];
        Matrix Ab = new Matrix(4,5);
        Ab.setMatrix(0,3,0,3,A);
        Ab.setMatrix(0,3,4,4,b.times(-1));*/

//        result = A.solve(b).getColumnPackedCopy();
//        result = Convert2DTo3D.linerFunction3_3(A,b);
        /*System.out.println("result[0] = "+result[0]);
        System.out.println("result[1] = "+result[1]);
        System.out.println("result[2] = "+result[2]);
        System.out.println("result[3] = "+result[3]);*/
        //System.out.println("result[4] = "+result[4]);

    }

    ///////////////////////////////

    private void ORBTest(ArrayList<ImageMarker> MarkerList1, ArrayList<ImageMarker> MarkerList2){
        Mat srcr = new Mat();
        Mat tempr = new Mat();
        Mat srcl = new Mat();
        Mat templ = new Mat();
        Mat MatdescriptorR = new Mat();
        Mat MatdescriptorL = new Mat();
        MatOfKeyPoint keypointsR = new MatOfKeyPoint();
        MatOfKeyPoint keypointsL = new MatOfKeyPoint();
        MatOfDMatch matches = new MatOfDMatch();
//        Bitmap imageR = BitmapFactory.decodeResource(this.getResources(),R.drawable.rr);
//        Bitmap imageL = BitmapFactory.decodeResource(this.getResources(),R.drawable.ll);

        Bitmap imageL = img1;
        Bitmap imageR = img2;
        Utils.bitmapToMat(imageR, srcr);//把image转化为Mat
        Utils.bitmapToMat(imageL, srcl);//把image转化为Mat
        Imgproc.cvtColor(srcr,tempr, Imgproc.COLOR_RGBA2RGB);
        Imgproc.cvtColor(srcl,templ, Imgproc.COLOR_RGBA2RGB);
        FeatureDetector detectorR = FeatureDetector.create(FeatureDetector.GRID_ORB);
        FeatureDetector detectorL = FeatureDetector.create(FeatureDetector.GRID_ORB);
        //检测特征点
        detectorR.detect(tempr, keypointsR);
        detectorL.detect(templ, keypointsL);
        /*Features2d.drawKeypoints(tempr,keypointsR,out,new Scalar(255, 0, 0), Features2d.DRAW_RICH_KEYPOINTS);
        Utils.matToBitmap(out, imageR);//把mat转化为bitmap

        imageView.setImageBitmap(imageR);*/

        //计算描述子
        DescriptorExtractor descriptorR = DescriptorExtractor.create(DescriptorExtractor.ORB);
        DescriptorExtractor descriptorL = DescriptorExtractor.create(DescriptorExtractor.ORB);
        descriptorR.compute(srcr,keypointsR,MatdescriptorR);
        descriptorL.compute(srcl,keypointsL,MatdescriptorL);
        // FlannBasedMatcher descriptormatcher = FlannBasedMatcher.create();
        DescriptorMatcher descriptormatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        descriptormatcher.match(MatdescriptorL,MatdescriptorR,matches);
        System.out.println("............."+matches.cols()+","+matches.rows());
        //-- Quick calculation of max and min distances between keypoints
        double max_dist = 0; double min_dist = 100;
        for( int i = 0; i < MatdescriptorL.rows(); i++ ) {
            double dist = matches.toArray()[i].distance;
            if( dist < min_dist ) min_dist = dist;
            if( dist > max_dist ) max_dist = dist;
        }

        System.out.println("-- Max dist : "+ max_dist );
        System.out.println("-- Min dist : "+ min_dist );
        MatOfDMatch good_matches = new MatOfDMatch();
        for( int i = 0; i < MatdescriptorL.rows(); i++ ) {
            if( matches.toArray()[i].distance <= StrictMath.max(2*min_dist, 20) ) {
                good_matches.push_back( matches.row(i));
            }
        }
        System.out.println("............."+good_matches.cols()+","+good_matches.rows());
        DMatch[] matchItem = good_matches.toArray();
        System.out.println("....@@@@@@@@@@...."+MarkerList1.isEmpty());
        System.out.println("%%!!!!!!!!!!!!!!!!!!%%% MarkerList Length : "+ MarkerList1.size()+"," + MarkerList2.size());
        for(int i=0; i<good_matches.rows(); i++){
//            System.out.println("....@@@@@@@@@@...."+matchItem.length);
            MarkerList1.add(new ImageMarker((float) keypointsL.toList().get(matchItem[i].queryIdx).pt.x, (float)keypointsL.toList().get(matchItem[i].queryIdx).pt.y, 0, 3));
            MarkerList2.add(new ImageMarker((float) keypointsR.toList().get(matchItem[i].trainIdx).pt.x, (float)keypointsR.toList().get(matchItem[i].trainIdx).pt.y, 0,3));
            Log.d("Mainactivity",i+":"+good_matches.toList().get(i));
            Log.d("LImage",i+":("+keypointsL.toList().get(matchItem[i].queryIdx).pt.x+","+keypointsL.toList().get(matchItem[i].queryIdx).pt.y);
            Log.d("RImage",i+":("+keypointsR.toList().get(matchItem[i].trainIdx).pt.x+","+keypointsR.toList().get(matchItem[i].trainIdx).pt.y);
            Log.d("RImage",MarkerList1.get(i).x+","+MarkerList1.get(i).y);
        }
        System.out.println("%%%%%%%%%%%%%%%%% MarkerList Length : "+ MarkerList1.size()+"," + MarkerList2.size());
        myGLSurfaceView.requestRender();



//        Bitmap outbmp = drawMatches(srcl,keypointsL,srcr,keypointsR,good_matches,false);

        //Features2d.drawMatches(srcl,keypointsL,srcr,keypointsR,matches,out);
        //  Utils.matToBitmap(out, imageR);//把mat转化为bitmap
        // Bitmap matchbitmap=Bitmap.createScaledBitmap(imageR, imageL.getWidth(),  imageR.getHeight(), false);
//        imageView.setImageBitmap(outbmp);
        srcr.release();
        srcl.release();
        templ.release();
        tempr.release();
        MatdescriptorR.release();
        MatdescriptorL.release();
    }


    /**
     * 可以画出匹配点
     * @param img1
     * @param key1
     * @param img2
     * @param key2
     * @param matches
     * @param imageOnly
     * @return
     */
    static Bitmap drawMatches(Mat img1, MatOfKeyPoint key1, Mat img2, MatOfKeyPoint key2, MatOfDMatch matches, boolean imageOnly) {
        Mat out = new Mat();
        Mat im1 = new Mat();
        Mat im2 = new Mat();
        Imgproc.cvtColor(img1, im1, Imgproc.COLOR_BGR2RGB);
        Imgproc.cvtColor(img2, im2, Imgproc.COLOR_BGR2RGB);
        if (imageOnly) {
            MatOfDMatch emptyMatch = new MatOfDMatch();
            MatOfKeyPoint emptyKey1 = new MatOfKeyPoint();
            MatOfKeyPoint emptyKey2 = new MatOfKeyPoint();
            Features2d.drawMatches(im1, emptyKey1, im2, emptyKey2, emptyMatch, out);
        } else {
            Features2d.drawMatches(im1, key1, im2, key2, matches, out);
        }
        System.out.println(out.cols()+","+out.rows());
        Bitmap bmp = Bitmap.createBitmap(out.cols(), out.rows(), Bitmap.Config.ARGB_8888);
        Imgproc.cvtColor(out, out, Imgproc.COLOR_BGR2RGB);
        Utils.matToBitmap(out, bmp);//把mat转化为bitmap
        return bmp;
    }


    /**
     * 测试外参矩阵的计算以及恢复三维坐标
     *
     */

    private void Convert2Dto3D_Test(){
        Mat m = new Mat();
        Convert2DTo3D p = new Convert2DTo3D();
        p.MyMobileModel = Convert2DTo3D.MobileModel.MIX2;
//        p.MyMobileModel = Convert2DTo3D.MobileModel.HUAWEI;
//        double[][] Po_list1 = new double[][]{{326, 314}, {325, 369}, {332, 445}, {334, 506}, {377, 314}, {377, 370}, {388, 426}, {392, 478}, {438, 312}};
//        double[][] Po_list2 = new double[][]{{456, 402}, {471, 465}, {486, 511}, {506, 557}, {497, 385}, {515, 424}, {536, 469}, {548, 512}, {539, 361}};
//        double[][] polist1 = new double[][]{{1052,758,1},{1851,2190,1},{3900,1356,1},{3138,1886,1},{787,2938,1},{1703,1799,1},{3027,1798,1},{2673,642,1},{3023,2381,1},{775,1985,1},{195,1601,1},{1294,2205,1},{3515,1543,1},{3882,896,1},{751,344,1},{2161,1428,1},{2458,64,1},{3060,2765,1},{3014,2123,1},{2556,2961,1},{1767,570,1},{706,2732,1},{519,1925,1},{573,567,1},{753,319,1},{1593,2379,1},{1864,2509,1},{2971,541,1},{3313,2610,1},{491,2456,1},{802,408,1},{1932,485,1},{3505,1101,1},{3636,2540,1},{1458,930,1},{3869,1111,1},{2777,2428,1},{405,2572,1},{901,1930,1},{978,1573,1},{3055,2967,1},{841,1566,1},{3214,1343,1},{2794,1116,1},{858,922,1},{2387,1063,1},{1989,2693,1},{3230,632,1},{332,1348,1},{1959,1850,1}};
//        double[][] polist2 = new double[][]{{1017.2,742.7,1},{1746.9,2183,1},{3825.9,1426.6,1},{3059,1799.4,1},{627.8,3078.1,1},{1597,1948.8,1},{2910.2,2032.8,1},{2604.4,929.9,1},{2880.9,2596.3,1},{665.9,2089.1,1},{122.3,1566.7,1},{1195.9,2145.5,1},{3444.4,1517.8,1},{3794.3,1238.7,1},{712,511,1},{2057,1699.1,1},{2425.3,288.3,1},{2928.5,2758.8,1},{2910.8,2135.1,1},{2410.2,2991.7,1},{1713.1,771,1},{558,2860.8,1},{394.9,2165.6,1},{535,645,1},{724.4,416.3,1},{1471.4,2436,1},{1743.6,2509.7,1},{2910.5,804.4,1},{3181.7,2658.9,1},{395.7,2282,1},{771.6,486.2,1},{1897.8,565.9,1},{3441.1,1186.1,1},{3484.6,2768.9,1},{1374.4,1225.8,1},{3789.9,1309.2,1},{2659.3,2438.6,1},{283.9,2553.2,1},{791.8,2055.6,1},{905,1551.4,1},{2900.1,3065.9,1},{747,1707.8,1},{3114,1615.6,1},{2736.6,1144.7,1},{791.8,1086.7,1},{2302.9,1315.1,1},{1843.7,2817.3,1},{3176,812.8,1},{243.8,1524.9,1},{1837.5,2100,1}};
//        double[][] X = new double[][]{{1052,758,90,1},{1851,2190,389,1},{3900,1356,343,1},{3138,1886,89,1},{787,2938,943,1},{1703,1799,699,1},{3027,1798,869,1},{2673,642,767,1},{3023,2381,944,1},{775,1985,653,1},{195,1601,247,1},{1294,2205,279,1},{3515,1543,157,1},{3882,896,920,1},{751,344,463,1},{2161,1428,907,1},{2458,64,494,1},{3060,2765,476,1},{3014,2123,387,1},{2556,2961,624,1},{1767,570,564,1},{706,2732,874,1},{519,1925,986,1},{573,567,295,1},{753,319,285,1},{1593,2379,595,1},{1864,2509,475,1},{2971,541,676,1},{3313,2610,572,1},{491,2456,73,1},{802,408,255,1},{1932,485,244,1},{3505,1101,337,1},{3636,2540,993,1},{1458,930,884,1},{3869,1111,608,1},{2777,2428,455,1},{405,2572,484,1},{901,1930,691,1},{978,1573,249,1},{3055,2967,779,1},{841,1566,656,1},{3214,1343,861,1},{2794,1116,222,1},{858,922,576,1},{2387,1063,776,1},{1989,2693,816,1},{3230,632,483,1},{332,1348,712,1},{1959,1850,950,1}};

//        double[][] polist1 = new double[][]{{1274,415,1},{1537,1007,1},{354,2652,1},{2276,1344,1},{1029,2173,1},{2356,1119,1},{2999,960,1},{317,827,1},{2100,3982,1},{1179,704,1},{1476,1255,1},{1112,2470,1},{2827,357,1},{2570,3346,1},{1725,2966,1},{100,3985,1},{1302,1010,1},{1561,1677,1},{785,2355,1},{268,3941,1},{637,154,1},{435,724,1},{1909,1438,1},{2267,1451,1},{2001,2358,1},{185,1901,1},{1492,3856,1},{1792,2626,1},{1779,1117,1},{114,2567,1},{2520,2034,1},{2140,1985,1},{514,3112,1},{377,1094,1},{676,3874,1},{2554,1692,1},{2870,797,1},{1643,3447,1},{904,2698,1},{511,532,1},{276,3822,1},{1876,3750,1},{2131,304,1},{348,3235,1},{772,2021,1},{2341,1881,1},{1639,2777,1},{2658,1626,1},{293,2333,1},{2357,2986,1}};

//        double[][] polist2 = new double[][]{{1.23076104E3,5.90092851E2,1},{1.47187979E3,1.13355665E3,1},{2.33181897E2,2.60191686E3,1},{2.19760944E3,1.44998958E3,1},{8.97328699E2,2.38214152E3,1},{2.27887361E3,1.29769766E3,1},{2.93354165E3,1.10838301E3,1},{2.70210802E2,8.77281067E2,1},{1.92740782E3,3.84452219E3,1},{1.12452499E3,8.59329398E2,1},{1.42383670E3,1.19155330E3,1},{1.00490169E3,2.38246083E3,1},{2.78227287E3,5.66876808E2,1},{2.41912890E3,3.27540264E3,1},{1.57139467E3,3.05411534E3,1},{-6.11803886e+01,3.75622434E3,1},{1.25794155E3,9.73829896E2,1},{1.48176842E3,1.66665726E3,1},{6.82888070E2,2.27086740E3,1},{1.04816899E2,3.74388737E3,1},{6.24524517E2,1.88041404E2,1},{3.59050081E2,1.03543694E3,1},{1.80367908E3,1.71534575E3,1},{2.19942593E3,1.43501200E3,1},{1.88424515E3,2.38661728E3,1},{6.44204259e+01,2.12347716E3,1},{1.31005001E3,3.83541132E3,1},{1.67992247E3,2.52050159E3,1},{1.69674445E3,1.33490133E3,1},{-1.48708416e+01,2.60933437E3,1},{2.42930534E3,1.98224418E3,1},{2.05532724E3,1.90451125E3,1},{3.46414454E2,3.25209504E3,1}
//                ,{3.15868498E2,1.15656889E3,1},{4.97283656E2,3.82092025E3,1},{2.45446031E3,1.83309283E3,1},{2.80984547E3,9.64188685E2,1},{1.47506955E3,3.46881744E3,1},{8.01933758E2,2.48825687E3,1},{4.87662170E2,5.10827155E2,1},{9.87477712e+01,3.77616180E3,1},{1.73432217E3,3.46028967E3,1},{2.07616079E3,6.09724564E2,1},{2.02895909E2,3.15771106E3,1},{6.95163950E2,1.86541176E3,1},{2.23167649E3,2.02760987E3,1},{1.51169166E3,2.73271394E3,1},{2.54928051E3,1.86167629E3,1},{1.87057196E2,2.28556097E3,1},{2.20055924E3,3.08940293E3,1}};

//        double[][] polist1 = new double[][]{{143,530,1},{426,274,1},{789,60,1},{149,395,1},{392,115,1},{866,866,1},{831,146,1},{265,751,1},{1286,68,1},{788,44,1},{495,1352,1},{619,69,1},{709,971,1},{819,433,1},{137,1160,1},{302,741,1},{448,543,1},{123,742,1},{183,806,1},{1312,714,1},{87,1259,1},{276,112,1},{307,353,1},{675,410,1},{669,143,1},{1334,279,1},{125,1244,1},{729,1046,1},{533,946,1},{952,416,1},{46,797,1},{382,116,1},{1020,98,1},{290,590,1},{162,718,1},{881,401,1},{552,91,1},{299,207,1},{643,203,1},{255,520,1},{311,72,1},{1252,540,1},{289,651,1},{285,75,1},{740,805,1},{1243,1249,1},{730,717,1},{757,381,1},{334,259,1},{255,448,1}};

//        double[][] polist2 = new double[][]{{-1.66508060E3,5.97800394E2,1},{-1.32380770E3,2.94874798E2,1},{-9.30737018E2,6.10165344E1,1},{-1.65711652E3,4.44969031E2,1},{-1.36165324E3,1.24859736E2,1},{-8.52900159E2,8.66352064E2,1},{-8.86667684E2,1.46872615E2,1},{-1.51509981E3,8.29946153E2,1},{-4.58097396E2,6.44699006E1,1},{-9.30294127E2,4.50162070E1,1},{-1.24599630E3,1.43558404E3,1},{-1.10839056E3,7.27723654E1,1},{-1.01277341E3,9.95588520E2,1},{-8.99828827E2,4.36325600E2,1},{-1.67554186E3,1.31008596E3,1},{-1.46802492E3,8.12461810E2,1},{-1.29860737E3,5.81024679E2,1},{-1.69083533E3,8.39393083E2,1},{-1.61503002E3,9.02651956E2,1},{-4.35462800E2,6.67784369E2,1},{-1.73967171E3,1.43397899E3,1},{-1.50046367E3,1.23642203E2,1},{-1.46399478E3,3.87553392E2,1},{-1.04850928E3,4.22744155E2,1},{-1.05464107E3,1.48198053E2,1},{-4.16862242E2,2.60692250E2,1},{-1.68908683E3,1.40710160E3,1},{-9.92500282E2,1.06913628E3,1},{-1.20254565E3,9.97917583E2,1},{-7.66666798E2,4.10433984E2,1},{-1.79043594E3,9.13690051E2,1},{-1.37468115E3,1.25984164E2,1},{-7.01679368E2,9.57705821E1,1},{-1.48276952E3,6.48824925E2,1},{-1.64074997E3,8.07257093E2,1},{-8.36408259E2,4.00679106E2,1},{-1.18110403E3,9.57673583E1,1},{-1.47252930E3,2.27980029E2,1},{-1.08130906E3,2.10504396E2,1},{-1.52579268E3,5.75155246E2,1},{-1.45746587E3,7.90201450E1,1},{-4.88515656E2,5.10147595E2,1},{-1.48418003E3,7.16230678E2,1},{-1.49000220E3,8.27360563E1,1},{-9.79882237E2,8.20978191E2,1},{-4.96663830E2,1.18004740E3,1},{-9.89346634E2,7.32323510E2,1},{-9.62442239E2,3.88326475E2,1},{-1.43045481E3,2.83168219E2,1},{-1.52686250E3,4.96340551E2,1}};

//        double[][] polist1 = new double[][]{{143,530,1},{426,274,1},{789,60,1},{149,395,1},{392,115,1},{866,866,1},{831,146,1},{265,751,1},{1286,68,1},{788,44,1},{495,1352,1},{619,69,1},{709,971,1},{819,433,1},{137,1160,1},{302,741,1},{448,543,1},{123,742,1},{183,806,1},{1312,714,1},{87,1259,1},{276,112,1},{307,353,1},{675,410,1},{669,143,1},{1334,279,1},{125,1244,1},{729,1046,1},{533,946,1},{952,416,1},{46,797,1},{382,116,1},{1020,98,1},{290,590,1},{162,718,1},{881,401,1},{552,91,1},{299,207,1},{643,203,1},{255,520,1},{311,72,1},{1252,540,1},{289,651,1},{285,75,1},{740,805,1},{1243,1249,1},{730,717,1},{757,381,1}
//                ,{334,259,1},{255,448,1}};

//        double[][] polist2 = new double[][]{{-1.66508060E3,5.97800394E2,1},{-1.32380770E3,2.94874798E2,1},{-9.30737018E2,6.10165344E1,1},{-1.65711652E3,4.44969031E2,1},{-1.36165324E3,1.24859736E2,1},{-8.52900159E2,8.66352064E2,1},{-8.86667684E2,1.46872615E2,1},{-1.51509981E3,8.29946153E2,1},{-4.58097396E2,6.44699006E1,1},{-9.30294127E2,4.50162070E1,1},{-1.24599630E3,1.43558404E3,1},{-1.10839056E3,7.27723654E1,1},{-1.01277341E3,9.95588520E2,1},{-8.99828827E2,4.36325600E2,1},{-1.67554186E3,1.31008596E3,1},{-1.46802492E3,8.12461810E2,1},{-1.29860737E3,5.81024679E2,1},{-1.69083533E3,8.39393083E2,1},{-1.61503002E3,9.02651956E2,1},{-4.35462800E2,6.67784369E2,1},{-1.73967171E3,1.43397899E3,1},{-1.50046367E3,1.23642203E2,1},{-1.46399478E3,3.87553392E2,1},{-1.04850928E3,4.22744155E2,1},{-1.05464107E3,1.48198053E2,1},{-4.16862242E2,2.60692250E2,1},{-1.68908683E3,1.40710160E3,1},{-9.92500282E2,1.06913628E3,1},{-1.20254565E3,9.97917583E2,1},{-7.66666798E2,4.10433984E2,1},{-1.79043594E3,9.13690051E2,1},{-1.37468115E3,1.25984164E2,1},{-7.01679368E2,9.57705821E1,1},{-1.48276952E3,6.48824925E2,1},{-1.64074997E3,8.07257093E2,1},{-8.36408259E2,4.00679106E2,1},{-1.18110403E3,9.57673583E1,1},{-1.47252930E3,2.27980029E2,1},{-1.08130906E3,2.10504396E2,1},{-1.52579268E3,5.75155246E2,1},{-1.45746587E3,7.90201450E1,1},{-4.88515656E2,5.10147595E2,1},{-1.48418003E3,7.16230678E2,1},{-1.49000220E3,8.27360563E1,1},{-9.79882237E2,8.20978191E2,1},{-4.96663830E2,1.18004740E3,1},{-9.89346634E2,7.32323510E2,1},{-9.62442239E2,3.88326475E2,1},{-1.43045481E3,2.83168219E2,1},{-1.52686250E3,4.96340551E2,1}};
        double[][] polist1 = new double[][]{{414,26,1},{291,176,1},{313,331,1},{333,166,1},{326,77,1},{113,86,1},{27,376,1},{156,253,1},{145,501,1},{367,255,1},{128,118,1},{167,280,1},{516,212,1},{187,281,1},{44,409,1},{172,128,1},{216,109,1},{95,58,1},{437,554,1},{70,61,1},{536,407,1},{145,36,1},{150,135,1},{227,244,1},{224,355,1},{99,264,1},{35,173,1},{122,252,1},{93,228,1},{315,188,1},{311,323,1},{426,396,1},{86,460,1},{212,207,1},{156,115,1},{80,451,1},{434,403,1}
                ,{211,342,1},{465,24,1},{15,284,1},{59,192,1},{198,83,1},{265,174,1},{270,61,1},{370,242,1},{378,132,1},{381,361,1},{87,38,1},{132,241,1},{101,190,1}};

        double[][] polist2 = new double[][]{{-1.33779634E3,2.79879007E1,1},{-1.48352742E3,1.94448985E2,1},{-1.45483107E3,3.62460413E2,1},{-1.43328292E3,1.81719148E2,1},{-1.43960597E3,8.43534916E1,1},{-1.70478330E3,9.83061236E1,1},{-1.81545924E3,4.33694521E2,1},{-1.64854639E3,2.84774872E2,1},{-1.66428341E3,5.65636917E2,1},{-1.39155324E3,2.76861249E2,1},{-1.68569933E3,1.33417468E2,1},{-1.63408949E3,3.14729116E2,1},{-1.22300693E3,2.24507688E2,1},{-1.61034738E3,3.15094302E2,1},{-1.79463278E3,4.70188651E2,1},{-1.62810391E3,1.43566468E2,1},{-1.57394321E3,1.21479770E2,1},{-1.72580536E3,6.60407563E1,1},{-1.31193666E3,5.94785347E2,1},{-1.75881539E3,6.97028924E1,1},{-1.20063089E3,4.29387208E2,1},{-1.66263228E3,4.09631067E1,1},{-1.65539471E3,1.52368335E2,1},{-1.55979184E3,2.71971996E2,1},{-1.56446282E3,3.95109126E2,1},{-1.72059603E3,3.00840889E2,1},{-1.80567006E3,1.99413005E2,1},{-1.69339287E3,2.86017799E2,1},{-1.72919413E3,2.59705597E2,1},{-1.45380169E3,2.06230340E2,1},{-1.45775345E3,3.53719673E2,1},{-1.32538168E3,4.26090292E2,1},{-1.73968032E3,5.24321489E2,1},{-1.57814876E3,2.30782057E2,1},{-1.64789226E3,1.30261863E2,1},{-1.74761143E3,5.14391840E2,1},{-1.31458524E3,4.32686969E2,1},{-1.58048763E3,3.81591976E2,1},{-1.27887877E3,2.62969983E1,1},{-1.83170388E3,3.27629610E2,1},{-1.77278190E3,2.20231144E2,1},{-1.59811556E3,9.35460019E1,1},{-1.51412345E3,1.92105790E2,1},{-1.50721054E3,6.78950862E1,1},{-1.38823903E3,2.62689570E2,1},{-1.37968557E3,1.43615107E2,1},{-1.37545463E3,3.90674830E2,1},{-1.73727989E3,4.33323957E1,1},{-1.68047676E3,2.72737170E2,1},{-1.71789902E3,2.16062993E2,1}};
//        Matrix F0 = new Matrix(new double[][]{{0.0,9.765625E-8,0,0},{2.70548366E-4,0.0,1.50015625},{0.0,-1.73205081,0.0}});


        p.Convert2DTo3D_Fun(polist1, polist2,p.MyMobileModel);
        p.Point3DTo2D(p.X_3D);
        System.out.println("==============MainActivity所得三维点坐标==================");
        for (int j=0; j<p.X_3D.getRowDimension(); j++){
            out.println(j+"点的值：" + p.X_3D.get(j,0)+","+p.X_3D.get(j,1)+ ","+p.X_3D.get(j,2)+ ","+p.X_3D.get(j,3));
        }
//        ArrayList<ImageMarker> Point3D = ArrayToMarkerList(p.X_3D);
//        System.out.println("==============所得三维点坐标==================");
//        for(ImageMarker im: Point3D){
//            Log.d("TestConvert3DFun","x:"+im.x+","+"y:"+im.y+","+"z:"+im.z);
//        }
//        double[][] Po_list = new double[][]{{326}, {325}, {332}, {334}, {377}, {377}, {388}, {392}, {438}};
//        Matrix test = matrixReshape(Po_list,3,3);
//        for (int i=0; i<3; i++){
//            for (int j=0; j<3; j++){
//                out.println("第"+i+","+j+"个点的值：" + test.get(i,j));
//            }
//        }
//        Matrix A = new Matrix(new double[][]{{4, -1, 6}, {1, 4, 0}, {5, 6, 1}});
//        SingularValueDecomposition s = A.svd()
//        Matrix V = s.getV();
//        for (int i=0; i<V.getRowDimension(); i++){
//            for (int j=0; j<V.getRowDimension(); j++){
//                out.println("第"+i+","+j+"个点的值：" + V.get(i,j));
//            }
//        }
        //double std1 = Objects.requireNonNull(p.getWholeMeanStdValue(new double[][]{{1,2},{3,4}}))[1];
        //Log.d("Std", String.valueOf(std1));
        // double[] po1mean = p.AvgValue(new Matrix(new double[][]{{1,2},{3,4}}));
        // Log.d("Mean", po1mean[0]+","+po1mean[1]);
        // Matrix a = new Matrix(new double[][]{{1,2},{3,4}}).times(0.5);
        // Log.d("Times", a.get(0,0)+","+a.get(0,1));
        //double[] error = p.CalculateError(p.X_3D,Po_list2);
    }




    /**
     * function for the Load button
     *
     * @param v the button: Laod
     */
    private void Loadimage(View v) {

        new XPopup.Builder(this)
                .atView(v)  // 依附于所点击的View，内部会自动判断在上方或者下方显示
                .asAttachList(new String[]{"Camera", "Album"},

                        new int[]{},
                        new OnSelectListener() {
                            @Override
                            public void onSelect(int position, String text) {

                                switch (text) {

                                    case "Camera":
                                        //拍照获取图片
                                        clearSelect_points();
                                        if (isP1){
                                            Log.v("loadImage", "load Image1");
                                            //functionMenu(isFinished1);
                                            checkPermission();
                                            MarkerList1.clear();
                                            ImageOpened = true;

                                        } else {
                                            Log.v("loadImage", "load Image2");
                                            //functionMenu(isFinished2);
                                            checkPermission();
                                            MarkerList2.clear();
                                            ImageOpened2 = true;

                                        }
                                        break;

                                    case "Album":
                                        //相册读取图片
                                        clearSelect_points();
                                        PickPhotoFromGallery();

                                        break;

                                }
                            }
                        })
                .show();
    }

    /**
     * function for the Load button
     *
     * @param v the button: Point
     */
    private void SelectPoint(View v) {

        new XPopup.Builder(this)
                .atView(v)  // 依附于所点击的View，内部会自动判断在上方或者下方显示

                .asAttachList(new String[]{"Refinement add", "General add", "Auto ORB","Delete", "Exit point"},


                        new int[]{},
                        new OnSelectListener() {
                            @Override
                            public void onSelect(int position, String text) {

                                switch (text) {

                                    case "Refinement add":
                                        //Harris角点检测矫正
                                        ifPoint = true;
                                        ifDelete = false;
                                        Pointing(true);

                                        break;

                                    case "General add":
                                        //无矫正
                                        ifPoint = true;
                                        ifDelete = false;
                                        Pointing(false);

                                        break;

                                    case "Auto ORB":
                                        ORBTest(MarkerList1, MarkerList2);//ORB提取匹配点
                                        Toast.makeText(getContext(), "Feature Points have been obtained!", Toast.LENGTH_SHORT).show();
                                        ifPoint = true;
                                        ifDelete = false;
                                        Pointing(true);

                                        break;

                                    case "Delete":
                                        //删除点
                                        ifPoint = false;
                                        ifDelete = true;
                                        Pointing(false);

                                        break;
                                    case "Exit point":
                                        //退出点操作模式
                                        clearSelect_points();

                                        break;

                                }
                            }
                        })
                .show();
    }

    private void Pointing(boolean ifRefine) {
        myrenderer.setIfRefine(ifRefine);
        System.out.println("here 111112222");
        if (ifPoint) {
            select_points.setTextColor(Color.BLUE);
            System.out.println("here 111112222");
        }
        if (ifDelete) {
            select_points.setTextColor(Color.RED);

        }
        if (!ifPoint && !ifDelete) {
            select_points.setTextColor(Color.BLACK);
        }
    }


    private void functionMenu (boolean isfinished) {
        if (isfinished) {

            loadImage.setVisibility(View.GONE);
            select_points.setVisibility(View.GONE);
            finished.setVisibility(View.GONE);
        } else {
            loadImage.setVisibility(View.VISIBLE);
            select_points.setVisibility(View.VISIBLE);
            finished.setVisibility(View.VISIBLE);
        }
    }

    private void clearSelect_points () {
        ifPoint = false;
        ifDelete = false;
        myrenderer.setIfRefine(false);
        select_points.setTextColor(Color.BLACK);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void clearPreImage() {
        Bitmap temp = myrenderer.GetBitmap();
        if (temp == null) {
            return;
        }
        int width = temp.getWidth();
        int height  = temp.getHeight();
        Bitmap myBitmap = Bitmap.createBitmap( width, height, Bitmap.Config.ARGB_8888 );
        int color;
        color = Color.argb(1, 1, 1, 1);
        //GLES30.glClearColor(121f / 255f, 134f / 255f, 203f / 255f, 1.0f);//浅紫

        myBitmap.eraseColor(color);
        myrenderer.ResetImage(myBitmap);
        myGLSurfaceView.requestRender();
    }

    //
    private void clear3D_Reconstruction() {
        img1 = null;
        img2 = null;
        MarkerList1.clear();
        MarkerList2.clear();
        isP1 = false;
        ifPoint = false;
        isFinished1 = false;
        isFinished2 = false;
        isMutiImg = false;
        ImageOpened = false;
        ImageOpened2 = false;
        isProcessed = false;
        isProcessed2 = false;
        img_switch.setVisibility(View.GONE);
        loadImage.setVisibility(View.GONE);
        select_points.setVisibility(View.GONE);
        finished.setVisibility(View.GONE);
        Process.setVisibility(View.GONE);
    }

    //private void resetButtonAfterProcess() {}

    private void interestPointsTo3D() {}

    private void initiateForReconstruction3D() {

//        MarkerList1.clear();
//        MarkerList2.clear();
//        if (isP1) {
//            myrenderer.ResetMarkerlist(MarkerList1);
//        } else {
//            myrenderer.ResetMarkerlist(MarkerList2);
//        }
//
//        myrenderer.setMarkerNum(0);
//        myGLSurfaceView.requestRender();

        isFinished1 = false;
        isFinished2 = false;

        Process.setVisibility(View.GONE);
        loadImage.setVisibility(View.GONE);
        img_switch.setVisibility(View.VISIBLE);

        FrameLayout.LayoutParams params_select_points = new FrameLayout.LayoutParams(230, 120);
        params_select_points.setMargins(300,20,0,0);
        select_points.setLayoutParams(params_select_points);//
        select_points.setVisibility(View.VISIBLE);

        FrameLayout.LayoutParams params_finished = new FrameLayout.LayoutParams(230, 120);
        params_finished.setMargins(550,20,0,0);
        finished.setLayoutParams(params_finished);//
        finished.setVisibility(View.VISIBLE);

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

    /*
     * 获取图片的旋转角度
     *
     * @param path 图片绝对路径
     * @return 图片的旋转角度
     */
    /*
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
    /*
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
    */


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
                        try{
                            if (!isZooming) {
                                if (ifPoint) {
                                    Log.v("actionUp", "Pointinggggggggggg");
//                                    System.out.println(myrenderer.getMarkerList().size());
//                                    System.out.println(myrenderer.getMarkerNum());
//                                    System.out.println("***---***");
//                                    Log.v("actionUping", "Pointinggggggggggg");
                                    if (isFinished1 || isFinished2) {
                                        if (myrenderer.getMarkerList().size() < myrenderer.getMarkerNum()) {
                                            myrenderer.setIsAddPoint(true);
                                            myrenderer.add2DMarker(normalizedX, normalizedY);
                                        } else {
                                            clearSelect_points();
                                            Toast.makeText(getContext(), "Point num is enough!", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        myrenderer.setIsAddPoint(true);
                                        myrenderer.add2DMarker(normalizedX, normalizedY);
                                    }

/*                                   if (myrenderer.getFileType() == MyRenderer.FileType.JPG || myrenderer.getFileType() == MyRenderer.FileType.PNG) {
//                                        System.out.println(myrenderer.getMarkerList().size());
//                                        System.out.println(myrenderer.getMarkerNum());
//                                        System.out.println("***---***");
                                        Log.v("actionUping", "Pointinggggggggggg");
                                        myrenderer.add2DMarker(normalizedX, normalizedY);
//                                        if (myrenderer.getMarkerList().size() < myrenderer.getMarkerNum()) {
//
//                                        } else {
//                                            ifPoint = !ifPoint;
//                                            Toast.makeText(getContext(), "Point num is enough!", Toast.LENGTH_SHORT).show();
//                                        }
                                    } else {
                                        Log.v("actionUping", "Pointinggggggggggg");
                                        myrenderer.setMarkerDrawed(normalizedX, normalizedY);
                                    }
                                    */
                                    Log.v("actionPointerDown", "(" + X + "," + Y + ")");
                                    if (isP1) {
                                        MarkerList1 = myrenderer.getMarkerList();
                                    } else {
                                        MarkerList2 = myrenderer.getMarkerList();
                                    }
                                    requestRender();

                                }

                                if (ifDelete) {
                                    Log.v("actionUp", "Deleting point!!!");
                                    myrenderer.setIsAddPoint(false);
                                    myrenderer.delete2DMarker(normalizedX, normalizedY);
                                    Log.v("actionPointerDown", "(" + X + "," + Y + ")");
                                    if (isP1) {
                                        MarkerList1 = myrenderer.getMarkerList();
                                    } else {
                                        MarkerList2 = myrenderer.getMarkerList();
                                    }
                                    requestRender();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
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

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        Log.d("Mainactivity","界面被销毁了");
    }


}
