package com.tyr.util;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.tyr.asynctask.ActionType;
import com.tyr.asynctask.AsyncHttpListener;
import com.tyr.asynctask.AsyncHttpTask;
import com.tyr.content.JSONKeyDef;
import com.tyr.content.UserInfoStruct;

/**
 * 封装HTTP 请求的Helper类，一些接口比如getUserInfo是经常会被调用到的
 */
public class HttpUtil {
    
    /**
     * 用户登录验证 
     */
    public static AsyncHttpTask login(Context context, AsyncHttpListener listener, String deviceToken, 
            String userId, String password, double version){
        AsyncHttpTask task = new AsyncHttpTask(context);
        task.setTaskListener(listener);
        JSONObject param = new JSONObject();
        try {
            param.put(JSONKeyDef.DEVICE_TOKEN, deviceToken);
            param.put(JSONKeyDef.USER_ID, userId);
            param.put(JSONKeyDef.PASSWORD, password);
            param.put(JSONKeyDef.VERSION, version);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        task.execute(String.valueOf(ActionType.LOGIN), param.toString());
        return task;
    }
    
    /**
     * 根据userId 获取用户信息
     * 
     * @return task 用于返回一个task句柄，用户可以对其进行cancel
     */
    public static AsyncHttpTask getUserInfo(Context context, AsyncHttpListener listener, String userId) {
        AsyncHttpTask task = new AsyncHttpTask(context);
        task.setTaskListener(listener);
        JSONObject param = new JSONObject();
        try {
            param.put(JSONKeyDef.USER_ID, userId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        task.execute(String.valueOf(ActionType.GET_USER_INFO), param.toString());
        return task;
    }
    
    /**
     * 添加好友
     */
    public static AsyncHttpTask addFriend(Context context, AsyncHttpListener listener, String userId, String targetId) {
        AsyncHttpTask task = new AsyncHttpTask(context);
        task.setTaskListener(listener);
        JSONObject param = new JSONObject();
        try {
            param.put(JSONKeyDef.USER_ID, userId);
            param.put(JSONKeyDef.TARGET_ID, targetId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        task.execute(String.valueOf(ActionType.ADD_FRIEND), param.toString());
        return task;
    }
    
    /**
     * 根据传入的displayName或者userId 进行模糊查询
     */
    public static AsyncHttpTask searchUsers(Context context, AsyncHttpListener listener, String userName){
        // get contact list via HTTP request
        AsyncHttpTask task = new AsyncHttpTask(context);
        task.setTaskListener(listener);
        JSONObject param = new JSONObject();
        try {
            param.put(JSONKeyDef.USER_NAME, userName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        task.execute(String.valueOf(ActionType.SEARCH_USER), param.toString());
        return task;
    }
    
    /**
     * 获取好友名单
     */
    public static AsyncHttpTask getContactList(Context context, AsyncHttpListener listener, String userId){
        // get contact list via HTTP request
        AsyncHttpTask task = new AsyncHttpTask(context);
        task.setTaskListener(listener);
        JSONObject param = new JSONObject();
        try {
            param.put(JSONKeyDef.USER_ID, userId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        task.execute(String.valueOf(ActionType.GET_CONTACT_LIST), param.toString());
        return task;
    }
    
    /**
     * 添加常用联系人，修改远程服务器的关系表
     */
    public static AsyncHttpTask updateUserState(Context context, AsyncHttpListener listener,
            String userId, String targetId, boolean flag){
        AsyncHttpTask task = new AsyncHttpTask(context);
        task.setTaskListener(listener);
        JSONObject param = new JSONObject();
        try {
            param.put(JSONKeyDef.USER_ID, userId);
            param.put(JSONKeyDef.TARGET_ID, targetId);
            param.put(JSONKeyDef.UPDATE_USER_FLAG, flag);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        task.execute(String.valueOf(ActionType.UPDATE_USER_STATE_USE_A_LOG), param.toString());
        return task;
    }
    
    /**
     * 保存用户信息
     */
    public static AsyncHttpTask saveUserInfo(Context context, AsyncHttpListener listener, UserInfoStruct userInfo){
        AsyncHttpTask task = new AsyncHttpTask(context);
        task.setTaskListener(listener);
        JSONObject param = new JSONObject();
        try {
            param.put(JSONKeyDef.USER_ID, userInfo.userId);
            param.put(JSONKeyDef.SIGNATURE, userInfo.description);
            param.put(JSONKeyDef.DESCRIPTION, userInfo.signature);
            // 图片上传后返回一个地址，需要在这里把地址关联到server的数据库里面
            param.put(JSONKeyDef.IMG, userInfo.img);
            param.put(JSONKeyDef.USED_A_LOT, userInfo.usedAlot);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        task.execute(String.valueOf(ActionType.MODIFY_USER), param.toString());
        return task;
    }
    
    /**
     * 保存好友备注信息和分组
     */
    public static AsyncHttpTask saveFriendInfo(Context context, AsyncHttpListener listener,
            String userId, UserInfoStruct userInfo){
        AsyncHttpTask task = new AsyncHttpTask(context);
        task.setTaskListener(listener);
        JSONObject param = new JSONObject();
        try {
            param.put(JSONKeyDef.USER_ID, userId);
            param.put(JSONKeyDef.TARGET_ID, userInfo.userId);
            param.put(JSONKeyDef.GROUP, userInfo.group);
            param.put(JSONKeyDef.REMARK_NAME, userInfo.remarkName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        task.execute(String.valueOf(ActionType.MODIFY_FRIEND), param.toString());
        return task;
    }
    
    /**
     * 上传用户头像
     */
    public static AsyncHttpTask uploadImage(Context context, AsyncHttpListener listener, String filePath){
        AsyncHttpTask task = new AsyncHttpTask(context);
        task.setTaskListener(listener);
        task.execute(String.valueOf(ActionType.UPLOAD_IMAGE), filePath);
        return task;
    }
    
    /**
     * 从服务器获取最新新闻推送的条数
     */
    public static AsyncHttpTask pullNewData(Context context, AsyncHttpListener listener, String refreshTime){
        AsyncHttpTask task = new AsyncHttpTask(context);
        task.setTaskListener(listener);
        JSONObject param = new JSONObject();
        try {
            param.put(JSONKeyDef.REFRESH_TIME, refreshTime);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        task.execute(String.valueOf(ActionType.GET_NEW_MSG_COUNT), param.toString());
        return task;
    }
    
    /**
     * 适用于特定的user 对应于一组特定的新闻，这样便可以设置其isRead的Flag，不适用于这里
     * 
     * @deprecated
     */
    public void sendReadMessagesOk(Context context, AsyncHttpListener listener, String userId, 
            int pageNumber, int pageCount) {
        // 删除通知栏图标
        NotificationUtil.canceMessageNotify(context, NotificationUtil.NEW_MSG);

        // 获取news list成功后，通知服务器，使其把read flag置为true
        AsyncHttpTask task = new AsyncHttpTask(context);
        // mTask.setTaskListener(this);
        JSONObject param = new JSONObject();
        try {
            param.put(JSONKeyDef.USER_ID, userId);
            param.put(JSONKeyDef.NEWS_PAGE_NUMBER, pageNumber);
            param.put(JSONKeyDef.NEWS_PAGE_COUNT, pageCount);
            param.put(JSONKeyDef.NEWS_TYPE, 1); // 新闻类型
        } catch (JSONException e) {
            e.printStackTrace();
        }
        task.execute(String.valueOf(ActionType.SEND_READ_MSG_OK), param.toString());
    }
    
    /**
     * 获取新闻详情 
     */
    public static AsyncHttpTask getNewsInfo(Context context, AsyncHttpListener listener, String newsId){
        AsyncHttpTask task = new AsyncHttpTask(context);
        task.setTaskListener(listener);
        JSONObject param = new JSONObject();
        try {
            param.put(JSONKeyDef.USER_ID, newsId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        task.execute(String.valueOf(ActionType.GET_NEWS_DETAIL), param.toString());
        return task;
    }
    
    /**
     * 分页获取新闻列表
     */
    public static AsyncHttpTask getNewsList(Context context, AsyncHttpListener listener,
            int pageNumber, int pageCount){
        AsyncHttpTask task = new AsyncHttpTask(context);
        task.setTaskListener(listener);
        JSONObject param = new JSONObject();
        try {
            param.put(JSONKeyDef.NEWS_PAGE_NUMBER, pageNumber);
            param.put(JSONKeyDef.NEWS_PAGE_COUNT, pageCount);
            param.put(JSONKeyDef.NEWS_TYPE, 1); // 新闻类型
        } catch (JSONException e) {
            e.printStackTrace();
        }

        task.execute(String.valueOf(ActionType.GET_NEWS_LIST), param.toString());
        return task;
    }
    
    /**
     * 
     */
    public static AsyncHttpTask fun(Context context, AsyncHttpListener listener, String userId){
        AsyncHttpTask task = new AsyncHttpTask(context);
        task.setTaskListener(listener);
        JSONObject param = new JSONObject();
        try {
            param.put(JSONKeyDef.USER_NAME, userId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        task.execute(String.valueOf(ActionType.SEARCH_USER), param.toString());
        return task;
    }
}
