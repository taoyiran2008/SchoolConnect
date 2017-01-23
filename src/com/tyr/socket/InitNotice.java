package com.tyr.socket;

public class InitNotice implements TCPNotice{
    public String userId; // 上线的userId
    
    public InitNotice(String userId) {
        super();
        this.userId = userId;
    }
}
