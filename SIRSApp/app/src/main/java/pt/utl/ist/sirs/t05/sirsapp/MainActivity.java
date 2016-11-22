package pt.utl.ist.sirs.t05.sirsapp;

import pt.utl.ist.sirs.t05.sirsapp.Crypto.RSA;
import pt.utl.ist.sirs.t05.sirsapp.Crypto.SessionKey;
import pt.utl.ist.sirs.t05.sirsapp.Crypto.InitialKey;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.net.Socket;
import java.security.PublicKey;
import java.security.SecureRandom;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;



public class MainActivity extends AppCompatActivity  {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button saveButton = (Button)findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                EditText passwordText = (EditText)findViewById(R.id.password_box);
                if(!passwordText.getText().toString().equals("")){
                    Toast.makeText(view.getContext(), "Launching connection", Toast.LENGTH_SHORT).show();
                    new Client(passwordText.getText().toString()).execute();
                    passwordText.setVisibility(View.INVISIBLE);
                    findViewById(R.id.insert_password_text).setVisibility(View.INVISIBLE);
                    findViewById(R.id.save_button).setVisibility(View.INVISIBLE);
                }
                else{
                    Toast.makeText(view.getContext(), "Insert password first", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    class Client extends AsyncTask<Void, Void, Void> {

        private SecretKey initialKey;
        private SecretKey sessionKey;
        private PublicKey publicKey;
        private String password;

        private InitialKey initialKeyCipher;
        private RSA rsaCipher;
        private SessionKey sessionKeyCipher;

        Client(String password){
            this.password = password;
        }

        private void generateInitialKeys(){
            this.initialKeyCipher = new InitialKey(password);
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

        private void encryptAndSendNonce(String nonce, SocketComms communication){
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

        private long receiveAndDecryptNonce(SocketComms communication){
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
        /* ---------------- ATTENTION ------------------------------
           The following code must be refactor in several methods and
           classes. Below it is just am early version.
           ---------------------------------------------------------
         */

        @Override
        protected Void doInBackground(Void... unused) {

            try {
                // Generate the initial key and the RSA pair
                generateInitialKeys();

                // Connect to the client
                Log.d(Constant.DEBUG_TAG, "[Socket] Connecting to " + Constant.IP_ADDR + " on port " + Constant.PORT);
                Socket client = new Socket(Constant.IP_ADDR, Constant.PORT);
                SocketComms communication = new SocketComms(client);
                Log.d(Constant.DEBUG_TAG, "[Socket] Just connected to " + client.getRemoteSocketAddress());

                // Encrypt public key with the initial key
                byte[] encryptedPublicKey = initialKeyCipher.encryptWithWeakKey(publicKey.getEncoded(), initialKey);
                Log.d(Constant.DEBUG_TAG,"[OUT] Public key encrpyted: " + Base64.encodeToString(encryptedPublicKey, Base64.DEFAULT));
                // Send the encrypted public key to the server
                communication.writeToServer(Base64.encodeToString(encryptedPublicKey, Base64.DEFAULT));

                // Receive from the server the session key encrypted with the public key
                String serverPublicEncrypted = communication.readFromServer();
                Log.d(Constant.DEBUG_TAG, "[IN] Session Key encrypted: " + serverPublicEncrypted);
                // Decrypt the key with the private key
                byte[] sessionKeyBytes = Base64.decode(serverPublicEncrypted, Base64.DEFAULT);
                byte[] decryptedSessionKey = rsaCipher.decrypt(sessionKeyBytes, rsaCipher.getKeyPair().getPrivate());
                Log.d(Constant.DEBUG_TAG, "[IN] Session Key received");
                this.sessionKey = new SecretKeySpec(decryptedSessionKey, "AES");

                // Send challenge to server ------------------------------------
                String nonceString = generateNonce();
                encryptAndSendNonce(nonceString, communication);

                // Receive the nonce from the server
                long nonce = receiveAndDecryptNonce(communication);

                if(nonce == Long.valueOf(nonceString).longValue() + 1){
                    Log.d(Constant.DEBUG_TAG, "Nonce is correct, proceed");
                }else{
                    Log.w(Constant.DEBUG_TAG, "Something is wrong, closing the connection");
                    client.close();
                }

                // Begin to respond to the server challenges ( 2 challenges per min) ----------------
                while (true){
                    // Receive and decrypt the nonce with the session key
                    Log.w(Constant.DEBUG_TAG, "Get ready for the challenge...");
                    nonce = receiveAndDecryptNonce(communication);

                    // Calculate
                    String calculatedNonce = calculateNonce(nonce);
                    Log.w(Constant.DEBUG_TAG, "Challenge accepted: Nonce calculated -> " + calculatedNonce);

                    // Encrypt and resend the nonce
                    encryptAndSendNonce(calculatedNonce, communication);
                }
            }catch(Exception e) {
                e.printStackTrace();
            }

            return (null);
        }
    }


}

