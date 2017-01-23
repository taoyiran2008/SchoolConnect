package com.tyr.asynctask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;

import com.tyr.util.CommonUtil;
import com.tyr.util.Debugger;

/**
 * An async-task performing common HTTP request.
 * 
 */
public class AsyncHttpTask extends AsyncTask<String, Integer, HttpResult> {

    public static final String TAG = "HttpAsyncTask";
    public static String SERVER_URL = "http://10.1.9.86:8080/schoolconnect/";
    public static String SERVER_IP = "10.1.13.187";
    public static String SERVER_PORT = "8080";
    public static final int SOCKT_TIMEOUT = 30 * 1000;
    public static final int CONNECT_TIMEOUT = 30 * 1000;
    public static final String CONTENT_TYPE = "text/plain; charset=utf-8";
    public static final int MOCK_HTTP_TIME = 300;
    public static final int TIMEOUT = 10000;

    private AsyncHttpListener mHttpListener = null;
    private Context mContext;
    // 超时控制
    private Timer mTimer = new Timer();
    private boolean mTimeout = false;
    private Thread mThread;
    private HttpResult mHttpResult;

    public AsyncHttpTask(Context context) {
        mContext = context;
    }

    @Override
    protected HttpResult doInBackground(String... params) {
        if (params.length > 1) {
            if (CommonUtil.isConnection(mContext)) {
                // return new HttpResult(actionType, null, HttpResult.RESPONSE_NO_INTERNET);
            }
            final int actionType = Integer.parseInt(params[0]);
            final String param = params[1];

            if (actionType < 0) {
                Debugger.logDebug(TAG, "no such action is defined");
                return null;
            }

            // 开始耗时处理的地方加上Timout控制
            mTimer.schedule(new TimerTask() {
                public void run() {
                    // onTimeout实现中如果有UI更新，会导致异常
                    // mHttpListener.onTimeout();
                    Debugger.logDebug("task has been canceled due to timeout");
                    if (mThread != null) {
                        mThread.interrupt();
                    }
                    AsyncHttpTask.this.cancel(true);
                    mTimeout = true;
                }
            }, TIMEOUT);

            // 在新的线程里面执行无法interrupt的阻塞操作
            mThread = new Thread(new Runnable() {
                public void run() {
                    mHttpResult = mockHttpReturn(actionType);
                    /*
                    try {
                        // 上传图片的处理比较特殊一点，param只有一个代表图片地址的值，而不是JSON键值对
                        if (actionType == ActionType.UPLOAD_IMAGE) {
                            File file = new File(param);
                            if (!file.exists()) {
                                mHttpResult = new HttpResult(-1, null,
                                        HttpResult.RESPONSE_FILE_NO_EXISTS);
                            } else {
                                mHttpResult = sendUploadRequest(mContext, file);
                            }
                        } else {
                            mHttpResult = sendHttpRequest(mContext, actionType, new JSONObject(
                                    param));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    */
                }
            });
            mThread.start();
            try {
                mThread.join(); // 阻塞当前线程，等待thread执行完毕
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return mHttpResult;
    }

    @Override
    protected void onPostExecute(HttpResult result) {
        mTimer.cancel();
        if (mHttpListener != null) {
            mHttpListener.onPost(result);
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
    }

    @Override
    protected void onCancelled() {
        // 调用cancel(true)的地方有超时，也有可能是用户点击进度条主动cancel
        if (mHttpListener != null) {
            if (mTimeout) {
                mHttpListener.onTimeout();
                return;
            }
            // 手动cancel，而非超时
            mHttpListener.onCancel();
        }
    }

    public void setTaskListener(AsyncHttpListener listener) {
        mHttpListener = listener;
    }

    private HttpResult sendHttpRequest(Context context, int actionType, JSONObject param) {
        HttpResult response = new HttpResult();
        response.requestCode = actionType;
        String method = ActionType.getMethod(actionType);

        BasicHttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, CONNECT_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpParams, SOCKT_TIMEOUT);
        HttpClientParams.setRedirecting(httpParams, true);

        DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);

        StringBuilder url = new StringBuilder(SERVER_URL);
        url.append(method);
        HttpPost httpPost = new HttpPost(url.toString());

        try {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

            if (param != null) {
                nameValuePairs.add(new BasicNameValuePair("jsonKey", param.toString()));
            }

            Debugger.logDebug("params: " + nameValuePairs.toString());
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));

            HttpResponse httpResponse = httpClient.execute(httpPost);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            Debugger.logDebug("statusCode: " + statusCode);

            if (statusCode == HttpStatus.SC_OK) {
                // HTTP return is ok
                try {
                    String rawData = EntityUtils.toString(httpResponse.getEntity());
                    Debugger.logDebug("data: " + response.body);

                    // get data by JSON format
                    JSONObject data = new JSONObject(rawData);
                    if (data != null && data.has("status")) {
                        response.responseCode = Integer.parseInt(data.getString("status"));
                        if (response.responseCode == HttpResult.RESPONSE_OK) {
                            // normal result, return a structured data which can be parsed properly
                            if (data.has("data")) {
                                response.body = data.getString("data");
                            }
                        } else {
                            // abnormal result, return a message String
                            response.body = data.getString("message");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                response.responseCode = HttpResult.RESPONSE_HTTP_EXCEPTION;
                response.body = statusCode + " " + httpResponse.getStatusLine().getReasonPhrase();
            }
        } catch (Exception e) {
            response.responseCode = HttpResult.RESPONSE_UNKNOW_ERROR;
            response.body = "unknow error";
            e.printStackTrace();
        }
        httpClient.getConnectionManager().shutdown();
        return response;
    }

    private HttpResult sendUploadRequest(Context context, File file) {
        HttpResult response = new HttpResult();
        HttpClient httpclient = new DefaultHttpClient();

        response.requestCode = ActionType.UPLOAD_IMAGE;

        StringBuilder url = new StringBuilder(SERVER_URL);
        url.append(ActionType.getMethod(ActionType.UPLOAD_IMAGE));

        HttpPost httpRequest = new HttpPost(url.toString());

        FileEntity fileEntity = new FileEntity(file, "binary/octet-stream");
        httpRequest.setEntity(fileEntity);
        fileEntity.setContentType("binary/octet-stream");
        HttpResponse httpResponse;
        try {
            httpResponse = httpclient.execute(httpRequest);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            Debugger.logDebug("statusCode: " + statusCode);

            if (statusCode == HttpStatus.SC_OK) {
                // HTTP return is ok
                try {
                    response.responseCode = HttpResult.RESPONSE_OK;
                    // get data by JSON format
                    response.body = EntityUtils.toString(httpResponse.getEntity());
                    Debugger.logDebug("data: " + response.body);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                response.responseCode = statusCode;
                response.body = statusCode + " " + httpResponse.getStatusLine().getReasonPhrase();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        httpclient.getConnectionManager().shutdown();
        return response;
    }

    // data部分的数据
    private HttpResult mockHttpReturn(int requestCode) {
        HttpResult response = new HttpResult();
        response.requestCode = requestCode;
        try {
            Thread.sleep(MOCK_HTTP_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        switch (requestCode) {
        case ActionType.LOGIN:
            response.responseCode = HttpResult.RESPONSE_OK;
               response.body = "{'deviceToken':'8cb87904129f45c58b5de57c9e18f70c' ,'hasUpdate':'0','url':'http://g.hiphotos.baidu.com/baike/w%3D268/sign=3924ce6ba9ec8a13141a50e6cf029157/8644ebf81a4c510fb7f1089e6059252dd42aa579.jpg'}";
//            response.body = "{'deviceToken':'8cb87904129f45c58b5de57c9e18f70c' ,'hasUpdate':'1','url':'http://gdown.baidu.com/data/wisegame/08ec3106c3a1c26a/UCliulanqi_120.apk'}";
            break;
        case ActionType.GET_USER_INFO:
            response.responseCode = HttpResult.RESPONSE_OK;
            response.body = "{'userId':'100', 'displayName':'Tom', 'title':'1', 'img':'http://pic22.nipic.com/20120701/10060471_110050715319_2.jpg',  'description':'主任' , 'signature' : '用户签名', 'birthday':'1900-3-3', 'sex':'1', 'phone':'110', 'group':'朋友', 'used_a_lot':'1', 'remark_name':'备注姓名'}";
            break;

        case ActionType.GET_CONTACT_LIST:
            response.responseCode = HttpResult.RESPONSE_OK;
            response.body = "{'contactList' : [{'userId':'110', 'displayName':'Jerry', 'title':'1', 'img':'http://pic22.nipic.com/20120701/10060471_110050715319_2.jpg',  'description':'主任' , 'signature' : '用户签名', 'birthday':'1900-3-3', 'sex':'1', 'phone':'110', 'group':'朋友', 'used_a_lot':'1', 'remark_name':'备注姓名'},"
                    + "{'userId':'111', 'displayName':'Susan', 'title':'1', 'img':'http://pic22.nipic.com/20120701/10060471_110050715319_2.jpg',  'description':'主任' , 'signature' : '用户签名2', 'birthday':'1900-2-13', 'sex':'0', 'phone':'110', 'group':'朋友', 'used_a_lot':'1', 'remark_name':'备注姓名'},"
                    + "{'userId':'112', 'displayName':'Angela', 'title':'1', 'img':'http://pic22.nipic.com/20120701/10060471_110050715319_2.jpg',  'description':'主任' , 'signature' : '用户签名3', 'birthday':'1900-2-13', 'sex':'0', 'phone':'110', 'group':'家人', 'used_a_lot':'0', 'remark_name':'备注姓名'},"
                    + "{'userId':'113', 'displayName':'Caroline', 'title':'1', 'img':'http://pic22.nipic.com/20120701/10060471_110050715319_2.jpg',  'description':'主任' , 'signature' : '用户签名4', 'birthday':'1900-2-13', 'sex':'0', 'phone':'110', 'group':'家人', 'used_a_lot':'0', 'remark_name':'备注姓名'},"
                    + "{'userId':'114', 'displayName':'Max', 'title':'1', 'img':'http://pic22.nipic.com/20120701/10060471_110050715319_2.jpg',  'description':'主任' , 'signature' : '用户签名5', 'birthday':'1900-2-13', 'sex':'0', 'phone':'110', 'group':'陌生人', 'used_a_lot':'0', 'remark_name':'备注姓名'},"
                    + "]}";
            break;
        case ActionType.MODIFY_USER:
            response.responseCode = HttpResult.RESPONSE_OK;
            break;
        case ActionType.ADD_FRIEND:
            response.responseCode = HttpResult.RESPONSE_OK;
            break;
        case ActionType.SEARCH_USER:
            response.responseCode = HttpResult.RESPONSE_OK;
            response.body = "{'contactList' : [{'userId':'120', 'displayName':'Rachel', 'title':'1', 'img':'http://pic22.nipic.com/20120701/10060471_110050715319_2.jpg',  'description':'主任' , 'signature' : '用户签名1', 'birthday':'1900-3-3', 'sex':'1', 'phone':'110', 'group':'朋友', 'used_a_lot':'0', 'remark_name':'备注姓名'},"
                    + "{'userId':'121', 'displayName':'Smith', 'title':'1', 'img':'http://pic22.nipic.com/20120701/10060471_110050715319_2.jpg',  'description':'主任' , 'signature' : '用户签名2', 'birthday':'1900-2-13', 'sex':'0', 'phone':'110', 'group':'朋友', 'used_a_lot':'0', 'remark_name':'备注姓名'},"
                    + "{'userId':'122', 'displayName':'Teddy', 'title':'1', 'img':'http://pic22.nipic.com/20120701/10060471_110050715319_2.jpg',  'description':'主任' , 'signature' : '用户签名3', 'birthday':'1900-2-13', 'sex':'0', 'phone':'110', 'group':'家人', 'used_a_lot':'0', 'remark_name':'备注姓名'},"
                    + "{'userId':'123', 'displayName':'Andrew', 'title':'1', 'img':'http://pic22.nipic.com/20120701/10060471_110050715319_2.jpg',  'description':'主任' , 'signature' : '用户签名4', 'birthday':'1900-2-13', 'sex':'0', 'phone':'110', 'group':'朋友', 'used_a_lot':'0', 'remark_name':'备注姓名'},"
                    + "]}";
            break;

        case ActionType.GET_NEWS_LIST:
            response.responseCode = HttpResult.RESPONSE_OK;
            response.body = "{'newsList' : ["
                    + "{'newsId':'1', 'title':'News1', 'description':'新闻摘要1', 'img':'http://img1.gtimg.com/hb/pics/hv1/195/143/1002/65191710.jpg',  'author':'安川' , 'news_type' : '1',  'date':'2014-5-13'},"
                    + "{'newsId':'1', 'title':'News2', 'description':'新闻摘要2', 'img':'http://pica.nipic.com/2007-07-11/2007711133922902_2.jpg',  'author':'安川' , 'news_type' : '1', 'date':'2014-5-13'},"
                    + "{'newsId':'1', 'title':'News3', 'description':'新闻摘要3', 'img':'http://pic22.nipic.com/20120701/10060471_110050715319_2.jpg',  'author':'安川' , 'news_type' : '1', 'date':'2014-5-13'},"
                    + "{'newsId':'1', 'title':'News4', 'description':'新闻摘要4', 'img':'http://pic22.nipic.com/20120701/10060471_110050715319_2.jpg',  'author':'安川' , 'news_type' : '1', 'date':'2014-5-13'},"
                    + "{'newsId':'1', 'title':'News5', 'description':'新闻摘要5', 'img':'http://pic22.nipic.com/20120701/10060471_110050715319_2.jpg',  'author':'安川' , 'news_type' : '1', 'date':'2014-5-13'},"
                    + "{'newsId':'1', 'title':'News6', 'description':'新闻摘要6', 'img':'http://pic22.nipic.com/20120701/10060471_110050715319_2.jpg',  'author':'安川' , 'news_type' : '1', 'date':'2014-5-13'},"
                    + "{'newsId':'1', 'title':'News7', 'description':'新闻摘要7', 'img':'http://pic22.nipic.com/20120701/10060471_110050715319_2.jpg',  'author':'安川' , 'news_type' : '1', 'date':'2014-5-13'},"
                    + "]}";
            break;
        case ActionType.GET_NEWS_DETAIL:
            response.responseCode = HttpResult.RESPONSE_OK;
            response.body = "{ 'content':'" + getNews() + "'}";
            break;
        case ActionType.UPLOAD_IMAGE:
            response.responseCode = HttpResult.RESPONSE_OK;
            response.body = "{ 'img':'http://pic22.nipic.com/20120701/10060471_110050715319_2.jpg'}";
            break;
        case ActionType.GET_NEW_MSG_COUNT:
            response.responseCode = HttpResult.RESPONSE_OK;
            response.body = "{ 'count':'10'}";
            break;
        default:
            break;
        }
        return response;
    }

    private String getNews() {
        StringBuilder builder = new StringBuilder();
        builder.append("    新华网北京5月16日电  外交部边海司司长欧阳玉靖16日就越南干扰中方企业在中国西沙群岛中建南海域钻探作业事举行吹风会。\n\n");
        builder.append("    欧阳玉靖表示，中方已多次要求越方尊重中国主权、主权权利和管辖权，停止对中方企业在中建南海域作业的干扰行动并将船只和人员撤出该海域，但越南船只有增无减，对中国现场船只的冲撞持续不断。中方对此强烈不满，已多次向越方提出交涉，要求越方立即停止对中国企业正常作业的干扰，立即撤走所有船只和人员。\n\n");
        builder.append("    欧阳玉靖表示，中方作业的海域位于中国西沙群岛附近，距离中国中建岛仅17海里，距离越南海岸将近150海里。越方对中方企业在中国近海正当、合法的钻探活动进行干扰是完全没有道理的。\n\n");
        builder.append("    欧阳玉靖表示，中方此次作业并不是今年或这个月才开始的。10年来，中国企业就一直在这一海域进行基础作业。去年5至6月，中国企业还在这个海域进行了三维地震作业和井场调查，为钻探作业做必要准备。此次钻探是10年来有关作业的例行延续，中方将坚决确保作业完成。\n\n");
        builder.append("    欧阳玉靖表示，中越之间的沟通交流是通畅的。截至目前，中越两国各个层级已进行过20多次外交沟通。\n\n");
        return builder.toString();
    }
}
