package com.tyr.service;

import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.tyr.asynctask.ActionType;
import com.tyr.asynctask.AsyncHttpListener;
import com.tyr.asynctask.HttpResult;
import com.tyr.content.MessageStruct;
import com.tyr.content.UserInfoStruct;
import com.tyr.content.parser.UserInfoParser;
import com.tyr.data.MyApplication;
import com.tyr.data.SchoolConnectPreferences;
import com.tyr.socket.FriendRequestNotice;
import com.tyr.socket.FriendResponseNotice;
import com.tyr.socket.HeartBeatNotice;
import com.tyr.socket.InitNotice;
import com.tyr.socket.KickNotice;
import com.tyr.socket.LoginNotice;
import com.tyr.socket.LogoutNotice;
import com.tyr.socket.MessageNotice;
import com.tyr.socket.SocketClient;
import com.tyr.socket.SocketClient.OnReceiveNoticeListener;
import com.tyr.socket.TCPNotice;
import com.tyr.util.CommonUtil;
import com.tyr.util.DatabaseUtil;
import com.tyr.util.Debugger;
import com.tyr.util.HttpUtil;

/**
 * 用Service提供Socket相关的服务和API调用，所有全局的内容放到Application里面耦合性太强，独立 出Socket的模块。
 */
public class MySocketService extends Service {
    public static final String SERVERIP = SchoolConnectPreferences.getSocketAddress();
    public static final int SERVERPORT = SchoolConnectPreferences.getSocketPort();
    // USERID和REQUEST_FRIENDID 不应该在好友列表中
    public static final String USERID = "121"; // for test
    public static final String FRIENDID = "119"; // for test
    public static final String REQUEST_FRIENDID = "120"; // for test
    private static final int SERVER_CONNECT_RETRY_CNT = 3;
    private static final int HEART_BEAT_NOT_RECEIVED_CNT = 5;
    private static final int SEND_HEART_BEAT_PERIOD = 20000;
    private static final int CHECK_HEART_BEAT_PERIOD = 20000;
    // 用于建立TCP连接，目前所有消息都是建立在TCP上的，图像传输考虑用UDP
    MyApplication mApplication;
    SocketClient mSocketClient;
    Context mContext;
    Timer mTimerSender = new Timer(); // 心跳包周期性发送
    Timer mTimerChecker = new Timer(); // 检查服务器是否回应心跳包

    class TaskSendHeartBeat extends TimerTask {
        public void run() {
            mSocketClient.send(new HeartBeatNotice(USERID));
        }
    }

    class TaskCheckHeartBeat extends TimerTask {
        public void run() {
            if (!mSocketClient.isRegistered) {
                Debugger.logDebug("check server heart beat periodically -- ng");
                mSocketClient.noCheckInCnt++;
                if (mSocketClient.noCheckInCnt > HEART_BEAT_NOT_RECEIVED_CNT) {
                    // 失去连接重连
                    Debugger.logDebug("lose connection, try to reconnect");
                    mSocketClient.isAlive = false;
                    // 释放当前socket client的资源，重连后会新创建一个
                    mSocketClient.closeSocket();
                    mTimerSender.cancel();
                    mTimerChecker.cancel();
                    connect();
                }
            } else {
                Debugger.logDebug("check server heart beat periodically -- ok");
                // 统计清零
                mSocketClient.isRegistered = false;
                mSocketClient.noCheckInCnt = 0;
            }
        }
    }
    
