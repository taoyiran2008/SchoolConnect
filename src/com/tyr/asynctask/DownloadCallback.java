package com.tyr.asynctask;

import java.io.File;

public interface DownloadCallback {
    public void onProgress(int progress); // 用于按照进度更新UI, 0~100
    public void onPost(File file); // 下载完成后的动作
}
