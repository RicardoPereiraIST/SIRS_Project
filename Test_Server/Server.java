import java.net.*;
import java.io.*;
import java.security.PublicKey;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import javax.crypto.Cipher;
import javax.xml.bind.DatatypeConverter;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import java.security.SecureRandom;


public class Server extends Thread {
   private ServerSocket serverSocket;

   private SecretKey weakKey;
   private SecretKey sessionKey;
   private PublicKey mobilePublicKey;
   
   public Server(int port) throws IOException {
      serverSocket = new ServerSocket(port);
   }

   public void run() {
      while(true) {
         try {
         
            // Generate the initial key based on the password
            weakKey = generateWeakKey("espargueteabolonhesa");

            System.out.println("Waiting for client on port " + serverSocket.getLocalPort() + "...");
            Socket server = serverSocket.accept();
            
            System.out.println("Just connected to " + server.getRemoteSocketAddress());
            DataInputStream in = new DataInputStream(server.getInputStream());

            // Receive from client the public key
            String received = in.readUTF();

            byte[] encrypted_public_key = DatatypeConverter.parseBase64Binary(received);
            byte[] decrypted_public_key = 
                                 decryptWithWeakKey(encrypted_public_key, weakKey);

            mobilePublicKey = KeyFactory.getInstance("RSA").
                                    generatePublic(new X509EncodedKeySpec(decrypted_public_key));

            // Generate and send the enrypted session key
            System.out.println("Sending session key encrypted...");
            sessionKey = generateSessionKey();

            String sessionKeyToSend = encryptWithPublicKey(sessionKey.getEncoded(), mobilePublicKey);

            DataOutputStream out = new DataOutputStream(server.getOutputStream());
            out.writeUTF(sessionKeyToSend);

            // Challenge implementation 
            // 1 - Respond to the client challenge

            String encodedNonce = in.readUTF();

            byte[] decodedNonce = Base64.getMimeDecoder().decode(encodedNonce);

            byte[] decryptedNonce = decryptWithSessionKey(decodedNonce, sessionKey);
            String stringNonce = new String(decryptedNonce, "UTF-8");

            long nonce = Long.valueOf(stringNonce).longValue();

            System.out.println("Nonce received from client -> " + nonce);
            // Calculate 
            nonce++;
            stringNonce = Long.toString(nonce);
            System.out.println("Nonce calculated -> " + stringNonce);

            // Encrypt and resend
            byte[] encrpytedNonce = encryptWithSessionKey(stringNonce, sessionKey);
            encodedNonce = Base64.getMimeEncoder().encodeToString(encrpytedNonce);

            System.out.println("Sending the nonce...");
            out.writeUTF(encodedNonce);

            // 2- Send coninuous challenges to the client ---------------------
            // ------------ START CHALLENGES ----------------------------------
            while(true){
               System.out.println("Sending the continuous challenge nonce...");
               SecureRandom random = new SecureRandom();
               Long generatedNonce = random.nextLong();
               String nonceString = Long.toString(generatedNonce);

               System.out.println("Nonce is -> " + nonceString);

               byte[] encryptedNonce = encryptWithSessionKey(nonceString, sessionKey);
               String encryptedNonceString = Base64.getMimeEncoder().encodeToString(encryptedNonce);

               out.writeUTF(encryptedNonceString);

               nonceString = in.readUTF();

               decodedNonce = Base64.getMimeDecoder().decode(nonceString);
               decryptedNonce = decryptWithSessionKey(decodedNonce, sessionKey);
               String decryptedNonceString = new String(decryptedNonce, "UTF-8");
               nonce = Long.valueOf(decryptedNonceString).longValue();

               System.out.println("Comparing " + nonce + " and " + (generatedNonce + 1));
               if(nonce == generatedNonce + 1){
                  System.out.println("Nonce is correct, sleeping...");
               }else{
                  System.out.println("Something is not right... closing the connection");
                  server.close();
               }
               // Sleeping 30 seconds
               Thread.sleep(30000);

            }

            
         }catch(SocketTimeoutException s) {
            System.out.println("Socket timed out!");
            break;
         }catch(IOException e) {
            e.printStackTrace();
            break;
         }catch(Exception e){
            e.printStackTrace();
         }
      }
   }
   
   public static void main(String [] args) {
      try {
         Thread t = new Server(6000);
         t.start();
      }catch(IOException e) {
         e.printStackTrace();
      }
   }

   // ------- Weak initial key -------------

   public SecretKey generateWeakKey(String password){
      try{
         byte[] saltBytes = "1234561234567812".getBytes();

         SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

         KeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, 1024, 128);
         SecretKey tmp = factory.generateSecret(spec);
         return new SecretKeySpec(tmp.getEncoded(), "AES");

      }catch(Exception e){
         e.printStackTrace();
      }
      return null;
   }

   public byte[] decryptWithWeakKey(byte[] ciphertext, SecretKey key)throws Exception{
      Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
      cipher.init(Cipher.DECRYPT_MODE, key);
      return cipher.doFinal(ciphertext);
   }

   // -------------------------------------


   // ------------ RSA --------------------

   public String encryptWithPublicKey(byte[] plaintext, PublicKey key)throws Exception{
      Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
      cipher.init(Cipher.ENCRYPT_MODE, key);
      byte[] ciphertext = cipher.doFinal(plaintext);
      return DatatypeConverter.printBase64Binary(ciphertext);
   }

   // ------------------------------------

   // ------ Session Key -----------------

   public byte[] encryptWithSessionKey(String nonce, SecretKey key) throws Exception{
      String ivStr = "Randominitvector";
      IvParameterSpec iv = new IvParameterSpec(ivStr.getBytes());

      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.ENCRYPT_MODE, key, iv);
      return cipher.doFinal(nonce.getBytes("UTF-8"));
   }

   public byte[] decryptWithSessionKey(byte[] nonce, SecretKey key) throws Exception{
      String ivStr = "Randominitvector";
      IvParameterSpec iv = new IvParameterSpec(ivStr.getBytes());

      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.DECRYPT_MODE, key, iv);
      return cipher.doFinal(nonce);
   }

   public SecretKey generateSessionKey() throws Exception{
      KeyGenerator keyGen = KeyGenerator.getInstance("AES");
      keyGen.init(128);
      return keyGen.generateKey();
   }

   // -------------------------------------



}