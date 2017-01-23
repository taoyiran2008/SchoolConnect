package com.tyr.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tyr.data.MyApplication;
import com.tyr.ui.view.ProgressAlertDialog;

public class BaseFragment extends Fragment {
    private static final String TAG = "BaseFragment";
    public ProgressAlertDialog mProgressDialog;
    public Context mContext;
    public MyApplication mApplication;
    public static final String EXTRA_USER_ID = "userId";
    public static final String EXTRA_SEARCH_FLAG = "search_flag";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "BaseFragment-----onCreate");
        Bundle args = getArguments();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "BaseFragment-----onCreateView");
        mContext = getActivity();
        mApplication = ((MyApplication) (mContext.getApplicationContext()));
        mProgressDialog = new ProgressAlertDialog(mContext);
        return null;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mProgressDialog.dismiss();
        Log.d(TAG, "BaseFragment-----onDestroy");
    }

}
