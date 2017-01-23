package com.tyr.socket;


public class FriendResponseNotice implements TCPNotice{
    
    public String src; // 发送者的userId
    public String dest;
    public boolean agree; // 是否同意添加
    
    public FriendResponseNotice(String src, String dest, boolean agree) {
        super();
        this.src = src;
        this.dest = dest;
        this.agree = agree;
    }
}
