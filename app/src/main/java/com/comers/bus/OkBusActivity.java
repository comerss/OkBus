package com.comers.bus;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.comers.okbus.OkBus;

public class OkBusActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.txShowText).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OkBus.INSTANCE.post("我是来自 OKBusActivity 的数据");
            }
        });
    }
}
