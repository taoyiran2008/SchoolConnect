package com.tyr.ui.fragment;

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

import com.tyr.activities.BaseActivity;
import com.tyr.activities.MainActivity;
import com.tyr.activities.NewsDetailActivity;
import com.tyr.asynctask.ActionType;
import com.tyr.asynctask.AsyncHttpListener;
import com.tyr.asynctask.AsyncHttpTask;
import com.tyr.asynctask.AsyncImageloader;
import com.tyr.asynctask.HttpResult;
import com.tyr.asynctask.ImageCallback;
import com.tyr.content.NewsStruct;
import com.tyr.content.parser.NewsListParser;
import com.tyr.ui.R;
import com.tyr.ui.view.MyToast;
import com.tyr.ui.view.PageListView;
import com.tyr.ui.view.PageListView.OnLoadMoreListener;
import com.tyr.ui.view.RefreshableView;
import com.tyr.ui.view.RefreshableView.PullToRefreshListener;
import com.tyr.ui.view.SingleAlertDialog;
import com.tyr.ui.view.SplashView;
import com.tyr.ui.view.SplashView.OnSplashImageClickListener;
import com.tyr.util.CommonUtil;
import com.tyr.util.DatabaseUtil;
import com.tyr.util.Debugger;
import com.tyr.util.HttpUtil;

