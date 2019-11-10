package com.comers.bus;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
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
        OkBus.getDefault().register(this);
        txShowText = findViewById(R.id.txShowText);
        txShowText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, OkBusActivity.class);
                startActivity(intent);
            }
        });
    }

    @EventReceiver(tag = "MainActivity", threadMode = Mode.MAIN)
    public void dataChanged(Integer hahha) {
        txShowText.setText(hahha + "---->00000");
    }

    @EventReceiver(threadMode = Mode.BACKGROUND)
    public void changed(String hahha) {
        txShowText.setText(hahha + "---->00000");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        OkBus.getDefault().unregister(this);
    }
}
