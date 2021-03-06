package com.example.si.IMG_PROCESSING.CornerDetection;

import android.graphics.Bitmap;

import com.example.si.IMG_PROCESSING.CornerDetection.HarrisMatrix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;


public class HarrisCornerDetector extends GrayFilter {
    private GaussianDerivativeFilter filter;
    private List<HarrisMatrix> harrisMatrixList;
    private double lambda = 0.04; // scope : 0.04 ~ 0.06
    public int[] corner_xy;
    public int[] globalMaxLocation;


    private double sigma = 1;
    private double window_radius = 7;
    public HarrisCornerDetector() {
        filter = new GaussianDerivativeFilter();
        harrisMatrixList = new ArrayList<HarrisMatrix>();
    }

    private Bitmap createCompatibleDestImage(Bitmap src) {
        Bitmap.Config config = src.getConfig() != null ? src.getConfig() : Bitmap.Config.ARGB_8888;
        Bitmap bitmap = Bitmap.createBitmap(src.getWidth(), src.getHeight(), config);
        return bitmap;
//			return new BufferedImage(dstCM, dstCM.createCompatibleWritableRaster(src.getWidth(), src.getHeight()), dstCM.isAlphaPremultiplied(), null);
    }

    @Override
    public Bitmap filter(Bitmap src, Bitmap dest) {
        int width = src.getWidth();
        int height = src.getHeight();
        initSettings(height, width);
        if ( dest == null )
            dest = createCompatibleDestImage( src);

        Bitmap grayImage = super.filter(src, null);
        int[] inPixels = new int[width*height];

        // first step  - Gaussian first-order Derivatives (3 �� 3) - X - gradient, (3 �� 3) - Y - gradient
        filter.setDirectionType(GaussianDerivativeFilter.X_DIRECTION);
        Bitmap xImage = filter.filter(grayImage, null);
        xImage.getPixels(inPixels,0,width,0,0,width,height);
//		getRGB( xImage, 0, 0, width, height, inPixels );
        extractPixelData(inPixels, GaussianDerivativeFilter.X_DIRECTION, height, width);

        filter.setDirectionType(GaussianDerivativeFilter.Y_DIRECTION);
        Bitmap yImage = filter.filter(grayImage, null);
        yImage.getPixels(inPixels,0,width,0,0,width,height);
//		getRGB( yImage, 0, 0, width, height, inPixels );
        extractPixelData(inPixels, GaussianDerivativeFilter.Y_DIRECTION, height, width);

        // second step - calculate the Ix^2, Iy^2 and Ix^Iy
        for(HarrisMatrix hm : harrisMatrixList)
        {
            double Ix = hm.getXGradient();
            double Iy = hm.getYGradient();
            hm.setIxIy(Ix * Iy);
            hm.setXGradient(Ix*Ix);
            hm.setYGradient(Iy*Iy);
        }

        // ���ڸ�˹���������ĵ㻯���ڼ���һ�׵����ͣ��ؼ�һ�� SumIx2, SumIy2 and SumIxIy, ��˹ģ��
        calculateGaussianBlur(width, height);

        // ��ȡHarris Matrix ����ֵ
        // ����Ƕ���ӦֵR R= Det(H) - lambda * (Trace(H))^2
        harrisResponse(width, height);

        // based on R, compute non-max suppression
        nonMaxValueSuppression(width, height);

        // match result to original image and highlight the key points
        corner_xy = matchToImage(width, height, src);

        //search for max response location
        globalMaxLocation = findMax(width, height, src);

        // return result image

//		dest.setPixels(outPixels,0,width,0,0,width,height);
//		setRGB( dest, 0, 0, width, height, outPixels );
        return dest;
    }


