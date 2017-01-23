package com.tyr.activities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.tyr.data.MyApplication;
import com.tyr.data.SchoolConnectPreferences;
import com.tyr.ui.R;
import com.tyr.ui.view.ConfirmAlertDialog;
import com.tyr.ui.view.ProgressAlertDialog;
import com.tyr.ui.view.SearchBar;
import com.tyr.ui.view.SingleAlertDialog;
import com.tyr.ui.view.TopBar;
import com.tyr.util.NotificationUtil;

public class BaseActivity extends FragmentActivity {
    private static List<Activity> mActivityList;
    private TopBar mTopBar;
    public SearchBar mSearchBar;
    public ProgressAlertDialog mProgressDialog;
    public Context mContext;
    private static BaseActivity top = null;
    // Side menu
    private DrawerLayout mDrawerLayout = null;
    private ListView mLeftDrawerList = null;
    private ListView mRightDrawerList = null;
    public MyApplication mApplication;
    public static final String EXTRA_USER_ID = "userId";
    public static final String EXTRA_SEARCH_FLAG = "search_flag";
    public static final String EXTRA_INDEX_TYPE = "index_type"; // 主页面的类型
    public static final String EXTRA_SUB_INDEX_TYPE = "sub"; // 子页面的类型
    public static final String EXTRA_INDEX_TYPE_PRE = "index_type_prev"; // 前一个类型
    public static final String EXTRA_NEW_MSG_CNT = "new_messages_count";
    public static final String EXTRA_NEWS_ID = "newsId";
    public static final String EXTRA_INCOMING_MSG = "incoming_msg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext = this;
        top = this;
        if (mActivityList == null) {
            mActivityList = new Stack<Activity>();
        }
        mActivityList.add(this);
        mProgressDialog = new ProgressAlertDialog(mContext);
        mApplication = (MyApplication) getApplicationContext();
        // 隐藏系统自身的action bar，使用Customized one
        getActionBar().hide();

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        mProgressDialog.dismiss();
        if (this != null) {
            mActivityList.remove(this);
            super.onDestroy();
        }
        // unregisterReceiver(broadcast);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void initSideMenu() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mLeftDrawerList = (ListView) findViewById(R.id.left_drawer);
        SimpleAdapter adapterLeft = new SimpleAdapter(this, inflateLeftMenu(),
                R.layout.side_menu_item, new String[] { "title", "img" }, new int[] { R.id.title,
                        R.id.img });
        mLeftDrawerList.setAdapter(adapterLeft);

