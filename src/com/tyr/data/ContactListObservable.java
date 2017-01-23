package com.tyr.data;

import java.util.ArrayList;
import java.util.Observable;

import com.tyr.content.UserInfoStruct;

public class ContactListObservable extends Observable {
    public static int UPDATE_TYPE_ADD = 0;
    public static int UPDATE_TYPE_REMOVE = 1;
    public static int UPDATE_TYPE_SET = 2;
    public static int UPDATE_TYPE_UPDATE = 3;
    // TODO user检索的效率问题，考虑修改数据结构为HashMap<userid, userinfo>
    private ArrayList<UserInfoStruct> mContactList = new ArrayList<UserInfoStruct>();

    public void setContactList(ArrayList<UserInfoStruct> contactList) {
        mContactList = contactList;
        // 数据发生变化，通知注册的观察者
        setChanged();
        notifyObservers(UPDATE_TYPE_SET);
    }

    /**
     * 一个从内存中快速查询到userinfo的方法， 而不需要从数据库中获取
     */
    public UserInfoStruct getUser(String userId) {
        UserInfoStruct userInfoStruct = null;
        for (UserInfoStruct userInfo : mContactList) {
            if (userInfo.userId.equals(userId)) {
                userInfoStruct = userInfo;
                break;
            }
        }
        return userInfoStruct;
    }
    
    public void addContact(UserInfoStruct user) {
        if (getUser(user.userId) != null) {
            return; // 该用户已经存在于好友列表中
        }
        mContactList.add(user);
        setChanged();
        notifyObservers(UPDATE_TYPE_ADD);
    }

    public ArrayList<UserInfoStruct> getContactList() {
        return mContactList;
    }

    public void deleteContact(int index) {
        mContactList.remove(index);
        setChanged();
        notifyObservers(UPDATE_TYPE_REMOVE);
    }

    public void updateContact(UserInfoStruct _user) {
        for (int i = 0; i < mContactList.size(); i++) {
            UserInfoStruct user = mContactList.get(i);
            if (user.userId.equals(_user.userId)) {
                mContactList.remove(i);
                // 把最新从服务器得到的数据用来更新内存时，因为state并未在服务器做保存，需要保留
                _user.state = user.state;
                mContactList.add(_user);
                setChanged();
                notifyObservers(UPDATE_TYPE_UPDATE);
                break;
            }
        }
    }
    
    public void setUserState(String userId, int state){
        UserInfoStruct user = getUser(userId);
        if (user != null && user.state != state) {
            user.state = state;
            setChanged();
            notifyObservers(UPDATE_TYPE_UPDATE);
        }
    }
}
