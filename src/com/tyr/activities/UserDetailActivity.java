package com.tyr.activities;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.tyr.asynctask.ActionType;
import com.tyr.asynctask.AsyncHttpListener;
import com.tyr.asynctask.AsyncHttpTask;
import com.tyr.asynctask.AsyncImageloader;
import com.tyr.asynctask.HttpResult;
import com.tyr.asynctask.ImageCallback;
import com.tyr.content.UserInfoStruct;
import com.tyr.content.parser.UserInfoParser;
import com.tyr.service.MySocketService;
import com.tyr.socket.FriendRequestNotice;
import com.tyr.ui.R;
import com.tyr.ui.view.InputDialog;
import com.tyr.ui.view.MyToast;
import com.tyr.ui.view.SingleAlertDialog;
import com.tyr.util.CommonUtil;
import com.tyr.util.DatabaseUtil;
import com.tyr.util.Debugger;
import com.tyr.util.HttpUtil;

/**
 * 封装了三种类似的界面，用户信息，好友信息以及Search 信息.
 * 
 */
public class UserDetailActivity extends BaseActivity implements AsyncHttpListener, OnClickListener {
    private Button mChatBtn;
    private Button mAddConventionalBtn;
    private Button mEditBtn;
    private Button mAddUser;
    private ImageView mHeadImage;
    private TextView mDisplayNameText;
    private TextView mTitleText;
    private TextView mDescriptionText;
    private TextView mSignatureText;
    private TextView mBirthday;
    private TextView mPhone;
    private TextView mGroup;
    private String mUserId;
    private boolean isMyself = false;
    private boolean isFriend = false;
    private boolean isSearch = false; // 是否从SearchActivity跳转过来
    private UserInfoStruct mUserInfo = null;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_detail);
        initData();
        initView();
    }

    private void initData() {
        Intent intent = getIntent();
        mUserId = intent.getStringExtra(EXTRA_USER_ID);
        isSearch = intent.getBooleanExtra(EXTRA_SEARCH_FLAG, false);
        // 确定该userid是自己、好友还是陌生人
        // 是否是当前用户自己
        if (mApplication.myself.userId.equals(mUserId)) {
            isMyself = true;
        }
        // 是否在好友列表里
        if (mApplication.mContactListObservable.getUser(mUserId) != null) {
            isFriend = true;
        }
        
        Debugger.logDebug("mUserId = " + mUserId);
        // 更新用户数据有两个时机：1 登陆的时候，2 进入用户详情的时候
        if (isMyself) {
            mUserInfo = mApplication.myself;
        } else {
            getUserInfo(mUserId);
        }
    }

    private void initView() {
        initTopBar("用户信息", true, new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                finish();
            }
        });
        mChatBtn = (Button) findViewById(R.id.btn_chat);
        mAddConventionalBtn = (Button) findViewById(R.id.btn_add_to_conventional);
        mEditBtn = (Button) findViewById(R.id.btn_edit);
        mAddUser = (Button) findViewById(R.id.btn_add_user);
        mHeadImage = (ImageView) findViewById(R.id.img_head);
        mDisplayNameText = (TextView) findViewById(R.id.text_name);
        mTitleText = (TextView) findViewById(R.id.text_title);
        mDescriptionText = (TextView) findViewById(R.id.text_description);
        mSignatureText = (TextView) findViewById(R.id.text_signature);
        mBirthday = (TextView) findViewById(R.id.text_birthday);
        mPhone = (TextView) findViewById(R.id.text_phone);
        mGroup = (TextView) findViewById(R.id.text_group);

        mChatBtn.setOnClickListener(this);
        mAddConventionalBtn.setOnClickListener(this);
        mEditBtn.setOnClickListener(this);
        mAddUser.setOnClickListener(this);
        if (isMyself) {
            // 不需要异步获取用户信息，直接填充画面
            inflateView();
            mChatBtn.setVisibility(View.GONE);
            mAddConventionalBtn.setVisibility(View.GONE);
            mEditBtn.setVisibility(View.VISIBLE);
            mAddUser.setVisibility(View.GONE);
        } else if (!isFriend) {
            // 不管从什么入口进入，只要不是当前的好友就可以进行添加
            mChatBtn.setVisibility(View.GONE);
            mAddConventionalBtn.setVisibility(View.GONE);
            mEditBtn.setVisibility(View.GONE);
            mAddUser.setVisibility(View.VISIBLE);
        } else {
            mChatBtn.setVisibility(View.VISIBLE);
            mAddConventionalBtn.setVisibility(View.VISIBLE);
            mEditBtn.setVisibility(View.VISIBLE);
            mAddUser.setVisibility(View.GONE);
        }
    }

    private void inflateView() {
        Bitmap bitmap = mApplication.mImageloader.loadImage(mUserInfo.img,
                AsyncImageloader.IMAGE_TYPE_THUMB, new Handler(), new ImageCallback() {
                    public void onImageLoaded(Bitmap bitmap) {
                        mHeadImage.setImageBitmap(bitmap);
                    }
                });
        // 同步返回
        if (bitmap != null) {
            mHeadImage.setImageBitmap(bitmap);
        } else {
            mHeadImage.setImageResource(R.drawable.ic_launcher);
        }
        // 小张( 男 )
        String sex = (mUserInfo.sex == 0) ? "女" : "男";
        String name = getString(R.string.name_sex, mUserInfo.displayName, sex);
        mDisplayNameText.setText(name);
        mTitleText.setText(mUserInfo.title);
        mDescriptionText.setText(mUserInfo.description);
        mSignatureText.setText(mUserInfo.signature);
        mBirthday.setText(mUserInfo.birthday);
        mPhone.setText(mUserInfo.phone);
        mGroup.setText(mUserInfo.group);

        if (mUserInfo.usedAlot == 1) {
            mAddConventionalBtn.setEnabled(false);
        } else {
            mAddConventionalBtn.setEnabled(true);
        }
    }

    private void getUserInfo(String userId) {
        final AsyncHttpTask task = HttpUtil.getUserInfo(mContext, this, userId);
        mProgressDialog.show("正在获取用户信息 ...", new OnClickListener() {
            @Override
            public void onClick(View v) {
                mProgressDialog.dismiss();
                task.cancel(true);
            }
        });
    }

    @Override
    public void onPost(HttpResult response) {
        if (response != null) {
            String data = response.body;
            mProgressDialog.dismiss();
            if (response.responseCode == HttpResult.RESPONSE_OK) {
                if (response.requestCode == ActionType.GET_USER_INFO) {
                    UserInfoParser parser = null;
                    try {
                        parser = new UserInfoParser(mContext, new JSONObject(data));
                        mUserInfo = parser.getUserInfo();
                        // 更新本地的用户信息
                        if (isMyself) {
                            mUserInfo.type = UserInfoStruct.TYPE_MYSELF;
                            mUserInfo = CommonUtil.getDummyUserInfo(mContext, MySocketService.USERID, UserInfoStruct.TYPE_MYSELF);
                        } else if (isFriend) {
                            mUserInfo.type = UserInfoStruct.TYPE_FRIEND;
                            mUserInfo = CommonUtil.getDummyUserInfo(mContext, mUserId, UserInfoStruct.TYPE_FRIEND);
                            // 更新内存
                            mApplication.mContactListObservable.updateContact(mUserInfo);
                        } else {
                            mUserInfo.type = UserInfoStruct.TYPE_TEMP;
                            mUserInfo = CommonUtil.getDummyUserInfo(mContext, MySocketService.REQUEST_FRIENDID, UserInfoStruct.TYPE_TEMP);
                            // 加入到临时用户列表中
                            mApplication.addTempContact(mUserInfo);
                        }
                        DatabaseUtil.addUpdateUser(mContext, mUserInfo);
                        
                        // 把解析出的数据呈现出来
                        if (mUserInfo != null) {
                            inflateView();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (response.requestCode == ActionType.UPDATE_USER_STATE_USE_A_LOG) {
                    MyToast.getInstance(mContext).display("修改成功");
                    // 修改本地db已经全局mContactList数据
                    mUserInfo.usedAlot = 1;
                    mApplication.mContactListObservable.updateContact(mUserInfo);
                    DatabaseUtil.updateUser(mContext, mUserInfo);
                    mAddConventionalBtn.setEnabled(false);
                }
            } else if (response.responseCode == HttpResult.RESPONSE_NO_INTERNET) {
                // new SingleAlertDialog(mContext).showDialog("网络异常\n" + data);
                // 从本地数据库读取缓存记录
                mUserInfo = mApplication.getUserInfo(mUserId);
                if (mUserInfo != null) {
                    inflateView();
                }
            } else {
                new SingleAlertDialog(mContext).showDialog("未知的错误\n" + data);
            }
        }

    }

    @Override
    public void onCancel() {
    }

    public void onTimeout() {
        mProgressDialog.dismiss();
        Debugger.logDebug("http 请求超时");
        MyToast.getInstance(mContext).display("http 请求超时");
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
        case R.id.btn_edit:
            mApplication.setUser(mUserInfo);
            Intent intent = new Intent(mContext, UserEditActivity.class);
            // 需要处理返回的内容
            startActivityForResult(intent, 0);
            break;
        case R.id.btn_chat:
            // getDummyData();
            intent = new Intent(mContext, ChatHistoryActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
            intent.putExtra(EXTRA_USER_ID, mUserId);
            startActivity(intent);
            break;
        case R.id.btn_add_user:
            final InputDialog inputDialog = new InputDialog(mContext);
            inputDialog.showDialog("请输入说明", new OnClickListener() {
                public void onClick(View arg0) {
                    if (mUserInfo == null) {
                        MyToast.getInstance(mContext).display("用户信息获取成功后才能进行好友添加");
                        return;
                    }
                    boolean sendSuccess = mApplication.sendNotice(new FriendRequestNotice(MySocketService.USERID, MySocketService.REQUEST_FRIENDID, inputDialog.getMessage()));
                    if (sendSuccess) {
                        // tacle WindowLeaked exception.
                        inputDialog.dismiss();
                        finish();
                    } else {
                        MyToast.getInstance(mContext).display("消息发送失败，请确认与socket服务器的连接状态");
                    }
                }
            });
            break;
        case R.id.btn_add_to_conventional:
            // 通知服务器所做的更改
            updateUserState();
            break;
        default:
            break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        mUserInfo = mApplication.getUser();
        inflateView();
    }

    /**
     * 通知服务器修改好友为常用联系人
     */
    private void updateUserState() {
        final AsyncHttpTask task = HttpUtil.updateUserState(mContext, this,
                mApplication.myself.userId, mUserInfo.userId, true);
        mProgressDialog.show("更新为常用联系人 ...", new OnClickListener() {
            @Override
            public void onClick(View v) {
                mProgressDialog.dismiss();
                task.cancel(true);
            }
        });
    }
}
