package com.comers.bus;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.comers.annotation.annotation.EventReceiver;
import com.comers.annotation.mode.Mode;
import com.comers.okbus.ClassTypeHelper;
import com.comers.okbus.OkBus;
import com.comers.okbus.PostData;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class OkBusActivity extends AppCompatActivity {

    private TextView txShowText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txShowText = findViewById(R.id.txShowText);
        OkBus.getDefault().register(this);
        findViewById(R.id.txShowText).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OkBus.getDefault().post("我是来自 OKBusActivity 的数据");
                //发送带tag 的数据，进行过滤，不区分页面，只要方法的tag一样则进行调用，当然我们会进行参数类型的校验
                OkBus.getDefault().post(11, "MainActivity");
                //发送给多个页面的。前提是对应的页面能支持这个类型的参数
                OkBus.getDefault().post("我是一个多个页面的发送", OkBusActivity.class, MainActivity.class);
                OkData<Success> data = new OkData<>();
                data.data = new Success("我的数据来自泛型");
                //发送泛型数据相对复杂一些，需要把数据存出来我实现写好的类里面，并且注意后面的{}，有点类似于Gson 的TypeToken获取数据的泛型
                OkBus.getDefault().post(new PostData<OkData<Success>>(data) {
                });
            }
        });
    }

    @EventReceiver(threadMode = Mode.POST_THREAD, tag = "OkActivity")
    public void changed(OkData<Success> hahha) {
        txShowText.setText(hahha.data.show);
    }

    @EventReceiver(threadMode = Mode.BACKGROUND)
    public Success change(String hahha) {
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        OkBus.getDefault().unregister(this);
    }
}
