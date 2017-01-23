package com.tyr.activities;

import java.io.File;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.tyr.asynctask.ActionType;
import com.tyr.asynctask.AsyncHttpListener;
import com.tyr.asynctask.AsyncHttpTask;
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
 * 登陆界面，用户验证 --> 获取用户个人信息 -- > 获取Contact list -- > 都成功，跳转到主页面 -- >
 * 启动连接Socket 服务器
 */
public class LoginActivity extends BaseActivity implements AsyncHttpListener {

    private Button loginBtn;
    private EditText userText;
    private EditText passwordText;
    private EditText socketAddressText;
    private EditText socketPortText;

    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initData();
        initView();
    }

    private void initData() {
    }

    private void initView() {
        initTopBar("用户登录", true, null);
        loginBtn = (Button) findViewById(R.id.login);
        userText = (EditText) findViewById(R.id.user_name);
        passwordText = (EditText) findViewById(R.id.password);
        socketAddressText = (EditText) findViewById(R.id.socket_address);
        socketPortText = (EditText) findViewById(R.id.socket_port);

        String user = SchoolConnectPreferences.getUserId();
        String pw = SchoolConnectPreferences.getPassword();
        String socketAddress = SchoolConnectPreferences.getSocketAddress();
        String socketPort = String.valueOf(SchoolConnectPreferences.getSocketPort());
        userText.setText(user);
        passwordText.setText(pw);
        socketAddressText.setText(socketAddress);
        socketPortText.setText(socketPort);
        
        loginBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                login();
            }
        });
    }

    private void login() {
        String userId = userText.getText().toString();
        String password = passwordText.getText().toString();
        if (userId.isEmpty() || password.isEmpty()) {
            new SingleAlertDialog(mContext).showDialog("用户名或密码为空");
            return;
        }
        
        final AsyncHttpTask task = HttpUtil.login(mContext, this, CommonUtil.getDeviceToken(mContext),
                userId, password, MyApplication.VERSION);
        
        mProgressDialog.show("用户登录中 ...", new OnClickListener() {
            @Override
            public void onClick(View v) {
                mProgressDialog.dismiss();
                task.cancel(true);
            }
        });
    }

    public void onPost(HttpResult response) {
        if (response != null) {
            mProgressDialog.dismiss();
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
                                final DownloadThread thread = new DownloadThread(mContext, apkURL, new Handler(), new DownloadCallback() {
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
                            }else {
                                // proceeds to next login step.
                                // get current user info.
                                HttpUtil.getUserInfo(mContext, this, userText.getText().toString());
                            }
                        }
                        // save for later use
                        SchoolConnectPreferences.setUserId(userText.getText().toString());
                        SchoolConnectPreferences.setPassword(passwordText.getText().toString());
                        SchoolConnectPreferences.setSocketAddress(socketAddressText.getText().toString());
                        SchoolConnectPreferences.setSocketPort(Integer.parseInt(socketPortText.getText().toString()));
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
                        
                        getContactListViaHttp();
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

    private void getContactListViaHttp() {
        // get contact list via HTTP request
        final AsyncHttpTask task = HttpUtil.getContactList(mContext, this, mApplication.myself.userId);
        
        mProgressDialog.show("获取好友列表中 ...", new OnClickListener() {
            @Override
            public void onClick(View v) {
                mProgressDialog.dismiss();
                task.cancel(true);
            }
        });
    }
}
