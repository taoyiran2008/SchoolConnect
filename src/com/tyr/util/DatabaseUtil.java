package com.tyr.util;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;

import com.tyr.content.MessageRecordStruct;
import com.tyr.content.MessageStruct;
import com.tyr.content.NewsStruct;
import com.tyr.content.UserInfoStruct;
import com.tyr.data.MyApplication;
import com.tyr.data.SchoolConnectProvider.DatabaseHelper;
import com.tyr.data.SchoolConnectProviderData.MessageTableData;
import com.tyr.data.SchoolConnectProviderData.NewsTableData;
import com.tyr.data.SchoolConnectProviderData.UserTableData;

public class DatabaseUtil {
    private static final int NEWS_LIMITED_NUMBER = 10;

    // user table
    /**
     * 添加用户信息到数据库，可以使好友信息，自己的账户信息(isMyself)和临时用户信息（isTemp） 
     */
    public static boolean addUser(Context context, UserInfoStruct userInfo) {
        ContentValues values = new ContentValues();
        String account = ((MyApplication) (context.getApplicationContext())).myself.userId;
        values.put(UserTableData.ACCOUNT, account);
        values.put(UserTableData.USER_ID, userInfo.userId);
        values.put(UserTableData.DISPLAY_NAME, userInfo.displayName);
        values.put(UserTableData.TITLE, userInfo.title);
        values.put(UserTableData.IMG, userInfo.img);
        values.put(UserTableData.SEX, userInfo.sex);
        values.put(UserTableData.DESCRIPTION, userInfo.description);
        values.put(UserTableData.SIGNATURE, userInfo.signature);
        values.put(UserTableData.BIRTHDAY, userInfo.birthday);
        values.put(UserTableData.PHONE, userInfo.phone);
        values.put(UserTableData.USED_A_LOT, userInfo.usedAlot);
        values.put(UserTableData.TYPE, userInfo.type);
        if (context.getContentResolver().insert(UserTableData.CONTENT_URI, values) == null) {
            return false;
        } else {
            return true;
        }
    }

