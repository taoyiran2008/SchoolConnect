package com.tyr.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tyr.asynctask.ActionType;
import com.tyr.asynctask.AsyncHttpListener;
import com.tyr.asynctask.AsyncHttpTask;
import com.tyr.asynctask.AsyncImageloader;
import com.tyr.asynctask.HttpResult;
import com.tyr.asynctask.ImageCallback;
import com.tyr.content.UserInfoStruct;
import com.tyr.ui.R;
import com.tyr.ui.view.MyToast;
import com.tyr.ui.view.SingleAlertDialog;
import com.tyr.util.BitmapUtil;
import com.tyr.util.DatabaseUtil;
import com.tyr.util.Debugger;
import com.tyr.util.HttpUtil;

public class UserEditActivity extends BaseActivity implements AsyncHttpListener, OnClickListener {

    private static final int REQUEST_PICK_IMAGE = 1;
    private static final int REQUEST_SHOT_IMAGE = 2;

    private Button mSaveBtn;
    private Button mCancelBtn;
    private ImageView mHeadImage;
    private TextView mDisplayNameText;
    private TextView mRemarkNameText;
    private TextView mSexText;
    private TextView mTitleText;
    private TextView mDescriptionText;
    private TextView mSignatureText;
    private TextView mBirthday;
    private UserInfoStruct mUserInfo;
    private boolean isChanged = false;
    private Bitmap mBitmap;
    private boolean isMyself = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_edit);
        initData();
        initView();
    }

    private void initData() {
        // 从全局数据中去的上个画面存入的内容
        mUserInfo = mApplication.getUser();

        // 是否是当前用户自己
        if (mApplication.myself.userId.equals(mUserInfo.userId)) {
            isMyself = true;
        }
    }

    private void initView() {
        initTopBar("编辑用户", true, new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                finish();
            }
        });
        mSaveBtn = (Button) findViewById(R.id.btn_save);
        mCancelBtn = (Button) findViewById(R.id.btn_cancel);
        mHeadImage = (ImageView) findViewById(R.id.img_head);
        mDisplayNameText = (TextView) findViewById(R.id.text_name);
        mRemarkNameText = (TextView) findViewById(R.id.text_remark_name);
        mSexText = (TextView) findViewById(R.id.text_sex);
        mTitleText = (TextView) findViewById(R.id.text_title);
        mDescriptionText = (TextView) findViewById(R.id.text_description);
        mSignatureText = (TextView) findViewById(R.id.text_signature);
        mBirthday = (TextView) findViewById(R.id.text_birthday);

        mSaveBtn.setOnClickListener(this);
        mCancelBtn.setOnClickListener(this);

        if (!isMyself) { // 只能修改好友的分组和备注姓名
            mDisplayNameText.setEnabled(false);
            mSexText.setEnabled(false);
            mTitleText.setEnabled(false);
            mDescriptionText.setEnabled(false);
            mSignatureText.setEnabled(false);
            mBirthday.setEnabled(false);
        } else {
            // 自己的信息界面不显示备注姓名
            LinearLayout remarkNameLayout = (LinearLayout) findViewById(R.id.remark_name_layout);
            remarkNameLayout.setVisibility(View.GONE);
            mRemarkNameText.setVisibility(View.GONE);
            registerForContextMenu(mHeadImage);
        }

        inflateView();
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
        String sex = (mUserInfo.sex == 0) ? "女" : "男";
        mSexText.setText(sex);
        mDisplayNameText.setText(mUserInfo.displayName);
        mRemarkNameText.setText(mUserInfo.remarkName);
        mTitleText.setText(mUserInfo.title);
        mDescriptionText.setText(mUserInfo.description);
        mSignatureText.setText(mUserInfo.signature);
        mBirthday.setText(mUserInfo.birthday);
    }

    private void save() {
        if (isMyself) {
            mUserInfo.description = mDescriptionText.getText().toString();
            mUserInfo.signature = mSignatureText.getText().toString();
            final AsyncHttpTask task = HttpUtil.saveUserInfo(mContext, this, mUserInfo);
           
            mProgressDialog.show("正在修改用户信息 ...", new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mProgressDialog.dismiss();
                    task.cancel(true);
                }
            });
        } else {
            // mUserInfo.group = mGroupText.getText().toString();
            mUserInfo.remarkName = mRemarkNameText.getText().toString();
            final AsyncHttpTask task = HttpUtil.saveFriendInfo(mContext, this,
                    mApplication.myself.account, mUserInfo);
           
            mProgressDialog.show("正在修改好友信息 ...", new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mProgressDialog.dismiss();
                    task.cancel(true);
                }
            });
        }
    }

    @Override
    public void onPost(HttpResult response) {
        if (response != null) {
            mProgressDialog.dismiss();
            String data = response.body;
                if (response.responseCode == HttpResult.RESPONSE_OK) {
                    if (response.requestCode == ActionType.MODIFY_USER
                            || response.requestCode == ActionType.MODIFY_FRIEND) {
                        mProgressDialog.dismiss();
                        MyToast.getInstance(mContext).display("修改成功");
                        onUserUpdated();
                        finish();
                    } else if (response.requestCode == ActionType.UPLOAD_IMAGE) {
                        String imgURL = mUserInfo.account + BitmapUtil.EXT;
//                        try {
//                            // 获取图片在服务器的保存路径
//                            imgURL = new JSONObject(data).getString(JSONKeyDef.IMG);
//                        } catch (JSONException e) {
//                            e.printStackTrace();
//                        }
                        MyToast.getInstance(mContext).display("图片上传成功");
                        mHeadImage.setImageBitmap(mBitmap);
                        // 更新本地数据库
                        mUserInfo.img = imgURL;
                        isChanged = true;
                    }
                } else if (response.responseCode == HttpResult.RESPONSE_NO_INTERNET) {
                    new SingleAlertDialog(mContext).showDialog("网络异常\n" + data);
                } else {
                    new SingleAlertDialog(mContext).showDialog("未知的错误\n" + data);
                }
        }
    }

    /**
     * 表示数据更改成功，或者是信息修改成功，或者是图片上传成功，需要注意的是即便是图片上传
     * 成功也需要save 更新图片路径
     */
    private void onUserUpdated() {
        setResult(Activity.RESULT_OK);
        mApplication.setUser(mUserInfo);
        DatabaseUtil.updateUser(mContext, mUserInfo);
    }


    @Override
    public void onCancel() {
        // TODO Auto-generated method stub

    }

    public void onTimeout() {
        mProgressDialog.dismiss();
        Debugger.logDebug("http 请求超时");
        MyToast.getInstance(mContext).display("http 请求超时");
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
        case R.id.btn_save:
            save();
            break;
        case R.id.btn_cancel:
            if (isChanged) {
                onUserUpdated();
            } else {
                setResult(RESULT_CANCELED);
            }
            finish();
            break;
        default:
            break;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        // 长按2秒出现上下文菜单，可以使用PopupWindow代替更灵活
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.select_pic_menu, menu); // menu.add(item)
        menu.setHeaderTitle("请选择图片上传");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_pick:
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, REQUEST_PICK_IMAGE);
            return true;
        case R.id.menu_shot:
            intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, REQUEST_SHOT_IMAGE);
            return true;
        default:
            return super.onContextItemSelected(item);// 事件继续往下传递
        }
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
        // TODO Auto-generated method stub
        super.onContextMenuClosed(menu);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_PICK_IMAGE || requestCode == REQUEST_SHOT_IMAGE) {
            Bitmap image = null;
            if (data != null) {
                Uri mImageCaptureUri = data.getData();
                if (mImageCaptureUri != null) {
                    try {
                        // 这个方法是根据Uri获取Bitmap图片的静态方法
                        image = MediaStore.Images.Media.getBitmap(this.getContentResolver(),
                                mImageCaptureUri);
                    } catch (Exception e) {
                    }
                } else {
                    Bundle extras = data.getExtras();
                    if (extras != null) {
                        // 这里是有些拍照后的图片是直接存放到Bundle中的所以我们可以从这里面获取Bitmap图片
                        image = extras.getParcelable("data");
                    }
                }
            }
            if (image != null) {
                mBitmap = image;
                String fileName = mUserInfo.account + BitmapUtil.EXT;
                // 压缩图片
                Bitmap compressedImage = BitmapUtil.getResizedBitmap(image, AsyncImageloader.IMAGE_TYPE_THUMB);
                BitmapUtil.cacheImage(fileName, compressedImage,  AsyncImageloader.IMAGE_TYPE_THUMB);
                // 把压缩后的小图传到服务器，而不是直接传送原图
                uploadImage(BitmapUtil.getImagePath(fileName));
            }
        }
    }

    private void uploadImage(String filePath) {
        final AsyncHttpTask task = HttpUtil.uploadImage(mContext, this, filePath);
        mProgressDialog.show("上传图片中 ...", new OnClickListener() {
            @Override
            public void onClick(View v) {
                mProgressDialog.dismiss();
                task.cancel(true);
            }
        });
    }
}
