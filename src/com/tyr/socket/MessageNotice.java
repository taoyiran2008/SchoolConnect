package com.tyr.socket;

public class MessageNotice implements TCPNotice{
    // 数据结构的封装没有按照成员变量的命名规范来，便于引用，也没有使用setter getter，在android
    // 里面直接引用效率更好
    
    public String src; // 发送者的userId
    public String dest;
    public String msg; // 消息内容
    
    public MessageNotice(String src, String dest, String msg) {
        super();
        this.src = src;
        this.dest = dest;
        this.msg = msg;
    }
}
