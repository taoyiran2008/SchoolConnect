package com.tyr.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tyr.ui.R;

public class TopBar extends LinearLayout
{
	public Button mLeftButton;
	public Button mRightButton;
	public TextView mTitle;

	public TopBar(Context context)
	{
		super(context);
		init();
	}

	public TopBar(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init();
	}

	private void init()
	{
		inflate(getContext(), R.layout.top_bar, this);
		mLeftButton = (Button) this.findViewById(R.id.btn_left);
		mRightButton = (Button) this.findViewById(R.id.btn_right);
		// UserDetailActivity里面有text_title
		mTitle = (TextView) this.findViewById(R.id.text_topbar_title);
	}
	    
	public void setTitle(String text)
	{
		mTitle.setText(text);
	}

	public void destroy()
	{
		mLeftButton = null;
		mTitle = null;
		this.removeAllViews();
	}
}
