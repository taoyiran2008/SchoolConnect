package com.tyr.socket;

public class KickNotice implements TCPNotice{
    public String userId; // 要踢出的userId
    public String ip; // 上一次登陆的IP
    
    public KickNotice(String userId, String ip) {
        super();
        this.userId = userId;
        this.ip = ip;
    }
}
