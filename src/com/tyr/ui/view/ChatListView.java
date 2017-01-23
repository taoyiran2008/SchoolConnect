package com.tyr.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.tyr.ui.R;

/**
 * 向上翻页控件
 */
public class ChatListView extends ListView implements OnScrollListener {
    private final static int  DIRECTION_DOWN = 0;
    private final static int  DIRECTION_UP = 1; // 手势向上滑动

    private boolean loading = false;
    private View mHeaderView;
    private ProgressBar mHeaderProgressBar;
    private OnLoadMoreListener onLoadMoreListener;
    private OnGestureListener onGestureListener = new MyOnGestureListener();
    GestureDetector gestureDetector = null;
    private int direction = -1;

    public ChatListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        mHeaderView = inflater.inflate(R.layout.footerview, null);
        addHeaderView(mHeaderView, null, false);
        mHeaderProgressBar = (ProgressBar) mHeaderView.findViewById(R.id.loading);
        // 去掉分隔线
        setFooterDividersEnabled(false);
        mHeaderProgressBar.setVisibility(View.GONE);

        setOnScrollListener(this);
        gestureDetector = new GestureDetector(getContext(), onGestureListener);  
        setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View arg0, MotionEvent ev) {
                return gestureDetector.onTouchEvent(ev);
            }
        });
        super.setAdapter(adapter);
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        int firstVisibleIndex = getFirstVisiblePosition();
        if (loading) { // 在加载结束前不再响应该事件
            return;
        }

        if (firstVisibleIndex == 0 && scrollState == SCROLL_STATE_IDLE
                && direction == DIRECTION_DOWN) {
            loading = true;
            mHeaderProgressBar.setVisibility(View.VISIBLE);
            if (onLoadMoreListener != null) {
                onLoadMoreListener.loadMore();
            }
        }
    }

    public void onLoadCompleted() {
        loading = false;
        direction = -1;
        mHeaderProgressBar.setVisibility(View.GONE);
    }

    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        this.onLoadMoreListener = listener;
    }

    public interface OnLoadMoreListener {
        public void loadMore();
    }
    
    /**
     * the adapter class of interface OnGestureListener
     */
    class MyOnGestureListener extends SimpleOnGestureListener {
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if ((e1.getY() > e2.getY()) && (e1.getY() - e2.getY() > 100)) {// up
                direction = DIRECTION_UP;
            } else if ((e1.getY() < e2.getY()) && (e2.getY() - e1.getY() > 100)) { // 手势向下 down
                direction = DIRECTION_DOWN;
            }
            return false;
        }
    }
}
