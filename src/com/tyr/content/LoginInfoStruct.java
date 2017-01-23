package com.tyr.content;

public class LoginInfoStruct {
    public String deviceToken;
    public String url;
    public int hasUpdate;

    public LoginInfoStruct() {
    }

    public LoginInfoStruct(String deviceToken, String url, int hasUpdate) {
        super();
        this.deviceToken = deviceToken;
        this.url = url;
        this.hasUpdate = hasUpdate;
    }
}
