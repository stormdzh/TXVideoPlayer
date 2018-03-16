package videoplayer.stromdzh.com.myalivideoplayer;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.rtmp.ITXVodPlayListener;
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXVodPlayer;
import com.tencent.rtmp.ui.TXCloudVideoView;

import java.lang.ref.WeakReference;

/**
 * author : dzh .
 * date   : 2018/1/26
 * desc   : 视屏播放器
 */
public class TxVideoPlayer extends RelativeLayout implements View.OnClickListener, ITXVodPlayListener {

    private static final String TAG = TxVideoPlayer.class.getSimpleName();

//    private final int CODE_VIDEO_PROGRESS = 0; //更新进度条
    private final int CODE_VIDEO_AUTO_HIDE = 1; //自动隐藏控制控件
    private static final int TIME_AUTO_HIDE_BARS_DELAY = 3800;
    private int iconPause = R.drawable.zz_player_pause;
    private int iconPlay = R.drawable.zz_player_play;
    int iconShrink = R.drawable.zz_player_shrink;
    int iconExpand = R.drawable.zz_player_expand;
    private Animation mEnterFromTop;
    private Animation mEnterFromBottom;
    private Animation mExitFromTop;
    private Animation mExitFromBottom;

    private ImageView mLoadingView;
    private TXVodPlayer mPlayer;
    private TXCloudVideoView mVideoView;
    private TextView positionTxt;
    private TextView durationTxt;
    private CustomSeekBar progressBar;
    private RelativeLayout rl_play_pause;
    private ImageView iv_play_pause;
    private RelativeLayout rl_toggle_expandable;
    private ImageView iv_toggle_expandable;
    private RelativeLayout mController;
    private LinearLayout mTitleBar;
    private RelativeLayout rl_back;
    private TextView tv_title;
    private String mUrl = null;
    private boolean inSeek = false;
    private boolean isCompleted = false;
    private WeakReference<Activity> mHostActivity;
    private int mLastPlayingPos = -1;//onPause时的播放位置
    private int mDuration;
    private boolean isActivityStop = false;
    private IPlayerImpl mIPlayerImpl;
    private boolean mShowVerticalTitleBar = false;

    public TxVideoPlayer(Context context) {
        this(context, null);
    }

    public TxVideoPlayer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TxVideoPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 初始化
     */
    private void init() {
        inflate(getContext(), R.layout.view_tx_player, this);
        mVideoView = findViewById(R.id.video_view);
        positionTxt = findViewById(R.id.tv_current_time);
        durationTxt = findViewById(R.id.tv_total_time);
        progressBar = findViewById(R.id.csb);
        rl_play_pause = findViewById(R.id.rl_play_pause);
        rl_play_pause.setOnClickListener(this);
        iv_play_pause = findViewById(R.id.iv_play_pause);
        rl_toggle_expandable = findViewById(R.id.rl_toggle_expandable);
        rl_toggle_expandable.setOnClickListener(this);
        iv_toggle_expandable = findViewById(R.id.iv_toggle_expandable);
        mController = findViewById(R.id.mController);
        mTitleBar = findViewById(R.id.ll_video_title);
        rl_back = findViewById(R.id.rl_back);
        rl_back.setOnClickListener(this);
        tv_title = findViewById(R.id.tv_title);
        mLoadingView = findViewById(R.id.loadingImageView);
        mVideoView.setOnClickListener(this);

//        mPhoneListener = new TXPhoneStateListener(getContext(), mPlayer);
//        mPhoneListener.startListen();

        initAnimation();
        initSeekBar();
        initVodPlayer();
    }


