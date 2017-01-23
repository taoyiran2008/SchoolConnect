package com.tyr.ui.fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.tyr.activities.UserDetailActivity;
import com.tyr.asynctask.ActionType;
import com.tyr.asynctask.AsyncHttpListener;
import com.tyr.asynctask.AsyncHttpTask;
import com.tyr.asynctask.AsyncImageloader;
import com.tyr.asynctask.HttpResult;
import com.tyr.asynctask.ImageCallback;
import com.tyr.content.UserInfoStruct;
import com.tyr.content.parser.ContactListParser;
import com.tyr.data.ContactListObservable;
import com.tyr.ui.R;
import com.tyr.ui.view.MyToast;
import com.tyr.ui.view.RefreshableView;
import com.tyr.ui.view.RefreshableView.PullToRefreshListener;
import com.tyr.ui.view.SingleAlertDialog;
import com.tyr.util.BitmapUtil;
import com.tyr.util.CommonUtil;
import com.tyr.util.DatabaseUtil;
import com.tyr.util.Debugger;
import com.tyr.util.HttpUtil;

public class IMContactFragment extends BaseFragment implements AsyncHttpListener {
    private ExpandableListView mExpandListView;
    private RefreshableView mRefreshableView;
    private TextView mNoResultText;
    private ContactListAdapter mAdapter;
    private PopupWindow mPopupWindow;
    private Handler mHandler = new Handler();

    // 必须在setAdapter之前赋值，不然NullPointer
    ArrayList<String> mGroupList = new ArrayList<String>();
    HashMap<String, ArrayList<UserInfoStruct>> mChildMap = new HashMap<String, ArrayList<UserInfoStruct>>();

