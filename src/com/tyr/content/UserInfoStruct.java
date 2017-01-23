package com.tyr.content;


public class UserInfoStruct implements Comparable<UserInfoStruct> {
    public static final int STATE_OFFLINE = 1;
    public static final int STATE_ONLINE = 0; // 值更小，排在list前面

    public static final int SEX_MALE = 0; // 代码可读性
    public static final int SEX_FEMALE = 1;
    
    public static final int USED_ALOT_YES = 1;
    public static final int USED_ALOT_NO = 0;

    public static final int TYPE_MYSELF = 0; // 是当前的登录用户
    public static final int TYPE_FRIEND = 1; // 好友
    public static final int TYPE_TEMP = 2; // 临时用户，还没与被正式添加为好友

    public String account;
    public String userId;
    public String displayName;
    public String title; // 职位
    public String img;
    public String description;
    public String signature;
    public String birthday; // YYYY-MM-DD
    public int sex; // 0男, 1女
    // -- v2 --
    public String phone;
    
    // 服务器db以关系表储存不同userId对应的值
    public String group;
    public int usedAlot = USED_ALOT_NO; // 是否是常用联系人
    public String remarkName; // 备注姓名

    // -- add-ons --
    // 服务器db没有保存这写状态，但是本地数据库中需要记录
    public int type = TYPE_FRIEND;

    // 服务器db和本地都不需要保存，它只是一个临时状态，因此也不在构造方法里面体现
    public int state = STATE_OFFLINE; // 用户在线状态 online, offline

    public UserInfoStruct() {
    }

    public UserInfoStruct(String account, String userId, String displayName, String title,
            String img, String description, String signature, String birthday, int sex,
            String phone, String group, int usedAlot, int type, String remarkName) {
        super();
        this.account = account;
        this.userId = userId;
        this.displayName = displayName;
        this.title = title;
        this.img = img;
        this.description = description;
        this.signature = signature;
        this.birthday = birthday;
        this.sex = sex;
        this.phone = phone;
        this.group = group;
        this.usedAlot = usedAlot;
        this.type = type;
        this.remarkName = remarkName;
    }

    @Override
    public int compareTo(UserInfoStruct other) {
        // 实现排序
        // 在线状态优先
        if (state > other.state) {
            return 1;
        } else if (state < other.state) {
            return -1;
        } else {
            // 按姓名排序 strcmp()
            // b > a, B < a, a 更小排在前面 合乎逻辑，不需要取反
            return displayName.toLowerCase().compareTo(other.displayName.toLowerCase());
        }
    }
}
