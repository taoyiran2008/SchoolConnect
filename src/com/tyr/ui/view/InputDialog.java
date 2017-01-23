package com.tyr.ui.view;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.tyr.ui.R;

public class InputDialog {
    private Context mContext;
    private Dialog mDialog;
    private EditText mMessage;

    public InputDialog(Context context) {
        mContext = context;
        mDialog = new Dialog(mContext, R.style.common_dialog_style);
    }

    public void showDialog(String title, View.OnClickListener listener) {
        LayoutInflater factory = LayoutInflater.from(mContext);
        View dialogView = factory.inflate(R.layout.input_dialog_view, null);

        mMessage = (EditText) dialogView.findViewById(R.id.text_message);
        Button confirm = (Button) dialogView.findViewById(R.id.btn_ok);
        confirm.setOnClickListener(listener);

        mDialog.setContentView(dialogView);
        mDialog.setTitle(title);
        mDialog.show();
    }

    public String getMessage() {
        return mMessage.getText().toString();
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
