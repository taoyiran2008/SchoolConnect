package com.tyr.content.parser;

import org.json.JSONException;
import org.json.JSONObject;

import com.tyr.content.JSONKeyDef;
import com.tyr.content.LoginInfoStruct;

import android.content.Context;

public class LoginInfoParser {
    private Context mContext;
    JSONObject mRoot;
    LoginInfoStruct mLoginInfo = null;

    public LoginInfoParser(Context context, JSONObject json) {
        mContext = context;
        mRoot = json;
        parse();
    }

    private void parse() {
        try {
            String deviceToken = mRoot.getString(JSONKeyDef.DEVICE_TOKEN);
            int hasUpdate = mRoot.getInt(JSONKeyDef.HASUPDATE);
            String url = mRoot.getString(JSONKeyDef.UPDATE_URL);
            mLoginInfo = new LoginInfoStruct(deviceToken, url, hasUpdate);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public LoginInfoStruct getLoginInfo(){
        return mLoginInfo;
    }
}
