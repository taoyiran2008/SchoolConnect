package com.tyr.content.parser;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.tyr.content.JSONKeyDef;
import com.tyr.content.NewsStruct;
import com.tyr.data.MyApplication;
import com.tyr.util.DatabaseUtil;

public class NewsListParser {
    private Context mContext;
    JSONObject mRoot;
    ArrayList<NewsStruct> mNewsList;

    public NewsListParser(Context context, JSONObject json) {
        mContext = context;
        mRoot = json;
        mNewsList = new ArrayList<NewsStruct>();
        parse();
    }

    private void parse() {
        try {
            JSONArray elements = mRoot.getJSONArray(JSONKeyDef.NEWS_LIST);
            for (int i = 0; i < elements.length(); i++) {
                JSONObject element = elements.getJSONObject(i);
                String newsId = element.getString(JSONKeyDef.NEWS_ID);
                String title = element.getString(JSONKeyDef.NEWS_TITLE);
                String img = element.getString(JSONKeyDef.NEWS_IMG);
                String description = element.getString(JSONKeyDef.NEWS_DESCRIPTION);
                String author = element.getString(JSONKeyDef.NEWS_AUTHOR);
                String date = element.getString(JSONKeyDef.NEWS_DATE);
                int type = element.getInt(JSONKeyDef.NEWS_TYPE);
                
                String account = ((MyApplication) (mContext.getApplicationContext())).myself.userId;
                NewsStruct news = new NewsStruct(account, newsId, title, img, author, date, description, type);
                mNewsList.add(news);
                
                boolean isExisted = DatabaseUtil.isNewsExists(mContext, newsId);
                if (!isExisted) {
                    DatabaseUtil.addLimitedNews(mContext, news, type);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<NewsStruct> getNewsList() {
        return mNewsList;
    }
}
