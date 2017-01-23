package com.tyr.content;

public class NewsStruct {
    public String account;
    public String newsId;
    public String title;
    public String img;
    // public String userid;
    public String author;
    public String description; // 新闻摘要
    public String date; // 发布日期 yyyy-MM-dd
    public int type; // 1：新闻；2通知；3教务信息；4：讲座；5活动

    // not usually stored in local DB, instead will get it via HTTP request.
    // 新闻具体内容为一次性获取，不在本地做任何储存
    // public String content;

    public NewsStruct() {
    }

    public NewsStruct(String account, String newsId, String title, String img, String author,
            String date, String description, int type) {
        this.account = account;
        this.newsId = newsId;
        this.title = title;
        this.img = img;
        this.author = author;
        this.date = date;
        this.description = description;
        this.type = type;
    }
}
