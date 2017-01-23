package com.tyr.ui.view;

import com.tyr.ui.R;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class SingleAlertDialog {
    private Context mContext;
    private Dialog mDialog;

    public SingleAlertDialog(Context context) {
        mContext = context;
        mDialog = new Dialog(mContext, R.style.common_dialog_style);
    }

    public void showDialog(String msg) {
        LayoutInflater factory = LayoutInflater.from(mContext);
        View dialogView = factory.inflate(R.layout.single_alert_dialog_view, null);
        TextView tv = (TextView) dialogView.findViewById(R.id.dialog_text);
        tv.setText(msg);

        Button confirm = (Button) dialogView.findViewById(R.id.confirm);
        confirm.setText(mContext.getString(R.string.confirm));
        confirm.setOnClickListener(new View.OnClickListener() {
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
