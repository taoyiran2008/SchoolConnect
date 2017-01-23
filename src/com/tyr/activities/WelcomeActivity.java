package com.tyr.activities;

import java.io.File;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ProgressBar;

import com.tyr.asynctask.ActionType;
import com.tyr.asynctask.AsyncHttpListener;
import com.tyr.asynctask.DownloadCallback;
import com.tyr.asynctask.DownloadThread;
import com.tyr.asynctask.HttpResult;
import com.tyr.content.LoginInfoStruct;
import com.tyr.content.UserInfoStruct;
import com.tyr.content.parser.ContactListParser;
import com.tyr.content.parser.LoginInfoParser;
import com.tyr.content.parser.UserInfoParser;
import com.tyr.data.MyApplication;
import com.tyr.data.SchoolConnectPreferences;
import com.tyr.service.MySocketService;
import com.tyr.ui.R;
import com.tyr.ui.view.MyToast;
import com.tyr.ui.view.ProgressbarDialog;
import com.tyr.ui.view.SingleAlertDialog;
import com.tyr.util.CommonUtil;
import com.tyr.util.DatabaseUtil;
import com.tyr.util.Debugger;
import com.tyr.util.HttpUtil;

/**
 * 初始化欢迎界面，进行数据预读取。如果用户已经登陆过就默默silent 加载进入主界面
 */
public class WelcomeActivity extends BaseActivity implements AsyncHttpListener {
    ProgressBar mProgressBar;
    String mUserName;
    String mPassword;
    String mSocketAddress;
    String mSocketPort;
    boolean mRegistered;

    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        initData();
        initView();
    }

    private void initData() {
        mUserName = SchoolConnectPreferences.getUserId();
        mPassword = SchoolConnectPreferences.getPassword();
        mSocketAddress = SchoolConnectPreferences.getSocketAddress();
        mSocketPort = String.valueOf(SchoolConnectPreferences.getSocketPort());
        mRegistered = SchoolConnectPreferences.getRegistered();
    }

    private void initView() {
        mProgressBar = (ProgressBar) findViewById(R.id.progressbar_welcome);

        // 用户此前登陆过，用此前保存的信息尝试silent 登陆
        if (mRegistered) {
            login();
        } else {
            Intent intent = new Intent(mContext, LoginActivity.class);
            startActivity(intent);
            finish(); // 不同于exit，finish 会在界面跳转后执行到
        }
    }

    private void login() {
        HttpUtil.login(mContext, this, CommonUtil.getDeviceToken(mContext),
                mUserName, mPassword, MyApplication.VERSION);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    public void onPost(HttpResult response) {
        if (response != null) {
            String data = response.body;
            if (response.responseCode == HttpResult.RESPONSE_OK) {
                if (response.requestCode == ActionType.LOGIN) {
                    // parse data
                    LoginInfoParser parser = null;
                    LoginInfoStruct struct = null;
                    try {
                        parser = new LoginInfoParser(mContext, new JSONObject(data));
                        struct = parser.getLoginInfo();
                        final String apkURL = struct.url;
                        if (struct != null) {
                            if (struct.hasUpdate == 1) {
                                // start a task to download apk from url, and install.
                                final ProgressbarDialog dialog = new ProgressbarDialog(mContext);
                                final DownloadThread thread = new DownloadThread(mContext, apkURL,
                                        new Handler(), new DownloadCallback() {
                                            public void onProgress(int progress) {
                                                dialog.setProgress(progress);
                                            }

                                            public void onPost(File file) {
                                                dialog.dismiss();
                                                if (file != null) {
                                                    CommonUtil.installApk(mContext, file);
                                                }
                                            }
                                        });
                                thread.start();
                                dialog.showDialog("正在下载更新", new OnClickListener() {
                                    public void onClick(View arg0) {
                                        thread.interrupt();
                                    }
                                });
                            } else {
                                // proceeds to next login step.
                                // get current user info.
                                HttpUtil.getUserInfo(mContext, this, mUserName);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (response.requestCode == ActionType.GET_USER_INFO) {
                    UserInfoParser parser = null;
                    UserInfoStruct userInfo = null;
                    try {
                        parser = new UserInfoParser(mContext, new JSONObject(data));
                        userInfo = parser.getUserInfo();
                        userInfo.type = UserInfoStruct.TYPE_MYSELF;
                        // 第一次需要明确指定account.
                        userInfo.account = userInfo.userId;
                        mApplication.myself = userInfo;
                        
                        // substitute with dummy data for test.
                        userInfo = CommonUtil.getDummyUserInfo(mContext, MySocketService.USERID, UserInfoStruct.TYPE_MYSELF);
                        userInfo.account = userInfo.userId;
                        mApplication.myself = userInfo;
                        
                        DatabaseUtil.addUpdateUser(mContext, userInfo);
                        
                        // get contact list via HTTP request
                        HttpUtil.getContactList(mContext, this, mApplication.myself.userId);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (response.requestCode == ActionType.GET_CONTACT_LIST) {
                    // Contact List 需要在Login时候预加载，进入主页面后，Message 页面 Contact 页面
                    // 都需要这个数据，不然会导致Message获取用户信息的时候Contact 还没有加载完毕
                    // 的情况

                    // parse data
                    ContactListParser parser = null;
                    try {
                        parser = new ContactListParser(mContext, new JSONObject(data));
                        mApplication.mContactListObservable.setContactList(parser.getContactList());

                        // jump to MainActivity
                        Intent intent = new Intent(mContext, MainActivity.class);
                        startActivity(intent);

                        // connect to socket server
                        mApplication.connectSocket();
                        SchoolConnectPreferences.setRegistered(true);
                        
                        finish(); // 不保留Welcome 界面
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                // 统一各种请求的异常处理，简化逻辑
            } else if (response.responseCode == HttpResult.RESPONSE_LOGIN_EXCEPTION) {
                new SingleAlertDialog(mContext).showDialog("登陆验证失败\n" + data);
            } else if (response.responseCode == HttpResult.RESPONSE_NO_INTERNET) {
                new SingleAlertDialog(mContext).showDialog("网络异常\n" + data);
                // 加载数据库的缓存信息
                mApplication.mContactListObservable.setContactList(DatabaseUtil.getContacts(mContext));
            } else {
                new SingleAlertDialog(mContext).showDialog("未知的错误\n" + data);
            }
        }
    }

    public void onCancel() {
        MyToast.getInstance(mContext).display("已取消登陆");
    }

    public void onTimeout() {
        mProgressDialog.dismiss();
        Debugger.logDebug("http 请求超时");
        MyToast.getInstance(mContext).display("http 请求超时");
    }
}
