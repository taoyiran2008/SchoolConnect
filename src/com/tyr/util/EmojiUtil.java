package com.tyr.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.util.LruCache;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.SparseArray;

import com.tyr.ui.R;

/**
 * 表情工具类，实现图文混排
 */
public class EmojiUtil {
    public static int[] imgs = { R.drawable.face_greeting, R.drawable.face_love,
            R.drawable.face_sad, R.drawable.face_smile, R.drawable.face_up };
    // 这个值不是图像的确切尺寸
    public static int EMOJI_WIDTH = 80;
    public static int EMOJI_HEIGHT = 80;
    // 表情的正则表达式匹配 e.g. (:smile:)
    String regex = "\\(:\\w+:\\)";

    /**
     * 缓存Image的类，当存储Image的大小大于LruCache设定的值， 系统自动释放内存
     *  这里使用Android 提供的缓存类，不需要自己去实现 SoftReference
     */
    private LruCache<Integer, Bitmap> mMemoryCache;
    private Context mContext;
    // 表情符号转义表
    private SparseArray<String> mEmojiMap = new SparseArray<String>();

    public EmojiUtil(Context context) {
        mContext = context;

        // 获取系统分配给每个应用程序的最大内存，每个应用系统分配32M
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int mCacheSize = maxMemory / 36;
        // 给LruCache分配1/8 1M
        mMemoryCache = new LruCache<Integer, Bitmap>(mCacheSize) {
            // 必须重写此方法，来测量Bitmap的大小
            @Override
            protected int sizeOf(Integer resId, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };
        mEmojiMap.append(imgs[0], "(:greeting:)");
        mEmojiMap.append(imgs[1], "(:love:)");
        mEmojiMap.append(imgs[2], "(:sad:)");
        mEmojiMap.append(imgs[3], "(:smile:)");
        mEmojiMap.append(imgs[4], "(:up:)");
    }

    public Integer getEmojiResId(String text) {
        // 如果效率优先，value反查key还是使用两个Map
        int resId = -1;
        for (int i = 0; i < mEmojiMap.size(); i++) {
            int key = mEmojiMap.keyAt(i);
            String value = mEmojiMap.valueAt(i);
            if (value.equals(text)) {
                resId = key;
                break;
            }
        }
        return resId;
    }

    public String getEmojiName(Integer resId) {
        return mEmojiMap.get(resId);
    }

    public SpannableStringBuilder format(String text) {
        String origin = text;
        int offset = 0;
        // 创建一个SpannableString对象，以便插入用ImageSpan对象封装的图像
        SpannableStringBuilder spannableString = new SpannableStringBuilder(text);
        Pattern pattern = Pattern.compile(regex);
        Matcher mat = pattern.matcher(text);
        while (mat.find()) {
            String face = mat.group(0);
            text = mat.replaceFirst("");
            int start = origin.indexOf(face, offset);
            int end = start + face.length();
            offset = end;
            // 根据Bitmap对象创建ImageSpan对象
            Bitmap bitmap = getBitmapFromMemCache(getEmojiResId(face));
            Drawable drawable = new BitmapDrawable(mContext.getResources(), bitmap);
            drawable.setBounds(0, 0, EMOJI_WIDTH, EMOJI_HEIGHT);
            ImageSpan imageSpan = new ImageSpan(drawable);
            spannableString.setSpan(imageSpan, start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            mat = pattern.matcher(text);
        }
        return spannableString;
    }

    private void addBitmapToMemoryCache(Integer resId, Bitmap bitmap) {
        if (bitmap != null) {
            mMemoryCache.put(resId, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(Integer resId) {
        Bitmap bitmap = mMemoryCache.get(resId);
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(mContext.getResources(), resId);
            addBitmapToMemoryCache(resId, bitmap);
        }
        // need API 19
        // bitmap.setWidth(EMOJI_WIDTH);
        // bitmap.setHeight(EMOJI_HEIGHT);
        return bitmap;
    }
}
