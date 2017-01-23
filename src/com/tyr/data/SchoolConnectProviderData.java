package com.tyr.data;

import android.net.Uri;
import android.provider.BaseColumns;

public class SchoolConnectProviderData {
    public static final String AUTHORITY = "com.tyr.data.SchoolConnectProvider";
    public static final String DATABASE_NAME = "school_connect.db";
    public static final int DATABASE_VERSION = 1;
    public static final String MESSAGE_TABLE_NAME = "message_table";
    public static final String USER_TABLE_NAME = "user_table";
    public static final String NEWS_TABLE_NAME = "news_table";

    // BaseColumn类中已经包含了 _id字段
    public static final class MessageTableData implements BaseColumns {
        private MessageTableData() {
        }

        public static final String TABLE_NAME = MESSAGE_TABLE_NAME;
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
                + "/" + TABLE_NAME);
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.schoolconnect.msgs";
        public static final String DEFAULT_SORT_ORDER = "_id desc";
        // 字段
        public static final String ACCOUNT = "account";
        public static final String USER_ID = "userid";
        public static final String SENT = "sent";
        public static final String DATE = "date";
        public static final String TIME = "local";
        public static final String CONTENT = "content";
        public static final String TYPE = "type";
        public static final String READ_FLAG = "read_flag";
//        public static final String IMG = "img"; 冗余数据
    }

    public static final class UserTableData implements BaseColumns {
        private UserTableData() {
        }

        public static final String TABLE_NAME = USER_TABLE_NAME;
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
                + "/" + TABLE_NAME);
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.schoolconnect.user";
        public static final String DEFAULT_SORT_ORDER = "_id desc";
        // 字段
        public static final String ACCOUNT = "account";
        public static final String USER_ID = "userid";
        public static final String DISPLAY_NAME = "displayname";
        public static final String TITLE = "title";
        public static final String IMG = "img";
        public static final String DESCRIPTION = "description";
        public static final String SIGNATURE = "signature";
        public static final String BIRTHDAY = "birthday";
        public static final String SEX = "sex";
        public static final String PHONE = "phone";
        public static final String GROUP = "contact_group"; // sql 系统关键字
        public static final String USED_A_LOT = "used_a_lot";
        public static final String TYPE = "type";
        public static final String REMARK_NAME = "remark_name";
    }
    
    public static final class NewsTableData implements BaseColumns {
        private NewsTableData() {
        }

        public static final String TABLE_NAME = NEWS_TABLE_NAME;
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
                + "/" + TABLE_NAME);
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.schoolconnect.news";
        // 默认排序
        public static final String DEFAULT_SORT_ORDER = "_id desc";
        // 字段
        public static final String ACCOUNT = "account";
        public static final String NEWS_ID = "newsid";
        public static final String TITLE = "title";
        public static final String IMG = "img";
        public static final String DESCRIPTION = "description";
        public static final String AUTHOR = "author";
        public static final String DATE = "date";
        public static final String TYPE = "type";
    }
}
