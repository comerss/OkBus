package com.comers.okbus;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.comers.annotation.annotation.EventReceiver;
import com.comers.annotation.mode.Mode;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @EventReceiver(from = {MainActivity.class},threadMode = Mode.MAIN)
    public void dataChanged(Integer hahha) {

    }
}
