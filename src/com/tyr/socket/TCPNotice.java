package com.tyr.socket;



/**
 * TCP 消息包的基类 
 * TCPNotice在传输前是按照约定的数据结构传输的，但是在网络传输中归根结底还是数据流的传输，
 * 我们无法保证每一次send都是发送的一个notice，可能是几个notice被一起发送过来，一次我们在
 * 解析中加入了start标签。
 * 虽然我们无法保证每一次receive都是一个完整的notice，但是可以确定的是发送和接受的时序，先发送
 * 先接受。
 * 每一个消息都包含几个基本的field，类型 type，发送者 src，接收者 dest
 */
public interface TCPNotice {
    public static final int TYPE_INIT = 0; // 初次建立连接发送的初始化信息
    public static final int TYPE_MESSAGE = 1;
    public static final int TYPE_LOGOUT = 2; // 用户正常退出
    public static final int TYPE_LOGIN = 3;
    public static final int TYPE_FRIEND_REQUEST = 4;
    public static final int TYPE_FRIEND_RESPONSE = 5;
    public static final int TYPE_HEART_BEAT = 6; // 心跳包，确认客户端与服务器的连接状态，断开则尝试重连，保持Socket 长连接
    public static final int TYPE_KICK = 7; // 被系统踢出通知（因为用户再次登录，被迫下线）
    
    public static final String NOTICE_START = "[start] ";
}
