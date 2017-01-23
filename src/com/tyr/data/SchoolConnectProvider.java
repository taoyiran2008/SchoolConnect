package com.tyr.data;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.tyr.data.SchoolConnectProviderData.MessageTableData;
import com.tyr.data.SchoolConnectProviderData.NewsTableData;
import com.tyr.data.SchoolConnectProviderData.UserTableData;

public class SchoolConnectProvider extends ContentProvider {
    private static final int CODE_MESSAGE = 1;
    private static final int CODE_USER = 2;
    private static final int CODE_NEWS = 3;
    private DatabaseHelper mOpenHelper;

    public static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(SchoolConnectProviderData.AUTHORITY, SchoolConnectProviderData.MESSAGE_TABLE_NAME,
                CODE_MESSAGE);
        uriMatcher.addURI(SchoolConnectProviderData.AUTHORITY, SchoolConnectProviderData.USER_TABLE_NAME,
                CODE_USER);
        uriMatcher.addURI(SchoolConnectProviderData.AUTHORITY, SchoolConnectProviderData.NEWS_TABLE_NAME,
                CODE_NEWS);
    }

    public static HashMap<String, String> messageProjectionMap;
    static {
        messageProjectionMap = new HashMap<String, String>();
        messageProjectionMap.put(MessageTableData._ID, MessageTableData._ID);
        messageProjectionMap.put(MessageTableData.ACCOUNT, MessageTableData.ACCOUNT);
        messageProjectionMap.put(MessageTableData.USER_ID, MessageTableData.USER_ID);
        messageProjectionMap.put(MessageTableData.SENT, MessageTableData.SENT);
        messageProjectionMap.put(MessageTableData.DATE, MessageTableData.DATE);
        messageProjectionMap.put(MessageTableData.TIME, MessageTableData.TIME);
        messageProjectionMap.put(MessageTableData.CONTENT, MessageTableData.CONTENT);
        messageProjectionMap.put(MessageTableData.TYPE, MessageTableData.TYPE);
        messageProjectionMap.put(MessageTableData.READ_FLAG, MessageTableData.READ_FLAG);
    }

    public static HashMap<String, String> userProjectionMap;
    static {
        userProjectionMap = new HashMap<String, String>();
        userProjectionMap.put(UserTableData._ID, UserTableData._ID);
        userProjectionMap.put(MessageTableData.ACCOUNT, MessageTableData.ACCOUNT);
        userProjectionMap.put(UserTableData.USER_ID, UserTableData.USER_ID);
        userProjectionMap.put(UserTableData.DISPLAY_NAME, UserTableData.DISPLAY_NAME);
        userProjectionMap.put(UserTableData.TITLE, UserTableData.TITLE);
        userProjectionMap.put(UserTableData.IMG, UserTableData.IMG);
        userProjectionMap.put(UserTableData.DESCRIPTION, UserTableData.DESCRIPTION);
        userProjectionMap.put(UserTableData.SIGNATURE, UserTableData.SIGNATURE);
        userProjectionMap.put(UserTableData.BIRTHDAY, UserTableData.BIRTHDAY);
        userProjectionMap.put(UserTableData.TYPE, UserTableData.TYPE);
        userProjectionMap.put(UserTableData.SEX, UserTableData.SEX);
        userProjectionMap.put(UserTableData.PHONE, UserTableData.PHONE);
        userProjectionMap.put(UserTableData.GROUP, UserTableData.GROUP);
        userProjectionMap.put(UserTableData.USED_A_LOT, UserTableData.USED_A_LOT);
        userProjectionMap.put(UserTableData.REMARK_NAME, UserTableData.REMARK_NAME);
    }

    public static HashMap<String, String> newsProjectionMap;
    static {
        newsProjectionMap = new HashMap<String, String>();
        newsProjectionMap.put(NewsTableData._ID, NewsTableData._ID);
        newsProjectionMap.put(MessageTableData.ACCOUNT, MessageTableData.ACCOUNT);
        newsProjectionMap.put(NewsTableData.TITLE, NewsTableData.TITLE);
        newsProjectionMap.put(NewsTableData.AUTHOR, NewsTableData.AUTHOR);
        newsProjectionMap.put(NewsTableData.NEWS_ID, NewsTableData.NEWS_ID);
        newsProjectionMap.put(NewsTableData.IMG, NewsTableData.IMG);
        newsProjectionMap.put(NewsTableData.DESCRIPTION, NewsTableData.DESCRIPTION);
        newsProjectionMap.put(NewsTableData.DATE, NewsTableData.DATE);
        newsProjectionMap.put(NewsTableData.TYPE, NewsTableData.TYPE);
    }

   public static class DatabaseHelper extends SQLiteOpenHelper {

        public DatabaseHelper(Context context) {
            super(context, SchoolConnectProviderData.DATABASE_NAME, null,
                    SchoolConnectProviderData.DATABASE_VERSION);
        }

        public DatabaseHelper(Context context, String name, CursorFactory factory, int version) {
            super(context, name, factory, version);
            // TODO Auto-generated constructor stub
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.i("neusoft", "dtatabase  created");
            db.execSQL("Create table " + MessageTableData.TABLE_NAME
                    + "( _id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + MessageTableData.ACCOUNT + " TEXT,"
                    + MessageTableData.USER_ID + " TEXT,"
                    + MessageTableData.DATE + " TEXT,"
                    + MessageTableData.TIME + " TEXT,"
                    + MessageTableData.SENT + " TINYINT,"
                    + MessageTableData.TYPE + " TINYINT,"
                    + MessageTableData.READ_FLAG + " TINYINT,"
                    + MessageTableData.CONTENT + " TEXT);");

            db.execSQL("Create table " + UserTableData.TABLE_NAME
                    + "( _id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + UserTableData.ACCOUNT + " TEXT,"
                    + UserTableData.USER_ID + " TEXT,"
                    + UserTableData.DISPLAY_NAME + " TEXT,"
                    + UserTableData.TITLE + " TEXT,"
                    + UserTableData.IMG + " TEXT,"
                    + UserTableData.DESCRIPTION + " TEXT,"
                    + UserTableData.SIGNATURE + " TEXT,"
                    + UserTableData.BIRTHDAY + " TEXT,"
                    + UserTableData.PHONE + " TEXT,"
                    + UserTableData.GROUP + " TEXT,"
                    + UserTableData.REMARK_NAME + " TEXT,"
                    + UserTableData.TYPE + " TINYINT,"
                    + UserTableData.USED_A_LOT + " TINYINT,"
                    + UserTableData.SEX + " TINYINT);");

            db.execSQL("Create table " + NewsTableData.TABLE_NAME
                    + "( _id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + NewsTableData.ACCOUNT + " TEXT,"
                    + NewsTableData.NEWS_ID + " TEXT,"
                    + NewsTableData.TITLE + " TEXT,"
                    + NewsTableData.AUTHOR + " TEXT,"
                    + NewsTableData.IMG + " TEXT,"
                    + NewsTableData.DESCRIPTION + " TEXT,"
                    + NewsTableData.DATE + " TEXT,"
                    + NewsTableData.TYPE + " TINYINT);");

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + MessageTableData.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + UserTableData.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + NewsTableData.TABLE_NAME);
            onCreate(db);

        }
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        switch (uriMatcher.match(uri)) {
        case CODE_MESSAGE: {
            qb.setTables(MessageTableData.TABLE_NAME);
            qb.setProjectionMap(messageProjectionMap);
            String orderBy;
            if (TextUtils.isEmpty(sortOrder)) {
                orderBy = MessageTableData.DEFAULT_SORT_ORDER;
            } else {
                orderBy = sortOrder;
            }
            Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        }
        case CODE_USER: {
            qb.setTables(UserTableData.TABLE_NAME);
            qb.setProjectionMap(userProjectionMap);
            String orderBy;
            if (TextUtils.isEmpty(sortOrder)) {
                orderBy = UserTableData.DEFAULT_SORT_ORDER;
            } else {
                orderBy = sortOrder;
            }
            Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        }
        case CODE_NEWS: {
            qb.setTables(NewsTableData.TABLE_NAME);
            qb.setProjectionMap(newsProjectionMap);
            String orderBy;
            if (TextUtils.isEmpty(sortOrder)) {
                orderBy = NewsTableData.DEFAULT_SORT_ORDER;
            } else {
                orderBy = sortOrder;
            }
            Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        }
        }
        return null;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
        case CODE_MESSAGE:
            return MessageTableData.CONTENT_TYPE;
        case CODE_USER:
            return UserTableData.CONTENT_TYPE;
        case CODE_NEWS:
            return NewsTableData.CONTENT_TYPE;
        default:
            throw new IllegalArgumentException("Unknown uri" + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        switch (uriMatcher.match(uri)) {
        case CODE_MESSAGE: {
            long rowId = db.insert(MessageTableData.TABLE_NAME, null, values);
            if (rowId > 0) {
                Uri insertedUserUri = ContentUris.withAppendedId(uri, rowId);
                getContext().getContentResolver().notifyChange(insertedUserUri, null);
                return insertedUserUri;
            }
            break;
        }
        case CODE_USER: {
            long rowId = db.insert(UserTableData.TABLE_NAME, null, values);
            if (rowId > 0) {
                Uri insertedUserUri = ContentUris.withAppendedId(uri, rowId);
                getContext().getContentResolver().notifyChange(insertedUserUri, null);
                return insertedUserUri;
            }
            break;
        }
        case CODE_NEWS: {
            long rowId = db.insert(NewsTableData.TABLE_NAME, null, values);
            if (rowId > 0) {
                Uri insertedUserUri = ContentUris.withAppendedId(uri, rowId);
                getContext().getContentResolver().notifyChange(insertedUserUri, null);
                return insertedUserUri;
            }
            break;
        }
        default:
            throw new SQLException("Failed to insert row into " + uri);
        }
        return null;

    }

    @Override
    public int delete(Uri uri, String where, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = 0;
        switch (uriMatcher.match(uri)) {
        case CODE_MESSAGE:
            count = db.delete(MessageTableData.TABLE_NAME, where, selectionArgs);
            break;
        case CODE_USER:
            count = db.delete(UserTableData.TABLE_NAME, where, selectionArgs);
            break;
        case CODE_NEWS:
            count = db.delete(NewsTableData.TABLE_NAME, where, selectionArgs);
            break;
        default:
            break;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = 0;
        switch (uriMatcher.match(uri)) {
        case CODE_MESSAGE:
            count = db.update(MessageTableData.TABLE_NAME, values, selection, selectionArgs);
            break;
        case CODE_USER:
            count = db.update(UserTableData.TABLE_NAME, values, selection, selectionArgs);
            break;
        case CODE_NEWS:
            count = db.update(NewsTableData.TABLE_NAME, values, selection, selectionArgs);
            break;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        // 事务能大大提高处理速度
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            int numValues = values.length;
            for (int i = 0; i < numValues; i++) {
                switch (uriMatcher.match(uri)) {
                case CODE_MESSAGE: {
                    if (db.insert(MessageTableData.TABLE_NAME, null, values[i]) < 0)
                        return 0;
                    break;
                }
                case CODE_USER: {
                    if (db.insert(UserTableData.TABLE_NAME, null, values[i]) < 0)
                        return 0;
                    break;
                }
                case CODE_NEWS: {
                    if (db.insert(NewsTableData.TABLE_NAME, null, values[i]) < 0)
                        return 0;
                    break;
                }
                default:
                    throw new SQLException("Failed to insert row into " + uri);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction(); // 事务中有一个错误，所有的数据库操作均回滚防止脏数据产生
        }
        return values.length;
    }
    
    public void queryMessage(){
        
    }

}
