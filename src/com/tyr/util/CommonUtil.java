package com.tyr.util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.view.inputmethod.InputMethodManager;

import com.tyr.content.UserInfoStruct;
import com.tyr.data.MyApplication;

public class CommonUtil {

    public static Boolean isConnection(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo != null
                && (activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI || activeNetInfo
                        .getType() == ConnectivityManager.TYPE_MOBILE)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * get phone number.
     * 
     * @return phone number.
     */
    public static String getTelePhoneNumber(Context context) {
        String phoneNumber = "";
        TelephonyManager tm = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        phoneNumber = tm.getLine1Number();
        return phoneNumber;
    }

    public static String getDeviceToken(Context context) {
        final TelephonyManager tm = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);

        final String deviceId, serialNo, androidId;
        deviceId = "" + tm.getDeviceId();
        serialNo = "" + tm.getSimSerialNumber();
        // only devices with a gogle account has an Android_ID
        androidId = ""
                + android.provider.Settings.Secure.getString(context.getContentResolver(),
                        android.provider.Settings.Secure.ANDROID_ID);

        UUID uuid = new UUID(androidId.hashCode(), ((long) deviceId.hashCode() << 32)
                | serialNo.hashCode());
        return uuid.toString();
    }

    public static void hideSoftKeyboard(Context context) {
        InputMethodManager imm = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        // imm.hideSoftInputFromWindow(myEditText.getWindowToken(), 0);
        if (imm.isActive()) {
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT,
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    public static String getCurrentTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        return formatter.format(new Date());
    }

    public static String getCurrentDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(new Date());
    }

    public static String getCurrentFullTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(new Date());
    }

    public static void call(Context context, String number) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tel:" + number)); // 不需要加权限
        context.startActivity(intent);
    }

    public static void sendSMS(Context context, String number) {
        Uri uri = Uri.parse("smsto:" + number);
        Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
        intent.putExtra("sms_body", "The SMS text");
        context.startActivity(intent);
    }

    public static String getAppVersion(Context context) {
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packInfo;
        try {
            packInfo = packageManager.getPackageInfo(context.getPackageName(),
                    PackageManager.GET_META_DATA);
            return packInfo.versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int getAndroidVersion() {
        return android.os.Build.VERSION.SDK_INT;
    }

    public static void installApk(Context context, File file) {
        Uri packageUri = Uri.fromFile(file);
        Intent intent = new Intent(Intent.ACTION_VIEW, packageUri);
        intent.setDataAndType(packageUri, "application/vnd.android.package-archive");
        context.startActivity(intent);
    }
    
    /**
     * for test.
     * Http 模拟返回的好友信息
     */
    public static UserInfoStruct getDummyUserInfo(Context context, String userId, int type) {
        Map<String, String> nameMap = new HashMap<String, String>();
        nameMap.put("100", "Tom");
        
        nameMap.put("110", "Jerry");
        nameMap.put("111", "Susan");
        nameMap.put("112", "Angela");
        nameMap.put("113", "Caroline");
        nameMap.put("114", "Max");
        nameMap.put("115", "Tom");
        
        nameMap.put("120", "Rachel");
        nameMap.put("121", "Smith");
        nameMap.put("122", "Teddy");
        nameMap.put("123", "Andrew");
        String account = ((MyApplication) (context.getApplicationContext())).myself.userId;
        UserInfoStruct userInfo = new UserInfoStruct(account, userId, "taoyr", "director", 
                "http://pic22.nipic.com/20120701/10060471_110050715319_2.jpg", "我的描述", 
                "个人签名", "1989-1-1", 
                UserInfoStruct.SEX_MALE, "150150166", "朋友", 
                UserInfoStruct.USED_ALOT_NO, 
                type, "taoyr");
        userInfo.displayName = nameMap.get(userId);
        userInfo.remarkName = nameMap.get(userId);
        return userInfo;
    }
}
