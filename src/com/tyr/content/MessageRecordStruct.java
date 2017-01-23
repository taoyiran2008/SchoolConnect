package com.tyr.content;

/**
 * 这个数据结构不是对应与数据库的vo，userinfo需要的字段(displayName, img)和message，包括
 * 未读的message条数集合到一起，用于表示消息记录
 */
public class MessageRecordStruct implements Comparable<MessageRecordStruct> {
    public String img;
    public String displayName;
    public String userId;
    public int cnt; // 未读信息的条数
    public MessageStruct message;

    public MessageRecordStruct() {
    }

    public MessageRecordStruct(String img, String displayName, String userId, int cnt,
            MessageStruct message) {
        super();
        this.img = img;
        this.displayName = displayName;
        this.userId = userId;
        this.cnt = cnt;
        this.message = message;
    }

    @Override
    public int compareTo(MessageRecordStruct other) {
        return displayName.toLowerCase().compareTo(other.displayName.toLowerCase());
    }
}
