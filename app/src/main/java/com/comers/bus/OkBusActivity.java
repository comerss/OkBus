package com.comers.bus;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.comers.annotation.annotation.EventReceiver;
import com.comers.annotation.mode.Mode;
import com.comers.okbus.OkBus;

public class OkBusActivity extends AppCompatActivity {

    private TextView txShowText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txShowText = findViewById(R.id.txShowText);
        OkBus.INSTANCE.register(this);
        findViewById(R.id.txShowText).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OkBus.INSTANCE.post("我是来自 OKBusActivity 的数据");
            }
        });
    }

    @EventReceiver(threadMode = Mode.MAIN)
    public void changed(String hahha) {
        txShowText.setText("我是来自 OKBusActivity 的数据");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        OkBus.INSTANCE.unregister(this);
    }
}
