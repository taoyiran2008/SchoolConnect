package com.tyr.activities;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tyr.service.MyBroadcastReceiver;
import com.tyr.ui.R;
import com.tyr.ui.fragment.IMContactFragment;
import com.tyr.ui.fragment.IMConventionalFragment;
import com.tyr.ui.fragment.IMMessageFragment;
import com.tyr.ui.fragment.SchoolNewsFragment;
import com.tyr.ui.fragment.TestFragment;
import com.tyr.ui.view.MyToast;
import com.tyr.util.Debugger;

/**
 * 主页面，分为4个画面，仿照Android原生Email程序，根据Intent extra，动态构建画面
 * */
public class MainActivity extends BaseActivity implements OnClickListener, OnPageChangeListener {
    // 四个类别的主页面
    public static final int INDEX_IM = 0;
    public static final int INDEX_TEACHING = 1;
    public static final int INDEX_SCHOOL = 2;
    public static final int INDEX_LIFE = 3;
    public static final int INDEX_CNT = 4;

    // 每个主页面包含的子页面
    public static final int SUB_INDEX_MESSAGE = 0;
    public static final int SUB_INDEX_CONTACTS = 1;
    public static final int SUB_INDEX_COMMON_CONTACTS = 2;

    public static final int SUB_INDEX_SCHEDULE = 0;
    public static final int SUB_INDEX_CLASSROOM = 1;
    public static final int SUB_INDEX_SCORE = 2;

    public static final int SUB_INDEX_NEWS = 0;
    public static final int SUB_INDEX_MEETING = 1;
    public static final int SUB_INDEX_NOTICE = 2;

    public static final int SUB_INDEX_UTILITY = 0;
    public static final int SUB_INDEX_B2C = 1;
    // 每个主页面最多容纳的子页面
    public static final int SUB_INDEX_MAX_CNT = 3;

    // Second menu
    TextView mTextMessage;
    TextView mTextContacts;
    TextView mTextCommonContacts;
    TextView mTextSchedule;
    TextView mTextClassroom;
    TextView mTextScore;
    TextView mTextMeeting;
    TextView mTextNotice;
    TextView mTextNews;
    TextView mTextUtility;
    TextView mTextB2C;

    // First Menu
    TextView mTextIM;
    TextView mTextTeaching;
    TextView mTextSchool;
    TextView mTextLife;
    LinearLayout mTextIMContainer;
    LinearLayout mTextTeachingContainer;
    LinearLayout mTextSchoolContainer;
    LinearLayout mTextLifeContainer;
    TextView mTextIMNum;
    TextView mTextTeachingNum;
    TextView mTextSchoolNum;
    TextView mTextLifeNum;

    HorizontalScrollView msecondMenu;
    FrameLayout msecondMenuContainer;
    ImageView mMovingBlock;
    ViewPager mContainer;
    // HashMap<Integer, ArrayList<Fragment>> mFragmentMap;
    MyFragmentPagerAdapter mAdapter;
    ArrayList<Fragment> mFragmentsList = new ArrayList<Fragment>();
    TextView mCurTxt;
    int mCurIndex = INDEX_IM;
    int[] mCntArray = new int[INDEX_CNT];// 每个页面维护一个新消息数
    // 手机屏幕的宽度
    int mScreenWidth;
    Handler mHandler;
    int mCurSubIndex = -1; // 要特定跳转的子页面
    int mMaxSubPageCount = SUB_INDEX_MAX_CNT; // 当前页面的子页面数量

