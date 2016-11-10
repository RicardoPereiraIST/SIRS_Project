package pt.utl.ist.sirs.t05.sirsapp;

import android.os.Handler;
import android.widget.TextView;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Diogo on 11/10/2016.
 */

public class ServerThread implements Runnable {

    public static final int PORT = 6000;
    private ServerSocket serverSocket;
    private TextView text;
    Handler handler;

    ServerThread(TextView text, Handler handler){
        this.text = text;
        this.handler = handler;
    }

    public void run(){
        Socket socket = null;
        try{
            serverSocket = new ServerSocket(PORT);
        }catch(IOException e){
            e.printStackTrace();
        }

        while (!Thread.currentThread().isInterrupted()){
            try{
                socket = serverSocket.accept();

                CommunicationThread commThread = new CommunicationThread(socket, handler, text);
                new Thread(commThread).start();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    public ServerSocket getServerSocket(){
        return serverSocket;
    }

}
