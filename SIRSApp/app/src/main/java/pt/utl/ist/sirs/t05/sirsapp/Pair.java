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

    private String generateNonce(){
        SecureRandom random = new SecureRandom();
        Long generatedNonce = random.nextLong();

        return Long.toString(generatedNonce);
    }

    private void encryptAndSendNonce(String nonce, CommunicationChannel communication){
        Log.d(Constant.DEBUG_TAG, "[OUT] Nonce -> " + nonce);
        try {
            byte[] encryptedNonce = sessionKeyCipher.encryptWithSessionKey(nonce, this.sessionKey);
            // Convert bytes to a base64 string
            String encryptedNonceString = Base64.encodeToString(encryptedNonce, Base64.DEFAULT);
            Log.d(Constant.DEBUG_TAG, "[OUT] Encrypted encoded nonce -> " + encryptedNonceString);
            communication.writeToServer(encryptedNonceString);
            Log.d(Constant.DEBUG_TAG, "[OUT] Sent");
        }catch (IOException e){
            Log.e("ERROR", "Error sending the nonce to the server");
        }catch(Exception e){
            Log.e("ERROR", "Error encrypting the nonce with the session key");
        }
    }

    private long receiveAndDecryptNonce(CommunicationChannel communication){
        String decrypted = "";
        try {
            String receivedNonce = communication.readFromServer();
            byte[] decodedNonce = Base64.decode(receivedNonce, Base64.DEFAULT);
            byte[] decryptedNonce = sessionKeyCipher.decryptWithSessionKey(decodedNonce, sessionKey);
            decrypted = new String(decryptedNonce, "UTF-8");
            Log.d(Constant.DEBUG_TAG, "[OUT] Nonce received  -> " + decrypted);
        }catch(IOException e){
            Log.e("ERROR", "Error reading the nonce from the server");
        }catch(Exception e){
            Log.e("ERROR", "Error decrypting the nonce with the session key");
        }
        return Long.valueOf(decrypted).longValue();
    }

    private String calculateNonce(long nonce){
        long n = nonce + 1;
        return Long.toString(n);
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
            Log.d(Constant.DEBUG_TAG, "[IN] Session Key encrypted: " + serverPublicEncrypted);
            // Decrypt the key with the private key
            byte[] sessionKeyBytes = Base64.decode(serverPublicEncrypted, Base64.DEFAULT);
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