    private int[] matchToImage(int width, int height, Bitmap src) {
        int[] inPixels = new int[width*height];
        int[] outPixels = new int[width*height];
        int[] corner_x_y;
        src.getPixels(inPixels,0,width,0,0,width,height);
//        getRGB( src, 0, 0, width, height, inPixels );
        int index = 0;
        int m=0;
        double max =0;
        double k = 0.06;
        int min_D = Math.round(Math.max(width,height) / 400f);
        min_D = min_D*min_D;
        Vector vet_xy=new Vector();



        for(int row=0; row<height; row++) {
            int ta = 0, tr = 0, tg = 0, tb = 0;
            for (int col = 0; col < width; col++) {
                index = row * width + col;
//				System.out.println("index");
//				System.out.println(index);
                ta = (inPixels[index] >> 24) & 0xff;
                tr = (inPixels[index] >> 16) & 0xff;
                tg = (inPixels[index] >> 8) & 0xff;
                tb = inPixels[index] & 0xff;
                HarrisMatrix hm = harrisMatrixList.get(index);
                if (hm.getMax() > 0)   //其实求最大值不应该放到这个循环里面来求，但是如果再来一个循环单独求最大值太费时间了，这样的话 也可以很好的起到最大值卡阈值的效果
                {
                    if (hm.getMax() > max) {
                        max = hm.getMax();
                    }
                }

                if (hm.getMax() > max * k) {
                    tr = 0;
                    tg = 255; // make it as green for corner key pointers
                    tb = 0;
                    outPixels[index] = (ta << 24) | (tr << 16) | (tg << 8) | tb;

                    vet_xy.add(row);
                    vet_xy.add(col);
                    m++;


                } else {
                    outPixels[index] = (ta << 24) | (tr << 16) | (tg << 8) | tb;
                }
//				System.out.println("tgtgtgtg:");
//                System.out.println(tg);

            }
        }
//		System.out.println("vet_xy.size()");
//		System.out.println(vet_xy.size());
//		System.out.println("mmmmmmmmm");
//		System.out.println(2*m);
//		System.out.println("m909090");
//		System.out.println(vet_xy.get(91));
        for (int i=0;i<vet_xy.size();i+=2)
            for (int j=0;j<vet_xy.size();j+=2)
            {
                if (i!=j)
                {
//						System.out.println("jj");
//						System.out.println(j);
//						System.out.println("ii");
//						System.out.println(vet_xy.get(i));
//						System.out.println("ijijij");
//						System.out.println(vet_xy.size());
                    int dx = (int) vet_xy.get(i)-(int) vet_xy.get(j);
                    int dy = (int) vet_xy.get(i+1)-(int) vet_xy.get(j+1);
                    if (dx*dx+dy*dy<min_D)
                    {
                        int index1 = ((int) vet_xy.get(i)) * width + (int) vet_xy.get(i+1);
                        int index2 = ((int) vet_xy.get(j)) * width + (int) vet_xy.get(j+1);
//							System.out.println("index1");
//							System.out.println(index1);
//							System.out.println("index2");
//							System.out.println(index2);
//							System.out.println("jjjjjj");
//							System.out.println(vet_xy.get(j));

                        HarrisMatrix hm1 = harrisMatrixList.get(index1);
                        HarrisMatrix hm2 = harrisMatrixList.get(index2);
                        if(hm1.getMax()>=hm2.getMax())
                        {
//								System.out.println("ijijij");
//								System.out.println(vet_xy.size());
//								System.out.println("jjj");
//								System.out.println(j+1);
                            vet_xy.remove(j);
                            vet_xy.remove(j);
                        }
                        else
                        {

                            vet_xy.remove(i);
                            vet_xy.remove(i);

                            break;
                        }


                    }
                }
            }

        corner_x_y = new int[2*vet_xy.size()];



        for (int j=0;j<vet_xy.size();j++)
        {
            corner_x_y[j] = (int) vet_xy.get(j);

        }

        corner_x_y = sortHarrisMatrixList(corner_x_y, width);  //排序并返回最大的三个点

//        globalMaxLocation = findMax(width, height, src);
        return corner_x_y;
    }
    /***
     * we still use the 3*3 windows to complete the non-max response value suppression
     */
    private void nonMaxValueSuppression(int width, int height) {
        int index = 0;
        int radius = (int)window_radius;
        for(int row=0; row<height; row++) {
            for(int col=0; col<width; col++) {
                index = row * width + col;
                HarrisMatrix hm = harrisMatrixList.get(index);
                double maxR = hm.getR();
                boolean isMaxR = true;
                for(int subrow =-radius; subrow<=radius; subrow++)
                {
                    for(int subcol=-radius; subcol<=radius; subcol++)
                    {
                        int nrow = row + subrow;
                        int ncol = col + subcol;
                        if(nrow >= height || nrow < 0)
                        {
                            nrow = 0;
                        }
                        if(ncol >= width || ncol < 0)
                        {
                            ncol = 0;
                        }
                        int index2 = nrow * width + ncol;
                        HarrisMatrix hmr = harrisMatrixList.get(index2);
                        if(hmr.getR() > maxR)
                        {
                            isMaxR = false;
                        }
                    }
                }
                if(isMaxR)
                {
                    hm.setMax(maxR);
                }
            }
        }

    }

