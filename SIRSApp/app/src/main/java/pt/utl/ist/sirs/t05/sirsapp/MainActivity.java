package pt.utl.ist.sirs.t05.sirsapp;

import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import java.net.Socket;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Random;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


public class MainActivity extends AppCompatActivity {

    private SecretKey weakKey;
    private SecretKey sessionKey;
    private static final String IP_ADDR = "192.168.1.136";
    private static final int PORT = 6000;
    private static final String DEBUG_TAG = "DEBUG";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Client().execute();
    }


    class Client extends AsyncTask<Void, Void, Void> {

        /* ---------------- ATTENTION ------------------------------
           The following code must be refactor in several methods and
           classes. Below it is just am early version.
           ---------------------------------------------------------
         */

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
                Log.d(DEBUG_TAG, "[Socket] Connecting to " + IP_ADDR + " on port " + PORT);
                Socket client = new Socket(IP_ADDR, PORT);

                SocketComms communication = new SocketComms(client);

                Log.d(DEBUG_TAG, "[Socket] Just connected to " + client.getRemoteSocketAddress());

                // Encrypt public key with the initial key
                byte[] encryptedPublicKey = keyClass.encryptWithWeakKey(public_key.getEncoded(), weakKey);

                Log.d(DEBUG_TAG,"[OUT] Public key encrpyted: " + Base64.encodeToString(encryptedPublicKey, Base64.DEFAULT));

                // Send the encrypted public key to the server
                communication.writeToServer(Base64.encodeToString(encryptedPublicKey, Base64.DEFAULT));

                // Receive from the server the session key encrypted with the public key
                String serverPublicEncrypted = communication.readFromServer();
                Log.d(DEBUG_TAG, "[IN] Session Key encrypted: " + serverPublicEncrypted);

                // Decrypt the key with the private key
                byte[] sessionKeyBytes = Base64.decode(serverPublicEncrypted, Base64.DEFAULT);
                byte[] decryptedSessionKey = rsa.decrypt(sessionKeyBytes, rsa.getKeyPair().getPrivate());

                Log.d(DEBUG_TAG, "[IN] Session Key received");

                sessionKey = new SecretKeySpec(decryptedSessionKey, "AES");

                // Send challenge to server ------------------------------------
                SessionKey sessionKeyClass = new SessionKey();

                SecureRandom random = new SecureRandom();
                Long generatedNonce = random.nextLong();
                String nonceString = Long.toString(generatedNonce);

                Log.w(DEBUG_TAG, "[OUT] Nonce -> " + nonceString);
                byte[] encryptedNonce = sessionKeyClass.encryptWithSessionKey(nonceString, sessionKey, iv);

                // Convert bytes to a base64 string
                String encryptedNonceString = Base64.encodeToString(encryptedNonce, Base64.DEFAULT);
                Log.w(DEBUG_TAG, "[OUT] Encrypted encoded nonce -> " + encryptedNonceString);

                communication.writeToServer(encryptedNonceString);
                Log.w(DEBUG_TAG, "[OUT] Sent");
                String receivedNonce = communication.readFromServer();

                byte[] decodedNonce = Base64.decode(receivedNonce, Base64.DEFAULT);
                byte[] decryptedNonce = sessionKeyClass.decryptWithSessionKey(decodedNonce, sessionKey, iv);

                String decryptedNonceString = new String(decryptedNonce, "UTF-8");
                Log.w(DEBUG_TAG, "[OUT] Nonce received  -> " + decryptedNonceString);

                long nonce = Long.valueOf(decryptedNonceString).longValue();

                if(nonce == generatedNonce + 1){
                    Log.d(DEBUG_TAG, "Nonce is correct, proceed");
                }else{
                    Log.w(DEBUG_TAG, "Something is wrong, shutdown the connection");
                    client.close();
                }

                // ------ END OF CLIENT CHALLENGE -----------
                // Begin to respond to the server challenges ( 2 challenges per min ----------------
                while (true){
                    // Receive and decrypt the nonce with the session key
                    Log.w(DEBUG_TAG, "Get ready for the challenge...");
                    receivedNonce = communication.readFromServer();

                    decodedNonce = Base64.decode(receivedNonce, Base64.DEFAULT);
                    decryptedNonce = sessionKeyClass.decryptWithSessionKey(decodedNonce, sessionKey, iv);
                    decryptedNonceString = new String(decryptedNonce, "UTF-8");
                    nonce = Long.valueOf(decryptedNonceString).longValue();

                    // Calculate
                    nonce += 1;
                    nonceString = Long.toString(nonce);
                    Log.w(DEBUG_TAG, "Challenge accepted: Nonce calculated -> " + nonceString);

                    // Encrypt and resend the nonce
                    encryptedNonce = sessionKeyClass.encryptWithSessionKey(nonceString, sessionKey, iv);
                    String encodedNonce = Base64.encodeToString(encryptedNonce, Base64.DEFAULT);
                    communication.writeToServer(encodedNonce);

                }
                // ----------------- END OF SERVER CHALLENGES ----------------------------------------

            }catch(Exception e) {
                e.printStackTrace();
            }

            return (null);
        }
    }


}

