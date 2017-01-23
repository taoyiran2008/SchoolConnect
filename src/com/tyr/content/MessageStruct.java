package com.tyr.content;

import java.io.Serializable;


public class MessageStruct implements Serializable{
    private static final long serialVersionUID = 1L;
    public static final int TYPE_NEWS = 0;
    public static final int TYPE_MSG = 1;
    public static final int TYPE_REQ = 2; // 好友请求
    
    // public long id; // 数据库id index
    public String account; // 用于保存各用户的聊天信息，即便注销也保留之前的聊天记录
    public String userId; // 本次会话的聊天对象的id
    public String time; // 发送时间 10:20
    public String date; // 发送日期 YYYY-MM-DD, 当天聊天内容只显示时间
    public int sent; // 会话中信息的发送者是否是自己, sqlite中没有boolean类型, 1表示是自己是发送者
    public String content; // 聊天内容
    
    // -- v2 --
    public int type; // 消息类型  （聊天消息 好友请求 新闻推送 图片消息）
    public int readFlag; // 是否已读，1为已读

    public MessageStruct() {
    }

    public MessageStruct(String account, String userId, String time, String date, int sent,
            String content, int type, int readFlag) {
        super();
        this.account = account;
        this.userId = userId;
        this.time = time;
        this.date = date;
        this.sent = sent;
        this.content = content;
        this.type = type;
        this.readFlag = readFlag;
    }
}
