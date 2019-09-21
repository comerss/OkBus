package com.comers.okbus;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.comers.annotation.annotation.EventReceiver;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @EventReceiver(from = {MainActivity.class})
    public void dataChanged() {

    }
}
