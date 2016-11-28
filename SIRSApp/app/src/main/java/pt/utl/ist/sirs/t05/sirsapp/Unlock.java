package pt.utl.ist.sirs.t05.sirsapp;

import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.net.Socket;
import java.security.SecureRandom;

import javax.crypto.SecretKey;

import pt.utl.ist.sirs.t05.sirsapp.Crypto.SessionKey;

public class Unlock extends AsyncTask<Void, Void, Void> {

    private SecretKey sessionKey;
    private SessionKey sessionKeyCipher;

    public Unlock(SecretKey sessionKey){
        this.sessionKey = sessionKey;
        this.sessionKeyCipher = new SessionKey();
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
    protected Void doInBackground(Void... unused) {

        try {
            Socket client = new Socket(Constant.IP_ADDR, Constant.PORT);
            CommunicationChannel channel = new CommunicationChannel(client);

            // Send challenge to server ------------------------------------
            String nonceString = generateNonce();
            encryptAndSendNonce(nonceString, channel);

            // Receive the nonce from the server
            long nonce = receiveAndDecryptNonce(channel);

            if(nonce == Long.valueOf(nonceString).longValue() + 1){
                Log.d(Constant.DEBUG_TAG, "Nonce is correct, proceed");
            }else{
                Log.w(Constant.DEBUG_TAG, "Something is wrong, closing the connection");
                client.close();
                return null;
            }

            // Begin to respond to the server challenges  ----------------
            while (true){
                // Receive and decrypt the nonce with the session key
                Log.w(Constant.DEBUG_TAG, "Get ready for the challenge...");
                nonce = receiveAndDecryptNonce(channel);

                // Calculate
                String calculatedNonce = calculateNonce(nonce);
                Log.w(Constant.DEBUG_TAG, "Challenge accepted: Nonce calculated -> " + calculatedNonce);

                // Encrypt and resend the nonce
                encryptAndSendNonce(calculatedNonce, channel);
            }
        }catch(Exception e) {
            e.printStackTrace();
        }

        return (null);
    }
}