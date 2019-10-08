package com.comers.okbus;

import android.os.Bundle;

import androidx.annotation.IntDef;
import androidx.appcompat.app.AppCompatActivity;

import com.comers.annotation.annotation.EventReceiver;
import com.comers.annotation.mode.Mode;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @EventReceiver(from = {MainActivity.class},threadMode = Mode.BACKGROUND)
    public void dataChanged(Integer hahha) {

    }

    @IntDef
    @Retention(RetentionPolicy.SOURCE)
    public  @interface ThreadHah{

    }
}