        mLeftDrawerList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                // Highlight the selected item, update the title, and close the drawer
                mLeftDrawerList.setItemChecked(position, true);
                // setTitle(mPlanetTitles[position]);
                mDrawerLayout.closeDrawer(mLeftDrawerList);
                switch (position) {
                case 0:
                    finish(); // 需要消除当前的页面
                    Intent intent = new Intent(mContext, MainActivity.class);
                    intent.putExtra(BaseActivity.EXTRA_INDEX_TYPE, MainActivity.INDEX_IM);
                    intent.putExtra(BaseActivity.EXTRA_SUB_INDEX_TYPE, MainActivity.SUB_INDEX_COMMON_CONTACTS);
                    startActivity(intent);
                    break;
                case 1:
                    showFunctionUnfinished();
                    break;
                case 2:
                    showFunctionUnfinished();
                    break;
                case 3:
                    intent = new Intent(mContext, SearchActivity.class);
                    startActivity(intent);
                    break;
                default:
                    break;
                }
            }
        });

        mRightDrawerList = (ListView) findViewById(R.id.right_drawer);
        SimpleAdapter adapterRight = new SimpleAdapter(this, inflateRightMenu(),
                R.layout.side_menu_item, new String[] { "title", "img" }, new int[] { R.id.title,
                        R.id.img });
        mRightDrawerList.setAdapter(adapterRight);

        mRightDrawerList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                mRightDrawerList.setItemChecked(position, true);
                mDrawerLayout.closeDrawer(mRightDrawerList);
                switch (position) {
                case 0:
                    showFunctionUnfinished();
                    break;
                case 1:
                    // jump to UserDetailActivity
                    Intent intent = new Intent(mContext, UserDetailActivity.class);
                    intent.putExtra(EXTRA_USER_ID, mApplication.myself.userId);
                    startActivity(intent);
                    break;
                case 2:
                    showLogoutDialog();
                    break;
                case 3:
                    showFunctionUnfinished();
                    break;
                default:
                    break;
                }
            }
        });
    }

    private List<Map<String, Object>> inflateLeftMenu() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("title", "常用联系人");
        map.put("img", R.drawable.ic_launcher);
        list.add(map);

        map = new HashMap<String, Object>();
        map.put("title", "工具");
        map.put("img", R.drawable.ic_launcher);
        list.add(map);

        map = new HashMap<String, Object>();
        map.put("title", "计划提醒");
        map.put("img", R.drawable.ic_launcher);
        list.add(map);

        map = new HashMap<String, Object>();
        map.put("title", "添加好友");
        map.put("img", R.drawable.ic_launcher);
        list.add(map);

        return list;
    }

    private List<Map<String, Object>> inflateRightMenu() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("title", "计划查询");
        map.put("img", R.drawable.ic_launcher);
        list.add(map);

        map = new HashMap<String, Object>();
        map.put("title", "个人信息设置"); // 注销，修改
        map.put("img", R.drawable.ic_launcher);
        list.add(map);

        map = new HashMap<String, Object>();
        map.put("title", "注销");
        map.put("img", R.drawable.ic_launcher);
        list.add(map);
        
        map = new HashMap<String, Object>();
        map.put("title", "工具");
        map.put("img", R.drawable.ic_launcher);
        list.add(map);

        return list;
    }

    public void popAllActivity() {
        if (mActivityList != null) {
            for (int i = 0; i < mActivityList.size(); i++) {
                Activity act = mActivityList.get(i);
                if (act == null) {
                    continue;
                }
                act.finish();
            }
        }
        mActivityList.clear();
    }

    private void logOut() {
        // stopService(new Intent(this, IR_UtilityService.class));
        // System.exit(0); // 结束所有正在进行的线程，AsyncTask（没有与Service绑定）
        popAllActivity();
        NotificationUtil.cancelAllNotify(mContext);
        
        // close socket connection
        mApplication.disconnectSocket();
        
        SchoolConnectPreferences.clear();
        
        Intent intent = new Intent(this, LoginActivity.class);
        // intent.putExtra(IntentTagDef.IS_LOGOUT, true);
        startActivity(intent);
        finish();
    }

    /**
     * 注销用户，会清空本地共享数据，但是会保留数据库中的聊天历史等数据 
     */
    public void showLogoutDialog() {
        final ConfirmAlertDialog dialog = new ConfirmAlertDialog(this, "确认注销当前用户?");
        dialog.showDialog(new OnClickListener() {
            public void onClick(View arg0) {
                logOut();
            }
        });
    }

    /**
     * initial when needed
     * 
     * TopBar 有三种形式 1. 主页面（MainActivity），左右按钮是侧边菜单栏按钮 2. 子页面（由主页面进入的子页面），只有一个返回菜单 3.
     * 独立页面（比如LoginActivity），归并到子页面，使用listener是否为null来区分，独立页面里 没有按钮
     */
    public void initTopBar(String title, boolean isSub, OnClickListener listener) {
        if (mTopBar == null) {
            mTopBar = (TopBar) this.findViewById(R.id.top_bar);
        }
        mTopBar.setTitle(title);
        if (mTopBar != null) {
            // back button 和 side menu button仅能存在一个，在子页面里面返回主页面 MainActivity是back
            // button
            if (isSub) {
                mTopBar.mLeftButton.setBackgroundResource(R.drawable.back_button_bg);
            } else {
                mTopBar.mLeftButton.setBackgroundResource(R.drawable.side_menu_button);
                mTopBar.mRightButton.setVisibility(View.VISIBLE);
                mTopBar.mRightButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View arg0) {
                        mDrawerLayout.openDrawer(Gravity.RIGHT);
                    }
                });
                listener = new OnClickListener() {
                    public void onClick(View arg0) {
                        mDrawerLayout.openDrawer(Gravity.LEFT);
                    }
                };
            }
            if (listener != null) {
                mTopBar.mLeftButton.setOnClickListener(listener);
                mTopBar.mLeftButton.setVisibility(View.VISIBLE);
            }
        }
    }

    // initial when needed
    public void initSearchBar(String hint, OnClickListener listener1, OnClickListener listener2) {
        if (mSearchBar == null) {
            mSearchBar = (SearchBar) this.findViewById(R.id.search_bar);
        }

        if (mSearchBar != null) {
            mSearchBar.setHint(hint);
            if (listener1 != null) {
                mSearchBar.setBackOnClickListener(listener1);
                mSearchBar.setBackVisibility(View.VISIBLE);
            }
            if (listener2 != null) {
                mSearchBar.setSearchOnClickListener(listener2);
            }
        }
    }

    public void showFunctionUnfinished() {
        new SingleAlertDialog(mContext).showDialog("功能尚未开放");
    }

    public static BaseActivity getTop() {
        return top;
    }
}
