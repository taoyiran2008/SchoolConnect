package com.tyr.asynctask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.os.Handler;

import com.tyr.util.CommonUtil;
import com.tyr.util.Debugger;
import com.tyr.util.FileUtil;

/**
 * 不同于图像下载，在后台silent进行的，我们也不需要取消这个任务，因为有超时控制。 这个类仅仅管理一个下载线程
 */
public class DownloadThread extends Thread{
    private static final String DIR = "download";
    private Context mContext;
    String url;
    DownloadCallback callback;
    Handler handler;
    HttpGet httpRequest;
    HttpClient httpClient;
    File file;

    public DownloadThread(Context context, String url, Handler handler, DownloadCallback callback) {
        mContext = context;
        this.url = url;
        this.callback = callback;
        this.handler = handler;
    }

    public void run() {
//        String filePath = FileUtil.getDownloadPath(mContext, DIR) + FileUtil.getFileName(url);
//        file = new File(filePath);
//        // if it's already exists
//        if (file.exists()) {
//            handler.post(new Runnable() {
//                public void run() {
//                    callback.onProgress(100);
//                    callback.onPost(file);
//                }
//            });
//            return;
//        }

        Debugger.logDebug("start downloading url: " + url);
        
        // download via internet
        file = downloadAndSave();

        handler.post(new Runnable() {
            public void run() {
                callback.onPost(file);
            }
        });
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

    private File downloadAndSave() {
        if (CommonUtil.isConnection(mContext)) {
            // return null;
        }

        httpRequest = new HttpGet(url);
        httpClient = new DefaultHttpClient();

        try {
            // 请求httpClient
            HttpResponse httpResponse = httpClient.execute(httpRequest);
            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity httpEntity = httpResponse.getEntity();
                InputStream is = httpEntity.getContent();
                // 读取is，并写入文件
                String filePath = FileUtil.getDownloadPath(mContext, DIR)
                        + FileUtil.getFileName(url);
                file = new File(filePath);
                FileOutputStream os = new FileOutputStream(file);
                byte[] buffer = new byte[1024];
                int cnt = 0;
                int readLength = 0;
                int totalLength = (int) httpEntity.getContentLength();
                while ((readLength = is.read(buffer)) != -1) {
                    cnt += readLength;
                    os.write(buffer);
                    final int percent = cnt * 100 / totalLength;
                    Debugger.logDebug("download percent: " + percent);
                    handler.post(new Runnable() {
                        public void run() {
                            callback.onProgress(percent);
                        }
                    });
                }
                is.close();
                os.close();
                return file; // 正常下载并保存，返回文件
            }
        } catch (Exception e) {
            // httpRequest.abort() 会解除阻塞
            if (file != null && file.exists()) {
                file.delete(); // 可能导致下载的文件不完全
            }
            e.printStackTrace();
            Debugger.logError("file download error");
        }
        return null;
    }
}
