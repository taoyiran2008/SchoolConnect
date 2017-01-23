package com.tyr.socket;

public class FriendRequestNotice implements TCPNotice{
    
    public String src; // 发送者的userId
    public String dest;
    public String msg; // 添加好友说明信息
    
    public FriendRequestNotice(String src, String dest, String msg) {
        super();
        this.src = src;
        this.dest = dest;
        this.msg = msg;
    }
}
