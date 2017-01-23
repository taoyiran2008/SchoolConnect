package com.tyr.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.tyr.activities.BaseActivity;
import com.tyr.activities.MainActivity;
import com.tyr.service.MyBroadcastReceiver;
import com.tyr.ui.R;

public class NotificationUtil {
    public final static int NEW_MSG = 1;

    public static void showNewMsgNotification(Context context, int cnt) {

        String title = "有新的消息了";
        String info = "您有" + cnt + "条新消息，请及时查看";
        // for temporary use
        int icon = R.drawable.notification_new_msg;
        Notification notification = new Notification.BigTextStyle(new Notification.Builder(context)
                .setContentTitle(title).setTicker(title).setContentText(info).setSmallIcon(icon)
                .setNumber(cnt).setAutoCancel(true)).bigText(info).build();

        Intent intent = new Intent(context, MainActivity.class);
        // 跳到MainActitiy页，cnt 传过去没有意义，因为一旦该页面被打开就认为消息已读，未读消息
        // 提示应该从一级菜单消除
        intent.putExtra(BaseActivity.EXTRA_INDEX_TYPE, MainActivity.INDEX_SCHOOL);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, NEW_MSG, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        notification.contentIntent = pendingIntent;
        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NEW_MSG, notification);

        // 发送一个广播通知界面更新number circle
        MyBroadcastReceiver.sendBroadcastNewMsgCnt(context, MainActivity.INDEX_SCHOOL, cnt);
    }

    public static void canceMessageNotify(Context context, int requestId) {
        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(requestId);
    }

    public static void cancelAllNotify(Context context) {
        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancelAll();
    }
}
