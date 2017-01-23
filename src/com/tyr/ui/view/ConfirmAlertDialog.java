package com.tyr.ui.view;

import com.tyr.ui.R;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ConfirmAlertDialog {
    private Context mContext;
    private String mMessage;
    private Dialog mDialog;

    public ConfirmAlertDialog(Context context, String message) {
        mContext = context;
        mMessage = message;
        mDialog = new Dialog(mContext, R.style.common_dialog_style);
    }

    public void showDialog(View.OnClickListener listener) {
        LayoutInflater factory = LayoutInflater.from(mContext);
        View dialogView = factory.inflate(R.layout.confirm_alert_dialog_view, null);
        TextView tv = (TextView) dialogView.findViewById(R.id.dialog_text);
        tv.setText(mMessage);

        Button confirm = (Button) dialogView.findViewById(R.id.confirm);
        confirm.setText(mContext.getString(R.string.confirm));
        confirm.setOnClickListener(listener);

        Button cancel = (Button) dialogView.findViewById(R.id.cancel);
        cancel.setText(mContext.getString(R.string.cancel));
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                dismiss();
            }
        });

        mDialog.setContentView(dialogView);
        mDialog.show();
    }

    public void dismiss() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    public void setCancelable(boolean flag) {
        mDialog.setCancelable(flag);
    }
}
