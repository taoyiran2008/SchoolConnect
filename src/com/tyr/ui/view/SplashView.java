package com.tyr.ui.view;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tyr.asynctask.AsyncImageloader;
import com.tyr.asynctask.ImageCallback;
import com.tyr.data.MyApplication;
import com.tyr.ui.R;
import com.tyr.util.Debugger;

/**
 * Splash 图片墙，因为ViewPager不支持动态刷新adapter，只在初始化的时候populate
 */
public class SplashView extends LinearLayout implements OnPageChangeListener {
    TextView mTextSplashTitle;
    ViewPager mSplashContainer;
    LinearLayout mSplashDots;
    ArrayList<View> mViewList;
    MyPagerAdapter mAdapter;
    ImageView[] mDots;
    int mSize;
    int mCurrentIndex = 0;
    OnSplashImageClickListener mListener;
    Context mContext;
    
    // 用于判断ViewPager的水平滑动事件
    float xDistance = 0f;
    float yDistance = 0f;
    float lastX = 0f;
    float lastY = 0f;

    public SplashView(Context context) {
        super(context);
        init(context);
    }

    public SplashView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        inflate(getContext(), R.layout.splash_view, this);
        mContext = context;
        mTextSplashTitle = (TextView) findViewById(R.id.txt_splash_title);
        mSplashContainer = (ViewPager) findViewById(R.id.splash_container);
    }

    /**
     * 因为不是本地res下的资源图片，传入imgs[resId]的形式不可靠。在这里只进行初始化的工作，异步 图片下载完成过后会回调方法进行图片更新
     */
    public void initSplashView(int size) {
        mSize = size;
        mViewList = new ArrayList<View>();
        for (int i = 0; i < size; i++) {
            mViewList.add(getPagerView(i));
        }

        mAdapter = new MyPagerAdapter();
        mSplashContainer.setAdapter(mAdapter);
        mSplashContainer.setCurrentItem(0);
        initDots(size);
        setCurrentDot(0);
        mSplashContainer.setOnPageChangeListener(this);
    }

    private void initDots(int size) {
        mSplashDots = (LinearLayout) findViewById(R.id.splash_dots);

        mDots = new ImageView[size];

        // 循环取得小点图片
        for (int i = 0; i < size; i++) {
            mDots[i] = getDotView();
            mSplashDots.addView(mDots[i]);
            mDots[i].setEnabled(true);
            final int index = i;
            mDots[i].setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    mSplashContainer.setCurrentItem(index);
                }
            });
        }
        mDots[mCurrentIndex].setEnabled(false);// 设置为黑，即选中状态 ，不可再点击状态
    }

    private View getPagerView(final int position) {
        ImageView view = new ImageView(getContext());
        view.setImageResource(R.drawable.splash_default);
        view.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        
        view.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                if (mListener != null) {
                    mListener.onClick(position);
                }
            }
        });
        return view;
    }

    public void setImage(String url, int position) {
        MyApplication application = (MyApplication) mContext.getApplicationContext();
        final ImageView view = (ImageView) mViewList.get(position);
        if (view != null) {
            Bitmap bitmap = application.mImageloader.loadImage(url,
                    AsyncImageloader.IMAGE_TYPE_BIGGER, new Handler(), new ImageCallback() {
                        public void onImageLoaded(Bitmap bitmap) {
                            view.setImageBitmap(bitmap);
                        }
                    });
            if (bitmap != null) {
                view.setImageBitmap(bitmap);
            } else {
                view.setImageResource(R.drawable.ic_launcher);
            }
        }
        
    }

    private ImageView getDotView() {
        ImageView view = new ImageView(getContext());
        view.setImageResource(R.drawable.dot_bg);
        view.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        view.setPadding(10, 0, 10, 0);
        return view;
    }

    private void setCurrentDot(int position) {
        if (position == mCurrentIndex) {
            return;
        }
        mDots[position].setEnabled(false);
        mDots[mCurrentIndex].setEnabled(true);

        mCurrentIndex = position;
    }

    class MyPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return mViewList.size();
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            // TODO Auto-generated method stub
            return arg0 == (arg1); // false 将不会加载图像
        }

        @Override
        public void destroyItem(View arg0, int arg1, Object arg2) {
            ((ViewPager) arg0).removeView(mViewList.get(arg1));
        }

        public Object instantiateItem(View parent, int position) {
            ((ViewPager) parent).addView(mViewList.get(position), 0);
            return mViewList.get(position);
        }
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onPageSelected(int arg0) {
        mSplashContainer.setCurrentItem(arg0);
        setCurrentDot(arg0);
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
        // 一次MotionEvent是以DOWN开始的，之后的MOVE任然算作一次，每一次Event其parent是否
        // 能够获得事件默认又还原为enable，也就是说requestDisallowInterceptTouchEvent 只对一次
        // 事件的截获有效
        case MotionEvent.ACTION_DOWN:
            Debugger.logDebug("DOWN");
            xDistance = yDistance = 0f;
            lastX = ev.getX();
            lastY = ev.getY();
            // 避免滑动过快的时候只响应了一下DOWN事件
            getParent().requestDisallowInterceptTouchEvent(true);
            break;
        case MotionEvent.ACTION_MOVE:
            Debugger.logDebug("MOVE");
            final float curX = ev.getX();
            final float curY = ev.getY();
//            Debugger.logDebug("curX = " + curX);
//            Debugger.logDebug("curY = " + curY);
//            Debugger.logDebug("lastX = " + lastX);
//            Debugger.logDebug("lastY = " + lastY);
            xDistance += Math.abs(curX - lastX);
            yDistance += Math.abs(curY - lastY);
//            Debugger.logDebug("xDistance = " + xDistance);
//            Debugger.logDebug("yDistance = " + yDistance);
            lastX = curX;
            lastY = curY;
            if (xDistance > yDistance) {
                getParent().requestDisallowInterceptTouchEvent(true);
            } else {
                getParent().requestDisallowInterceptTouchEvent(false);
            }
        }
        return super.dispatchTouchEvent(ev);
    }
    
    public void setOnSplashImageClickListener(OnSplashImageClickListener listener){
        mListener = listener;
    }
    public interface OnSplashImageClickListener{
        public void onClick(int position);
    }
}
