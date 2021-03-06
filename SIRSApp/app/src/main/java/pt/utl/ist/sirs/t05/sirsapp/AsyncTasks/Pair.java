package pt.utl.ist.sirs.t05.sirsapp.AsyncTasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.net.Socket;
import java.security.PublicKey;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import pt.utl.ist.sirs.t05.sirsapp.Crypto.Hash;
import pt.utl.ist.sirs.t05.sirsapp.SocketFunctions.CommunicationChannel;
import pt.utl.ist.sirs.t05.sirsapp.Constants.Constant;
import pt.utl.ist.sirs.t05.sirsapp.Crypto.InitialKey;
import pt.utl.ist.sirs.t05.sirsapp.Crypto.RSA;
import pt.utl.ist.sirs.t05.sirsapp.Crypto.SessionKey;
import pt.utl.ist.sirs.t05.sirsapp.SocketFunctions.TimeStamps;

public class Pair extends AsyncTask<Context, Void, SecretKey> {

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
            Log.e("ERROR", "Error generating keys");
        }
    }

    @Override
    protected SecretKey doInBackground(Context... params) {

        Context context = params[0];
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String ipAddress = prefs.getString("edit_text_ip_address", "");
        Socket client = null;

        try {
            // Generate the initial key and the RSA pair
            generateInitialKeys();

            // Connect to the client
            Log.d(Constant.DEBUG_TAG, "[Socket] Connecting to " + ipAddress + " on port " + Constant.PORT);
            client = new Socket(ipAddress, Constant.PORT);
            CommunicationChannel channel = new CommunicationChannel(client);
            Log.d(Constant.DEBUG_TAG, "[Socket] Just connected to " + client.getRemoteSocketAddress());

            // Encrypt public key with the initial key
            byte[] encryptedPublicKey = initialKeyCipher.encryptWithWeakKey(publicKey.getEncoded(), initialKey);
            Log.d(Constant.DEBUG_TAG, "[OUT] Public key encrpyted: " + Base64.encodeToString(encryptedPublicKey, Base64.DEFAULT));
            // Send the encrypted public key to the server

            TimeStamps ts = new TimeStamps();
            long timestamp = ts.generateTimeStamp();

            Hash h = new Hash();
            String hash_toSend = h.generateHash(String.valueOf(timestamp));

            channel.writeToServer(Base64.encodeToString(encryptedPublicKey, Base64.DEFAULT) +  "." + String.valueOf(timestamp) + "." + hash_toSend);

            client.setSoTimeout(2000);

            // Receive from the server the session key encrypted with the public key
            String serverPublicEncrypted = channel.readFromServer();

            String[] parts = serverPublicEncrypted.split("\\.");
            String hash = h.generateHash(parts[1]);

            if(!parts[2].equals(hash)) {
                client.close();
                return null;
            }

            if (!ts.isWithinRange(Long.valueOf(parts[1]).longValue())) {
                client.close();
                return null;
            }

            Log.d(Constant.DEBUG_TAG, "[IN] Session Key encrypted: " + parts[0]);
            // Decrypt the key with the private key
            byte[] sessionKeyBytes = Base64.decode(parts[0], Base64.DEFAULT);
            byte[] decryptedSessionKey = rsaCipher.decrypt(sessionKeyBytes, rsaCipher.getKeyPair().getPrivate());
            Log.d(Constant.DEBUG_TAG, "[IN] Session Key received");
            this.sessionKey = new SecretKeySpec(decryptedSessionKey, "AES");

            client.close();

        } catch (Exception e) {
            try {
                client.close();
            }catch (IOException s){
                return null;
            }
            return null;
        }
        return sessionKey;
    }

    @Override
    protected void onPostExecute(SecretKey result) {
        super.onPostExecute(sessionKey);
    }
}