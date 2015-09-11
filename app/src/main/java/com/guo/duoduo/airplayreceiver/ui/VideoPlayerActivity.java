package com.guo.duoduo.airplayreceiver.ui;


import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.guo.duoduo.airplayreceiver.MyController;
import com.guo.duoduo.airplayreceiver.R;
import com.guo.duoduo.airplayreceiver.constant.Constant;

import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.widget.VideoView;


/**
 * Created by Guo.Duo duo on 2015/8/25.
 */
public class VideoPlayerActivity extends Activity
    implements
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener
{

    private static final String tag = "VideoPlayerActivity";
    private static volatile long duration = 0;
    private static volatile long curPosition = 0;
    private static volatile boolean isVideoActivityFinished = false;
    private VideoView mVideoView;
    private String mPath;
    private double position;
    private Handler handler;
    private MyController controller;
    private Timer timer;
    private TimerTask timerTask;

    public static boolean isVideoActivityFinished()
    {
        return isVideoActivityFinished;
    }

    public static long getDuration()
    {
        return duration;
    }

    public static long getCurrentPosition()
    {
        return curPosition;
    }

    /**
     *
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);

        isVideoActivityFinished = false;

        if (!LibsChecker.checkVitamioLibs(this))
            return;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_video);

        handler = new VideoHandler(this);
        controller = new MyController(VideoPlayerActivity.class.getName(), handler);

        mVideoView = (VideoView) findViewById(R.id.surface);

        playVideo();

        timer = new Timer();
        timerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                if (mVideoView != null && mVideoView.isPlaying())
                {
                    duration = mVideoView.getDuration();
                }
                else
                    duration = 0;
                if (mVideoView != null && mVideoView.isPlaying())
                    curPosition = mVideoView.getCurrentPosition();
                else
                    curPosition = 0;
            }
        };
        timer.scheduleAtFixedRate(timerTask, 0, 1 * 1000);
    }

    private void playVideo()
    {
        try
        {
            Intent intent = getIntent();
            if (intent == null)
            {
                return;
            }
            mPath = intent.getStringExtra("path");
            position = intent.getDoubleExtra("position", 0);
            Log.d(tag, "airplay path = " + mPath + "; position = " + position);

            mVideoView.setVideoPath(mPath);
            mVideoView.setOnCompletionListener(this);
            mVideoView.setVideoLayout(VideoView.VIDEO_LAYOUT_STRETCH, 0);
            mVideoView.requestFocus();
            mVideoView.start();
        }
        catch (Exception e)
        {
            Log.e(tag, "error: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (mVideoView != null)
            mVideoView.resume();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        isVideoActivityFinished = true;
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Log.d(tag, "airplay VideoPlayerActivity onDestroy");
        if (controller != null)
            controller.destroy();

        if (mVideoView != null)
            mVideoView.stopPlayback();

        if (timerTask != null)
        {
            timerTask.cancel();
            timerTask = null;
        }
        if (timer != null)
        {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp)
    {
        finish();
    }

    @Override
    public void onPrepared(MediaPlayer mp)
    {
        long pos = (long) (mVideoView.getDuration() * position);
        mVideoView.seekTo(pos);
    }

    private static class VideoHandler extends Handler
    {
        private WeakReference<VideoPlayerActivity> weakReference;

        public VideoHandler(VideoPlayerActivity activity)
        {
            weakReference = new WeakReference<VideoPlayerActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);

            VideoPlayerActivity activity = weakReference.get();
            if (activity == null)
                return;
            if (activity.isFinishing())
                return;

            switch (msg.what)
            {
                case Constant.Msg.Msg_Video_Seek :
                    float posFloat = (float) msg.obj;
                    long pos = Long.valueOf((long) (posFloat * 1000));
                    Log.d(tag, "airplay seek post = " + pos);
                    activity.mVideoView.seekTo(pos);
                    break;
                case Constant.Msg.Msg_Stop :
                    if (activity.mVideoView != null)
                        activity.mVideoView.stopPlayback();
                    activity.finish();
                    break;
                case Constant.Msg.Msg_Video_Pause :
                    if (activity.mVideoView.isPlaying())
                        activity.mVideoView.pause();
                    break;
                case Constant.Msg.Msg_Video_Resume :
                    if (!activity.mVideoView.isPlaying())
                        activity.mVideoView.start();
                    break;
                case Constant.Msg.Msg_Photo :
                    activity.finish();
                    break;
            }

        }
    }
}
