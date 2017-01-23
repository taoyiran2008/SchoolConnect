package com.tyr.service;

import java.util.Calendar;

import org.json.JSONException;
import org.json.JSONObject;

import android.R.integer;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.tyr.activities.BaseActivity;
import com.tyr.activities.MainActivity;
import com.tyr.asynctask.ActionType;
import com.tyr.asynctask.AsyncHttpListener;
import com.tyr.asynctask.HttpResult;
import com.tyr.content.JSONKeyDef;
import com.tyr.content.MessageStruct;
import com.tyr.data.MyApplication;
import com.tyr.util.Debugger;
import com.tyr.util.HttpUtil;
import com.tyr.util.NotificationUtil;

public class MyBroadcastReceiver extends BroadcastReceiver {
    public static final String ACTION_PULL_MSG = "action.pull.msg"; // 定时轮询
    public static final String ACTION_NEW_MSG = "action.new.msg"; // 轮询获取服务器消息，并产生推送通知
    public static final String ACTION_INCOMING_MSG = "action.incoming.message"; // 收到新的聊天消息
    public static final int REQUEST_PULL_MSG = 1;
    // 原则上只有当前进程能收到这个通知，测试的时候一个设备上会跑多个程序
    public static final String PERMISSION = "com.tyr.ui.permission.receive";
    public MyApplication mApplication;

    @Override
    public void onReceive(Context context, Intent intent) {
        Debugger.logDebug("onReceive: " + intent.getAction());
        if (mApplication == null) {
            mApplication = ((MyApplication) (context.getApplicationContext()));
        }

        if (intent.getAction().equals(ACTION_PULL_MSG)) {
            pullNewData(context);
        } else if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            // start service

            Debugger.logDebug("boot completed");
            setAlarmTime(context, MyApplication.mPollingTime);
        }
    }

    private void pullNewData(final Context context) {
        AsyncHttpListener listener = new AsyncHttpListener() {
            public void onPost(HttpResult response) {
                if (response != null) {
                    String data = response.body;
                    if (response.responseCode == HttpResult.RESPONSE_OK) {
                        if (response.requestCode == ActionType.GET_NEW_MSG_COUNT) {
                            try {
                                // json data is way too simple，A specified Parser is not necessary
                                JSONObject json = new JSONObject(data);
                                int cnt = 0;
                                if (json.has(JSONKeyDef.NEWS_NEW_MSG_CNT)) {
                                    cnt = json.getInt(JSONKeyDef.NEWS_NEW_MSG_CNT);
                                }
                                if (cnt > 0) {
                                    // 发送通知提醒用户
                                    NotificationUtil.showNewMsgNotification(context, cnt);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancel() {
            }

            public void onTimeout() {
                Debugger.logDebug("http 请求超时");
            }
        };
        MyApplication application = (MyApplication) context.getApplicationContext();
        HttpUtil.pullNewData(context, listener, application.mRefreshTime[MainActivity.INDEX_SCHOOL]);
    }

    /**
     * 设置轮询事件，并启动Alarm，这种方式在系统休眠的时候也会进行
     */
    public static void setAlarmTime(Context context, long time) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(ACTION_PULL_MSG);
        PendingIntent sender = PendingIntent.getBroadcast(context, REQUEST_PULL_MSG, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        am.setRepeating(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis(), time,
                sender);
    }
    
    /**
     * 发送更新主页面最新消息条数的通知 
     */
    public static void sendBroadcastNewMsgCnt(Context context, int index, int cnt){
        Intent intent = new Intent(MyBroadcastReceiver.ACTION_NEW_MSG);
        intent.putExtra(BaseActivity.EXTRA_INDEX_TYPE, index);
        intent.putExtra(BaseActivity.EXTRA_NEW_MSG_CNT, cnt);
        context.sendBroadcast(intent, PERMISSION);
    }
    
    /**
     * 发送接收到新的聊天信息的通知 
     */
    public static void sendBroadcastIncomingMsg(Context context, MessageStruct message){
        Intent intent = new Intent(MyBroadcastReceiver.ACTION_INCOMING_MSG);
        intent.putExtra(BaseActivity.EXTRA_INCOMING_MSG, message);
        context.sendBroadcast(intent, PERMISSION);
    }
}
