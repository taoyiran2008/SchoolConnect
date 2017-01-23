package com.tyr.data;

import com.tyr.service.MySocketService;

import android.content.Context;
import android.content.SharedPreferences;

public class SchoolConnectPreferences {

    protected static SharedPreferences mPreferences;
    protected static SharedPreferences.Editor mEditor;

    private static final String PREFERENCE_FILE_NAME = "SchoolConnect";
    private static final String SERVER_HTTP_ADDRESS = "server_http_ip";
    private static final String SERVER_HTTP_PORT = "server_http_port";
    private static final String SERVER_SOCKET_ADDRESS = "server_socket_ip";
    private static final String SERVER_SOCKET_PORT = "server_socket_port";
    private static final String POLLING_TIME = "polling_time";
    private static final String USERID = "user_id";
    private static final String PASSWORD = "password";
    private static final String REGISTERED = "registered"; // 用于判断用户是否登入

    public static void load(Context context) {
        try {
            mPreferences = context.getSharedPreferences(PREFERENCE_FILE_NAME, 0);
        } catch (Exception e) {
        }
    }

    public static void setHttpAddress(String ip) {
        mEditor = mPreferences.edit();
        mEditor.putString(SERVER_HTTP_ADDRESS, ip);
        mEditor.commit();
    }

    public static String getHttpAddress() {
        return mPreferences.getString(SERVER_HTTP_ADDRESS, "127.0.0.1");
    }

    public static void setHttpPort(int port) {
        mEditor = mPreferences.edit();
        mEditor.putInt(SERVER_HTTP_PORT, port);
        mEditor.commit();
    }

    public static int getHttpPort() {
        return mPreferences.getInt(SERVER_HTTP_PORT, 8080);
    }

    public static void setSocketAddress(String ip) {
        mEditor = mPreferences.edit();
        mEditor.putString(SERVER_HTTP_ADDRESS, ip);
        mEditor.commit();
    }

    public static String getSocketAddress() {
        return mPreferences.getString(SERVER_SOCKET_ADDRESS, "192.168.42.184");
    }

    public static void setSocketPort(int port) {
        mEditor = mPreferences.edit();
        mEditor.putInt(SERVER_SOCKET_PORT, port);
        mEditor.commit();
    }

    public static int getSocketPort() {
        return mPreferences.getInt(SERVER_SOCKET_PORT, 8888);
    }

    public static void setUserId(String userid) {
        mEditor = mPreferences.edit();
        mEditor.putString(USERID, userid);
        mEditor.commit();
    }

    public static String getUserId() {
        return mPreferences.getString(USERID, MySocketService.USERID);
    }

    public static void setPassword(String password) {
        mEditor = mPreferences.edit();
        mEditor.putString(PASSWORD, password);
        mEditor.commit();
    }

    public static String getPassword() {
        return mPreferences.getString(PASSWORD, "0000");
    }

    public static void setPollingTime(int pollingTime) {
        mEditor = mPreferences.edit();
        mEditor.putInt(POLLING_TIME, pollingTime);
        mEditor.commit();
    }

    public static int getPollingTime() {
        return mPreferences.getInt(POLLING_TIME, 6000);
    }
    
    public static void setRegistered(boolean registered) {
        mEditor = mPreferences.edit();
        mEditor.putBoolean(REGISTERED, registered);
        mEditor.commit();
    }

    public static boolean getRegistered() {
        return mPreferences.getBoolean(REGISTERED, false);
    }
    
    /**
     * 用户注销的时候清空所有本地 property 
     */
    public static void clear(){
        mEditor = mPreferences.edit();
        mEditor.clear();
        mEditor.commit();
    }
}
