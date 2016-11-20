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
            weakKey = generateWeakKey("espargueteabolonhesa"), "1234561234567812");

            System.out.println("Waiting for client on port " + 
               serverSocket.getLocalPort() + "...");
            Socket server = serverSocket.accept();
            
            System.out.println("Just connected to " + server.getRemoteSocketAddress());
            DataInputStream in = new DataInputStream(server.getInputStream());

            String received = in.readUTF();

            byte[] encrypted_public_key = DatatypeConverter.parseBase64Binary(received);
            byte[] decrypted_public_key = 
                                 decryptWithWeakKey(encrypted_public_key, weakKey);

            mobilePublicKey = KeyFactory.getInstance("RSA").
                                    generatePublic(new X509EncodedKeySpec(decrypted_public_key));

            System.out.println("Sending session key encrypted...");
            sessionKey = generateSessionKey();

            String sessionKeyToSend = encryptWithPublicKey(sessionKey.getEncoded(), mobilePublicKey);

            DataOutputStream out = new DataOutputStream(server.getOutputStream());
            out.writeUTF(sessionKeyToSend);

            // Challenge implementation 
            // TODO

            // Tests on the Session Key
            // TODO

            server.close();
            
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

   public SecretKey generateWeakKey(String password, String salt){
      try{
         byte[] saltBytes = salt.getBytes();

         SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

         KeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, 1024, 128);
         SecretKey tmp = factory.generateSecret(spec);
         return new SecretKeySpec(tmp.getEncoded(), "AES");

      }catch(Exception e){
         e.printStackTrace();
      }
   }

   public byte[] decryptWithWeakKey(byte[] ciphertext, SecretKey key)throws Exception{
      Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
      cipher.init(Cipher.DECRYPT_MODE, key);
      return cipher.doFinal(ciphertext);
   }


   public String encryptWithPublicKey(byte[] plaintext, PublicKey key)throws Exception{
      Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
      cipher.init(Cipher.ENCRYPT_MODE, key);
      byte[] ciphertext = cipher.doFinal(plaintext);
      return Base64.getEncoder().encodeToString(ciphertext);
   }

   public byte[] encryptWithSessionKey(String nonce, SecretKey key, IvParameterSpec iv) throws Exception{
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.ENCRYPT_MODE, key, iv);
      return cipher.doFinal(nonce.getBytes("UTF-8"));
   }

   public byte[] decryptWithSessionKey(String nonce, SecretKey key, IvParameterSpec iv) throws Exception{
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.DECRYPT_MODE, key, iv);
      return cipher.doFinal(nonce.getBytes("UTF-8"));
   }

   public SecretKey generateSessionKey(){
      KeyGenerator keyGen = KeyGenerator.getInstance("AES");
      keyGen.init(256);
      return keyGen.generateKey();
   }



}