    /**
     * 初始化标题栏/控制栏显隐动画效果
     */
    private void initAnimation() {
        mEnterFromTop = AnimationUtils.loadAnimation(getContext(), R.anim.enter_from_top);
        mEnterFromBottom = AnimationUtils.loadAnimation(getContext(), R.anim.enter_from_bottom);
        mExitFromTop = AnimationUtils.loadAnimation(getContext(), R.anim.exit_from_top);
        mExitFromBottom = AnimationUtils.loadAnimation(getContext(), R.anim.exit_from_bottom);

        mEnterFromTop.setAnimationListener(new AnimationImpl() {
            @Override
            public void onAnimationEnd(Animation animation) {
                super.onAnimationEnd(animation);
                mTitleBar.setVisibility(VISIBLE);
            }
        });
        mEnterFromBottom.setAnimationListener(new AnimationImpl() {
            @Override
            public void onAnimationEnd(Animation animation) {
                super.onAnimationEnd(animation);
                mController.setVisibility(VISIBLE);
            }
        });
        mExitFromTop.setAnimationListener(new AnimationImpl() {
            @Override
            public void onAnimationEnd(Animation animation) {
                super.onAnimationEnd(animation);
                mTitleBar.setVisibility(GONE);
            }
        });
        mExitFromBottom.setAnimationListener(new AnimationImpl() {
            @Override
            public void onAnimationEnd(Animation animation) {
                super.onAnimationEnd(animation);
                mController.setVisibility(GONE);
            }
        });
    }

    /**
     * 初始化进度
     */
    private void initSeekBar() {

        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mPlayer != null) {
                    mPlayer.seek(seekBar.getProgress() / 1000.f);
                    if (isCompleted) {
                        inSeek = false;
                    } else {
                        inSeek = true;
                    }
                }
            }
        });
    }

    /**
     * 初始化视屏播放器
     */
    private void initVodPlayer() {
        //创建player对象
        mPlayer = new TXVodPlayer(getContext());
        //关键player对象与界面view
        mPlayer.setPlayerView(mVideoView);
        mPlayer.setVodListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //播放暂停
            case R.id.rl_play_pause:
                isCompleted = false;
                inSeek = false;
                if (mPlayer.isPlaying()) {
                    pause();
                    setPlayState(PlayState.PAUSE);
                } else {
                    resume();
                }
                break;
            //切换全屏
            case R.id.rl_toggle_expandable:
                OrientationUtil.changeOrientation(mHostActivity.get());
                break;
            //视屏上面的返回
            case R.id.rl_back:
                if (mIPlayerImpl != null) {
                    mIPlayerImpl.onBack();
                } else {
                    if (mHostActivity.get() != null)
                        mHostActivity.get().finish();
                }
                break;
            //点击播放控件
            case R.id.video_view:
                if (mController.getVisibility() == VISIBLE) {
                    showOrHideBars(false, true);
                } else {
                    showOrHideBars(true, true);
                }
                break;
        }
    }


    /**
     * 显隐标题栏和控制条
     *
     * @param show          是否显示
     * @param animateEffect 是否需要动画效果
     */
    private void showOrHideBars(boolean show, boolean animateEffect) {
        if (animateEffect) {
            animateShowOrHideBars(show);
        } else {
            forceShowOrHideBars(show);
        }
    }

    /**
     * 带动画效果的显隐标题栏和控制栏
     */
    private void animateShowOrHideBars(boolean show) {
        mController.clearAnimation();
        mTitleBar.clearAnimation();

        if (show) {
            if (mController.getVisibility() != VISIBLE) {
                if (isVerticalShow())
                    mTitleBar.startAnimation(mEnterFromTop);
                mController.startAnimation(mEnterFromBottom);
            }
            sendAutoHideBarsMsg();
        } else {
            if (mController.getVisibility() != GONE) {
                if (isVerticalShow())
                    mTitleBar.startAnimation(mExitFromTop);
                else
                    mTitleBar.setVisibility(GONE);
                mController.startAnimation(mExitFromBottom);
            }
        }
    }

    /**
     * 竖屏时候是否需要显示标题栏
     *
     * @return 返回true 需要显示 false：不需要显示
     */
    private boolean isVerticalShow() {
        Activity activity = mHostActivity.get();
        if (activity == null) return false;
        int orientation = OrientationUtil.getOrientation(mHostActivity.get());
        if (orientation == OrientationUtil.VERTICAL) {
            return mShowVerticalTitleBar;
        } else
            return true;
    }

    /**
     * 直接显隐标题栏和控制栏
     */
    private void forceShowOrHideBars(boolean show) {
        mTitleBar.clearAnimation();
        mController.clearAnimation();

        if (show) {
            mController.setVisibility(VISIBLE);
            if (isVerticalShow())
                mTitleBar.setVisibility(VISIBLE);
            else
                mTitleBar.setVisibility(GONE);
        } else {
            mController.setVisibility(GONE);
            mTitleBar.setVisibility(GONE);
        }
    }


    private void resume() {
        if (mPlayer != null) {
            mPlayer.resume();
            setPlayState(PlayState.PLAY);
        }
    }

    /**
     * 发送message给handler,自动隐藏标题栏
     */
    private void sendAutoHideBarsMsg() {
        //  初始自动隐藏标题栏和控制栏
        mHanlder.removeMessages(CODE_VIDEO_AUTO_HIDE);
        mHanlder.sendEmptyMessageDelayed(CODE_VIDEO_AUTO_HIDE, TIME_AUTO_HIDE_BARS_DELAY);
    }

    /**
     * 设置播放按钮的状态
     *
     * @param curPlayState 是不是播放状态
     */
    public void setPlayState(int curPlayState) {

        switch (curPlayState) {
            case PlayState.PLAY:
                iv_play_pause.setImageResource(iconPause);
                break;
            case PlayState.PAUSE:
            case PlayState.STOP:
            case PlayState.COMPLETE:
            case PlayState.ERROR:
                iv_play_pause.setImageResource(iconPlay);
                break;
        }
    }

    private void onCompleted() {
        mLastPlayingPos = 0;
        isCompleted = true;
        showVideoProgressInfo(0);
//        stopUpdateTimer();
        if (mIPlayerImpl != null) {
            mIPlayerImpl.onComplete();
        }
    }

    private void onError() {
        pause();
        if (mIPlayerImpl != null) {
            mIPlayerImpl.onError();
        }
    }

    private void pause() {
        if (mPlayer != null) {
            mPlayer.pause();
            setPlayState(PlayState.ERROR);
        }
    }

