package pt.utl.ist.sirs.t05.sirsapp;

import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.net.Socket;
import java.security.PublicKey;
import java.security.SecureRandom;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import pt.utl.ist.sirs.t05.sirsapp.Activities.HomeActivity;
import pt.utl.ist.sirs.t05.sirsapp.Crypto.InitialKey;
import pt.utl.ist.sirs.t05.sirsapp.Crypto.RSA;
import pt.utl.ist.sirs.t05.sirsapp.Crypto.SessionKey;

public class Pair extends AsyncTask<Void, Void, SecretKey> {

    private SecretKey initialKey;
    private SecretKey sessionKey;
    private PublicKey publicKey;
    private String token;

    private InitialKey initialKeyCipher;
    private RSA rsaCipher;
    private SessionKey sessionKeyCipher;

    public Pair(String token){
        this.token = token;
    }

    private void generateInitialKeys(){
        this.initialKeyCipher = new InitialKey(token);
        this.initialKey = initialKeyCipher.getKey();

        this.sessionKeyCipher = new SessionKey();

        try {
            this.rsaCipher = new RSA();
            this.publicKey = rsaCipher.getKeyPair().getPublic();
        }catch (Exception e){
            Log.e("ERROR", "Error generating RSA keys");
        }
    }

    @Override
    protected SecretKey doInBackground(Void... unused) {

        try {
            // Generate the initial key and the RSA pair
            generateInitialKeys();

            // Connect to the client
            Log.d(Constant.DEBUG_TAG, "[Socket] Connecting to " + Constant.IP_ADDR + " on port " + Constant.PORT);
            Socket client = new Socket(Constant.IP_ADDR, Constant.PORT);
            CommunicationChannel channel = new CommunicationChannel(client);
            Log.d(Constant.DEBUG_TAG, "[Socket] Just connected to " + client.getRemoteSocketAddress());

            // Encrypt public key with the initial key
            byte[] encryptedPublicKey = initialKeyCipher.encryptWithWeakKey(publicKey.getEncoded(), initialKey);
            Log.d(Constant.DEBUG_TAG,"[OUT] Public key encrpyted: " + Base64.encodeToString(encryptedPublicKey, Base64.DEFAULT));
            // Send the encrypted public key to the server
            channel.writeToServer(Base64.encodeToString(encryptedPublicKey, Base64.DEFAULT));

            // Receive from the server the session key encrypted with the public key
            String serverPublicEncrypted = channel.readFromServer();
            String[] parts = serverPublicEncrypted.split(".");

            TimeStamps ts = new TimeStamps();
            if(!ts.compareTimeStamp(parts[1])){
                client.close();
            }

            Log.d(Constant.DEBUG_TAG, "[IN] Session Key encrypted: " + parts[0]);
            // Decrypt the key with the private key
            byte[] sessionKeyBytes = Base64.decode(parts[0], Base64.DEFAULT);
            byte[] decryptedSessionKey = rsaCipher.decrypt(sessionKeyBytes, rsaCipher.getKeyPair().getPrivate());
            Log.d(Constant.DEBUG_TAG, "[IN] Session Key received");
            this.sessionKey = new SecretKeySpec(decryptedSessionKey, "AES");

            client.close();

        }catch(Exception e) {
            e.printStackTrace();
        }

        return (null);
    }

    @Override
    protected void onPostExecute(SecretKey result) {
        super.onPostExecute(sessionKey);
    }
}