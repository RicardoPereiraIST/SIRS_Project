package pt.utl.ist.sirs.t05.sirsapp;

import android.widget.TextView;

/**
 * Created by Diogo on 11/10/2016.
 */

public class UpdateUIThread implements Runnable {
    private String msg;
    private TextView text;

    public UpdateUIThread(String str, TextView text){
        this.msg = str;
        this.text = text;
    }

    @Override
    public void run(){
        try {
            text.setText(msg + "\n");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