//    private void startUpdateTimer() {
//        mHanlder.removeMessages(CODE_VIDEO_PROGRESS);
//        mHanlder.sendEmptyMessageDelayed(CODE_VIDEO_PROGRESS, 1000);
//    }

//    private void stopUpdateTimer() {
//        mHanlder.removeMessages(CODE_VIDEO_PROGRESS);
//    }

    private Handler mHanlder = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
//                case CODE_VIDEO_PROGRESS:
//                    showVideoProgressInfo(mLastPlayingPos);
//                    break;
                case CODE_VIDEO_AUTO_HIDE:
                    animateShowOrHideBars(false);
                    break;
                default:
                    break;
            }

        }
    };

    public void updateActivityOrientation() {
        int orientation = OrientationUtil.getOrientation(mHostActivity.get());

        //更新播放器宽高
        float width = DensityUtil.getWidthInPx(mHostActivity.get());
        float height = DensityUtil.getHeightInPx(mHostActivity.get());
        if (orientation == OrientationUtil.HORIZONTAL) {
            getLayoutParams().height = (int) height;
            getLayoutParams().width = (int) width;
        } else {
            width = DensityUtil.getWidthInPx(mHostActivity.get());
            height = DensityUtil.dip2px(mHostActivity.get(), 200f);
        }
        getLayoutParams().height = (int) height;
        getLayoutParams().width = (int) width;

        //需要强制显示再隐藏控制条,不然若切换为横屏时控制条是隐藏的,首次触摸显示时,会显示在200dp的位置
        forceShowOrHideBars(true);
        sendAutoHideBarsMsg();
        //更新全屏图标
        setOrientation(orientation);
    }

    public void setOrientation(int orientation) {
        //更新全屏图标
        if (orientation == OrientationUtil.HORIZONTAL) {
            iv_toggle_expandable.setImageResource(iconShrink);
        } else {
            iv_toggle_expandable.setImageResource(iconExpand);
        }
    }

    /**
     * 播放器控制功能对外开放接口,包括返回按钮,播放等...
     */
    public void setPlayerController(IPlayerImpl IPlayerImpl) {
        mIPlayerImpl = IPlayerImpl;
    }


    /**
     * 设置视屏标题
     *
     * @param title 标题
     */
    public void setTitle(String title) {
        if (tv_title != null)
            tv_title.setText(title);
    }

    /**
     * 设置视屏播放地址
     *
     * @param url 播放地址
     */
    public void setUrl(String url) {
        this.mUrl = url;
        start();
    }

    /**
     * 在竖屏时候是否需要标题栏
     */
    public void showTitleBarVertical(boolean showVerticalTitleBar) {
        mShowVerticalTitleBar = showVerticalTitleBar;
    }

    public void setmHostActivity(Activity activity) {
        mHostActivity = new WeakReference<Activity>(activity);
    }

    private void start() {
        if (mPlayer != null) {
            mPlayer.startPlay(mUrl);
            setPlayState(PlayState.PLAY);
        }
    }

    /**
     * 宿主页面onResume的时候从上次播放位置继续播放
     */
    public void onHostResume() {

        if (isActivityStop) {
            mPlayer.resume();
            isActivityStop = false;
        }
        //强制弹出标题栏和控制栏
        forceShowOrHideBars(true);
        sendAutoHideBarsMsg();
    }

    /**
     * 宿主页面onPause的时候记录播放位置，好在onResume的时候从中断点继续播放
     * 如果在宿主页面onStop的时候才来记录位置,则取到的都会是0
     */
    public void onHostPause() {
        if (mPlayer != null && mPlayer.isPlaying())
            isActivityStop = true;
//        stopUpdateTimer();
//        mHanlder.removeMessages(CODE_VIDEO_PROGRESS);
        mHanlder.removeMessages(CODE_VIDEO_AUTO_HIDE);
        // 在这里不进行stop或者pause播放的行为，因为特殊情况下会导致ANR出现
    }


    /**
     * 宿主页面destroy的时候页面恢复成竖直状态
     */
    public void onHostDestroy() {
        OrientationUtil.forceOrientation(mHostActivity.get(), OrientationUtil.VERTICAL);
//        if (mPhoneListener != null)
//            mPhoneListener.stopListen();
    }


    private void startLoadingAnimation() {
        if (mLoadingView != null) {
            mLoadingView.setVisibility(View.VISIBLE);
            ((AnimationDrawable) mLoadingView.getDrawable()).start();
        }
    }

    private void stopLoadingAnimation() {
        if (mLoadingView != null) {
            mLoadingView.setVisibility(View.GONE);
            ((AnimationDrawable) mLoadingView.getDrawable()).stop();
        }
    }

    private void showVideoProgressInfo(int curPosition) {
        if ((mPlayer.isPlaying())
                && !inSeek) {
            positionTxt.setText(Formatter.formatTime(curPosition));
            durationTxt.setText(Formatter.formatTime(mDuration));
            progressBar.setMax(mDuration);
            progressBar.setProgress(curPosition);
        }
        mLastPlayingPos = curPosition;
//        startUpdateTimer();
    }

    /**
     * 电话监听
     */
    /*static class TXPhoneStateListener extends PhoneStateListener implements Application.ActivityLifecycleCallbacks {
        WeakReference<TXVodPlayer> mPlayer;
        Context mContext;
        int activityCount;

        public TXPhoneStateListener(Context context, TXVodPlayer player) {
            mPlayer = new WeakReference<>(player);
            mContext = context.getApplicationContext();
        }

        public void startListen() {
//            TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Service.TELEPHONY_SERVICE);
//            tm.listen(this, PhoneStateListener.LISTEN_CALL_STATE);
//            MyApplication.getApplication().registerActivityLifecycleCallbacks(this);
        }

        public void stopListen() {
            TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Service.TELEPHONY_SERVICE);
            tm.listen(this, PhoneStateListener.LISTEN_NONE);

            MyApplication.getApplication().unregisterActivityLifecycleCallbacks(this);
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            TXVodPlayer player = mPlayer.get();
            switch (state) {
                //电话等待接听
                case TelephonyManager.CALL_STATE_RINGING:
                    Log.d(TAG, "CALL_STATE_RINGING");
                    if (player != null) player.pause();
                    break;
                //电话接听
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    Log.d(TAG, "CALL_STATE_OFFHOOK");
                    if (player != null) player.pause();
                    break;
                //电话挂机
                case TelephonyManager.CALL_STATE_IDLE:
                    Log.d(TAG, "CALL_STATE_IDLE");
                    if (player != null && activityCount >= 0) player.resume();
                    break;
            }
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

        }

        @Override
        public void onActivityPaused(Activity activity) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {

        }

        @Override
        public void onActivityResumed(Activity activity) {
            activityCount++;
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityStarted(Activity activity) {

        }

        @Override
        public void onActivityStopped(Activity activity) {
            activityCount--;
        }

        boolean isInBackground() {
            return (activityCount < 0);
        }
    }*/

