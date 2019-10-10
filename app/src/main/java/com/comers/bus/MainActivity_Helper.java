package com.comers.bus;

import java.util.ArrayList;

public class MainActivity_Helper {
    MainActivity target;
   ArrayList<Object> tags=new ArrayList<>();
    public MainActivity_Helper(MainActivity var1) {
        this.target = var1;
    }

    public void postdataChanged(int var1) {
        this.target.dataChanged(var1);
    }

    public void postchanged(String text) {
        target.changed(text);
    }
    public void post(Object obj){
        if(obj instanceof String){
            postchanged((String) obj);
        }
    }

}
