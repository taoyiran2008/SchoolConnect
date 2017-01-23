package com.tyr.content.parser;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.tyr.content.JSONKeyDef;
import com.tyr.content.UserInfoStruct;
import com.tyr.data.MyApplication;
import com.tyr.util.DatabaseUtil;

public class UserInfoParser {
    private Context mContext;
    JSONObject mRoot;
    UserInfoStruct mUserInfo = null;

    public UserInfoParser(Context context, JSONObject json) {
        mContext = context;
        mRoot = json;
        parse();
    }

    private void parse() {
        try {
            String userId = mRoot.getString(JSONKeyDef.USER_ID);
            String displayName = mRoot.getString(JSONKeyDef.DISPLAYNAME);
            String title = mRoot.getString(JSONKeyDef.USER_TITLE);
            String img = mRoot.getString(JSONKeyDef.IMG);
            String description = mRoot.getString(JSONKeyDef.DESCRIPTION);
            String signature = mRoot.getString(JSONKeyDef.SIGNATURE);
            String birthday = mRoot.getString(JSONKeyDef.BIRTHDAY);
            String phone = mRoot.getString(JSONKeyDef.PHONE);
            String group = mRoot.getString(JSONKeyDef.GROUP);
            int sex = mRoot.getInt(JSONKeyDef.SEX);
            int usedAlot = mRoot.getInt(JSONKeyDef.USED_A_LOT);
            String remarkName = mRoot.getString(JSONKeyDef.REMARK_NAME);

            // 用户信息的获取可能是在用户登录前，也就是myself为空的时候
            String account = "unknown";
            UserInfoStruct myself = ((MyApplication)mContext.getApplicationContext()).myself;
            if (myself != null) {
                account = myself.account;
            }
            // type 在远程服务器没有储存，在获取到用户信息的时间点进行处理，这里设为
            // 默认值
            mUserInfo = new UserInfoStruct(account, userId, displayName, title, img,
                    description, signature, birthday, sex, phone, group, usedAlot, UserInfoStruct.TYPE_FRIEND, remarkName);
            
            // 每次通过HTTP 获取解析出数据后，都做一次数据库插入或者更新，这是parser的通用动作，
            // 其他的修正操作外面做（比如是获取的自己的用户信息，还是临时用户信息），Parser只负责
            // 解析出数据，简单做保存，更新isMyself isTemp flag是调用元的工作
            if (myself != null) {
                DatabaseUtil.addUpdateUser(mContext, mUserInfo);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public UserInfoStruct getUserInfo(){
        return mUserInfo;
    }
}
