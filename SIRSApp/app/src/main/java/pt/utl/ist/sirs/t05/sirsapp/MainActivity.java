package pt.utl.ist.sirs.t05.sirsapp;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import java.net.Socket;
import java.security.PublicKey;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


public class MainActivity extends AppCompatActivity {

    private SecretKey weakKey;
    private SecretKey sessionKey;
    private static final String IP_ADDR = "192.168.1.239";
    private static final int PORT = 6000;
    private static final String DEBUG_TAG = "DEBUG";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Client().execute();
    }


    class Client extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... unused) {

            try {

                // Create a pre determined iv for the session key
                String ivStr = "Randominitvector";
                IvParameterSpec iv = new IvParameterSpec(ivStr.getBytes());

                // Create the key to start the communication with the server
                // Function receives the password and the salt associated
                WeakKey keyClass = new WeakKey("espargueteabolonhesa", "1234561234567812");
                weakKey = keyClass.getKey();

                // Generate a pair of RSA keys to start the protocol
                RSA rsa = new RSA();
                PublicKey public_key = rsa.getKeyPair().getPublic();

                // Connect to the client
                Log.d(DEBUG_TAG, "Connecting to " + IP_ADDR + " on port " + PORT);
                Socket client = new Socket(IP_ADDR, PORT);

                SocketComms communication = new SocketComms(client);

                Log.d(DEBUG_TAG, "Just connected to " + client.getRemoteSocketAddress());

                // Encrypt public key with the initial key
                byte[] encryptedPublicKey = keyClass.encryptWithWeakKey(public_key.getEncoded(), weakKey);

                Log.d(DEBUG_TAG,Base64.encodeToString(encryptedPublicKey, Base64.DEFAULT));

                // Send the encrypted public key to the server
                communication.writeToServer(Base64.encodeToString(encryptedPublicKey, Base64.DEFAULT));

                // Receive from the server the session key encrypted with the public key
                String serverPublicEncrypted = communication.readFromServer();
                Log.d(DEBUG_TAG, serverPublicEncrypted);

                // Decrypt the key with the private key
                byte[] sessionKeyBytes = Base64.decode(serverPublicEncrypted, Base64.DEFAULT);
                byte[] decryptedSessionKey = rsa.decrypt(sessionKeyBytes, rsa.getKeyPair().getPrivate());

                Log.d(DEBUG_TAG, "Session Key received");

                sessionKey = new SecretKeySpec(decryptedSessionKey, 0, decryptedSessionKey.length, "AES");



                client.close();

            }catch(Exception e) {
                e.printStackTrace();
            }

            return (null);
        }
    }


}