    public static boolean deleteUser(Context context, String userId) {
        String account = ((MyApplication) (context.getApplicationContext())).myself.userId;
        String where = UserTableData.USER_ID + "=? AND " + UserTableData.ACCOUNT + "=? ";
        String selectionArgs[] = { userId, account };
        if (context.getContentResolver().delete(UserTableData.CONTENT_URI, where, selectionArgs) > 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 获取好友列表
     */
    public static ArrayList<UserInfoStruct> getContacts(Context context) {
        String account = ((MyApplication) (context.getApplicationContext())).myself.userId;
        String where = UserTableData.ACCOUNT + "=? ";
        String selectionArgs[] = { account };
        ArrayList<UserInfoStruct> list = new ArrayList<UserInfoStruct>();
        Cursor cursor = context.getContentResolver().query(UserTableData.CONTENT_URI, null, where,
                selectionArgs, null);
        while (cursor != null && cursor.moveToNext()) {
            String userId = cursor.getString(cursor.getColumnIndex(UserTableData.USER_ID));
            String displayName = cursor
                    .getString(cursor.getColumnIndex(UserTableData.DISPLAY_NAME));
            String title = cursor.getString(cursor.getColumnIndex(UserTableData.TITLE));
            String img = cursor.getString(cursor.getColumnIndex(UserTableData.IMG));
            String description = cursor.getString(cursor.getColumnIndex(UserTableData.DESCRIPTION));
            String signature = cursor.getString(cursor.getColumnIndex(UserTableData.SIGNATURE));
            String birthday = cursor.getString(cursor.getColumnIndex(UserTableData.BIRTHDAY));
            String phone = cursor.getString(cursor.getColumnIndex(UserTableData.PHONE));
            String group = cursor.getString(cursor.getColumnIndex(UserTableData.GROUP));
            int sex = cursor.getInt(cursor.getColumnIndex(UserTableData.SEX));
            int usedAlot = cursor.getInt(cursor.getColumnIndex(UserTableData.USED_A_LOT));

            int type = cursor.getInt(cursor.getColumnIndex(UserTableData.TYPE));
            String remarkName = cursor.getString(cursor.getColumnIndex(UserTableData.REMARK_NAME));
            if (type == UserInfoStruct.TYPE_FRIEND) {
                // 不把自身和临时用户添加进去
                list.add(new UserInfoStruct(account, userId, displayName, title, img, description,
                        signature, birthday, sex, phone, group, usedAlot, type, remarkName));
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        return list;
    }

    public static UserInfoStruct getUser(Context context, String userId) {
        UserInfoStruct userinfo = null;
        String account = ((MyApplication) (context.getApplicationContext())).myself.userId;
        String where = UserTableData.USER_ID + "=? AND " + UserTableData.ACCOUNT + "=? ";
        String selectionArgs[] = { userId, account };
        // projection, where, selection
        Cursor cursor = context.getContentResolver().query(UserTableData.CONTENT_URI, null, where,
                selectionArgs, null);
        if (cursor != null && cursor.moveToNext()) {
            String displayName = cursor
                    .getString(cursor.getColumnIndex(UserTableData.DISPLAY_NAME));
            String title = cursor.getString(cursor.getColumnIndex(UserTableData.TITLE));
            String img = cursor.getString(cursor.getColumnIndex(UserTableData.IMG));
            String description = cursor.getString(cursor.getColumnIndex(UserTableData.DESCRIPTION));
            String signature = cursor.getString(cursor.getColumnIndex(UserTableData.SIGNATURE));
            String birthday = cursor.getString(cursor.getColumnIndex(UserTableData.BIRTHDAY));
            String phone = cursor.getString(cursor.getColumnIndex(UserTableData.PHONE));
            String group = cursor.getString(cursor.getColumnIndex(UserTableData.GROUP));
            int sex = cursor.getInt(cursor.getColumnIndex(UserTableData.SEX));
            int usedAlot = cursor.getInt(cursor.getColumnIndex(UserTableData.USED_A_LOT));

            int type = cursor.getInt(cursor.getColumnIndex(UserTableData.TYPE));
            String remarkName = cursor.getString(cursor.getColumnIndex(UserTableData.REMARK_NAME));
            userinfo = new UserInfoStruct(account, userId, displayName, title, img, description,
                    signature, birthday, sex, phone, group, usedAlot, type, remarkName);
        }
        if (cursor != null) {
            cursor.close();
        }
        return userinfo;
    }

    public static UserInfoStruct getMyself(Context context) {
        UserInfoStruct userinfo = null;
        String account = ((MyApplication) (context.getApplicationContext())).myself.userId;
        String where = UserTableData.TYPE + "=? AND " + UserTableData.ACCOUNT + "=? ";
        String selectionArgs[] = { String.valueOf(UserInfoStruct.TYPE_MYSELF), account };
        // projection, where, selection
        Cursor cursor = context.getContentResolver().query(UserTableData.CONTENT_URI, null, where,
                selectionArgs, null);
        if (cursor != null && cursor.moveToNext()) {
            String displayName = cursor
                    .getString(cursor.getColumnIndex(UserTableData.DISPLAY_NAME));
            String userId = cursor.getString(cursor.getColumnIndex(UserTableData.USER_ID));
            String title = cursor.getString(cursor.getColumnIndex(UserTableData.TITLE));
            String img = cursor.getString(cursor.getColumnIndex(UserTableData.IMG));
            String description = cursor.getString(cursor.getColumnIndex(UserTableData.DESCRIPTION));
            String signature = cursor.getString(cursor.getColumnIndex(UserTableData.SIGNATURE));
            String birthday = cursor.getString(cursor.getColumnIndex(UserTableData.BIRTHDAY));
            String phone = cursor.getString(cursor.getColumnIndex(UserTableData.PHONE));
            String group = cursor.getString(cursor.getColumnIndex(UserTableData.GROUP));
            int sex = cursor.getInt(cursor.getColumnIndex(UserTableData.SEX));
            int usedAlot = cursor.getInt(cursor.getColumnIndex(UserTableData.USED_A_LOT));

            int type = cursor.getInt(cursor.getColumnIndex(UserTableData.TYPE));
            String remarkName = cursor.getString(cursor.getColumnIndex(UserTableData.REMARK_NAME));
            userinfo = new UserInfoStruct(account, userId, displayName, title, img, description,
                    signature, birthday, sex, phone, group, usedAlot, type, remarkName);
        }
        if (cursor != null) {
            cursor.close();
        }
        return userinfo;
    }

    /**
     * 可能在用户登陆前被调用 
     */
    public static boolean isUserExists(Context context, String userId) {
        boolean isExisted = false;
        String projection[] = { UserTableData.USER_ID };
        UserInfoStruct myself = ((MyApplication) (context.getApplicationContext())).myself;
        String account = myself.userId;
        String where = UserTableData.USER_ID + "=? AND " + UserTableData.ACCOUNT + "=? ";
        String selectionArgs[] = { userId, account };
        Cursor curor = context.getContentResolver().query(UserTableData.CONTENT_URI, projection,
                where, selectionArgs, null);
        if (curor != null && curor.moveToNext()) {
            isExisted = true;
        }
        if (curor != null) {
            curor.close();
        }
        return isExisted;
    }

    public static boolean updateUser(Context context, UserInfoStruct userInfo) {
        String userId = userInfo.userId;
        if (deleteUser(context, userId)) {
            return addUser(context, userInfo);
        } else {
            return false;
        }
    }
    
    public static boolean addUpdateUser(Context context, UserInfoStruct userInfo) {
        boolean isExisted = isUserExists(context, userInfo.userId);
        if (isExisted) {
            return updateUser(context, userInfo);
        } else {
            return addUser(context, userInfo);
        }
    }

    /**
     * 删除本地数据库中与服务器不同步的信息（可能是在其他地方做过删除用户操作）
     * 
     */
    public static void syncUsers(Context context, ArrayList<String> idList) {
        // delete * from table where id not in ('a','b')
        // String where = OrderTableData.ORDER_GROSS_TYPE + " =?";
        String account = ((MyApplication) (context.getApplicationContext())).myself.userId;
        String where = UserTableData.ACCOUNT + "=? ";
        String args[] = { account };
        Cursor cursor = context.getContentResolver().query(UserTableData.CONTENT_URI, null, where,
                args, UserTableData.DEFAULT_SORT_ORDER);
        while (cursor != null && cursor.moveToNext()) {
            String userId = cursor.getString(cursor.getColumnIndex(UserTableData.USER_ID));
            if (!idList.contains(userId)) {
                deleteUser(context, userId);
            }
        }
        if (cursor != null) {
            cursor.close();
        }
    }

    // message table
    public static boolean addMessage(Context context, MessageStruct message) {
        ContentValues values = new ContentValues();
        String account = ((MyApplication) (context.getApplicationContext())).myself.userId;
        values.put(MessageTableData.ACCOUNT, account);
        values.put(MessageTableData.USER_ID, message.userId);
        values.put(MessageTableData.SENT, message.sent);
        values.put(MessageTableData.DATE, message.date);
        values.put(MessageTableData.TIME, message.time);
        values.put(MessageTableData.CONTENT, message.content);
        values.put(MessageTableData.READ_FLAG, message.readFlag);
        values.put(MessageTableData.TYPE, message.type);

        if (context.getContentResolver().insert(MessageTableData.CONTENT_URI, values) == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 获取特定聊天对象最近一次的聊天内容(任意的消息类型)
     */
    public static MessageStruct getLatestMessage(Context context, String userId) {
        MessageStruct message = null;
        String account = ((MyApplication) (context.getApplicationContext())).myself.userId;
        String where = MessageTableData.ACCOUNT + "=? AND " + MessageTableData.USER_ID + "=? ";
        String args[] = { account, userId };
        // projection, where, selection
        Cursor cursor = context.getContentResolver().query(MessageTableData.CONTENT_URI, null,
                where, args, null);
        if (cursor != null && cursor.moveToNext()) {
            int sent = cursor.getInt(cursor.getColumnIndex(MessageTableData.SENT));
            String date = cursor.getString(cursor.getColumnIndex(MessageTableData.DATE));
            String time = cursor.getString(cursor.getColumnIndex(MessageTableData.TIME));
            String content = cursor.getString(cursor.getColumnIndex(MessageTableData.CONTENT));
            int type = cursor.getInt(cursor.getColumnIndex(MessageTableData.TYPE));
            int readFlag = cursor.getInt(cursor.getColumnIndex(MessageTableData.READ_FLAG));
            message = new MessageStruct(account, userId, time, date, sent, content, type, readFlag);
        }
        if (cursor != null) {
            cursor.close();
        }
        return message;
    }

    /**
     * 获取所有特定聊天对象的信息，每次获取之后把read flag设为true
     */
    public static ArrayList<MessageStruct> getMessages(Context context, String userId) {
        ArrayList<MessageStruct> messages = new ArrayList<MessageStruct>();
        String account = ((MyApplication) (context.getApplicationContext())).myself.userId;
        String where = MessageTableData.ACCOUNT + "=? AND " + MessageTableData.USER_ID
                + "=? AND " + MessageTableData.TYPE + "=? ";
        // 仅查询聊天消息类型
        String args[] = { account, userId, String.valueOf(MessageStruct.TYPE_MSG) };
        // projection, where, selection
        Cursor cursor = context.getContentResolver().query(MessageTableData.CONTENT_URI, null,
                where, args, null);
        while (cursor != null && cursor.moveToNext()) {
            // _id _count 为默认字段
            int _id = cursor.getInt(cursor.getColumnIndex(MessageTableData._ID));
            int sent = cursor.getInt(cursor.getColumnIndex(MessageTableData.SENT));
            String date = cursor.getString(cursor.getColumnIndex(MessageTableData.DATE));
            String time = cursor.getString(cursor.getColumnIndex(MessageTableData.TIME));
            String content = cursor.getString(cursor.getColumnIndex(MessageTableData.CONTENT));
            int type = cursor.getInt(cursor.getColumnIndex(MessageTableData.TYPE));
            int readFlag = cursor.getInt(cursor.getColumnIndex(MessageTableData.READ_FLAG));
            messages.add(new MessageStruct(account, userId, time, date, sent, content, type,
                    readFlag));
            
            if (readFlag == 0) {
                // set read flag true
                setMessageReadFlag(context, _id, 1);
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        return messages;
    }

    /**
     * 分页获取特定聊天对象的信息，每次获取之后把read flag设为true
     */
    public static ArrayList<MessageStruct> getMessages(Context context, String userId, int page,
            int pageNumber, int startOff) {
        ArrayList<MessageStruct> messages = new ArrayList<MessageStruct>();
        String account = ((MyApplication) (context.getApplicationContext())).myself.userId;
        // projection, where, selection
        DatabaseHelper helper = new DatabaseHelper(context);
        int limit = pageNumber;
        int offset = page * pageNumber + startOff;
        // order by _id desc, 按插入顺序倒叙排列，保证最新的消息在最前面(不能放在offset ? 之后)
        Cursor cursor = helper
                .getWritableDatabase()
                .rawQuery(
                        "select * from message_table where account = ? and userid = ? and type = ? order by _id desc limit ? offset ?",
                        new String[] { account, userId, String.valueOf(MessageStruct.TYPE_MSG), String.valueOf(limit), String.valueOf(offset) });
        while (cursor != null && cursor.moveToNext()) {
            int _id = cursor.getInt(cursor.getColumnIndex(MessageTableData._ID));
            int sent = cursor.getInt(cursor.getColumnIndex(MessageTableData.SENT));
            String date = cursor.getString(cursor.getColumnIndex(MessageTableData.DATE));
            String time = cursor.getString(cursor.getColumnIndex(MessageTableData.TIME));
            String content = cursor.getString(cursor.getColumnIndex(MessageTableData.CONTENT));
            int type = cursor.getInt(cursor.getColumnIndex(MessageTableData.TYPE));
            int readFlag = cursor.getInt(cursor.getColumnIndex(MessageTableData.READ_FLAG));
            messages.add(0, new MessageStruct(account, userId, time, date, sent, content, type,
                    readFlag));

            if (readFlag == 0) {
                // set read flag true
                setMessageReadFlag(context, _id, 1);
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        return messages;
    }

    /**
     * 获取未读message的记录数
     */
    public static int getUnreadMessagesCnt(Context context, String userId) {
        int cnt = 0;
        String account = ((MyApplication) (context.getApplicationContext())).myself.userId;
        String where = MessageTableData.ACCOUNT + "=? AND " + MessageTableData.USER_ID
                + "=? AND " + MessageTableData.TYPE + "=? " ;
        // 仅查询聊天消息类型
        String args[] = { account, userId, String.valueOf(MessageStruct.TYPE_MSG) };
        
        // projection, where, selection
        Cursor cursor = context.getContentResolver().query(MessageTableData.CONTENT_URI, null,
                where, args, null);
        while (cursor != null && cursor.moveToNext()) {
            int readFlag = cursor.getInt(cursor.getColumnIndex(MessageTableData.READ_FLAG));
            if (readFlag == 0) {
                cnt++;
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        return cnt;
    }

    /**
     * 修改消息的read flag值，不同于其他更新方法，因为message的插入顺序是不能修改的，因此在
     * 这里单纯修改其状态而非删除 重建。另外由于MessageStruct中没有保存_id值，我们在获取消息
     * 列表的方法里进行设置
     */
    private static void setMessageReadFlag(Context context, int _id, int readFlag) {
        String account = ((MyApplication) (context.getApplicationContext())).myself.userId;
        String where = UserTableData.ACCOUNT + "=? AND " + MessageTableData._ID + "=? ";
        String args[] = { account, String.valueOf(_id) };

        ContentValues values = new ContentValues();
        values.put(MessageTableData.READ_FLAG, readFlag);

        context.getContentResolver().update(MessageTableData.CONTENT_URI, values, where, args);
    }

    /**
     * 获取每一个会话最近一次的message record信息.
     */
    public static ArrayList<MessageRecordStruct> getLatestMessageRecords(Context context) {
        ArrayList<MessageRecordStruct> messageRecords = new ArrayList<MessageRecordStruct>();
        ArrayList<String> userList = getMessageUserList(context);
        for (String userId : userList) {
            MessageStruct message = getLatestMessage(context, userId);
            MyApplication application = (MyApplication) context.getApplicationContext();
            UserInfoStruct user = application.getUserInfo(userId);
            
            if (message.type == MessageStruct.TYPE_REQ) {
                // 好友请求只最近一条有效
                messageRecords.add(new MessageRecordStruct(user.img, user.displayName, userId,
                        1, message));
            } else {
                int unreadCnt = getUnreadMessagesCnt(context, userId);
                messageRecords.add(new MessageRecordStruct(user.img, user.displayName, userId,
                        unreadCnt, message));
            }
        }
        return messageRecords;
    }

    /**
     * 获取聊天历史记录对象的列表
     */
    public static ArrayList<String> getMessageUserList(Context context) {
        ArrayList<String> userList = new ArrayList<String>();
        String account = ((MyApplication) (context.getApplicationContext())).myself.userId;
        String where = UserTableData.ACCOUNT + "=? ";
        String args[] = { account };
        // projection, where, selection
        Cursor cursor = context.getContentResolver().query(MessageTableData.CONTENT_URI, null,
                where, args, null);
        while (cursor != null && cursor.moveToNext()) {
            String userId = cursor.getString(cursor.getColumnIndex(MessageTableData.USER_ID));
            if (!userList.contains(userId)) {
                userList.add(userId);
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        return userList;
    }

    /**
     * 删除特定聊天对象的有关所有聊天记录
     */
    public static boolean deleteMessages(Context context, String userId) {
        String account = ((MyApplication) (context.getApplicationContext())).myself.userId;
        String where = UserTableData.USER_ID + "=? AND " + UserTableData.ACCOUNT + "=? ";
        String selectionArgs[] = { userId, account };
        if (context.getContentResolver().delete(MessageTableData.CONTENT_URI, where, selectionArgs) > 0) {
            return true;
        } else {
            return false;
        }
    }

    public static void registerMessagesObserver(Context context, ContentObserver observer) {
        context.getContentResolver().registerContentObserver(MessageTableData.CONTENT_URI, true,
                observer);
    }

    public static void unregisterMessagesObserver(Context context, ContentObserver observer) {
        context.getContentResolver().unregisterContentObserver(observer);
    }

    // news table
    private static boolean addNews(Context context, NewsStruct news) {
        ContentValues values = new ContentValues();
        String account = ((MyApplication) (context.getApplicationContext())).myself.userId;
        values.put(NewsTableData.ACCOUNT, account);
        values.put(NewsTableData.NEWS_ID, news.newsId);
        values.put(NewsTableData.TITLE, news.title);
        values.put(NewsTableData.IMG, news.img);
        values.put(NewsTableData.AUTHOR, news.author);
        values.put(NewsTableData.DESCRIPTION, news.description);
        values.put(NewsTableData.DATE, news.date);
        values.put(NewsTableData.TYPE, news.type);

        if (context.getContentResolver().insert(NewsTableData.CONTENT_URI, values) == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 一种type的新闻只在数据库中保留一页（10条）数据，如果数据条目大于限制的数量，则删除最后一条记录，然后 在最前面做插入
     */
    public static void addLimitedNews(Context context, NewsStruct news, int type) {
        // 先做插入
        addNews(context, news);

        String account = ((MyApplication) (context.getApplicationContext())).myself.userId;
        String where = NewsTableData.ACCOUNT + "=? AND " + NewsTableData.TYPE + "=? ";
        String args[] = { account, String.valueOf(type) };
        // projection, where, selection
        Cursor cursor = context.getContentResolver().query(NewsTableData.CONTENT_URI, null, where,
                args, null);

        if (cursor.getCount() > NEWS_LIMITED_NUMBER) {
            // 记录集中跳到指定的记录项目，得到id号，进行删除
            if (cursor != null && cursor.moveToLast()) {
                String newsId = cursor.getString(cursor.getColumnIndex(NewsTableData.NEWS_ID));
                where = NewsTableData.NEWS_ID + "=? AND " + NewsTableData.ACCOUNT + "=? ";
                String selectionArgs[] = { newsId, account };
                context.getContentResolver()
                        .delete(UserTableData.CONTENT_URI, where, selectionArgs);
            }
        }
        if (cursor != null) {
            cursor.close();
        }
    }

    public static ArrayList<NewsStruct> getNews(Context context, int type) {
        String account = ((MyApplication) (context.getApplicationContext())).myself.userId;
        String where = NewsTableData.ACCOUNT + "=? AND " + NewsTableData.TYPE + "=? ";
        String selectionArgs[] = { account, String.valueOf(type) };
        ArrayList<NewsStruct> list = new ArrayList<NewsStruct>();
        Cursor cursor = context.getContentResolver().query(NewsTableData.CONTENT_URI, null, where,
                selectionArgs, null);
        while (cursor != null && cursor.moveToNext()) {
            String newsId = cursor.getString(cursor.getColumnIndex(NewsTableData.NEWS_ID));
            String title = cursor.getString(cursor.getColumnIndex(NewsTableData.TITLE));
            String img = cursor.getString(cursor.getColumnIndex(NewsTableData.IMG));
            String description = cursor.getString(cursor.getColumnIndex(NewsTableData.DESCRIPTION));
            String author = cursor.getString(cursor.getColumnIndex(NewsTableData.AUTHOR));
            String date = cursor.getString(cursor.getColumnIndex(NewsTableData.DATE));
            list.add(new NewsStruct(account, newsId, title, img, author, date, description, type));
        }
        if (cursor != null) {
            cursor.close();
        }
        return list;
    }

    public static boolean isNewsExists(Context context, String newsId) {
        boolean isExisted = false;
        String projection[] = { NewsTableData.NEWS_ID };
        String account = ((MyApplication) (context.getApplicationContext())).myself.userId;
        String where = NewsTableData.NEWS_ID + "=? AND " + NewsTableData.ACCOUNT + "=? ";
        String selectionArgs[] = { newsId, account };
        Cursor curor = context.getContentResolver().query(NewsTableData.CONTENT_URI, projection,
                where, selectionArgs, null);
        if (curor != null && curor.moveToNext()) {
            isExisted = true;
        }
        if (curor != null) {
            curor.close();
        }
        return isExisted;
    }

    /**
     * 清空数据表，用户注销的时候调用
     */
    public static void clearData(Context context) {
        // 清除数据库内容
        context.getContentResolver().delete(UserTableData.CONTENT_URI, null, null);
        context.getContentResolver().delete(NewsTableData.CONTENT_URI, null, null);
        context.getContentResolver().delete(MessageTableData.CONTENT_URI, null, null);
    }
}
