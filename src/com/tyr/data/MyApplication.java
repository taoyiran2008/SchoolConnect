package com.tyr.data;

import java.util.ArrayList;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.support.v4.util.LruCache;

import com.tyr.activities.MainActivity;
import com.tyr.asynctask.AsyncImageloader;
import com.tyr.content.NewsStruct;
import com.tyr.content.UserInfoStruct;
import com.tyr.service.MySocketService;
import com.tyr.socket.TCPNotice;
import com.tyr.util.DatabaseUtil;
import com.tyr.util.Debugger;
import com.tyr.util.FileUtil;

/**
 * 应用的全局资源维护类， 这个类中static与否关系并不大，因为都是在该应用程序进程退出时释放的， 
 * static 只是为了便于访问
 */
public class MyApplication extends Application {
    // version number, should be in consistency with versionName defined in Manifest.
    public static final double VERSION = 1.0;
    
    // image cached in memory
    // referenced by BitmapUtil, only one copy existed
    // file name --> Bitmap pairs，全局变量设为static，没有资源无法释放的风险
    public static LruCache<String, Bitmap> mImageCache;
    public static String mCacheDir;
    // cache by default, tend to be memory overflow
    public static boolean mCache2Memory = true;
    public static int mPollingTime = 60000; // ms, 1分钟轮询一次

    // only if user logged in, "myself" should not be null, can be used to determine if user has
    // logged in.
    public UserInfoStruct myself;

    // used to exchange data, serialize a data-structure is not needed in the same process.
    public UserInfoStruct mUserInfo;
    public NewsStruct mNewsInfo;

    // Imageloader should be in application-wide context
    public AsyncImageloader mImageloader;
    public ContactListObservable mContactListObservable = new ContactListObservable();
    // 刷新消息的最近时间，用于新消息轮询时候的查询新闻条数
    public String mRefreshTime[] = new String[MainActivity.INDEX_CNT];
    
    // 临时用户信息，比如收到添加好友请求后获取的该用户信息，它也会被保存到本地db中，一旦作为
    // 正式好友添加到db，将会删除临时用户条目（保证userId是唯一的）
    public ArrayList<UserInfoStruct> mTempUserList = new ArrayList<UserInfoStruct>();

    MySocketService mSocketService;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mSocketService = ((MySocketService.MyBinder) service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            mSocketService = null;
        }
    };

    public void onCreate() {
        super.onCreate();

        // start socket service
        Intent service = new Intent(this, MySocketService.class);
        bindService(service, mConnection, BIND_AUTO_CREATE);

        // load preference
        SchoolConnectPreferences.load(this);
        // 获取应用程序最大可用内存
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheSize = maxMemory / 8;
        Debugger.logDebug("maxMemory = " + maxMemory);
        Debugger.logDebug("cacheSize = " + cacheSize);
        // 设置图片缓存大小为程序最大可用内存的1/8
        mImageCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount();
            }
        };

        mCacheDir = FileUtil.getCacheFilePath(this);
        mImageloader = new AsyncImageloader(this);
        mPollingTime = SchoolConnectPreferences.getPollingTime();
    }

    public void onLowMemory() {
        super.onLowMemory();
    }

    public void onTerminate() {
        // Android系统机制会尽量让进程一直保存，该方法即便是kill掉进程，也不一定会执行到
        Debugger.logDebug("MyApplication terminated");
        unbindService(mConnection);
        super.onTerminate();
    }

    /**
     * 这种机制保证了数据在被设置后必须被获取并使用了，才能进行下一次的设置，保证了数据的正确
     * 性，虽然可能会导致下一次数据设置的丢失，但是基本上不可能发生。对于数据交互频繁的操作
     * 最好还是用广播来获取数据。适用于使用Intent在Activity间传递数据
     */
    public void setUser(UserInfoStruct userInfo) {
        synchronized (this) {
            if (mUserInfo != null) {
                Debugger.logError("previous data has not been fetched yet");
                return;
            }
            mUserInfo = userInfo;
        }
    }

    public UserInfoStruct getUser() {
        synchronized (this) {
            UserInfoStruct temp = mUserInfo;
            mUserInfo = null;
            return temp;
        }
    }

    public void setNews(NewsStruct news) {
        synchronized (this) {
            if (mNewsInfo != null) {
                Debugger.logError("previous data has not been fetched yet");
                return;
            }
            mNewsInfo = news;
        }
    }

    public NewsStruct getNews() {
        synchronized (this) {
            NewsStruct temp = mNewsInfo;
            mNewsInfo = null;
            return temp;
        }
    }
    
    public boolean sendNotice(TCPNotice notice) {
        if (mSocketService != null) {
            return mSocketService.send(notice);
        } else {
            return false;
        }
    }

    /**
     * 试图从本地获取用户的详细信息，最坏的情况是本地内存（好友，临时用户）以及数据库都查找
     * 不到，则返回一个非空的信息
     */
    public UserInfoStruct getUserInfo(String userId) {
        UserInfoStruct userInfo = null;
        // 首先从好友列表获取
        userInfo = mContactListObservable.getUser(userId);
        if (userInfo == null) {
            // 从临时用户信息列表获取
            userInfo = getTempUser(userId);
            if (userInfo == null) {
                // 从数据库获取
                userInfo = DatabaseUtil.getUser(this, userId);
                if (userInfo == null) {
                    // 都获取失败，new一个对象，保证app不crash
                    userInfo = new UserInfoStruct();
                    userInfo.displayName = "未知";
                    userInfo.img = ""; // img 会作为HTTP请求的参数，不能为null
                }
            }
        }
        return userInfo;
    }
    
    private UserInfoStruct getTempUser(String userId) {
        UserInfoStruct userInfo = null;
        for (UserInfoStruct user : mTempUserList) {
            if (user.userId.equals(userId)) {
                userInfo = user;
                break;
            }
        }
        return userInfo;
    }
    
    public void addTempContact(UserInfoStruct user) {
        if (getTempUser(user.userId) != null) {
            return; // 该用户已经存在于好友列表中
        }
        mTempUserList.add(user);
    }
    
    /**
     * 与服务器建立Socket 连接，用户登录成功后调用 
     */
    public void connectSocket() {
        new Thread(new Runnable() {
            public void run() {
                mSocketService.connect();
            }
        }).start();
    }
    
    public void disconnectSocket(){
        mSocketService.disconnect();
    }
}
