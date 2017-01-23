package com.tyr.activities;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.tyr.asynctask.AsyncImageloader;
import com.tyr.asynctask.ImageCallback;
import com.tyr.content.JSONKeyDef;
import com.tyr.content.MessageStruct;
import com.tyr.content.UserInfoStruct;
import com.tyr.service.MyBroadcastReceiver;
import com.tyr.service.MySocketService;
import com.tyr.socket.MessageNotice;
import com.tyr.ui.R;
import com.tyr.ui.view.EmojiDialog;
import com.tyr.ui.view.EmojiDialog.EmojiOnClickListener;
import com.tyr.ui.view.MyToast;
import com.tyr.ui.view.ChatListView;
import com.tyr.ui.view.ChatListView.OnLoadMoreListener;
import com.tyr.util.CommonUtil;
import com.tyr.util.DatabaseUtil;
import com.tyr.util.Debugger;
import com.tyr.util.EmojiUtil;

/**
 * 聊天窗口. 使用分页的ListView实现，从上到下的瀑布流排列方式。如果使用按照新旧顺序从下往上的排列 可以考虑动态 add black 结合 ScrollView的形式。
 * 
 */
public class ChatHistoryActivity extends BaseActivity implements OnLoadMoreListener,
        OnItemClickListener, OnClickListener {
    private DatabaseAyncTask mTask;
    private ChatListView mChatListView;
    private Button mEmojiButton;
    private Button mSendButton;
    private EditText mChatText;
    private TextView mNoResultText;
    private MessageListAdapter mAdapter;
    private ArrayList<MessageStruct> mMessageList = new ArrayList<MessageStruct>();
    private String mUserId;
    private int mPage = 0; // 当前分页
    private int mPageCount = 5; // 每一页加载的条数
    private int mIndex = 0; // 加载新数据前的位置，新数据append尾部后保持原位
    private EmojiUtil mEmojiUtil;
    private int mOffset = 0; // 获取消息的偏移量

    /**
     * 广播的方式替代监听数据库变更的方式, mApplication.set/getMessage 的机制可能会导致数据丢失 
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (MyBroadcastReceiver.ACTION_INCOMING_MSG.equals(action)) {
                MessageStruct newMessage = (MessageStruct) intent.getSerializableExtra(
                        BaseActivity.EXTRA_INCOMING_MSG);
                if (newMessage != null) {
                    Debugger.logDebug("new message : " + newMessage.content);
                }
                if (newMessage != null && newMessage.userId.equals(mUserId)) {
                    appendMessage(newMessage, false);
                }
            }
        }
    };
    
    /**
     * 保存消息到数据库，更新到界面上，并改变消息状态为已读
     */
    private void appendMessage(MessageStruct newMessage, boolean isSend) {
        // 该方法在本地发送send 消息后调用，也会在收到聊天消息notice的时候调用（该界面内）
        // SocketService监听到聊天消息便会写入数据库，因此这里不做再次插入
        if (isSend) {
            DatabaseUtil.addMessage(mContext, newMessage);
        }
        
        mMessageList.add(newMessage);
        mAdapter.notifyDataSetChanged();
        
        // 滑动到底部，显示最新输入的消息
        mChatListView.setSelection(mAdapter.getCount());
        
       // 更新最新一条消息(刚插入数据库的消息)为已读
        DatabaseUtil.getMessages(mContext, mUserId, 0, 1, 0);
        mOffset ++;
        updateView();
    }
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_im_chat_history);
        initData();
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // update list view
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        mContext.unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    private void initData() {
        Intent intent = getIntent();
        mUserId = intent.getStringExtra(JSONKeyDef.USER_ID);
        getLocalMessages();
        
        // register broadcast to monitor new incoming message
        IntentFilter filter = new IntentFilter();
        filter.addAction(MyBroadcastReceiver.ACTION_INCOMING_MSG);
        mContext.registerReceiver(mReceiver, filter);
    }

    private void getLocalMessages() {
        // 从本地数据库读取聊天记录
        mTask = new DatabaseAyncTask();
        mTask.execute();
    }

    private void initView() {
        mChatListView = (ChatListView) findViewById(R.id.chat_history_list);
        mNoResultText = (TextView) findViewById(R.id.text_no_content);
        UserInfoStruct userInfo = mApplication.getUserInfo(mUserId);
        
        // 在标题栏上显示聊天对象姓名
        initTopBar(userInfo.displayName, true, new OnClickListener() {
                    public void onClick(View arg0) {
                        finish();
                    }
        });
        mEmojiButton = (Button) findViewById(R.id.btn_emoji);
        mSendButton = (Button) findViewById(R.id.btn_send);
        mChatText = (EditText) findViewById(R.id.text_content);

        mEmojiButton.setOnClickListener(this);
        mSendButton.setOnClickListener(this);

        // initialize side bar
        // initSideMenu();

        // initialize listview
        mAdapter = new MessageListAdapter(mContext);
        mChatListView.setAdapter(mAdapter);
        mChatListView.setOnLoadMoreListener(this);
        mChatListView.setOnItemClickListener(this);

        mEmojiUtil = new EmojiUtil(mContext);
    }

    private void updateView() {
        if (mMessageList != null && mMessageList.size() > 0) {
            mChatListView.setVisibility(View.VISIBLE);
            mNoResultText.setVisibility(View.GONE);
        } else {
            mChatListView.setVisibility(View.GONE);
            mNoResultText.setVisibility(View.VISIBLE);
        }
    }

    class MessageListAdapter extends BaseAdapter {
        private ViewHolder holder;
        private LayoutInflater mInflater;
        private final int TYPE_LEFT = 0; // 自己，也就是发送者在左边
        private final int TYPE_RIGHT = 1;

        public MessageListAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        public int getCount() {
            return mMessageList.size();
        }

        @Override
        public Object getItem(int position) {
            return mMessageList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getViewTypeCount() {
            // 必须得重写，配合getItemViewType 使用，不然依然会混乱
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return (mMessageList.get(position).sent == 1) ? TYPE_LEFT : TYPE_RIGHT;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int type = getItemViewType(position);
            if (convertView == null) {
                holder = new ViewHolder();
                if (type == TYPE_LEFT) {
                    convertView = mInflater.inflate(R.layout.bubble_block_left, parent, false);
                } else {
                    convertView = mInflater.inflate(R.layout.bubble_block_right, parent, false);
                }
                holder.time = (TextView) convertView.findViewById(R.id.text_bubble_time);
                holder.content = (TextView) convertView.findViewById(R.id.text_bubble_content);
                holder.headImage = (ImageView) convertView.findViewById(R.id.img_head);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            UserInfoStruct userInfo = mApplication.getUserInfo(mMessageList
                    .get(position).userId);
            
            SpannableStringBuilder formatedString = mEmojiUtil
                    .format(mMessageList.get(position).content);
            holder.content.setText(formatedString);
            String sentTime = mMessageList.get(position).date + " "
                    + mMessageList.get(position).time;
            holder.time.setText(sentTime);

            final String tag = "tag" + position;
            holder.headImage.setTag(tag);
            Bitmap bitmap = mApplication.mImageloader.loadImage(userInfo.img,
                    AsyncImageloader.IMAGE_TYPE_THUMB, new Handler(), new ImageCallback() {
                        public void onImageLoaded(Bitmap bitmap) {
                            ImageView imageView = (ImageView) mChatListView.findViewWithTag(tag);
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
        public TextView content;
        public TextView time;
    }

    @Override
    public void loadMore() {
        mPage ++;
        getLocalMessages();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }

    class DatabaseAyncTask extends AsyncTask<Void, Void, ArrayList<MessageStruct>> {

        protected ArrayList<MessageStruct> doInBackground(Void... args) {
            return DatabaseUtil.getMessages(mContext, mUserId, mPage, mPageCount, mOffset);
        }

        @Override
        protected void onPostExecute(ArrayList<MessageStruct> result) {
            boolean hasResult = false;
            if (result != null && result.size() > 0) {
                // 历史消息放在上面
                mMessageList.addAll(0, result);
                mAdapter.notifyDataSetChanged();
                hasResult = true;
            } else {
                // 初始化加载第一页的信息，即便内容为空也不给提示
                if (mPage > 0) {
                    MyToast.getInstance(mContext).display("没有更多的消息了");
                    // 下一页没有加载成功，还原为正确的页数
                    mPage--;
                }
            }
            mChatListView.onLoadCompleted();
            updateView();
            if (mPage == 0) { // 第一次加载
                mChatListView.setSelection(mAdapter.getCount());
            } else if (hasResult) {
                // 停留在加载前的位置
                mChatListView.setSelection(mPageCount + 1);
            } else {
                // 不动
                mChatListView.setSelection(0);
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
        case R.id.btn_emoji:
            EmojiDialog emojiDialog = new EmojiDialog(mContext);
            emojiDialog.setEmojiOnClickListener(new EmojiOnClickListener() {
                @Override
                public void onClick(String emojiText) {
                    mChatText.append(emojiText);
                    // chatText.setSelection(emojiText.length());
                    // chatText.requestFocus();
                }
            });
            emojiDialog.showDialog();
            break;
        case R.id.btn_send:
            String text = mChatText.getText().toString();
            if (text.isEmpty()) {
                MyToast.getInstance(mContext).display("发送内容不能为空");
                return;
            }
            // socket transfer
            boolean sendSuccess = mApplication.sendNotice(new MessageNotice(MySocketService.USERID,
                    mUserId, text));
            
//            if (sendSuccess) {
                // 消息发送到服务器即为发送成功，不需要等待与seq number对应的 ack
                // save to local db.
                MessageStruct message = new MessageStruct(mApplication.myself.account, mUserId,
                        CommonUtil.getCurrentTime(), CommonUtil.getCurrentDate(), 1, text,
                        MessageStruct.TYPE_MSG, 0);
                appendMessage(message, true);
//            } else {
//                MyToast.getInstance(mContext).display("消息发送失败，请确认与服务器的连接状态");
//            }

            // reset edit view
            mChatText.setText("");
            break;
        default:
            break;
        }
    }
}