//    private TXPhoneStateListener mPhoneListener = null;

    //以下是视屏监听

    @Override
    public void onPlayEvent(TXVodPlayer player, int event, Bundle param) {
        String playEventLog = "receive event: " + event + ", " + param.getString(TXLiveConstants.EVT_DESCRIPTION);
        if (event == TXLiveConstants.PLAY_EVT_PLAY_BEGIN) { //开始播放
            stopLoadingAnimation();
            setPlayState(PlayState.PLAY);
//            if (mPhoneListener.isInBackground()) {
//                mPlayer.pause();
//            }
        } else if (event == TXLiveConstants.PLAY_EVT_PLAY_PROGRESS) { //播放进度条
            mLastPlayingPos = param.getInt(TXLiveConstants.EVT_PLAY_PROGRESS_MS);
            mDuration = param.getInt(TXLiveConstants.EVT_PLAY_DURATION_MS);
            inSeek = false;
            showVideoProgressInfo(mLastPlayingPos);
        } else if (event == TXLiveConstants.PLAY_EVT_PLAY_END) {
            onCompleted();

        } else if (event == TXLiveConstants.PLAY_ERR_NET_DISCONNECT || event == TXLiveConstants.PLAY_ERR_FILE_NOT_FOUND) {
            onError();
            showVideoProgressInfo(0);
        } else if (event == TXLiveConstants.PLAY_EVT_PLAY_LOADING) {
            startLoadingAnimation();
        } else if (event == TXLiveConstants.PLAY_EVT_RCV_FIRST_I_FRAME) {
            stopLoadingAnimation();
            pause();
        } else if (event == TXLiveConstants.PLAY_EVT_CHANGE_RESOLUTION) {
        } else if (event == TXLiveConstants.PLAY_ERR_HLS_KEY) {
        } else if (event == TXLiveConstants.PLAY_WARNING_RECONNECT) {
            startLoadingAnimation();
        } else if (event == TXLiveConstants.PLAY_EVT_CHANGE_ROTATION) {
            return;
        }
        if (event < 0) {
            Toast.makeText(getContext().getApplicationContext(), param.getString(TXLiveConstants.EVT_DESCRIPTION), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onNetStatus(TXVodPlayer txVodPlayer, Bundle bundle) {
    }
}
