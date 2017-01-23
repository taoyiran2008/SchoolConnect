package com.tyr.asynctask;

public interface AsyncHttpListener {
    // parse HTTP response data
    public void onPost(HttpResult response);
    // public void onUpdate(int progress);
    public void onCancel();
    public void onTimeout();
}
