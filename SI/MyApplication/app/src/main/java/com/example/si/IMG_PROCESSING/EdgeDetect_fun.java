package com.example.si.IMG_PROCESSING;

import android.graphics.Bitmap;
import android.widget.ImageView;

import com.example.si.ImageTools;
import com.example.si.R;

public class EdgeDetect_fun {
    public static boolean EdgeDetect(RoberEdgeDetect ro, Bitmap img)throws Exception{
        //Bitmap img = ImageTools.getBitmapfromimageView(imageView);
        ro.readImage(img);
        ro.EdgeImage = ro.createEdgeImage();
        return true;
    }

    //////////Canny算子//////////////


}
