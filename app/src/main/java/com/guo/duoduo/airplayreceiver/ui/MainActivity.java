package com.guo.duoduo.airplayreceiver.ui;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.guo.duoduo.airplayreceiver.R;
import com.guo.duoduo.airplayreceiver.service.RegisterService;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        startListenService();
        onBackPressed();
    }

    private void startListenService() {
        Intent intent = new Intent(getApplicationContext(), RegisterService.class);
        startService(intent);
        finish();
    }

}