    /***
     *
     * 	A = Sxx;
     *	B = Syy;
     *  C = Sxy*Sxy*4;
     *	lambda = 0.04;
     *	H = (A*B - C) - lambda*(A+B)^2;
     *
     * @param width
     * @param height
     */
    private void harrisResponse(int width, int height) {
        int index = 0;
        for(int row=0; row<height; row++) {
            for(int col=0; col<width; col++) {
                index = row * width + col;
                HarrisMatrix hm = harrisMatrixList.get(index);
                double c =  hm.getIxIy() * hm.getIxIy();
                double ab = hm.getXGradient() * hm.getYGradient();
                double aplusb = hm.getXGradient() + hm.getYGradient();
//        		double response = (ab -c) - lambda * Math.pow(aplusb, 2);
                double response = Math.min(hm.getXGradient()-c/hm.getYGradient(),hm.getYGradient());
//				double response = 0.5*aplusb - Math.sqrt(0.5*(hm.getXGradient() - hm.getYGradient())*(hm.getXGradient() - hm.getYGradient()) + c);
                hm.setR(response);
            }
        }
    }

    public int[] findMax(int width, int height, Bitmap src) {
        int[] maxPosition = new int[2];
        int index = 0;
        double max =0;

        for(int row=0; row<height; row++) {
            for (int col = 0; col < width; col++) {
                index = row * width + col;
//				System.out.println("index");
//				System.out.println(index);
                HarrisMatrix hm = harrisMatrixList.get(index);
                if (hm.getMax() > 0)   //其实求最大值不应该放到这个循环里面来求，但是如果再来一个循环单独求最大值太费时间了，这样的话 也可以很好的起到最大值卡阈值的效果
                {
                    if (hm.getMax() > max) {
                        max = hm.getMax();
                        maxPosition[0] = col;
                        maxPosition[1] = row;
                    }
                }
            }
        }
        return maxPosition;
    }

    private void calculateGaussianBlur(int width, int height) {
        int index = 0;
        int radius = (int)window_radius;
        double[][] gw = get2DKernalData(radius, sigma);
        double sumxx = 0, sumyy = 0, sumxy = 0;
        for(int row=0; row<height; row++) {
            for(int col=0; col<width; col++) {
                for(int subrow =-radius; subrow<=radius; subrow++)
                {
                    for(int subcol=-radius; subcol<=radius; subcol++)
                    {
                        int nrow = row + subrow;
                        int ncol = col + subcol;
                        if(nrow >= height || nrow < 0)
                        {
                            nrow = 0;
                        }
                        if(ncol >= width || ncol < 0)
                        {
                            ncol = 0;
                        }
                        int index2 = nrow * width + ncol;
                        HarrisMatrix whm = harrisMatrixList.get(index2);
                        sumxx += (gw[subrow + radius][subcol + radius] * whm.getXGradient());
                        sumyy += (gw[subrow + radius][subcol + radius] * whm.getYGradient());
                        sumxy += (gw[subrow + radius][subcol + radius] * whm.getIxIy());
                    }
                }
                index = row * width + col;
                HarrisMatrix hm = harrisMatrixList.get(index);
                hm.setXGradient(sumxx);
                hm.setYGradient(sumyy);
                hm.setIxIy(sumxy);

                // clean up for next loop
                sumxx = 0;
                sumyy = 0;
                sumxy = 0;
            }
        }
    }

    public double[][] get2DKernalData(int n, double sigma) {
        int size = 2*n +1;
        double sigma22 = 2*sigma*sigma;
        double sigma22PI = Math.PI * sigma22;
        double[][] kernalData = new double[size][size];
        int row = 0;
        for(int i=-n; i<=n; i++) {
            int column = 0;
            for(int j=-n; j<=n; j++) {
                double xDistance = i*i;
                double yDistance = j*j;
                kernalData[row][column] = Math.exp(-(xDistance + yDistance)/sigma22)/sigma22PI;
                column++;
            }
            row++;
        }

//		for(int i=0; i<size; i++) {
//			for(int j=0; j<size; j++) {
//				System.out.print("\t" + kernalData[i][j]);
//			}
//			System.out.println();
//			System.out.println("\t ---------------------------");
//		}
        return kernalData;
    }

