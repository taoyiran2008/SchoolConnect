package com.tyr.asynctask;

import android.util.SparseArray;

public class ActionType {

    // HTTP action definition
    public static final int LOGIN = 0;
    public static final int GET_CONTACT_LIST = 1; // 获取好友列表
    public static final int GET_GROUP_LIST = 2; // 获取群列表
    public static final int GET_NEWS_LIST = 3; // 获取新闻信息,摘要列表
    public static final int SEARCH_SCHEDULE = 4; //检索课程
    public static final int ADD_FRIEND = 5; // 添加好友
    public static final int GET_USER_INFO = 6; // 获取用户信息
    public static final int SEARCH_USER = 7; // 检索用户
    public static final int MODIFY_USER = 8; // 修改用户信息
    public static final int UPDATE_USER_STATE_USE_A_LOG = 15; // 更新为常用联系人
    public static final int MODIFY_FRIEND = 16;
    
    public static final int LOAD_IMAGE_THUMB = 9;// 加载图片(thumb缩略图)
    public static final int LOAD_IMAGE_BIGGER = 10;// 加载图片(较大的缩略图，比如新闻展示图)
    public static final int UPLOAD_IMAGE = 11;// 上传图片
    public static final int GET_NEWS_DETAIL = 12; // 修改用户信息
    
    public static final int GET_NEW_MSG_COUNT = 13; // 获取未读的消息记录
//    public static final int PULL_MSG = 14; // 轮询方式向服务器请求数据，实现服务器Push功能
    public static final int SEND_READ_MSG_OK = 14; // 通知服务器消息已被该用户阅读
    

    private static SparseArray<String> mMethodMap = null;
    static {
        mMethodMap = new SparseArray<String>();
        mMethodMap.put(LOGIN, "login.action");
        mMethodMap.put(GET_CONTACT_LIST, "get_contact_list.action");
        mMethodMap.put(GET_GROUP_LIST, "get_group_list.action");
        mMethodMap.put(GET_NEWS_LIST, "get_news.action");
        mMethodMap.put(SEARCH_SCHEDULE, "search_schedule.action");
        mMethodMap.put(ADD_FRIEND, "add_friend.action");
        mMethodMap.put(GET_USER_INFO, "get_user_info.action");
        mMethodMap.put(UPLOAD_IMAGE, "upload_img.action");
        mMethodMap.put(SEARCH_USER, "search_user.action");
        mMethodMap.put(MODIFY_USER, "modify_user.action");
        mMethodMap.put(GET_NEWS_DETAIL, "get_news_detail.action");
    }

    public static String getMethod(int type){
        return mMethodMap.get(type);
    }

    // SOCKET action type
}
