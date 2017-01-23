package com.tyr.activities;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.tyr.ui.R;

public class SimpleActivity extends BaseActivity{
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initData();
        initView();
    }
    
    private void initData(){
    }
    
    private void initView(){
    	initTopBar("Title", true, new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				finish();
			}
		});
    }
}
