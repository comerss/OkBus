package com.comers.bus;

public class MainActivity_Helper {
    MainActivity target;

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
