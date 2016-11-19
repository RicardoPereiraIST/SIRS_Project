package pt.utl.ist.sirs.t05.sirsapp;

import android.os.AsyncTask;
import android.os.Debug;
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

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.math.BigInteger;
import java.security.SecureRandom;


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

            String serverName = "192.168.1.5";
            int port = 6000;

            try {
                Key key = new Key("espargueteabolonhesa");
                SecretKey secret = key.getKey();
                Log.w(TAG, Base64.encodeToString(key.getKey().getEncoded(), Base64.DEFAULT));

                String plaintext = "banana";
                try {

                    String initVector = "RandomInitVector";
                    IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));

                    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
                    cipher.init(Cipher.ENCRYPT_MODE, secret, iv);
                    byte[] ciphertext = cipher.doFinal(plaintext.getBytes());
                    cipher.init(Cipher.DECRYPT_MODE, secret, iv);
                    String result = new String(cipher.doFinal(ciphertext), "UTF-8");
                    Log.w("RESULT", result);


                    RSA testText = new RSA();
                    String ze = testText.encrypt("banana");
                    Log.w("Debug", ze);
                    byte[] r = testText.decrypt(ze);

                    Log.w("Debug", new String(r, "UTF-8" ));



                }catch (Exception e){
                    Log.w(TAG, e.getMessage());
                }


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