    boolean mFirstTime = true;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context arg0, Intent intent) {
            final String action = intent.getAction();
            if (MyBroadcastReceiver.ACTION_NEW_MSG.equals(action)) {
                Debugger.logDebug("new message is coming");
                int index = intent.getIntExtra(EXTRA_INDEX_TYPE, INDEX_IM);
                int cnt = intent.getIntExtra(EXTRA_NEW_MSG_CNT, 0);
                // Incoming New message count, set zero.
                hideCircle();
                mCntArray[index] = cnt;
                showCircle(index, cnt);
            }
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initData();
        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mContext.unregisterReceiver(mReceiver);
    }

    private void initData() {
        Intent intent = getIntent();
        mCurIndex = intent.getIntExtra(EXTRA_INDEX_TYPE, INDEX_IM);
        mCurSubIndex = intent.getIntExtra(EXTRA_SUB_INDEX_TYPE, INDEX_IM);
        // 接收上一个页面的未读消息数
        mCntArray = intent.getIntArrayExtra(EXTRA_NEW_MSG_CNT);
        if (mCntArray == null) {
            // 第一次启动
            mCntArray = new int[INDEX_CNT];
        }

        // register broadcast to monitor new messages
        IntentFilter filter = new IntentFilter();
        filter.addAction(MyBroadcastReceiver.ACTION_NEW_MSG);
        mContext.registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mApplication.myself == null) {
            // has not logged in
            MyToast.getInstance(mContext).display("请先登录");
            finish();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // 点击通知栏时
        int index = intent.getIntExtra(EXTRA_INDEX_TYPE, INDEX_IM);
        switchPage(index);
    }

    private void initView() {
        initTopBar("SchoolConnect", false, null);

        initSideMenu();
        msecondMenu = (HorizontalScrollView) findViewById(R.id.second_menu);
        mContainer = (ViewPager) findViewById(R.id.main_container);
        msecondMenuContainer = (FrameLayout) findViewById(R.id.second_menu_container);
        mTextMessage = (TextView) findViewById(R.id.text_message);
        mTextContacts = (TextView) findViewById(R.id.text_contacts);
        mTextCommonContacts = (TextView) findViewById(R.id.text_common_contacts);
        mTextSchedule = (TextView) findViewById(R.id.text_schedule);
        mTextClassroom = (TextView) findViewById(R.id.text_classroom);
        mTextScore = (TextView) findViewById(R.id.text_score);
        mTextMeeting = (TextView) findViewById(R.id.text_meeting);
        mTextNotice = (TextView) findViewById(R.id.text_notice);
        mTextNews = (TextView) findViewById(R.id.text_news);
        mTextUtility = (TextView) findViewById(R.id.text_utility);
        mTextB2C = (TextView) findViewById(R.id.text_b2c);

        mTextIM = (TextView) findViewById(R.id.text_im);
        mTextTeaching = (TextView) findViewById(R.id.text_teaching);
        mTextSchool = (TextView) findViewById(R.id.text_school);
        mTextLife = (TextView) findViewById(R.id.text_life);
        mTextIMContainer = (LinearLayout) findViewById(R.id.text_im_container);
        mTextTeachingContainer = (LinearLayout) findViewById(R.id.text_teaching_container);
        mTextSchoolContainer = (LinearLayout) findViewById(R.id.text_school_container);
        mTextLifeContainer = (LinearLayout) findViewById(R.id.text_life_container);
        mTextIMNum = (TextView) findViewById(R.id.text_im_num);
        mTextTeachingNum = (TextView) findViewById(R.id.text_teaching_num);
        mTextSchoolNum = (TextView) findViewById(R.id.text_school_num);
        mTextLifeNum = (TextView) findViewById(R.id.text_life_num);

        mTextMessage.setOnClickListener(this);
        mTextContacts.setOnClickListener(this);
        mTextCommonContacts.setOnClickListener(this);
        mTextSchedule.setOnClickListener(this);
        mTextClassroom.setOnClickListener(this);
        mTextScore.setOnClickListener(this);
        mTextMeeting.setOnClickListener(this);
        mTextNotice.setOnClickListener(this);
        mTextNews.setOnClickListener(this);
        mTextUtility.setOnClickListener(this);
        mTextB2C.setOnClickListener(this);
        mTextIM.setOnClickListener(this);
        mTextTeaching.setOnClickListener(this);
        mTextSchool.setOnClickListener(this);
        mTextLife.setOnClickListener(this);

        mMovingBlock = (ImageView) findViewById(R.id.image_moving_block);

        mHandler = new Handler();
        // 当前TextView默认为第一个
        mCurTxt = mTextMessage;
        // 设置默认位置的字体颜色为白色的选中效果
        mTextMessage.setTextColor(getResources().getColor(R.color.white));
        // 获取手机屏幕宽度
        mScreenWidth = getWindowManager().getDefaultDisplay().getWidth();

        initMenu(mCurIndex);
        initMainPages(mCurIndex);
        // 显示其他页面的未读消息
        for (int i = 0; i < mCntArray.length; i++) {
            showCircle(i, mCntArray[i]);
        }

        // 当前index的页面被加载，消息同时被加载，认为消息已读并清除该页的新消息提示
        // 聊天消息和新闻不同，虽然也会在初始化界面的时候被清空，但是会从数据库得到未读消息条数
        // 后以广播通知更新number circle
        hideCircle();
        if (mCurSubIndex != -1) {
            gotoSecondPage(mCurSubIndex);
        }
    }

    private void gotoSecondPage(int subIndex) {
        switch (mCurIndex) {
        case INDEX_IM:
            switch (subIndex) {
            case SUB_INDEX_MESSAGE:
                mTextMessage.performClick();
                break;
            case SUB_INDEX_CONTACTS:
                mTextContacts.performClick();
                break;
            case SUB_INDEX_COMMON_CONTACTS:
                mTextCommonContacts.performClick();
                break;
            }

            break;
        case INDEX_TEACHING:
            switch (subIndex) {
            case SUB_INDEX_SCHEDULE:
                mTextSchedule.performClick();
                break;
            case SUB_INDEX_CLASSROOM:
                mTextClassroom.performClick();
                break;
            case SUB_INDEX_SCORE:
                mTextScore.performClick();
                break;
            }

            break;
        case INDEX_SCHOOL:
            switch (subIndex) {
            case SUB_INDEX_NEWS:
                mTextNews.performClick();
                break;
            case SUB_INDEX_MEETING:
                mTextMeeting.performClick();
                break;
            case SUB_INDEX_NOTICE:
                mTextNotice.performClick();
                break;
            }

            break;
        case INDEX_LIFE:
            switch (subIndex) {
            case SUB_INDEX_UTILITY:
                mTextUtility.performClick();
                break;
            case SUB_INDEX_B2C:
                mTextB2C.performClick();
                break;
            }

            break;
        default:
            break;
        }
    }

    private void initMainPages(int index) {

        switch (index) {
        case INDEX_IM:
            mFragmentsList.add(new IMMessageFragment());
            mFragmentsList.add(new IMContactFragment());
            mFragmentsList.add(new IMConventionalFragment());

            break;
        case INDEX_TEACHING:
            mFragmentsList.add(TestFragment.newInstance("课程表"));
            mFragmentsList.add(TestFragment.newInstance("教师管理"));
            mFragmentsList.add(TestFragment.newInstance("成绩管理"));

            break;
        case INDEX_SCHOOL:
            mFragmentsList.add(new SchoolNewsFragment());
            mFragmentsList.add(TestFragment.newInstance("会议"));
            mFragmentsList.add(TestFragment.newInstance("通知"));

            break;
        case INDEX_LIFE:
            mFragmentsList.add(TestFragment.newInstance("工具"));
            mFragmentsList.add(TestFragment.newInstance("在线商城"));

            break;
        default:
            break;
        }

        mAdapter = new MyFragmentPagerAdapter(getSupportFragmentManager());
        mContainer.setAdapter(mAdapter);
        mContainer.setCurrentItem(0);
        // 保存最多三个页面，ViewPager默认是保存当前可见页的前后两个页面，这样一旦切换到上下文
        // 以外的页面，再次进入就会从新刷新数据
        mContainer.setOffscreenPageLimit(SUB_INDEX_MAX_CNT);
        mContainer.setOnPageChangeListener(this);

    }

    private void switchPage(int index) {
        finish();
        Intent intent = new Intent(mContext, MainActivity.class);
        intent.putExtra(EXTRA_INDEX_TYPE, index);
        // 把当前保存的各个页面的信息数传递下去
        intent.putExtra(EXTRA_NEW_MSG_CNT, mCntArray);
        // intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    private void initMenu(int index) {
        mCurIndex = index;
        switch (index) {
        case INDEX_IM:
            mTextMessage.setVisibility(View.VISIBLE);
            mTextContacts.setVisibility(View.VISIBLE);
            mTextCommonContacts.setVisibility(View.VISIBLE);
            mTextSchedule.setVisibility(View.GONE);
            mTextClassroom.setVisibility(View.GONE);
            mTextScore.setVisibility(View.GONE);
            mTextMeeting.setVisibility(View.GONE);
            mTextNotice.setVisibility(View.GONE);
            mTextNews.setVisibility(View.GONE);
            mTextUtility.setVisibility(View.GONE);
            mTextB2C.setVisibility(View.GONE);

            mTextIMContainer.setBackgroundColor(mContext.getResources().getColor(R.color.orange));
            mMaxSubPageCount = 3;
            break;
        case INDEX_TEACHING:
            mTextMessage.setVisibility(View.GONE);
            mTextContacts.setVisibility(View.GONE);
            mTextCommonContacts.setVisibility(View.GONE);
            mTextSchedule.setVisibility(View.VISIBLE);
            mTextClassroom.setVisibility(View.VISIBLE);
            mTextScore.setVisibility(View.VISIBLE);
            mTextMeeting.setVisibility(View.GONE);
            mTextNotice.setVisibility(View.GONE);
            mTextNews.setVisibility(View.GONE);
            mTextUtility.setVisibility(View.GONE);
            mTextB2C.setVisibility(View.GONE);

            mTextTeachingContainer.setBackgroundColor(mContext.getResources().getColor(
                    R.color.orange));
            mMaxSubPageCount = 3;
            break;
        case INDEX_SCHOOL:
            mTextMessage.setVisibility(View.GONE);
            mTextContacts.setVisibility(View.GONE);
            mTextCommonContacts.setVisibility(View.GONE);
            mTextSchedule.setVisibility(View.GONE);
            mTextClassroom.setVisibility(View.GONE);
            mTextScore.setVisibility(View.GONE);
            mTextMeeting.setVisibility(View.VISIBLE);
            mTextNotice.setVisibility(View.VISIBLE);
            mTextNews.setVisibility(View.VISIBLE);
            mTextUtility.setVisibility(View.GONE);
            mTextB2C.setVisibility(View.GONE);

            mTextSchoolContainer.setBackgroundColor(mContext.getResources()
                    .getColor(R.color.orange));
            mMaxSubPageCount = 3;
            break;
        case INDEX_LIFE:
            mTextMessage.setVisibility(View.GONE);
            mTextContacts.setVisibility(View.GONE);
            mTextCommonContacts.setVisibility(View.GONE);
            mTextSchedule.setVisibility(View.GONE);
            mTextClassroom.setVisibility(View.GONE);
            mTextScore.setVisibility(View.GONE);
            mTextMeeting.setVisibility(View.GONE);
            mTextNotice.setVisibility(View.GONE);
            mTextNews.setVisibility(View.GONE);
            mTextUtility.setVisibility(View.VISIBLE);
            mTextB2C.setVisibility(View.VISIBLE);

            mTextLifeContainer.setBackgroundColor(mContext.getResources().getColor(R.color.orange));
            mMaxSubPageCount = 2;
            break;
        default:
            break;
        }

        // 初始化完成之前 width无法正常获取到
        mHandler.postDelayed(new Runnable() {
            public void run() {
                if (msecondMenuContainer.getWidth() > mScreenWidth) {
                    // 如果second menu内容超出屏幕距离，设置其布局gravity属性为LEFT
                    FrameLayout.LayoutParams params = (LayoutParams) msecondMenuContainer
                            .getLayoutParams();
                    params.gravity = Gravity.LEFT;
                    // 需要重新设置布局
                    msecondMenuContainer.setLayoutParams(params);
                }
            }
        }, 100);
    }

    /**
     * 移动标签页的指示条，并且在都动画结束后切换到对应的子页面
     */
    private void imgTrans(final TextView endTxt, final int position) {
        // 当前TextView的中心点
        int startMid = mCurTxt.getLeft() + mCurTxt.getWidth() / 2;
        // 移动开始位置左边缘
        int startLeft = startMid - mMovingBlock.getWidth() / 2;
        // 目的TextView的中心点
        int endMid = endTxt.getLeft() + endTxt.getWidth() / 2;
        // 移动结束位置左边缘
        int endLeft = endMid - mMovingBlock.getWidth() / 2;
        // 构造动画

        TranslateAnimation move = new TranslateAnimation(startLeft, endLeft, 0, 0);
        move.setAnimationListener(new AnimationListener() {
            public void onAnimationStart(Animation arg0) {

            }

            public void onAnimationRepeat(Animation arg0) {
            }

            public void onAnimationEnd(Animation arg0) {
                // 切换字体颜色
                mCurTxt.setTextColor(getResources().getColor(R.color.gray));
                endTxt.setTextColor(getResources().getColor(R.color.white));

                // 更新当前TextView的记录
                mCurTxt = endTxt;
                mContainer.setCurrentItem(position);
            }
        });
        move.setDuration(100);
        move.setFillAfter(true);
        mMovingBlock.startAnimation(move);

        /*
         * 以下步骤用于处理ScrollView根据滑块的位置来调整自身的滚动, 以便达到更好的视觉效果
         */
        int[] location = new int[2];
        // 获取目的TextViw在当前屏幕中的坐标点,主要用到X轴方向坐标:location[0]
        endTxt.getLocationOnScreen(location);
        // 调整ScrollView的位置
        if (location[0] < 0) {
            // 目的位置超出左边屏幕,则调整到紧靠该位置的左边
            // 此处ScrollView直接根据位置点滑动
            if (position > 0) { // 左边还有子页存在，多移动一格
                msecondMenu.smoothScrollTo(endTxt.getLeft() - mMovingBlock.getWidth(), 0);
            } else {
                msecondMenu.smoothScrollTo(endTxt.getLeft(), 0);
            }
        } else if ((location[0] + endTxt.getWidth()) > mScreenWidth) {
            // 目的位置超出右边屏幕,则调整到紧靠该位置的右边
            // 此处ScrollView需计算滑动距离
            if (position < mMaxSubPageCount - 1) { // 右边还有子页存在，多移动一格
                msecondMenu.smoothScrollBy(location[0] + endTxt.getWidth() - mScreenWidth + mMovingBlock.getWidth(), 0);
            } else {
                msecondMenu.smoothScrollBy(location[0] + endTxt.getWidth() - mScreenWidth, 0);
            }
        } else {
            // 此处如果没超出屏幕,也需保持原地滑动
            // 如果不加该效果,则滑块可能出现动画延迟或停滞
            msecondMenu.smoothScrollTo(msecondMenu.getScrollX(), 0);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
        case R.id.text_message:
            imgTrans(mTextMessage, SUB_INDEX_MESSAGE);
            break;
        case R.id.text_contacts:
            imgTrans(mTextContacts, SUB_INDEX_CONTACTS);
            break;
        case R.id.text_common_contacts:
            imgTrans(mTextCommonContacts, SUB_INDEX_COMMON_CONTACTS);
            break;

        case R.id.text_schedule:
            imgTrans(mTextSchedule, SUB_INDEX_SCHEDULE);
            break;
        case R.id.text_classroom:
            imgTrans(mTextClassroom, SUB_INDEX_CLASSROOM);
            break;
        case R.id.text_score:
            imgTrans(mTextScore, SUB_INDEX_SCORE);
            break;

        case R.id.text_news:
            imgTrans(mTextNews, SUB_INDEX_NEWS);
            break;
        case R.id.text_meeting:
            imgTrans(mTextMeeting, SUB_INDEX_MEETING);
            break;
        case R.id.text_notice:
            imgTrans(mTextNotice, SUB_INDEX_NOTICE);
            break;

        case R.id.text_utility:
            imgTrans(mTextUtility, SUB_INDEX_UTILITY);
            break;
        case R.id.text_b2c:
            imgTrans(mTextB2C, SUB_INDEX_B2C);
            break;

        case R.id.text_im:
            switchPage(INDEX_IM);
            break;
        case R.id.text_teaching:
            switchPage(INDEX_TEACHING);
            break;
        case R.id.text_school:
            switchPage(INDEX_SCHOOL);
            break;
        case R.id.text_life:
            switchPage(INDEX_LIFE);
            break;
        default:
            break;
        }

    }

    /**
     * 更新底部菜单的number circle未查看信息条目的通知
     */
    private void showCircle(int index, int num) {
        if (num < 1) {
            return;
        }
        
        switch (index) {
        case INDEX_IM:
            mTextIMNum.setVisibility(View.VISIBLE);
            mTextIMNum.setText(String.valueOf(num));
            break;
            
        case INDEX_TEACHING:
            mTextTeachingNum.setVisibility(View.VISIBLE);
            mTextTeachingNum.setText(String.valueOf(num));

            break;
        case INDEX_SCHOOL:
            mTextSchoolNum.setVisibility(View.VISIBLE);
            mTextSchoolNum.setText(String.valueOf(num));

            break;
        case INDEX_LIFE:
            mTextLifeNum.setVisibility(View.VISIBLE);
            mTextLifeNum.setText(String.valueOf(num));

            break;
        default:
            break;
        }
    }

    /**
     * 删除当前页面的新消息提示，在每次进入页面的时候调用
     */
    private void hideCircle() {
        mCntArray[mCurIndex] = 0;
        switch (mCurIndex) {
        case INDEX_IM:
            mTextIMNum.setVisibility(View.GONE);
            break;
        case INDEX_TEACHING:
            mTextTeachingNum.setVisibility(View.GONE);
            break;
        case INDEX_SCHOOL:
            mTextSchoolNum.setVisibility(View.GONE);
            break;
        case INDEX_LIFE:
            mTextLifeNum.setVisibility(View.GONE);
            break;
        default:
            break;
        }
    }

    class MyFragmentPagerAdapter extends FragmentStatePagerAdapter {
        FragmentManager fm;

        public MyFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
            this.fm = fm;
        }

        @Override
        public int getCount() {
            return mFragmentsList.size();
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentsList.get(position);
        }

    }

    @Override
    public void onPageScrollStateChanged(int arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onPageSelected(int position) {
        gotoSecondPage(position);
    }

    @Override
    public void onBackPressed() {
        // 不屏蔽用户的back 键也不返回到Login画面，直接弹出所有Activity
        popAllActivity();
    }
}