    private void extractPixelData(int[] pixels, int type, int height, int width)
    {
        int index = 0;
        for(int row=0; row<height; row++) {
            int ta = 0, tr = 0, tg = 0, tb = 0;
            for(int col=0; col<width; col++) {
                index = row * width + col;
                ta = (pixels[index] >> 24) & 0xff;
                tr = (pixels[index] >> 16) & 0xff;
                tg = (pixels[index] >> 8) & 0xff;
                tb = pixels[index] & 0xff;
                HarrisMatrix matrix = harrisMatrixList.get(index);
                if(type == GaussianDerivativeFilter.X_DIRECTION)
                {
                    matrix.setXGradient(tr);
                }
                if(type == GaussianDerivativeFilter.Y_DIRECTION)
                {
                    matrix.setYGradient(tr);
                }
            }
        }
    }

    private void initSettings(int height, int width)
    {
        int index = 0;
        for(int row=0; row<height; row++) {
            for(int col=0; col<width; col++) {
                index = row * width + col;
                HarrisMatrix matrix = new HarrisMatrix();
                harrisMatrixList.add(index, matrix);
            }
        }
    }

    /**
     * harrisMatrixList 降序排序
     */
    private int[] sortHarrisMatrixList(int[] corner_x_y, int width) {
        ArrayList<HarrisMatrix> temp = new ArrayList<HarrisMatrix>();
        int[] corner_x = new int[corner_x_y.length/2];
        int[] corner_y = new int[corner_x_y.length/2];
        for (int n=0;n<corner_x_y.length/2;n++)
        {
            corner_x[n]=corner_x_y[2*n+1];
            corner_y[n]=corner_x_y[2*n];
            HarrisMatrix hm = new HarrisMatrix();
            hm = harrisMatrixList.get(corner_y[n]*width+corner_x[n]);
            hm.setX(corner_x[n]);
            hm.setY(corner_y[n]);
            temp.add(hm);
            //System.out.println(corner_x[n]);
            //System.out.println(corner_y[n]);
        }


//
//        int indextemp;
//        System.out.println("-------------------------------------------------------");
//        for (int index = 0; index < harrisMatrixList.size(); index++) {
//            HarrisMatrix hm = harrisMatrixList.get(0);
//            System.out.println("hm.R = "+hm.getR());
//            //hm = new HarrisMatrix();
//            indextemp = 0;
//            for (int index2 = 1; index2 < harrisMatrixList.size()-index; index2++) {
//                if (hm.getR() < harrisMatrixList.get(index2).getR()) {
//                    hm = harrisMatrixList.get(index2).copy();
//                    indextemp = index2;
//                }
//            }
//            temp.add(hm);
//            harrisMatrixList.remove(indextemp);
//
//        }
//
//        for (HarrisMatrix harrisMatrix:temp) {
//            System.out.println(harrisMatrix.getR());
//        }
        System.out.println("-------------------------------------------------------");

        Collections.sort(temp, new Comparator<HarrisMatrix>() {

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

        int c = 0;
        for (int index = 0; index < temp.size(); index++) {
            HarrisMatrix harrisMatrix = temp.get(index);
//            System.out.println("R :"+harrisMatrix.getR());
//            System.out.println("x :"+harrisMatrix.getX());
//            System.out.println("y :"+harrisMatrix.getY());

            if (Double.isNaN(harrisMatrix.getR())) {
                temp.remove(index);
                index--;
                c++;
            }
//            if (harrisMatrix.getY() == 0 || harrisMatrix.getX() == 0) {
//                temp.remove(index);
//                c++;
//            }
        }
//        System.out.println("++++++++++++++++++");
//        System.out.println("c :"+c);
//        System.out.println("++++++++++++++++++");

//        int a = 0;
//        for (HarrisMatrix harrisMatrix:temp) {
//            System.out.println(a+" "+harrisMatrix.getR());
//            a++;
//        }

        if (temp.size() > 3) {
            int[] new_corner_x_y = new int[6];
            for (int i = 0; i < 3; i++) {
                new_corner_x_y[2*i + 1] = temp.get(i).getX();
                new_corner_x_y[2*i] = temp.get(i).getY();
//                System.out.println(i+" "+temp.get(i).getX()+","+temp.get(i).getY());
            }

            return new_corner_x_y;
        } else if (temp.size() > 0) {
            int[] new_corner_x_y = new int[2*temp.size()];
            for (int i = 0; i < temp.size(); i++) {
                new_corner_x_y[2*i + 1] = temp.get(i).getX();
                new_corner_x_y[2*i] = temp.get(i).getY();
//                System.out.println(i+" "+temp.get(i).getX()+","+temp.get(i).getY());
            }
            return new_corner_x_y;
        } else {
            return new int[2];
        }



    }


}
