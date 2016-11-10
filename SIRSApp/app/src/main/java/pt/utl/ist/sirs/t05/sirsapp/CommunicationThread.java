package pt.utl.ist.sirs.t05.sirsapp;

import android.os.Handler;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Created by Diogo on 11/10/2016.
 */

public class CommunicationThread implements Runnable {
    private Socket clientSocket;
    private BufferedReader input;
    private Handler updateConversationHandler;
    private TextView text;

    public CommunicationThread(Socket clientSocket, Handler handler, TextView text){
        this.clientSocket = clientSocket;
        this.updateConversationHandler = handler;
        this.text = text;

        try{
            this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void run() {
        while(!Thread.currentThread().isInterrupted()){
            try{
                String read = input.readLine();
                if(read != null)
                    updateConversationHandler.post(new UpdateUIThread(read, text));
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}
