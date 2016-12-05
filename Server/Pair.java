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
import java.security.MessageDigest;

public class Pair extends Thread {
   private ServerSocket serverSocket;

   private SecretKey initialKey;
   private SecretKey sessionKey;
   private PublicKey mobilePublicKey;

   private String token;
   
   public Pair(int port, String token) throws IOException {
      this.serverSocket = new ServerSocket(port);
      this.token = token;
      }

   public SecretKey getSessionKey(){
    return sessionKey;
   }

   public void run() {
         try {
         
            // Generate the initial key based on the password
            initialKey = generateInitialKey(this.token);

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

            serverSocket.close();
     
         }catch(SocketTimeoutException s) {
            System.out.println("Socket timed out!");
         }catch(javax.crypto.BadPaddingException e){
            System.out.println("The token appears to be incorrect! Try again!");
            sessionKey = null;
            try{
              serverSocket.close();
            }catch(Exception z){
              System.out.println("Failed to close socket!");
              return;
            }
            return;
         }catch(IOException e) {
            e.printStackTrace();
         }catch(Exception e){
            e.printStackTrace();
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

      String hash = generateHash(String.valueOf(timestamp));

      String dataToSend = encryptedNonceString + "." + String.valueOf(timestamp) + "." + hash;
      System.out.println("Sending the nonce...");
      out.writeUTF(dataToSend);
   }
   
   public long receiveAndDecryptNonce(DataInputStream in) throws Exception{
      String nonceString = in.readUTF();

      String[] parts = nonceString.split("\\.");

      String hash = generateHash(parts[1]);
      if(!hash.equals(parts[2]))
        return 0;

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

   public SecretKey generateInitialKey(String token){
      try{
         byte[] saltBytes = "1234561234567812".getBytes();

         SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

         KeySpec spec = new PBEKeySpec(token.toCharArray(), saltBytes, 1024, 128);
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

      public String generateHash(String stringToEncrypt) throws Exception{
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(stringToEncrypt.getBytes());
        String encryptedString = new String(messageDigest.digest());
        return encryptedString;
      }

}