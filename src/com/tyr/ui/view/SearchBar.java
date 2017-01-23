package com.tyr.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.tyr.ui.R;

public class SearchBar extends LinearLayout {
    private Button mBackButton;
    private Button mSearchButton;
    private EditText mSearchText;

    public SearchBar(Context context) {
        super(context);
        init();
    }

    public SearchBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.search_bar, this);
        mBackButton = (Button) this.findViewById(R.id.btn_back);
        mSearchButton = (Button) this.findViewById(R.id.btn_search);
        mSearchText = (EditText) this.findViewById(R.id.text_search);
    }

    public void setBackOnClickListener(OnClickListener listener) {
        mBackButton.setOnClickListener(listener);
    }

    public void setBackVisibility(int visibility) {
        mBackButton.setVisibility(visibility);
    }

    public void setSearchOnClickListener(OnClickListener listener) {
        mSearchButton.setOnClickListener(listener);
    }

    public void setHint(String text) {
        mSearchText.setHint(text);
    }

    public String getSearchText() {
        return mSearchText.getText().toString();
    }

    public void destroy() {
        mBackButton = null;
        mSearchButton = null;
        mSearchText = null;
        this.removeAllViews();
    }
}
