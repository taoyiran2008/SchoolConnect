package com.tyr.socket;

public class LoginNotice implements TCPNotice{
    public String src; // 上线的userId
    public String dest; // 需要通知的user，如果为空则广播所有好友
    
    public LoginNotice(String src, String dest) {
        super();
        this.src = src;
        this.dest = dest;
    }
}