    // Adapter 不是直接与mApplication.mContactList绑定的，而是在这个基础上构造的，数据改变
    // 后需要通知他从新构造填充数据
    private Observer mObserver = new Observer() {
        @Override
        public void update(Observable observable, Object obj) {
            int type = Integer.valueOf(obj.toString()).intValue();
            // int type2 = Integer.parseInt(obj.toString());
            if (type == ContactListObservable.UPDATE_TYPE_ADD
                    || type == ContactListObservable.UPDATE_TYPE_REMOVE
                    || type == ContactListObservable.UPDATE_TYPE_SET
                    || type == ContactListObservable.UPDATE_TYPE_UPDATE) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        inflateListView();
                        mAdapter.notifyDataSetChanged();
                        updateView();
                    }
                });
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 注销观察者
        mApplication.mContactListObservable.deleteObserver(mObserver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_im_contact, container, false);
        initData();
        initView(view);
        return view;
    }

    private void initData() {
        // 如果本地已经保存有数据，就不需要每次都从网络加载了
        if (mApplication.mContactListObservable.getContactList() == null
                || mApplication.mContactListObservable.getContactList().size() <= 0) {
            getContactListViaHttp();
        }
        // 注册观察者
        mApplication.mContactListObservable.addObserver(mObserver);
    }

    // populate data
    private void inflateListView() {
        mGroupList.clear();
        mChildMap.clear();
        int len = mApplication.mContactListObservable.getContactList().size();
        for (int i = 0; i < len; i++) {
            UserInfoStruct user = mApplication.mContactListObservable.getContactList().get(i);
            String group = user.group;
            if (!mGroupList.contains(group)) {
                mGroupList.add(group);
            }
            if (mChildMap.get(group) == null) {
                mChildMap.put(group, new ArrayList<UserInfoStruct>());
                mChildMap.get(group).add(user);
            } else {
                mChildMap.get(group).add(user);
            }
            // 按照离线状态和姓名排序
            Collections.sort(mChildMap.get(group));
        }
    }

    private void getContactListViaHttp() {
        // get contact list via HTTP request
        final AsyncHttpTask task = HttpUtil.getContactList(mContext, this, mApplication.myself.userId);
        
        mProgressDialog.show("更新好友列表中 ...", new OnClickListener() {
            @Override
            public void onClick(View v) {
                mProgressDialog.dismiss();
                task.cancel(true);
            }
        });
    }

    private void initView(View view) {
        mExpandListView = (ExpandableListView) view.findViewById(R.id.refresh_list_view);
        mNoResultText = (TextView) view.findViewById(R.id.text_no_content);
        mRefreshableView = (RefreshableView) view.findViewById(R.id.refreshable_view_contact);

        mRefreshableView.setOnRefreshListener(new PullToRefreshListener() {
            @Override
            public void onRefresh() {
                getContactListViaHttp();
            }
        }, R.id.refreshable_view_contact);

        // initialize listview
        mAdapter = new ContactListAdapter(mContext);
        mExpandListView.setAdapter(mAdapter);
        mExpandListView.setOnChildClickListener(new OnChildClickListener() {
            public boolean onChildClick(ExpandableListView parent, View view, int groupPosition,
                    int childPosition, long id) {
                // jump to UserDetailActivity
                Intent intent = new Intent(mContext, UserDetailActivity.class);
                intent.putExtra(EXTRA_USER_ID,
                        mChildMap.get(mGroupList.get(groupPosition)).get(childPosition).userId);
                startActivity(intent);
                return false;
            }
        });
        mExpandListView.setOnItemLongClickListener(new OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                long packedPosition = mExpandListView.getExpandableListPosition(position);
                int type = ExpandableListView.getPackedPositionType(packedPosition);
                if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                    int groupPos = ExpandableListView.getPackedPositionGroup(packedPosition);
                    int childPos = ExpandableListView.getPackedPositionChild(packedPosition);
                    if (mRefreshableView.getCurrentStatus() != RefreshableView.STATUS_RELEASE_TO_REFRESH
                            && mRefreshableView.getCurrentStatus() != RefreshableView.STATUS_PULL_TO_REFRESH) {
                        showPopupMenu(view, childPos, groupPos);
                    }
                } else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                    int groupPos = ExpandableListView.getPackedPositionGroup(packedPosition);
                }
                return true;
            }
        });
        // mExpandListView.setOnGroupCollapseListener(onGroupCollapseListener);
        // mExpandListView.setOnGroupExpandListener(onGroupExpandListener);
        // mExpandListView.setGroupIndicator(null);
        inflateListView();
        updateView();
    }

    @Override
    public void onResume() {
        super.onResume();
//        // update list view
//        initData();
//        inflateListView();
    }

    private void updateView() {
        if (mApplication.mContactListObservable.getContactList() != null
                && mApplication.mContactListObservable.getContactList().size() > 0) {
            mExpandListView.setVisibility(View.VISIBLE);
            mNoResultText.setVisibility(View.GONE);
        } else {
            mExpandListView.setVisibility(View.GONE);
            mNoResultText.setVisibility(View.VISIBLE);
        }
    }

    class ContactListAdapter extends BaseExpandableListAdapter {
        private ViewHolder holder;
        private LayoutInflater mInflater;

        public ContactListAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        // -----------------Child----------------//
        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return mChildMap.get(mGroupList.get(groupPosition)).get(childPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return mChildMap.get(mGroupList.get(groupPosition)).size();
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent) {
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.contact_list_item, parent, false);
                holder.displayName = (TextView) convertView.findViewById(R.id.text_name);
                holder.signature = (TextView) convertView.findViewById(R.id.text_signature);
                holder.headImage = (ImageView) convertView.findViewById(R.id.img_head);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            UserInfoStruct user = (UserInfoStruct) getChild(groupPosition, childPosition);
            holder.displayName.setText(user.displayName);
            holder.signature.setText(user.signature);

            // 设置头像明暗状态
            if (user.state == UserInfoStruct.STATE_OFFLINE) {
                holder.headImage.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
            } else {
                holder.headImage.clearColorFilter();
            }
            // 给ImageView设置一个Tag，保证异步加载图片时不会乱序
            final String tag = "tag" + childPosition + "-" + groupPosition;
            holder.headImage.setTag(tag);
            // 图片加载会阻塞线程，异步处理
            Bitmap bitmap = mApplication.mImageloader.loadImage(user.img,
                    AsyncImageloader.IMAGE_TYPE_THUMB, new Handler(), new ImageCallback() {
                        public void onImageLoaded(Bitmap bitmap) {
                            ImageView imageView = (ImageView) mExpandListView.findViewWithTag(tag);
                            imageView.setImageBitmap(bitmap);
                            // 可能同时加载了相同的url，后进队列的下载进程会因为已经开始下载而不另起下载
                            // 线程，需要通知其他没有注册到callback的item，更新image
                            mAdapter.notifyDataSetChanged();
                        }
                    });
            // 同步返回
            if (bitmap != null) {
                holder.headImage.setImageBitmap(bitmap);
            } else {
                holder.headImage.setImageResource(R.drawable.ic_launcher);
            }
            return convertView;
        }

        // ----------------Group----------------//
        @Override
        public Object getGroup(int groupPosition) {
            return mGroupList.get(groupPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public int getGroupCount() {
            return mGroupList.size();
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                ViewGroup parent) {
            return getGenericView(mGroupList.get(groupPosition));
        }

        // 创建组视图
        public TextView getGenericView(String s) {
            // Layout parameters for the ExpandableListView
            int heightInDp = BitmapUtil.pxToDp(mContext, 30);
            AbsListView.LayoutParams lp = new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT,
                    heightInDp);

            TextView text = new TextView(mContext);
            text.setLayoutParams(lp);
            // Center the text vertically
            text.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            // Set the text starting position
            text.setPadding(150, 10, 0, 10);
            text.setTextSize(18);

            text.setText(s);
            return text;
        }

        @Override
        public boolean hasStableIds() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            // TODO Auto-generated method stub
            return true;
        }
    }

    class ViewHolder {
        public ImageView headImage;
        public TextView displayName;
        public TextView signature;
    }

    public void onPost(HttpResult response) {
        if (response != null) {
            mProgressDialog.dismiss();
            String data = response.body;
            if (response.requestCode == ActionType.GET_CONTACT_LIST) {
                if (response.responseCode == HttpResult.RESPONSE_OK) {
                    // parse data
                    ContactListParser parser = null;
                    try {
                        parser = new ContactListParser(mContext, new JSONObject(data));
                        mApplication.mContactListObservable.setContactList(parser.getContactList());
                        inflateListView();
                        // update listview
                        mAdapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (response.responseCode == HttpResult.RESPONSE_NO_INTERNET) {
                    new SingleAlertDialog(mContext).showDialog("没有网络连接" + data);
                    // 加载数据库的缓存信息
                    mApplication.mContactListObservable.setContactList(DatabaseUtil
                            .getContacts(mContext));
                } else {
                    new SingleAlertDialog(mContext).showDialog("HTTP 请求异常" + data);
                }
                mRefreshableView.finishRefreshing();
            }
        }
    }

    public void onCancel() {
    }
    
    public void onTimeout() {
        mProgressDialog.dismiss();
        Debugger.logDebug("http 请求超时");
        MyToast.getInstance(mContext).display("http 请求超时");
    }

    private void showPopupMenu(View parent, int chidPos, int groupPos) {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        View popup = layoutInflater.inflate(R.layout.popup_menu_contact, null);
        TextView phone = (TextView) popup.findViewById(R.id.text_button_phone);
        TextView sms = (TextView) popup.findViewById(R.id.text_button_sms);
        final String phoneNumber = mChildMap.get(mGroupList.get(groupPos)).get(chidPos).phone;
        phone.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                CommonUtil.call(mContext, phoneNumber);
                mPopupWindow.dismiss();
            }
        });
        sms.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                CommonUtil.sendSMS(mContext, phoneNumber);
                mPopupWindow.dismiss();
            }
        });

        int widthInDp = BitmapUtil.pxToDp(mContext, 300);
        int heightInDp = BitmapUtil.pxToDp(mContext, 100);
        mPopupWindow = new PopupWindow(popup, widthInDp, heightInDp);

        mPopupWindow.setFocusable(false);
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
        int[] location = new int[2];
        parent.getLocationInWindow(location); // 获取在当前窗口内的绝对坐标
        int screenWidth = getActivity().getWindowManager().getDefaultDisplay().getWidth();
        // int width = popup.getWidth(); 获取不到，除非延迟获取
        int left = (screenWidth - widthInDp) / 2;
        int top = location[1] + parent.getMeasuredHeight();
        mPopupWindow.showAtLocation(parent, Gravity.NO_GRAVITY, left, top);
    }

}
