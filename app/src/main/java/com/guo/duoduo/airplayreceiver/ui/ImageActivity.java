package com.guo.duoduo.airplayreceiver.ui;


import java.io.ByteArrayInputStream;
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
        ByteArrayInputStream bin = new ByteArrayInputStream(pic);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferQualityOverSpeed = true; //提升画质
        Bitmap bit = BitmapFactory.decodeStream(bin);
        iv.setImageBitmap(bit);
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
            }
        }
    }
}
