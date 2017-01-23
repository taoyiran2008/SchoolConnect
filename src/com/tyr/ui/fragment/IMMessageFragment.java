package com.tyr.ui.fragment;

import java.util.ArrayList;
import java.util.Collections;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.tyr.activities.BaseActivity;
import com.tyr.activities.ChatHistoryActivity;
import com.tyr.activities.MainActivity;
import com.tyr.asynctask.AsyncImageloader;
import com.tyr.asynctask.ImageCallback;
import com.tyr.content.MessageRecordStruct;
import com.tyr.content.MessageStruct;
import com.tyr.content.UserInfoStruct;
import com.tyr.service.MyBroadcastReceiver;
import com.tyr.socket.FriendResponseNotice;
import com.tyr.socket.LoginNotice;
import com.tyr.ui.R;
import com.tyr.ui.view.ConfirmAlertDialog;
import com.tyr.ui.view.PageListView;
import com.tyr.ui.view.SingleAlertDialog;
import com.tyr.util.BitmapUtil;
import com.tyr.util.CommonUtil;
import com.tyr.util.DatabaseUtil;
import com.tyr.util.EmojiUtil;

/**
 * 聊天记录一览画面.
 */
public class IMMessageFragment extends BaseFragment implements OnItemClickListener,
        OnItemLongClickListener {
    private static final String TAG = "IMMessageFragment";
    private DatabaseAyncTask mTask;
    private PageListView mPageListView;
    private TextView mNoResultText;
    private MessageListAdapter mAdapter;
    private ArrayList<MessageRecordStruct> mMessageRecordList = new ArrayList<MessageRecordStruct>();
    private PopupWindow mPopupWindow;

    // 该画面是由数据库的内容填充的，数据库改变后通过观察者来更新画面
    private ContentObserver mContentObserver = new MyObserver(new Handler());

    /**
     * Message table的数据有更新，则重新查询数据库，构建画面
     */
    class MyObserver extends ContentObserver {
        public void onChange(boolean selfChange) {
            getLocalMessageRecords();
            updateView();
        }

        public MyObserver(Handler handler) {
            super(handler);
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log.d(TAG, "TestFragment-----onCreateView");
        View view = inflater.inflate(R.layout.fragment_im_message, container, false);
        initData();
        initView(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // update list view
        initData();
    }

    @Override
    public void onDestroy() {
        DatabaseUtil.unregisterMessagesObserver(mContext, mContentObserver);
        super.onDestroy();
    }
    
    private void initData() {
        DatabaseUtil.registerMessagesObserver(mContext, mContentObserver);
        getLocalMessageRecords();
    }
    
    private void getLocalMessageRecords() {
        // 从本地数据库读取聊天记录
        mTask = new DatabaseAyncTask();
        mTask.execute();
    }

    private void initView(View view) {
        mPageListView = (PageListView) view.findViewById(R.id.message_list);
        mNoResultText = (TextView) view.findViewById(R.id.text_no_content);

        // initialize listview
        mAdapter = new MessageListAdapter(mContext);
        mPageListView.setPagable(false);
        mPageListView.setAdapter(mAdapter);
        mPageListView.setOnItemClickListener(this);
        mPageListView.setOnItemLongClickListener(this);
    }

    // 数据一旦有改变，需要调用这个方法更新界面，因为该数据不是全局的，所有更改都在内部
    // 完成，并没有做成Observable数据
    private void updateView() {
        if (mMessageRecordList != null && mMessageRecordList.size() > 0) {
            mPageListView.setVisibility(View.VISIBLE);
            mNoResultText.setVisibility(View.GONE);
        } else {
            mPageListView.setVisibility(View.GONE);
            mNoResultText.setVisibility(View.VISIBLE);
        }
    }

    class MessageListAdapter extends BaseAdapter {
        private ViewHolder holder;
        private LayoutInflater mInflater;

        public MessageListAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        public int getCount() {
            return mMessageRecordList.size();
        }

        @Override
        public Object getItem(int position) {
            return mMessageRecordList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.message_record_item, parent, false);
                holder.name = (TextView) convertView.findViewById(R.id.text_name);
                holder.time = (TextView) convertView.findViewById(R.id.text_time);
                holder.content = (TextView) convertView.findViewById(R.id.text_content);
                holder.headImage = (ImageView) convertView.findViewById(R.id.img_head);
                holder.cnt = (TextView) convertView.findViewById(R.id.text_num);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            
            holder.name.setText(mMessageRecordList.get(position).displayName);
            SpannableStringBuilder formatedString = new EmojiUtil(mContext).format(mMessageRecordList
                    .get(position).message.content);
            holder.content.setText(formatedString);
            String sentTime = mMessageRecordList.get(position).message.date + " "
                    + mMessageRecordList.get(position).message.time;
            holder.time.setText(sentTime);
            // 设置未读信息条数
            if (mMessageRecordList.get(position).cnt > 0) {
                holder.cnt.setVisibility(View.VISIBLE);
                holder.cnt.setText(String.valueOf(mMessageRecordList.get(position).cnt));
            } else {
                holder.cnt.setVisibility(View.GONE);
            }
            
            final String tag = "tag" + position;
            holder.headImage.setTag(tag);
            // 图片加载会阻塞线程，异步处理
            Bitmap bitmap = mApplication.mImageloader.loadImage(mMessageRecordList.get(position).img,
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
        public TextView name;
        public TextView content;
        public TextView time;
        public TextView cnt;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final MessageRecordStruct record = mMessageRecordList.get(position);
        if (record.message.type == MessageStruct.TYPE_MSG) {
            Intent intent = new Intent(mContext, ChatHistoryActivity.class);
            intent.putExtra(BaseActivity.EXTRA_USER_ID, record.userId);
            startActivity(intent);
        } else if (record.message.type == MessageStruct.TYPE_REQ) {
            // 好友请求的聊天记录，则需要先同意添加好友才能进入聊天界面聊天
            showFriendRequestDialog(record);
        }
    }

    private void showFriendRequestDialog(final MessageRecordStruct record) {
        final ConfirmAlertDialog dialog = new ConfirmAlertDialog(mContext, record.message.content);
        dialog.showDialog(new OnClickListener() {
            public void onClick(View arg0) {
                boolean sendSuccess = mApplication.sendNotice(new FriendResponseNotice(mApplication.myself.account,
                        record.message.userId, true));
                if (sendSuccess) {
                    // socket 响应发送成功
                    // 添加一条普通消息，现在可以通过聊天界面进入进行正常对话了
                    String msg = "你已经接受来自 " + record.displayName + "的好友请求";
                    MessageStruct message = new MessageStruct(mApplication.myself.account, record.message.userId,
                            CommonUtil.getCurrentTime(), CommonUtil.getCurrentDate(), 1, msg, MessageStruct.TYPE_MSG, 1);
                    DatabaseUtil.addMessage(mContext, message);
                    
                    // 该userId的详细用户信息在收到好友通知notice的时候已经作为临时用户添加到了数据库
                    UserInfoStruct userInfo = mApplication.getUserInfo(record.message.userId);
                    // 更新内存
                    mApplication.mContactListObservable.addContact(userInfo);
                    
                    // 发送上线通知
                    mApplication.sendNotice(new LoginNotice(mApplication.myself.account, record.message.userId));
                    
                    // 调用http接口，修改远程服务器的好友列表（添加彼此为好友）
                    // 这一步应该由Socket服务器代理完成，虽然一般来说Socket和Http
                    // 使用的是同一个地址，但是不能保证两个server同时都是可用的，
                    // 比如port 被关闭，socket 服务器来完成可以保证这次好友添加流程
                    // 不会失效（类似三次握手的应答响应流程）
                    //HttpUtil.addFriend(mContext, null, mApplication.myself.account, record.userId);  
                } else {
                    new SingleAlertDialog(mContext).showDialog("回复好友请求失败，请确认socket连接");
                }
                dialog.dismiss();
            }
        });
    }

    class DatabaseAyncTask extends AsyncTask<Void, Void, ArrayList<MessageRecordStruct>> {

        protected ArrayList<MessageRecordStruct> doInBackground(Void... args) {
            return DatabaseUtil.getLatestMessageRecords(mContext);
        }

        @Override
        protected void onPostExecute(ArrayList<MessageRecordStruct> result) {
            if (result != null && result.size() > 0) {
                // mMessageList.addAll(result);
                mMessageRecordList = result;
                // 排序
                Collections.sort(mMessageRecordList);
                mAdapter.notifyDataSetChanged();
                updateView();
                
                // 统计出所有未读信息的条数，发送广播通知界面更新number circle
                int cnt = 0;
                for (MessageRecordStruct msgRecord : mMessageRecordList) {
                    cnt += msgRecord.cnt;
                }
                MyBroadcastReceiver.sendBroadcastNewMsgCnt(mContext, MainActivity.INDEX_IM, cnt);
            } else {
                // 删除所有消息记录后，发送广播清零
                MyBroadcastReceiver.sendBroadcastNewMsgCnt(mContext, MainActivity.INDEX_IM, 0);
            }
        }
    }
    
    private void deleteMessage(int position) {
        DatabaseUtil.deleteMessages(mContext, mMessageRecordList.get(position).userId);
        mMessageRecordList.remove(position);
        mAdapter.notifyDataSetChanged();
        mPopupWindow.dismiss();
        updateView();
    }

    private void showPopupMenu(View parent, final int position) {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        View popup = layoutInflater.inflate(R.layout.popup_menu_message, null);
        TextView deleteMessage = (TextView) popup
                .findViewById(R.id.text_button_delete_message);
        deleteMessage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                deleteMessage(position);
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
//        int width = popup.getWidth(); 获取不到，除非延迟获取
        int left = (screenWidth - widthInDp) / 2;
        int top = location[1] + parent.getMeasuredHeight();
        mPopupWindow.showAtLocation(parent, Gravity.NO_GRAVITY, left, top);
    }
    
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        // PageListView
        showPopupMenu(view, position);
        return  true; // 事件不再往下传递，onitemclick收不着
    }
}
