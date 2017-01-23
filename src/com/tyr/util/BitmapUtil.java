package com.tyr.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.TypedValue;

import com.tyr.asynctask.AsyncImageloader;
import com.tyr.data.MyApplication;

/**
 * 管理图片加载 缓存的类，和ContactList一样，应该作为一个全局的对象给Application引用
 */
public class BitmapUtil {
    private static final String TAG = "BitmapUtil";
    private static final String PREFIX_THUMB_NAIL = "thumbnail_";
    private static final String PREFIX_BIT_PIC = "bigpic_";
    public static final int HEIGHT_THUMB = 100; // in dp unit
    public static final int WIDTH_THUMB = 60;
    public static final int HEIGHT_BIG = 300;
    public static final int WIDTH_BIG = 150;
    public static final String EXT = ".jpg";

    private static String getImageName(String url) {
        String filename = null;
        if (url != null) {
            try {
                if (url.lastIndexOf("/") != -1) {
                    filename = url.substring(url.lastIndexOf("/") + 1);
                } else {
                    filename = url;
                }
                filename = filename.substring(0, filename.lastIndexOf("."));
                filename += EXT;
            } catch (Exception e) {
            }
        }
        return filename;
    }

    public static Bitmap getResizedBitmap(Bitmap bitmap, int type) {
        if (type == AsyncImageloader.IMAGE_TYPE_THUMB) {
            return resizeBitmap(bitmap, WIDTH_THUMB, HEIGHT_THUMB);
        } else {
            return resizeBitmap(bitmap, WIDTH_BIG, HEIGHT_BIG);
        }
    }

    public static Bitmap getBitmapFromFile(String url, int type) {
        Bitmap bitmap = null;
        String fileName = "";
        if (type == AsyncImageloader.IMAGE_TYPE_THUMB) {
            fileName = PREFIX_THUMB_NAIL + getImageName(url);
        } else {
            fileName = PREFIX_BIT_PIC + getImageName(url);
        }
        try {
            FileInputStream fis = new FileInputStream(MyApplication.mCacheDir + fileName);
            bitmap = BitmapFactory.decodeStream(fis);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            // Drawable drawable = mContext.getResources().getDrawable(R.drawable.ic_media_play);
            // BitmapDrawable bd = (BitmapDrawable)drawable;
            // bitmap = bd.getBitmap();
            bitmap = null;
        }
        return bitmap;
    }

    /**
     * 在decode的时候直接调用JNI>>nativeDecodeAsset()来完成压缩，无需再使用java层的createBitmap，从而节省了java层的空间
     * 
     * 但是对于upload 图片，bitmap是直接得到的，在保存上传前还是需要压缩一下
     */
    private static Bitmap resizeBitmap(Bitmap bmp, float width, float height) {
        float xscale = width / bmp.getWidth();
        float yscale = height / bmp.getHeight();
        Matrix matrix = new Matrix();
        // resize original image
        matrix.postScale(xscale, yscale);
        Bitmap dstbmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix,
                true);
        return dstbmp;
    }

    /**
     * 从网络下载图片，把bitmap数据缓存到内存，并把bitmap压缩保存到本地储存介质上 在调用之前bitmap 是经过decode 压缩过的，不需要resizeBitmap
     */
    public static Bitmap cacheImage(String url, Bitmap bitmap, int type) {
        String fileName = "";
        if (type == AsyncImageloader.IMAGE_TYPE_THUMB) {
            fileName = PREFIX_THUMB_NAIL + getImageName(url);
        } else {
            fileName = PREFIX_BIT_PIC + getImageName(url);
        }
        if (bitmap == null) {
            return null;
        }

        if (MyApplication.mCache2Memory) {
            addBitmapToMemory(fileName, bitmap);
        }

        // 缓存bitmap至/data/data/packageName/cache/文件夹中
        // Note: bitmap 是已经按比例压缩过的
        saveImage(fileName, bitmap);
        return bitmap;
    }

    public static String getImagePath(String fileName) {
        return MyApplication.mCacheDir + fileName;
    }

    /**
     * 只储存缩略图，原图不保存在本地
     */
    private static String saveImage(String fileName, Bitmap bitmap) {
        String filePath = "";
        if (bitmap == null) {
            return "";
        } else {
            filePath = MyApplication.mCacheDir + fileName;
            File destFile = new File(filePath);
            OutputStream os = null;
            try {
                os = new FileOutputStream(destFile);
                bitmap.compress(CompressFormat.PNG, 100, os);
                os.flush();
                os.close();
            } catch (IOException e) {
                filePath = "";
            }
        }
        return filePath;
    }

    private static void addBitmapToMemory(String key, Bitmap bitmap) {
        if (getBitmapFromMemory(key) == null) {
            if (MyApplication.mImageCache != null) {
                MyApplication.mImageCache.put(key, bitmap);
            }
        }
    }

    public static Bitmap getBitmapFromMemory(String key) {
        if (MyApplication.mImageCache != null && key != null) {
            return MyApplication.mImageCache.get(key);
        } else {
            return null;
        }
    }
    
    public static Bitmap getBitmapFromMemory(String url, int type) {
        // 从url和希望获取的图片大小，找到对应的文件名key
        String fileName = "";
        if (type == AsyncImageloader.IMAGE_TYPE_THUMB) {
            fileName = PREFIX_THUMB_NAIL + getImageName(url);
        } else {
            fileName = PREFIX_BIT_PIC + getImageName(url);
        }
        if (MyApplication.mImageCache != null && fileName != null) {
            return MyApplication.mImageCache.get(fileName);
        } else {
            return null;
        }
    }

    /**
     * 按比例解码图片资源，而不是直接把文件或者网络的流decode，不然会因为原始图片过大而导致 decode 失败，甚至OUT OF MEMORY
     */
    public static Bitmap decodeBitmap(Context context, byte[] data, int type) {
        int reqWidth = (type == AsyncImageloader.IMAGE_TYPE_THUMB) ? WIDTH_THUMB : WIDTH_BIG;
        int reqHeight = (type == AsyncImageloader.IMAGE_TYPE_THUMB) ? HEIGHT_THUMB : HEIGHT_BIG;

        // 第一次解析将inJustDecodeBounds设置为true，来获取图片大小
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);
        // 设置采样率  
        options.inSampleSize = calculateInSampleSize(options, reqHeight, reqWidth);
        // 使用获取到的inSampleSize值再次解析图片
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }
    
    public static byte[] readStream(InputStream inStream) throws Exception {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        outStream.close();
        inStream.close();
        return outStream.toByteArray();
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth,
            int reqHeight) {
        // 源图片的高度和宽度
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            // 计算出实际宽高和目标宽高的比率
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            // 选择宽和高中最小的比率作为inSampleSize的值，这样可以保证最终图片的宽和高
            // 一定都会大于等于目标的宽和高。
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    public static int pxToDp(Context context, int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, context
                .getResources().getDisplayMetrics());
    }
}