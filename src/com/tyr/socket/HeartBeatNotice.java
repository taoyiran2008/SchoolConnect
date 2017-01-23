package com.tyr.socket;

public class HeartBeatNotice implements TCPNotice{
    public String src;
    
    public HeartBeatNotice(String src) {
        super();
        this.src = src;
    }
}
