package com.guo.duoduo.airplayreceiver.ui;


import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.guo.duoduo.airplayreceiver.MyController;
import com.guo.duoduo.airplayreceiver.R;
import com.guo.duoduo.airplayreceiver.constant.Constant;
import com.guo.duoduo.airplayreceiver.httpProcess.HttpProcess;
import com.guo.duoduo.airplayreceiver.httpcore.RequestListenerThread;


/**
 * Created by Guo.Duo duo on 2015/8/23.
 */
public class ImageActivity extends Activity
{
    private static final String tag = ImageActivity.class.getSimpleName();
    private ImageView iv;
    private MyController mController;
    private ImageHandler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_image);

        handler = new ImageHandler(ImageActivity.this);

        mController = new MyController(ImageActivity.class.getName(), handler);

        initView();
    }

    private void initView()
    {
        iv = (ImageView) findViewById(R.id.image_view);
        Intent intent = getIntent();
        if (intent != null)
        {
            byte[] pic = intent.getByteArrayExtra("picture");
            this.showImage(pic);
        }
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        if (intent != null)
        {
            byte[] pic = intent.getByteArrayExtra("picture");
            this.showImage(pic);
        }
    }

    private void showImage(byte[] pic)
    {
        //感谢  MilesChan 2015-09-18
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferQualityOverSpeed = true; //提升画质

        int size = (options.outWidth * options.outHeight);
        int size_limit = 1920 * 1080 * 4;
        if (size > 1920 * 1080 * 4)
        {
            int zoomRate = (int) Math.ceil(size * 1.0 / size_limit);
            if (zoomRate <= 0)
                zoomRate = 1;
            options.inSampleSize = zoomRate;
        }

        if (!Thread.currentThread().isInterrupted())
        {
            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeByteArray(pic, 0, pic.length, options);
            iv.setImageBitmap(bitmap);
        }
    }

    public void onBackPressed()
    {
        super.onBackPressed();
    }

    public void onDestroy()
    {
        super.onDestroy();
        Log.d(tag, "airplay ImageActivity onDestroy");
        RequestListenerThread.photoCacheMaps.clear();
        HttpProcess.photoCache.clear();
        mController.destroy();
    }

    private static class ImageHandler extends Handler
    {

        private WeakReference<ImageActivity> imageActivityWeakReference;

        public ImageHandler(ImageActivity activity)
        {
            imageActivityWeakReference = new WeakReference<ImageActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg)
        {
            final ImageActivity activity = this.imageActivityWeakReference.get();
            if (activity == null)
            {
                return;
            }
            if (activity.isFinishing())
                return;

            switch (msg.what)
            {
                case Constant.Msg.Msg_Stop :
                    activity.finish();
                    break;
                case Constant.Msg.Msg_Video_Play :
                    activity.finish();
                    break;
            }
        }
    }
}