    public void connect() {
        int retry = 0;
        mTimerSender = new Timer();
        mTimerChecker = new Timer();
        Debugger.logDebug("start connectting ... hold on");
        while (retry <= SERVER_CONNECT_RETRY_CNT) {
            // 如果因为服务器断开，连接失败，会发生 failed to connet to address，而且有较长一段时间的阻塞
            mSocketClient = new SocketClient(SERVERIP, SERVERPORT);
            if (!mSocketClient.isAlive) {
                retry++;
                Debugger.logDebug("Connection retry attempts: " + retry);
                try {
                    Thread.sleep(1000); // 1s 后尝试重连
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                // 断线重连的case里面，因为此前建立的socket已经失效，无法发送exit指令，在server维护
                // 的socket会在一段时间后自行销毁，因此需要重新与server建立连接，并且发送初始化信息
                mSocketClient.send(new InitNotice(USERID));
                mSocketClient.setOnReceiveNoticeListener(new OnReceiveNoticeListener() {
                    public void onReceive(TCPNotice notice) {
                        handleNotice(notice);
                    }

                    public void onInit(InitNotice initNotice, SocketClient client) {
                    }
                });
                 mTimerSender.schedule(new TaskSendHeartBeat(), 0, SEND_HEART_BEAT_PERIOD);
                 mTimerChecker.schedule(new TaskCheckHeartBeat(), 0, CHECK_HEART_BEAT_PERIOD);
                break;
            }
        }
        if (!mSocketClient.isAlive) {
            Debugger.logDebug("you are offline!!!!!!!!");
            // new SingleAlertDialog(mContext).showDialog("用户名或密码为空");
        }
    }
    
    public void disconnect(){
        mTimerSender.cancel();
        mTimerChecker.cancel();
        mSocketClient.closeSocket();
    }
    
    public void onCreate() {
        Debugger.logDebug("socket service started");
        mContext = this;
        mApplication = (MyApplication) getApplicationContext();

        super.onCreate();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        super.onDestroy();
    }

    public IBinder onBind(Intent arg0) {
        return new MyBinder();
    }

    public class MyBinder extends Binder {
        public MySocketService getService() {
            return MySocketService.this;
        }
    }

    // 收到的TCP消息是全局的，和UI的交互可以考虑更新全局数据，被观察者通知UI update。
    // 也可以发送广播，这样相关的UI便可以接收到消息
    // TODO 咱没有加同步控制，会有方法重入race风险
    private void handleNotice(TCPNotice _notice) {
        if (_notice instanceof MessageNotice) {
            final MessageNotice notice = (MessageNotice) _notice;
            Debugger.logDebug("receive a new message from " + notice.src + " msg = " + notice.msg);
            MessageStruct message = new MessageStruct(mApplication.myself.account, notice.src,
                    CommonUtil.getCurrentTime(), CommonUtil.getCurrentDate(), 0, notice.msg, MessageStruct.TYPE_MSG, 0);
            DatabaseUtil.addMessage(mContext, message);
            
            // 发送收到新聊天消息的广播，通知UI更新
            MyBroadcastReceiver.sendBroadcastIncomingMsg(mContext, message);
        } else if (_notice instanceof FriendResponseNotice) {
            final FriendResponseNotice notice = (FriendResponseNotice) _notice;
            if (notice.agree) {
                // 早在发送添加好友请求的时候（发送者），就已经获取过一次了，也只有获取用户信息成功后才能
                // 发送好友添加请求，所以这里不需要做从新http获取的保险处理
                UserInfoStruct userInfo = mApplication.getUserInfo(notice.src);
                String msg = userInfo.displayName + "已经同意你的好友申请，你们可以开始聊天了";
                Debugger.logDebug(msg);
                MessageStruct message = new MessageStruct(mApplication.myself.account, notice.src,
                        CommonUtil.getCurrentTime(), CommonUtil.getCurrentDate(), 0, msg, MessageStruct.TYPE_MSG, 0);
                DatabaseUtil.addMessage(mContext, message);
                
                // 把新增的好友从临时用户变为联系人，并修改本地数据库
                userInfo.type = UserInfoStruct.TYPE_FRIEND;
                DatabaseUtil.addUpdateUser(mContext, userInfo);
                
                // 更新内存中的好友列表
                mApplication.mContactListObservable.addContact(userInfo);
                
                // 发送上线通知
                mApplication.sendNotice(new LoginNotice(mApplication.myself.account, notice.src));
            }

        } else if (_notice instanceof FriendRequestNotice) {
            final FriendRequestNotice notice = (FriendRequestNotice) _notice;
            // 请求用户的详细信息还没有保存在本地缓存里面，要么把displayName放到socket notice里，
            //  要么在这里进行一次HTTP请求
            // Debugger.logDebug("来自" + notice.src + "的好友请求");
            
            // 接收者此前没有保存该用户的任何信息，需要进行一次请求
            HttpUtil.getUserInfo(mContext, new AsyncHttpListener() {
                public void onTimeout() {
                }
                
                public void onPost(HttpResult response) {
                    if (response != null) {
                        String data = response.body;
                        if (response.responseCode == HttpResult.RESPONSE_OK) {
                            if (response.requestCode == ActionType.GET_USER_INFO) {
                                UserInfoParser parser = null;
                                try {
                                    parser = new UserInfoParser(mContext, new JSONObject(data));
                                    UserInfoStruct userInfo = parser.getUserInfo();
                                    userInfo = CommonUtil.getDummyUserInfo(mContext, MySocketService.REQUEST_FRIENDID, UserInfoStruct.TYPE_TEMP);
                                    // 标记为临时用户信息
                                    userInfo.type = UserInfoStruct.TYPE_TEMP;
                                    // 加入到临时用户列表中
                                    mApplication.addTempContact(userInfo);
                                    // 任然添加到数据库中
                                    DatabaseUtil.addUpdateUser(mContext, userInfo);
                                    
                                    String msg = "来自" + userInfo.displayName + "的好友请求\n\n" + notice.msg;
                                    MessageStruct message = new MessageStruct(mApplication.myself.account, notice.src,
                                            CommonUtil.getCurrentTime(), CommonUtil.getCurrentDate(), 0, msg, MessageStruct.TYPE_REQ, 0);
                                    DatabaseUtil.addMessage(mContext, message);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            // 这一次好友添加请求可能会在此丢失
                            Debugger.logError("获取用户信息失败，网络异常或者用户信息无效！");
                        }
                    }
                }
                
                public void onCancel() {
                }
            }, notice.src);
            
        } else if (_notice instanceof LoginNotice) {
            LoginNotice notice = (LoginNotice) _notice;
            String src = notice.src;
            Debugger.logDebug("a friend is on line " + src);
            // 这里有一个setChange 观察者的地方，会触发update 方法，该方法是在本线程执行的，因此
            // 在相应UI更新的地方需要使用UI线程的handler
            mApplication.mContactListObservable.setUserState(src, UserInfoStruct.STATE_ONLINE);
        } else if (_notice instanceof LogoutNotice) {
            LogoutNotice notice = (LogoutNotice) _notice;
            String src = notice.src;
            Debugger.logDebug("a friend is off line " + src);
            mApplication.mContactListObservable.setUserState(src, UserInfoStruct.STATE_OFFLINE);
        } else if (_notice instanceof HeartBeatNotice) {
            HeartBeatNotice notice = (HeartBeatNotice) _notice;
            String src = notice.src;
            Debugger.logDebug("server responce heart beat " + src);
        } else if (_notice instanceof InitNotice) {
        } else if (_notice instanceof KickNotice) {
            KickNotice notice = (KickNotice) _notice;
            String src = notice.userId;
            Debugger.logDebug("you are kicked out, please re-login");
        }
    }

    public boolean send(TCPNotice notice) {
        if (mSocketClient == null) {
            return false;
        } else {
            return mSocketClient.send(notice);
        }
    }
}
