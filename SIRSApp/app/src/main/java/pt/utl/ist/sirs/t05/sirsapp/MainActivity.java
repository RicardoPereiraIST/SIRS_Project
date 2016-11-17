package pt.utl.ist.sirs.t05.sirsapp;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Client().execute();
    }

    class Client extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... unused) {
            String TAG = "DEBUG";

            String serverName = "192.168.1.239";
            int port = 6000;

            try {
                Key key = new Key("espargueteabolonhesa");
                Log.w(TAG, Base64.encodeToString(key.getKey().getEncoded(), Base64.DEFAULT));

                Log.w(TAG, "Connecting to " + serverName + " on port " + port);
                Socket client = new Socket(serverName, port);

                Log.w(TAG, "Just connected to " + client.getRemoteSocketAddress());
                OutputStream outToServer = client.getOutputStream();
                DataOutputStream out = new DataOutputStream(outToServer);

                out.writeUTF("Hello from " + client.getLocalSocketAddress());
                InputStream inFromServer = client.getInputStream();
                DataInputStream in = new DataInputStream(inFromServer);

                Log.w(TAG, "Server says " + in.readUTF());
                client.close();
            }catch(IOException e) {
                e.printStackTrace();
            }

            return (null);
        }
    }


}

