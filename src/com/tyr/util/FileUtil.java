package com.tyr.util;

import java.io.File;

import android.content.Context;
import android.os.Environment;

public class FileUtil {
    public static String seperator = File.separator;
    public static String getCacheFilePath(Context context) {
        String path = "";
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            // path = mContext.getExternalCacheDir().getAbsolutePath() + "/";
            path = context.getApplicationInfo().dataDir + seperator;
        } else {
            path = context.getApplicationInfo().dataDir + seperator;
            // path = mContext.getCacheDir().getAbsolutePath()+ "/";
        }
        Debugger.logDebug(path);
        return path;
    }
    
    public static String getDownloadPath(Context context, String subdir){
        String path = "";
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            path = context.getApplicationInfo().dataDir + seperator + subdir + seperator;
        } else {
            path = context.getApplicationInfo().dataDir + seperator + subdir + seperator;
        }
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdir();
        }
        Debugger.logDebug(path);
        return path;
    }
    
    public static String getFileName(String url) {
        String filename = null;
        if (url != null) {
            try {
                if (url.lastIndexOf("/") != -1) {
                    filename = url.substring(url.lastIndexOf("/") + 1);
                } else {
                    filename = url;
                }
            } catch (Exception e) {
            }
        }
        return filename;
    }

}