public class SchoolNewsFragment extends BaseFragment implements AsyncHttpListener,
        OnLoadMoreListener, OnItemClickListener {
    private PageListView mPageListView;
    private RefreshableView mRefreshableView;
    private TextView mNoResultText;
    private NewsListAdapter mAdapter;
    private SplashView mSplashView;

    // 最好在声明部分赋值，如果在setAdapter前list为空，listview会空指针异常
    private ArrayList<NewsStruct> mNewsList = new ArrayList<NewsStruct>();
    // 用于保存图片墙展示的新闻条目

    private int mSplashImagesCnt = 3;
    private NewsStruct[] mSplashArray = new NewsStruct[mSplashImagesCnt];
    private int mPage = 0; // 当前分页
    private int mPageCount = 10; // 每一页加载的条数
    private int mIndex = 0; // 加载新数据前的位置，新数据append尾部后保持原位

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_school_news, container, false);
        initView(view);
        initData();
        return view;
    }
    
    private void initData() {
        getNewsListViaHttp(false);
    }

    private void initView(View view) {
        mPageListView = (PageListView) view.findViewById(R.id.refresh_list_view);
        mNoResultText = (TextView) view.findViewById(R.id.text_no_content);
        mRefreshableView = (RefreshableView) view.findViewById(R.id.refreshable_view_news);
        mRefreshableView.setOnRefreshListener(new PullToRefreshListener() {
            @Override
            public void onRefresh() {
                getNewsListViaHttp(false);
            }
        }, R.id.refreshable_view_contact);

        // initialize splashview
        mSplashView = new SplashView(mContext);
        mSplashView.initSplashView(mSplashImagesCnt);
        mSplashView.setOnSplashImageClickListener(new OnSplashImageClickListener() {
            public void onClick(int position) {
                if (mNewsList != null) {
                    Intent intent = new Intent(mContext, NewsDetailActivity.class);
                    intent.putExtra(BaseActivity.EXTRA_NEWS_ID, mSplashArray[position].newsId);
                    mApplication.setNews(mSplashArray[position]);
                    startActivity(intent);
                }
            }
        });

        // initialize listview
        mAdapter = new NewsListAdapter(mContext);
        mPageListView.setAdapter(mAdapter);
        mPageListView.setOnLoadMoreListener(this);
        mPageListView.setOnItemClickListener(this);
    }

    /**
     * @param append
     *        false when data is totally refreshed from scratch.
     */
    private void getNewsListViaHttp(boolean append) {
        // get news list via HTTP request
        final AsyncHttpTask task = HttpUtil.getNewsList(mContext, this, mPage, mPageCount);

        if (!append) {
            // 下拉窗口刷新
            mPage = 0;
            // 刷新后，定位到顶部
            mIndex = 0;
            mProgressDialog.show("正在获取新闻列表 ...", new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mProgressDialog.dismiss();
                    mRefreshableView.finishRefreshing();
                    task.cancel(true);
                    // mApplication.mImageloader.cancelAllTasks();
                }
            });
        } else {
            // 滑动进度条自动加载下一页的信息，不显示Progressbar
        }
    }

    private void updateView() {
        if (mNewsList != null && mNewsList.size() > 0) {
            mRefreshableView.setVisibility(View.VISIBLE);
            mNoResultText.setVisibility(View.GONE);
        } else {
            mRefreshableView.setVisibility(View.GONE);
            mNoResultText.setVisibility(View.VISIBLE);
        }
        if (mPage == 0) {
            mPageListView.setSelection(0);
        } else {
            mPageListView.setSelection(mIndex);
        }
    }

    class NewsListAdapter extends BaseAdapter {
        private ViewHolder holder;
        private LayoutInflater mInflater;
        private final int TYPE_PAGER_ITEM = 0; // 自己，也就是发送者在左边
        private final int TYPE_LIST_ITEM = 1;

        public NewsListAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        public int getCount() {
            return mNewsList.size();
        }

        @Override
        public Object getItem(int position) {
            return mNewsList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public int getViewTypeCount() {
            // 必须得重写，配合getItemViewType 使用，不然依然会混乱
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            // 通过position的位置来判定是不可靠的
            // return (position == 0) ? TYPE_PAGER_ITEM : TYPE_LIST_ITEM;
            return (mNewsList.get(position).img == null) ? TYPE_PAGER_ITEM : TYPE_LIST_ITEM;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int type = getItemViewType(position);
            if (convertView == null) {
                holder = new ViewHolder();
                // position == 0是listview的第一个非header的view
                // 使用headerview来做更容易
                if (type == TYPE_PAGER_ITEM) {
                    convertView = mSplashView;
                } else {
                    convertView = mInflater.inflate(R.layout.news_list_item, parent, false);
                    holder.thumbImage = (ImageView) convertView.findViewById(R.id.img_news_thumb);
                    holder.title = (TextView) convertView.findViewById(R.id.text_news_title);
                    holder.description = (TextView) convertView
                            .findViewById(R.id.text_news_description);
                    convertView.setTag(holder);
                }
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            if (type == TYPE_LIST_ITEM) {
                NewsStruct news = mNewsList.get(position);
                holder.title.setText(news.title);
                holder.description.setText(news.description);

                final String tag = "tag" + position;
                holder.thumbImage.setTag(tag);
                // 图片加载会阻塞线程，异步处理
                Bitmap bitmap = mApplication.mImageloader.loadImage(news.img,
                        AsyncImageloader.IMAGE_TYPE_THUMB, new Handler(), new ImageCallback() {
                            public void onImageLoaded(Bitmap bitmap) {
                                ImageView imageView = (ImageView) mPageListView
                                        .findViewWithTag(tag);
                                imageView.setImageBitmap(bitmap);
                                mAdapter.notifyDataSetChanged();
                            }
                        });
                // 同步返回
                if (bitmap != null) {
                    holder.thumbImage.setImageBitmap(bitmap);
                } else {
                    holder.thumbImage.setImageResource(R.drawable.ic_launcher);
                }
            } else {
                for (int i = 0; i < mSplashImagesCnt; i++) {
                    if (i < mSplashImagesCnt) {
                        mSplashView.setImage(mSplashArray[i].img, i);
                    }
                }
            }
            return convertView;
        }
    }

    class ViewHolder {
        public ImageView thumbImage;
        public TextView title;
        public TextView description;
    }

    @Override
    public void onPost(HttpResult response) {
        if (response != null) {
            mProgressDialog.dismiss();
            String data = response.body;
            if (response.requestCode == ActionType.GET_NEWS_LIST) {
                if (response.responseCode == HttpResult.RESPONSE_OK) {
                    // parse data
                    NewsListParser parser = null;
                    try {
                        parser = new NewsListParser(mContext, new JSONObject(data));
                        ArrayList<NewsStruct> newsList = parser.getNewsList();
                        // 如果是加载的第一页信息，需要将前三条记录分开取出来，用于新闻图片的展示
                        if (mPage == 0) {
                            // 从头加载，初始化数据
                            mNewsList.clear();
                            mNewsList.add(new NewsStruct());// 空数据，预留给SplashView
                            for (int i = 0; i < newsList.size(); i++) {
                                if (i < mSplashImagesCnt) {
                                    mSplashArray[i] = newsList.get(i);
                                    // 可能是还没初始化完毕，设置的图片不会更新到UI上
                                    // mSplashView.setImage(newsList.get(i).img, i);
                                } else {
                                    mNewsList.add(newsList.get(i));
                                }
                            }
                        } else {
                            mNewsList.addAll(newsList);
                        }
                        mAdapter.notifyDataSetChanged();
                        updateView();
                        // 更新最近刷新时间
                        mApplication.mRefreshTime[MainActivity.INDEX_SCHOOL] = CommonUtil
                                .getCurrentFullTime();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (response.responseCode == HttpResult.RESPONSE_NO_INTERNET) {
                    new SingleAlertDialog(mContext).showDialog("没有网络连接" + data);
                    // 加载数据库的缓存信息
                    ArrayList<NewsStruct> newsList = DatabaseUtil.getNews(mContext, 1);
                    for (int i = 0; i < newsList.size(); i++) {
                        if (i < mSplashImagesCnt) {
                            mSplashArray[i] = newsList.get(i);
                        }
                        mNewsList.add(newsList.get(i));
                    }
                    mAdapter.notifyDataSetChanged();
                    updateView();
                } else {
                    new SingleAlertDialog(mContext).showDialog("HTTP 请求异常" + data);
                }
                mRefreshableView.finishRefreshing();
                mPageListView.onLoadCompleted();
            }
        }
    }

    @Override
    public void onCancel() {
        // TODO Auto-generated method stub
    }

    public void onTimeout() {
        mProgressDialog.dismiss();
        Debugger.logDebug("http 请求超时");
        MyToast.getInstance(mContext).display("http 请求超时");
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(mContext, NewsDetailActivity.class);
        intent.putExtra(BaseActivity.EXTRA_NEWS_ID, mNewsList.get(position).newsId);
        mApplication.setNews(mNewsList.get(position));
        startActivity(intent);
    }

    @Override
    public void loadMore() {
        mIndex = mPageListView.getFirstVisiblePosition();
        mPage++;
        getNewsListViaHttp(true);
    }

}
