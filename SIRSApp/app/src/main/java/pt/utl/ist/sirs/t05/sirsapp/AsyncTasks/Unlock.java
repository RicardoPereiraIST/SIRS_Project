package pt.utl.ist.sirs.t05.sirsapp.AsyncTasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.net.Socket;
import java.security.SecureRandom;

import javax.crypto.SecretKey;

import pt.utl.ist.sirs.t05.sirsapp.SocketFunctions.CommunicationChannel;
import pt.utl.ist.sirs.t05.sirsapp.Constants.Constant;
import pt.utl.ist.sirs.t05.sirsapp.Crypto.SessionKey;
import pt.utl.ist.sirs.t05.sirsapp.SocketFunctions.TimeStamps;

public class Unlock extends AsyncTask<Context, Void, Void> {

    private SecretKey sessionKey;
    private SessionKey sessionKeyCipher;
    private TimeStamps ts;

    public Unlock(SecretKey sessionKey){
        this.sessionKey = sessionKey;
        this.sessionKeyCipher = new SessionKey();
        this.ts = new TimeStamps();
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

            long timestamp = ts.generateTimeStamp();
            String dataToSend = encryptedNonceString + "." + String.valueOf(timestamp);

            communication.writeToServer(dataToSend);
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
            String[] parts = receivedNonce.split("\\.");
            if(ts.isWithinRange(Long.valueOf(parts[1]).longValue()) == false)
                return 0;

            byte[] decodedNonce = Base64.decode(parts[0], Base64.DEFAULT);
            byte[] decryptedNonce = sessionKeyCipher.decryptWithSessionKey(decodedNonce, sessionKey);
            decrypted = new String(decryptedNonce, "UTF-8");
            Log.d(Constant.DEBUG_TAG, "[OUT] Nonce received  -> " + decrypted);
        }catch(IOException e){
            Log.e("ERROR", "Error reading the nonce from the server");
            return -1;

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
    protected Void doInBackground(Context... params) {

        Context context = params[0];
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String ipAddress = prefs.getString("edit_text_ip_address", "");

        try {
            Socket client = new Socket(ipAddress, 6100);
            CommunicationChannel channel = new CommunicationChannel(client);

            Constant.unlockSocketOpen = true;

            // Send challenge to server ------------------------------------
            String nonceString = generateNonce();
            encryptAndSendNonce(nonceString, channel);

            // Receive the nonce from the server
            long nonce = receiveAndDecryptNonce(channel);

            if(nonce == 0){
                  Log.d(Constant.DEBUG_TAG, "Replay attack incoming!!!");
                  Constant.unlockSocketOpen = false;
                  client.close();
            }

            if(nonce == Long.valueOf(nonceString).longValue() + 1){
                Log.d(Constant.DEBUG_TAG, "Nonce is correct, proceed");
            }else{
                Log.w(Constant.DEBUG_TAG, "Something is wrong, closing the connection");
                Constant.unlockSocketOpen = false;
                client.close();
                return null;
            }

            // Begin to respond to the server challenges  ----------------
            while (true){
                // Receive and decrypt the nonce with the session key
                Log.w(Constant.DEBUG_TAG, "Get ready for the challenge...");
                nonce = receiveAndDecryptNonce(channel);

                if(nonce == 0){
                  Log.d(Constant.DEBUG_TAG, "Replay attack detected!");
                    Constant.unlockSocketOpen = false;
                  client.close();
                }

                if(nonce == -1){
                    Log.d(Constant.DEBUG_TAG, "You are out of the network range!");
                    Constant.unlockSocketOpen = false;
                    client.close();
                    return null;
                }

                // Calculate
                String calculatedNonce = calculateNonce(nonce);
                Log.w(Constant.DEBUG_TAG, "Challenge accepted: Nonce calculated -> " + calculatedNonce);

                // Encrypt and resend the nonce
                encryptAndSendNonce(calculatedNonce, channel);
            }
        }catch(Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}