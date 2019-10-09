package com.comers.bus;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.comers.annotation.annotation.EventReceiver;
import com.comers.annotation.mode.Mode;
import com.comers.okbus.OkBus;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        OkBus.INSTANCE.register(this);
    }

    @EventReceiver(from = {MainActivity.class},threadMode = Mode.MAIN)
    public void dataChanged(Integer hahha) {

    }
}
