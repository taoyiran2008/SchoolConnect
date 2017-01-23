package com.tyr.activities;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.tyr.asynctask.ActionType;
import com.tyr.asynctask.AsyncHttpListener;
import com.tyr.asynctask.AsyncHttpTask;
import com.tyr.asynctask.AsyncImageloader;
import com.tyr.asynctask.HttpResult;
import com.tyr.asynctask.ImageCallback;
import com.tyr.content.UserInfoStruct;
import com.tyr.content.parser.ContactListParser;
import com.tyr.ui.R;
import com.tyr.ui.view.MyToast;
import com.tyr.ui.view.PageListView;
import com.tyr.ui.view.PageListView.OnLoadMoreListener;
import com.tyr.ui.view.SingleAlertDialog;
import com.tyr.util.Debugger;
import com.tyr.util.HttpUtil;

public class SearchActivity extends BaseActivity implements AsyncHttpListener,
        OnLoadMoreListener, OnItemClickListener {
    private PageListView mPageListView;
    private TextView mNoResultText;
    private ArrayList<UserInfoStruct> mContactResultList = new ArrayList<UserInfoStruct>();
    private ContactListAdapter mAdapter;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_im_search);
        initView();
    }

    private void search() {
        String userName = mSearchBar.getSearchText();
        if (userName.isEmpty()) {
            MyToast.getInstance(mContext).display("请输入检索的内容");
            return;
        }
       final AsyncHttpTask task = HttpUtil.searchUsers(mContext, this, userName);
        mProgressDialog.show("检索用户中 ...", new OnClickListener() {
            @Override
            public void onClick(View v) {
                mProgressDialog.dismiss();
                task.cancel(true);
            }
        });
    }

    private void initView() {
        mPageListView = (PageListView) findViewById(R.id.contact_list);
        mNoResultText = (TextView) findViewById(R.id.text_no_content);

        // initialize search bar
        initSearchBar("输入用户名或用户id", new OnClickListener() {
            public void onClick(View arg0) {
                finish();
            }
        }, new OnClickListener() {
            public void onClick(View arg0) {
                // search users
                search();
            }
        });

        // initialize listview
        mAdapter = new ContactListAdapter(mContext);
        mPageListView.setAdapter(mAdapter);
        // mPageListView.setOnLoadMoreListener(this);
        mPageListView.setOnItemClickListener(this);

//        updateView();
    }

    private void updateView() {
        if (mContactResultList != null && mContactResultList.size() > 0) {
            mPageListView.setVisibility(View.VISIBLE);
            mNoResultText.setVisibility(View.GONE);
        } else {
            mPageListView.setVisibility(View.GONE);
            mNoResultText.setVisibility(View.VISIBLE);
        }
    }

    class ContactListAdapter extends BaseAdapter {
        private ViewHolder holder;
        private LayoutInflater mInflater;

        public ContactListAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        public int getCount() {
            return mContactResultList.size();
        }

        @Override
        public Object getItem(int position) {
            return mContactResultList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
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
            holder.displayName.setText(mContactResultList.get(position).displayName);
            holder.signature.setText(mContactResultList.get(position).signature);

            final String tag = "tag" + position;
            holder.headImage.setTag(tag);
            Bitmap bitmap = mApplication.mImageloader.loadImage(mContactResultList.get(position).img,
                    AsyncImageloader.IMAGE_TYPE_THUMB, new Handler(), new ImageCallback() {
                        public void onImageLoaded(Bitmap bitmap) {
                            ImageView imageView = (ImageView) mPageListView.findViewWithTag(tag);
                            imageView.setImageBitmap(bitmap);
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
            if (response.requestCode == ActionType.SEARCH_USER) {
                if (response.responseCode == HttpResult.RESPONSE_OK) {
                    // parse data
                    ContactListParser parser = null;
                    try {
                        parser = new ContactListParser(mContext, new JSONObject(data));
                        mContactResultList = parser.getContactList();
                        // update listview
                        mAdapter.notifyDataSetChanged();
                        updateView();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (response.responseCode == HttpResult.RESPONSE_NO_INTERNET) {
                    new SingleAlertDialog(mContext).showDialog("没有网络连接" + data);
                } else {
                    new SingleAlertDialog(mContext).showDialog("HTTP 请求异常" + data);
                }
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

    @Override
    public void loadMore() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // jump to UserDetailActivity
        Intent intent = new Intent(mContext, UserDetailActivity.class);
        intent.putExtra(EXTRA_USER_ID, mContactResultList.get(position).userId);
        intent.putExtra(EXTRA_SEARCH_FLAG, true);
        startActivity(intent);
    }
}
