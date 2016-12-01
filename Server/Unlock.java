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

public class Unlock extends Thread {
   private ServerSocket serverSocket;

   private SecretKey sessionKey;
   private Boolean lock;
 
   public Unlock(int port, SecretKey sessionKey, Boolean lock) throws IOException {
      this.serverSocket = new ServerSocket(port);
      this.sessionKey = sessionKey;
      this.lock = lock;
    }

    public Boolean getLockVar(){
      return this.lock;
    }

   public void run() {
         try {
            Socket server = serverSocket.accept();
            DataInputStream in = new DataInputStream(server.getInputStream());
            DataOutputStream out = new DataOutputStream(server.getOutputStream());

            //Respond to the client challenge
            long nonce = receiveAndDecryptNonce(in);

            if(nonce == 0){
                  System.out.println("Replay attack detected, closing connection!");
                  server.close();
            }

            System.out.println("Nonce received from client -> " + nonce);
            // Calculate 
            nonce++;
            String stringNonce = Long.toString(nonce);
            System.out.println("Nonce calculated -> " + stringNonce);

            encryptAndSendNonce(stringNonce, out);


            // if (lock == true){
            //   UnlockFilesOnManager();
            // }

            // ------------ START CHALLENGES ----------------------------------
            while(true){
               System.out.println("Sending the continuous challenge nonce...");
               String nonceString = generateNonce();
               System.out.println("Nonce is -> " + nonceString);

               encryptAndSendNonce(nonceString, out);

               nonce = receiveAndDecryptNonce(in);

               if(nonce == 0){
                  System.out.println("Replay attack detected, closing connection!");
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
            System.out.println("Client is away, locking files!");
            lock = true;
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
      String dataToSend = encryptedNonceString + "." + String.valueOf(timestamp);
      System.out.println("Sending the nonce...");
      out.writeUTF(dataToSend);
   }
   
   public long receiveAndDecryptNonce(DataInputStream in) throws Exception{
      String nonceString = in.readUTF();

      String[] parts = nonceString.split("\\.");
      if(!isWithinRange(Long.valueOf(parts[1]).longValue()))
         return 0;

      byte[] decodedNonce = Base64.getMimeDecoder().decode(parts[0]);
      byte[] decryptedNonce = decryptWithSessionKey(decodedNonce, sessionKey);
      String decryptedNonceString = new String(decryptedNonce, "UTF-8");
      long nonce = Long.valueOf(decryptedNonceString).longValue();
      return nonce;
   }
   


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