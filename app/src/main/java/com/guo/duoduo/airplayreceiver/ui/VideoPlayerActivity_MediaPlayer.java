package com.guo.duoduo.airplayreceiver.ui;


import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.guo.duoduo.airplayreceiver.MyController;
import com.guo.duoduo.airplayreceiver.R;
import com.guo.duoduo.airplayreceiver.constant.Constant;

import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnBufferingUpdateListener;
import io.vov.vitamio.widget.VideoView;


/**
 * Created by Guo.Duo duo on 2015/8/25.
 */
public class VideoPlayerActivity_MediaPlayer extends Activity
    implements
        OnBufferingUpdateListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnVideoSizeChangedListener,
        SurfaceHolder.Callback
{

    private static final String tag = "VideoPlayerActivity";
    private static volatile long duration = 0;
    private static volatile long curPosition = 0;
    private static volatile boolean isVideoActivityFinished = false;
    private int mVideoWidth;
    private int mVideoHeight;
    private MediaPlayer mMediaPlayer;
    private SurfaceView mPreview;
    private SurfaceHolder holder;
    private String mPath;
    private double position;
    private boolean mIsVideoSizeKnown = false;
    private boolean mIsVideoReadyToBePlayed = false;
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

        setContentView(R.layout.activity_video_mediaplayer);

        handler = new VideoHandler(this);
        controller = new MyController(VideoPlayerActivity.class.getName(), handler);

        mPreview = (SurfaceView) findViewById(R.id.surface);
        holder = mPreview.getHolder();
        holder.addCallback(this);
        holder.setFormat(PixelFormat.RGBA_8888);

        timer = new Timer();
        timerTask = new TimerTask()
        {
            @Override
            public void run()
            {
                if (mMediaPlayer != null && mMediaPlayer.isPlaying())
                {
                    duration = mMediaPlayer.getDuration();
                }
                else
                    duration = 0;
                if (mMediaPlayer != null && mMediaPlayer.isPlaying())
                    curPosition = mMediaPlayer.getCurrentPosition();
                else
                    curPosition = 0;
            }
        };
        timer.scheduleAtFixedRate(timerTask, 0, 1 * 1000);
    }

    private void playVideo()
    {
        doCleanUp();
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

            // Create a new media player and set the listeners
            mMediaPlayer = new MediaPlayer(this);
            mMediaPlayer.setDataSource(mPath);
            mMediaPlayer.setDisplay(holder);
            mMediaPlayer.prepare();
            mMediaPlayer.setOnBufferingUpdateListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnVideoSizeChangedListener(this);
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
        }
        catch (Exception e)
        {
            Log.e(tag, "error: " + e.getMessage(), e);
        }
    }

    public void onBufferingUpdate(MediaPlayer arg0, int percent)
    {
        Log.d(tag, "onBufferingUpdate percent:" + percent);
    }

    public void onCompletion(MediaPlayer arg0)
    {
        Log.d(tag, "onCompletion called");
    }

    public void onVideoSizeChanged(MediaPlayer mp, int width, int height)
    {
        Log.v(tag, "onVideoSizeChanged called");
        if (width == 0 || height == 0)
        {
            Log.e(tag, "invalid video width(" + width + ") or height(" + height + ")");
            return;
        }
        mIsVideoSizeKnown = true;
        mVideoWidth = width;
        mVideoHeight = height;
        if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown)
        {
            startVideoPlayback();
        }
    }

    public void onPrepared(MediaPlayer mediaplayer)
    {
        Log.d(tag, "onPrepared called");
        mIsVideoReadyToBePlayed = true;
        if (mIsVideoReadyToBePlayed && mIsVideoSizeKnown)
        {
            startVideoPlayback();
        }
    }

    public void surfaceChanged(SurfaceHolder surfaceholder, int i, int j, int k)
    {
        Log.d(tag, "surfaceChanged called");

    }

    public void surfaceDestroyed(SurfaceHolder surfaceholder)
    {
        Log.d(tag, "surfaceDestroyed called");
    }

    public void surfaceCreated(SurfaceHolder holder)
    {
        Log.d(tag, "surfaceCreated called");
        playVideo();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        isVideoActivityFinished = true;
        releaseMediaPlayer();
        doCleanUp();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Log.d(tag, "airplay VideoPlayerActivity onDestroy");
        releaseMediaPlayer();
        doCleanUp();
        if (controller != null)
            controller.destroy();

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

    private void releaseMediaPlayer()
    {
        if (mMediaPlayer != null)
        {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    private void doCleanUp()
    {
        mVideoWidth = 0;
        mVideoHeight = 0;
        mIsVideoReadyToBePlayed = false;
        mIsVideoSizeKnown = false;
    }

    private void startVideoPlayback()
    {
        Log.v(tag, "startVideoPlayback");
        holder.setFixedSize(mVideoWidth, mVideoHeight);
        mMediaPlayer.start();
        //实现端断点许序播
        long pos = (long) (mMediaPlayer.getDuration() * position);
        mMediaPlayer.seekTo(pos);
    }

    private static class VideoHandler extends Handler
    {
        private WeakReference<VideoPlayerActivity_MediaPlayer> weakReference;

        public VideoHandler(VideoPlayerActivity_MediaPlayer activity)
        {
            weakReference = new WeakReference<VideoPlayerActivity_MediaPlayer>(activity);
        }

        @Override
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);

            VideoPlayerActivity_MediaPlayer activity = weakReference.get();
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
                    activity.mMediaPlayer.seekTo(pos);
                    break;
                case Constant.Msg.Msg_Stop :
                    activity.mMediaPlayer.stop();
                    activity.releaseMediaPlayer();
                    activity.finish();
                    break;
                case Constant.Msg.Msg_Video_Pause :
                    if (activity.mMediaPlayer.isPlaying())
                        activity.mMediaPlayer.pause();
                    break;
                case Constant.Msg.Msg_Video_Resume :
                    if (!activity.mMediaPlayer.isPlaying())
                        activity.mMediaPlayer.start();
                    break;
            }

        }
    }
}
