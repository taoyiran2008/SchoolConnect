package com.tyr.ui.view;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.tyr.ui.R;

public class ProgressAlertDialog {
    protected Context mContext;
    private TextView mMessageView;
    private Dialog mProgressDialog;
    private ProgressBar mProgress;
    
    public ProgressAlertDialog(Context context) {
        mContext = context;
        mProgressDialog = new Dialog(mContext, R.style.common_dialog_style);
    }

    public boolean isShowing() {
        return mProgressDialog.isShowing();
    }

    public void show(String msg, OnClickListener listener) {
        LayoutInflater mInflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = mInflater.inflate(R.layout.progress_dialog_view, null);
        mMessageView = (TextView) v.findViewById(R.id.dialog_text);

        Button cancelButton = (Button) v.findViewById(R.id.dialog_button);

        mMessageView.setText(msg);

        mProgress = (ProgressBar) v.findViewById(R.id.dialog_progressbar);
        mProgressDialog.setContentView(v);
        mProgressDialog.setCancelable(false);
        cancelButton.setOnClickListener(listener);
        mProgress.setVisibility(View.VISIBLE);
        mProgressDialog.show();
    }

    public void dismiss() {
        try {
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
                mProgress.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
