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

public class ProgressbarDialog {
    private Context mContext;
    private Dialog mDialog;
    private ProgressBar mProgressBar;
    private TextView mProgressText;
    private Button mCancelBtn;

    public ProgressbarDialog(Context context) {
        mContext = context;
        mDialog = new Dialog(mContext);
        mDialog.setCancelable(false); // 模态Dialog
    }

    public void showDialog(String title, OnClickListener listener) {
        mDialog.setTitle(title);
        LayoutInflater factory = LayoutInflater.from(mContext);
        View dialogView = factory.inflate(R.layout.progressbar_dialog_view, null);
        mProgressBar = (ProgressBar) dialogView.findViewById(R.id.progress_bar);
        mProgressText = (TextView) dialogView.findViewById(R.id.txt_progress);
        mCancelBtn = (Button) dialogView.findViewById(R.id.btn_cancel);
        mProgressBar.setMax(100);
        mDialog.setContentView(dialogView);
        mCancelBtn.setOnClickListener(listener);
        mDialog.show();
    }

    public void dismiss() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    public void setProgress(int progress){
        mProgressBar.setProgress(progress);
        mProgressText.setText(progress + " %");
    }
}
