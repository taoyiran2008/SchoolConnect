package com.tyr.asynctask;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;

import com.tyr.util.BitmapUtil;
import com.tyr.util.CommonUtil;
import com.tyr.util.Debugger;

/**
 * Helper class for image downloading/uploading
 * 
 * 图片下载如果在断网的情况下，http请求会有明显的阻塞延迟，如果下载图片的请求过多, 
 * AsyncTask 线程池满了，等很长时间后才会响应，图片下载用Thread + Handler实现。
 */
public class AsyncImageloader{
    public static int IMAGE_TYPE_THUMB = 0;
    public static int IMAGE_TYPE_BIGGER = 1;
    // picture downloading task queue
    private HashSet<String> mURLSet = new HashSet<String>();
    private Context mContext;
    // 线程池，每次最多并行执行10个下载任务
    ExecutorService mPool = Executors.newFixedThreadPool(10);

    public AsyncImageloader(Context context) {
        mContext = context;
    }

    /**
     * 图片的下载流程：根据url从server download，在本地压缩保存。本地从不保存原始大图，只保存 两种类型，thumb和bigger。type 参数值关系到图像的压缩储存
     */
    public Bitmap loadImage(String url, int type, Handler handler, ImageCallback callback) {
        // load图片前先应该判断缓存中是否存在，如果不存在才启动一个load 线程，而不是每次
        // 加载都在线程池里面新建一个线程

        // firstly, fetch image from cache.
        Bitmap bitmap = BitmapUtil.getBitmapFromMemory(url, type);
        if (bitmap != null) {
            return bitmap;
        }
        // secondly, start a new thread to load image from disk and internet.
        Debugger.logDebug("start downloading url: " + url);
        if (mURLSet.contains("download => " + url)) {
            Debugger.logDebug("url already exists, return");
            return null;
        }
        DownloadThread thread = new DownloadThread(url, type, handler, callback);
        mPool.execute(thread);
        mURLSet.add("download => " + url);
        return null;
    }

    class DownloadThread extends Thread {
        String url;
        int type; // image type
        ImageCallback callback;
        // Thread + handler的模型，不同于AsyncTask的onpost是在调用task的UI线程执行的
        Handler handler;
        HttpGet httpRequest;
        HttpClient httpClient;
        
        // 处理和获取的数据
        Bitmap bitmap = null;

        // 超时控制
        public static final int TIMEOUT = 60000;
        private Timer mTimer = new Timer();

        public DownloadThread(String url, int type, Handler handler, ImageCallback callback) {
            this.url = url;
            this.type = type;
            this.callback = callback;
            this.handler = handler;
        }

        // this is a blocking thread
        public void run() {
            // 超时控制在thread 内部自己维护，不需要外部调用，超时后自动interrupt，从线程池中
            // 释放出来
            mTimer.schedule(new TimerTask() {
                public void run() {
                    Debugger.logDebug("download thread has been interrupted due to timeout");
                    interrupt();
                }
            }, TIMEOUT);
            
            // try to get Bitmap resource from file.
            bitmap = BitmapUtil.getBitmapFromFile(url, type);
            if (bitmap == null) {
                // download via internet
                bitmap = downloadImage(mContext, url, type);
            }
            if (bitmap != null) {
                // 图片加载完成后进行缓存
                BitmapUtil.cacheImage(url, bitmap, type);
                if (callback != null) {
                    // bitmap 为空会导致listview描画失败
                    handler.post(new Runnable() {
                        public void run() {
                            callback.onImageLoaded(bitmap);
                        }
                    });
                }
            }
            mURLSet.remove("download => " + url);
        }

        // 用于终止阻塞的线程，释放掉系统资源
        public void interrupt() {
            if (httpClient != null) {
                httpClient.getConnectionManager().shutdown();
            }
            if (httpRequest != null) {
                httpRequest.abort();
            }
        }

        private Bitmap downloadImage(Context context, String url, int type) {
            if (CommonUtil.isConnection(context)) {
                // return null;
            }

            Bitmap bitmap = null;
            httpRequest = new HttpGet(url);
            httpClient = new DefaultHttpClient();

            try {
                // 请求httpClient
                HttpResponse httpResponse = httpClient.execute(httpRequest);
                if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    HttpEntity httpEntity = httpResponse.getEntity();
                    InputStream is = httpEntity.getContent();
                    // 先下载，然后decodeFile不太科学，因为我们不能直接把一个高清大图保存到本地
                    // 缓存到本地的图片也应该是压缩过的，这里我们只单纯的做好一件事，下载图片，编码
                    // 并返回编码后的Bitmap
                    byte data[] = BitmapUtil.readStream(is);
                    bitmap = BitmapUtil.decodeBitmap(mContext, data, type);
                    is.close();
                }
            } catch (Exception e) {
                // httpRequest.abort() 会解除阻塞
                Debugger.logError("Image download error");
            }
            return bitmap;
        }
    }
}
