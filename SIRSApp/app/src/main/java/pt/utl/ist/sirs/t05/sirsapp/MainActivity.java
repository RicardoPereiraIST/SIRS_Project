package pt.utl.ist.sirs.t05.sirsapp;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import java.net.Socket;
import java.security.PublicKey;

import javax.crypto.SecretKey;
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
                // Create the key to start the communication with the server
                WeakKey keyClass = new WeakKey("espargueteabolonhesa", "1234561234567812");
                SecretKey weak = keyClass.getKey();

                RSA rsa = new RSA();
                PublicKey public_key = rsa.getKeyPair().getPublic();

                // Connect to the client
                Log.d(DEBUG_TAG, "Connecting to " + IP_ADDR + " on port " + PORT);
                Socket client = new Socket(IP_ADDR, PORT);

                SocketComms communication = new SocketComms(client);

                Log.d(DEBUG_TAG, "Just connected to " + client.getRemoteSocketAddress());

                // Encrypt public key
                String public_string = Base64.encodeToString(public_key.getEncoded(), Base64.DEFAULT);
                byte[] encryptedPublicKey = keyClass.encryptWithWeakKey(public_string, weak);

                Log.d(DEBUG_TAG,Base64.encodeToString(encryptedPublicKey, Base64.DEFAULT));

                // Send the encrypted key to the server
                communication.writeToServer(Base64.encodeToString(encryptedPublicKey, Base64.DEFAULT));

                // Read response from the server
                String serverPublicEncrypted = communication.readFromServer();
                Log.d(DEBUG_TAG, serverPublicEncrypted);

                // Convert the string received to a Session Key
                byte[] sessionKeyBytes = Base64.decode(
                        rsa.decrypt(serverPublicEncrypted, rsa.getKeyPair().getPrivate()), Base64.DEFAULT);

                SecretKey sessionKey =
                        new SecretKeySpec(Base64.decode(sessionKeyBytes, Base64.DEFAULT), 0, sessionKeyBytes.length, "AES");

                client.close();

            }catch(Exception e) {
                e.printStackTrace();
            }

            return (null);
        }
    }


}

