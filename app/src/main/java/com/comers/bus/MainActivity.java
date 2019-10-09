package com.comers.bus;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.HttpAuthHandler;
import android.widget.TextView;


import com.comers.annotation.annotation.EventReceiver;
import com.comers.annotation.mode.Mode;
import com.comers.okbus.OkBus;

public class MainActivity extends AppCompatActivity {

    private TextView txShowText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        OkBus.INSTANCE.register(this);
        txShowText = findViewById(R.id.txShowText);
    }

    @EventReceiver(from = {MainActivity.class}, threadMode = Mode.MAIN)
    public void dataChanged(Integer hahha) {
        txShowText.setText(hahha.toString());
    }
}
