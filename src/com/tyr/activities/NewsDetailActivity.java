package com.tyr.activities;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.tyr.asynctask.ActionType;
import com.tyr.asynctask.AsyncHttpListener;
import com.tyr.asynctask.AsyncHttpTask;
import com.tyr.asynctask.AsyncImageloader;
import com.tyr.asynctask.HttpResult;
import com.tyr.asynctask.ImageCallback;
import com.tyr.content.JSONKeyDef;
import com.tyr.content.NewsStruct;
import com.tyr.ui.R;
import com.tyr.ui.view.MyToast;
import com.tyr.ui.view.SingleAlertDialog;
import com.tyr.util.Debugger;
import com.tyr.util.HttpUtil;

/**
 * 该页面根据newid从server上获取详细内容，其他诸如author  img等信息由新闻列表传过来 
 */
public class NewsDetailActivity extends BaseActivity implements AsyncHttpListener {
    private TextView mTextTitle;
    private TextView mTextAuthor;
    private TextView mTextDate;
    private TextView mTextContent;
    private ImageView mImage;
    private String mNewsId;
    private String mNewsDetail;

    private NewsStruct mNewsInfo;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_detail);
        initData();
        initView();
    }

    private void initData() {
        Intent intent = getIntent();
        mNewsId = intent.getStringExtra(EXTRA_NEWS_ID);
        mNewsInfo = mApplication.getNews();
        
        Debugger.logDebug("mNewsId = " + mNewsId);

        getNewsInfo(mNewsId);
    }

    private void initView() {
        initTopBar("", true, new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                finish();
            }
        });

        mTextTitle = (TextView) findViewById(R.id.txt_title);
        mTextAuthor = (TextView) findViewById(R.id.txt_author);
        mTextDate = (TextView) findViewById(R.id.txt_date);
        mTextContent = (TextView) findViewById(R.id.text_content);
        mImage = (ImageView) findViewById(R.id.img_news);
        
        if (mNewsInfo != null) {
            mTextTitle.setText(mNewsInfo.title);
            mTextAuthor.setText(mNewsInfo.author);
            mTextDate.setText(mNewsInfo.date);
            
            Bitmap bitmap = mApplication.mImageloader.loadImage(mNewsInfo.img,
                    AsyncImageloader.IMAGE_TYPE_BIGGER, new Handler(), new ImageCallback() {
                        public void onImageLoaded(Bitmap bitmap) {
                            mImage.setImageBitmap(bitmap);
                        }
                    });
            // 同步返回
            if (bitmap != null) {
                mImage.setImageBitmap(bitmap);
            } else {
                mImage.setImageResource(R.drawable.ic_launcher);
            }
        }
    }

    private void getNewsInfo(String newsId) {
        final AsyncHttpTask task = HttpUtil.getNewsInfo(mContext, this, newsId);
        mProgressDialog.show("正在获取新闻信息 ...", new OnClickListener() {
            @Override
            public void onClick(View v) {
                mProgressDialog.dismiss();
                task.cancel(true);
            }
        });
    }

    @Override
    public void onPost(HttpResult response) {
        if (response != null) {
            String data = response.body;
            mProgressDialog.dismiss();
            if (response.responseCode == HttpResult.RESPONSE_OK) {
                if (response.requestCode == ActionType.GET_NEWS_DETAIL) {
                    try {
                        // json数据过于简单，A specified Parser is not necessary
                        mNewsDetail = new JSONObject(data).getString(JSONKeyDef.NEWS_CONTENT);
                        mTextContent.setText(mNewsDetail);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } else if (response.responseCode == HttpResult.RESPONSE_NO_INTERNET) {
                new SingleAlertDialog(mContext).showDialog("网络异常\n" + data);
            } else {
                new SingleAlertDialog(mContext).showDialog("未知的错误\n" + data);
            }
        }
    }

    @Override
    public void onCancel() {
    }
    
    public void onTimeout() {
        mProgressDialog.dismiss();
        Debugger.logDebug("http 请求超时");
        MyToast.getInstance(mContext).display("http 请求超时");
    }
}
