package com.guo.duoduo.airplayreceiver.ui;


import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.guo.duoduo.airplayreceiver.R;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;


/**
 * Created by Guo.Duoduo on 2015/8/25.
 */
public class VideoPlayerActivity extends Activity
    implements
        MediaPlayer.OnCompletionListener
{

    private static final String tag = VideoPlayerActivity.class.getSimpleName();
    private String mPath;
    private VideoView mVideoView;
    private ImageView mOperationPercent;
    private double position;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_video);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        Intent intent = getIntent();
        mPath = intent.getStringExtra("path");
        position = intent.getDoubleExtra("position", 0);
        Log.d(tag, "airplay path = " + mPath + "; position = " + position);
        initView();
    }

    private void initView()
    {
        mVideoView = (VideoView) findViewById(R.id.surface_view);

        mVideoView.setVideoPath(mPath);
        mVideoView.setMediaController(new MediaController(this));
        mVideoView.setOnCompletionListener(this);
        mVideoView.requestFocus();
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setPlaybackSpeed(1.0f);
            }
        });

    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if (mVideoView != null)
            mVideoView.pause();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (mVideoView != null)
            mVideoView.resume();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (mVideoView != null)
            mVideoView.stopPlayback();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer)
    {
        finish();
    }
}
