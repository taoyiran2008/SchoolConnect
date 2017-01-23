package com.tyr.ui.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.SimpleAdapter;

import com.tyr.ui.R;
import com.tyr.util.EmojiUtil;

public class EmojiDialog {
    private Context mContext;
    private Dialog mDialog;
    private GridView mGridView;
    private static final Integer[] images = { R.drawable.face_greeting, R.drawable.face_love,
            R.drawable.face_sad, R.drawable.face_smile, R.drawable.face_up };
    private EmojiOnClickListener mEmojiOnClickListener;

    public EmojiDialog(Context context) {
        mContext = context;
        mDialog = new Dialog(mContext, R.style.common_dialog_style);
    }

    public void showDialog() {
        LayoutInflater factory = LayoutInflater.from(mContext);
        View dialogView = factory.inflate(R.layout.emoji_dialog_view, null);
        
        mGridView = (GridView) dialogView.findViewById(R.id.gridview_emoji_dialog);
        SimpleAdapter adapter = new SimpleAdapter(mContext, inflateGridView(),
                R.layout.emoji_grid_item, new String[] {"img" }, new int[] {R.id.img });
        mGridView.setAdapter(adapter);
        mGridView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mEmojiOnClickListener != null) {
                    mEmojiOnClickListener.onClick(new EmojiUtil(mContext)
                            .getEmojiName(images[position]));
                }
            }
        });
        
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

    private List<Map<String, Object>> inflateGridView() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = new HashMap<String, Object>();
        for (int i = 0; i < images.length; i++) {
            // 必须new 一下，不然图片会显示为一个
            map = new HashMap<String, Object>();
            map.put("img", images[i]);
            list.add(map);
        }
        return list;
    }

    public void setEmojiOnClickListener(EmojiOnClickListener listener) {
        mEmojiOnClickListener = listener;
    }

    public void dismiss() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    public void setCancelable(boolean flag) {
        mDialog.setCancelable(flag);
    }

    public interface EmojiOnClickListener {
        public void onClick(String emojiText);
    }
}
