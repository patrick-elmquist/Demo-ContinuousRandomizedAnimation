package com.patrickiv.demo.continuousanimationdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private StarAnimationView mAnimationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(R.layout.activity_main);
        mAnimationView = (StarAnimationView) findViewById(R.id.animated_view);
        findViewById(R.id.btn_pause).setOnClickListener(this);
        findViewById(R.id.btn_resume).setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAnimationView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAnimationView.pause();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_pause:
                mAnimationView.pause();
                break;
            case R.id.btn_resume:
                mAnimationView.resume();
                break;
        }
    }
}
