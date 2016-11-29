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
import javax.crypto.Cipher;
import javax.xml.bind.DatatypeConverter;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import java.security.SecureRandom;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class ServerSocketFunctions extends Thread {
   private ServerSocket serverSocket;

   private SecretKey initialKey;
   private SecretKey sessionKey;
   private PublicKey mobilePublicKey;
   
   private String password;
   
   public ServerSocketFunctions(int port, String password) throws IOException {
      serverSocket = new ServerSocket(port);
      this.password = password;
      }

   public void run() {
      while(true) {
         try {
         
            // Generate the initial key based on the password
            initialKey = generateInitialKey();

            System.out.println("\nWaiting for client on port " + serverSocket.getLocalPort() + "...");
            Socket server = serverSocket.accept();
            
            System.out.println("Just connected to " + server.getRemoteSocketAddress());
            DataInputStream in = new DataInputStream(server.getInputStream());

            // Receive from client the public key
            String received = in.readUTF();

            byte[] encrypted_public_key = DatatypeConverter.parseBase64Binary(received);
            byte[] decrypted_public_key = decryptWithInitialKey(encrypted_public_key, initialKey);

            mobilePublicKey = KeyFactory.getInstance("RSA").
                                    generatePublic(new X509EncodedKeySpec(decrypted_public_key));

            // Generate and send the enrypted session key
            System.out.println("Sending session key encrypted...");
            sessionKey = generateSessionKey();

            String sessionKeyToSend = encryptWithPublicKey(sessionKey.getEncoded(), mobilePublicKey);
            long timestamp = generateTimeStamp();
            String dataToSend = sessionKeyToSend + "." + String.valueOf(timestamp);

            DataOutputStream out = new DataOutputStream(server.getOutputStream());
            out.writeUTF(dataToSend);

            server.close();

            serverSocket = new ServerSocket(6100);
            server = serverSocket.accept();
            in = new DataInputStream(server.getInputStream());
            out = new DataOutputStream(server.getOutputStream());

            //Respond to the client challenge
            long nonce = receiveAndDecryptNonce(in);

            if(nonce == 0){
                  System.out.println("Replay attack incoming!!!");
                  server.close();
            }

            System.out.println("Nonce received from client -> " + nonce);
            // Calculate 
            nonce++;
            String stringNonce = Long.toString(nonce);
            System.out.println("Nonce calculated -> " + stringNonce);

            encryptAndSendNonce(stringNonce, out);

            // ------------ START CHALLENGES ----------------------------------
            while(true){
               System.out.println("Sending the continuous challenge nonce...");
               String nonceString = generateNonce();
               System.out.println("Nonce is -> " + nonceString);

               encryptAndSendNonce(nonceString, out);

               nonce = receiveAndDecryptNonce(in);

               if(nonce == 0){
                  System.out.println("Replay attack incoming!!!");
                  server.close();
               }

               System.out.println("Comparing " + nonce + " and " + (Long.valueOf(nonceString).longValue() + 1));
               if(nonce == Long.valueOf(nonceString).longValue() + 1){
                  System.out.println("Nonce is correct, sleeping...");
               }else{
                  System.out.println("Something is not right... closing the connection");
                  server.close();
                  return;
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
   
   // --------- Nonce Functions ------------------
   
   public String generateNonce(){
      SecureRandom random = new SecureRandom();
       Long generatedNonce = random.nextLong();
       return Long.toString(generatedNonce);
   }
   
   public void encryptAndSendNonce(String nonce, DataOutputStream out) throws Exception{
      byte[] encryptedNonce = encryptWithSessionKey(nonce, sessionKey);
      String encryptedNonceString = Base64.getMimeEncoder().encodeToString(encryptedNonce);
      long timestamp = generateTimeStamp();
      String dataToSend = encryptedNonceString + "." + String.valueOf(timestamp);
      System.out.println("Sending the nonce...");
      out.writeUTF(dataToSend);
   }
   
   public long receiveAndDecryptNonce(DataInputStream in) throws Exception{
      System.out.println("Waiting to receive token...");
      String nonceString = in.readUTF();

      System.out.println("Token is heere bois...");
      String[] parts = nonceString.split("\\.");
      if(!isWithinRange(Long.valueOf(parts[1]).longValue()))
         return 0;

      byte[] decodedNonce = Base64.getMimeDecoder().decode(parts[0]);
      byte[] decryptedNonce = decryptWithSessionKey(decodedNonce, sessionKey);
      String decryptedNonceString = new String(decryptedNonce, "UTF-8");
      long nonce = Long.valueOf(decryptedNonceString).longValue();
      return nonce;
   }
   
   // ---------------------------------------------

   // ------- Initial key -------------------------

   public SecretKey generateInitialKey(){
      try{
         byte[] saltBytes = "1234561234567812".getBytes();

         SecureRandom r = new SecureRandom();
         String ks = new BigInteger(32, r).toString(32);
         System.out.println("Write this token in your app: " + ks);

         SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

         KeySpec spec = new PBEKeySpec(ks.toCharArray(), saltBytes, 1024, 128);
         SecretKey tmp = factory.generateSecret(spec);
         return new SecretKeySpec(tmp.getEncoded(), "AES");

      }catch(Exception e){
         e.printStackTrace();
      }
      return null;
   }

   public byte[] decryptWithInitialKey(byte[] ciphertext, SecretKey key)throws Exception{
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

   // ------ Timestamps -----------------
   public long generateTimeStamp() throws Exception{

        final CountDownLatch latch = new CountDownLatch(1);
        final String[] timeStamp = new String[1];

        new Thread()
        {
            public void run() {
                try {
                    URL obj = new URL("http://www.google.pt");
                    URLConnection conn = obj.openConnection();

                    Map<String, List<String>> map = conn.getHeaderFields();

                    List<String> timeList = map.get("Date");
                    String time = timeList.get(0).split(",")[1].replaceFirst(" ", "");

                    timeStamp[0] = time;
                    latch.countDown();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }.start();
        latch.await();

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss z");
        Date date = sdf.parse(timeStamp[0]);

        return date.getTime();
   }


      public boolean isWithinRange(long receivedDate) throws Exception{
        long time = generateTimeStamp();

        if(time >= receivedDate && time <= receivedDate+10000)
           return true;

        return false;
      }

}