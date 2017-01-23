package com.tyr.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.tyr.ui.R;
import com.tyr.util.Debugger;

/**
 * 支持向下翻页的通用类，不支持向上翻页，这样会增加控件的使用问题，考虑到header的一直存在 position需要-1
 */
public class PageListView extends ListView implements OnScrollListener {

    private boolean loading = false;
    private View mFooterview;
    private ProgressBar mFooterProgressBar;
    private OnLoadMoreListener onLoadMoreListener;
    private boolean mPagable = true; // 是否可以翻页，false 则为一般的ListView

    public PageListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        // header在setAdapter之前add，使得adapter适配成headadapterlistview，不然后续的remove和add header/footer
        // 都会出现ClassCastException
        LayoutInflater inflater = LayoutInflater.from(getContext());
        mFooterview = inflater.inflate(R.layout.footerview, null);
        addFooterView(mFooterview, null, false);
        mFooterProgressBar = (ProgressBar) mFooterview.findViewById(R.id.loading);
        // 去掉分隔线
        setFooterDividersEnabled(false);
        mFooterProgressBar.setVisibility(View.GONE);

        setOnScrollListener(this);
        super.setAdapter(adapter);
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        int lastVisibleIndex = getLastVisiblePosition();
        int firstVisibleIndex = getFirstVisiblePosition();
        int visibleCount = lastVisibleIndex - firstVisibleIndex;
        int totalCount = this.getAdapter().getCount();
        int lastIndex = totalCount - 1; // 数据集最后一项的索引
        if (loading) { // 在加载结束前不再响应该事件
            return;
        }
        // totalCount - 1 减去不可见的footerview
        if (lastVisibleIndex == lastIndex && scrollState == SCROLL_STATE_IDLE && mPagable) {
            Debugger.logDebug("visibleCount = " + visibleCount);
            Debugger.logDebug("totalCount = " + totalCount);
            loading = true;
            mFooterProgressBar.setVisibility(View.VISIBLE);
            this.setSelection(this.getAdapter().getCount()); // listview 拉倒最底部
            if (onLoadMoreListener != null) {
                onLoadMoreListener.loadMore();
            }
        }
    }

    public void onLoadCompleted() {
        loading = false;
        mFooterProgressBar.setVisibility(View.GONE);
    }

    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        this.onLoadMoreListener = listener;
    }

    public interface OnLoadMoreListener {
        public void loadMore();
    }

    public void setPagable(boolean pagable){
        mPagable = pagable;
    }
}
