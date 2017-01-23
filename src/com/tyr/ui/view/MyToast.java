package com.tyr.ui.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.tyr.ui.R;

public class MyToast extends Toast {
    public Context context;
    TextView text;
    static MyToast instance;

    private MyToast(Context context) {
        super(context);
        this.context = context;
        initView();
        // TODO Auto-generated constructor stub
    }

    public static MyToast getInstance(Context context) {
        if (instance == null) {
            instance = new MyToast(context);
        }
        return instance;
    }

    public void initView() {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.toast_layout, null);
        text = (TextView) view.findViewById(R.id.text);
        setView(view);

    }

    public void display(String str) {
        text.setText(str);
        // makeText(context, str, LENGTH_SHORT).show();
        // textView.setText(Html.fromHtml("<font size=\"3\" color=\"red\">今天天气好吗？</font><font size=\"3\" color=\"green\">挺好的</font>"));
        show();
    }

    public void displayClassic(String str) {
        Toast.makeText(context, str, LENGTH_SHORT).show();
    }
}
