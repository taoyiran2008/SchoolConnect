package com.tyr.asynctask;

import org.apache.http.HttpStatus;

import android.graphics.Bitmap;


public class HttpResult {
    // HttpStatus.SC_OK 200 is a common practice, we won't define it here
    public static final int RESPONSE_OK = 0;
    public static final int RESPONSE_UNKNOW_ERROR = -1;
    public static final int RESPONSE_NO_INTERNET = -2;
    public static final int RESPONSE_FILE_NO_EXISTS = -3;
    public static final int RESPONSE_LOGIN_EXCEPTION = -4;
    public static final int RESPONSE_HTTP_EXCEPTION = -5;

    public int requestCode;
    public String body; // jason data
    public int responseCode; // response code,

    public HttpResult() {
    }

    public HttpResult(int requestCode, String body, int responseCode) {
        super();
        this.requestCode = requestCode;
        this.body = body;
        this.responseCode = responseCode;
    }
}
