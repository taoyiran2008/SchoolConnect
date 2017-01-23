package com.tyr.content.parser;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.tyr.content.JSONKeyDef;
import com.tyr.content.UserInfoStruct;
import com.tyr.data.MyApplication;
import com.tyr.util.DatabaseUtil;

import android.content.Context;

public class ContactListParser {
    private Context mContext;
    JSONObject mRoot;
    ArrayList<UserInfoStruct> mContactList;
    ArrayList<String> mIdList;

    public ContactListParser(Context context, JSONObject json) {
        mContext = context;
        mRoot = json;
        mContactList = new ArrayList<UserInfoStruct>();
        mIdList = new ArrayList<String>();
        parse();
    }

    private void parse() {
        try {
            JSONArray elements = mRoot.getJSONArray(JSONKeyDef.CONTACT_LIST);
            for (int i = 0; i < elements.length(); i++) {
                JSONObject element = elements.getJSONObject(i);
                String userId = element.getString(JSONKeyDef.USER_ID);
                String displayName = element.getString(JSONKeyDef.DISPLAYNAME);
                String title = element.getString(JSONKeyDef.USER_TITLE);
                String img = element.getString(JSONKeyDef.IMG);
                String description = element.getString(JSONKeyDef.DESCRIPTION);
                String signature = element.getString(JSONKeyDef.SIGNATURE);
                String birthday = element.getString(JSONKeyDef.BIRTHDAY);
                String phone = element.getString(JSONKeyDef.PHONE);
                String group = element.getString(JSONKeyDef.GROUP);
                int sex = element.getInt(JSONKeyDef.SEX);
                int usedAlot = element.getInt(JSONKeyDef.USED_A_LOT);
                String remarkName = element.getString(JSONKeyDef.REMARK_NAME);
                
                mIdList.add(userId);

                String account = ((MyApplication) (mContext.getApplicationContext())).myself.userId;
                UserInfoStruct userInfo = new UserInfoStruct(account, userId, displayName, title, img,
                        description, signature, birthday, sex, phone, group, usedAlot, UserInfoStruct.TYPE_FRIEND, remarkName);
                mContactList.add(userInfo);
                
                // TODO 判断用户是否在本地数据库中存在，存在则update，不存在则insert
                DatabaseUtil.addUpdateUser(mContext, userInfo);
            }
            // 最后把数据库中与服务器不同步的信息删除
            DatabaseUtil.syncUsers(mContext, mIdList);
            
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<UserInfoStruct> getContactList() {
        return mContactList;
    }
}
