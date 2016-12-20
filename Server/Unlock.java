import java.net.*;
import java.io.*;
import javax.crypto.SecretKey;
import java.util.Base64;
import javax.crypto.spec.IvParameterSpec;
import java.security.SecureRandom;


public class Unlock extends Thread {
   private ServerSocket serverSocket;

   private SecretKey sessionKey;
   private SecretKey fileKey;
   private User currentUser;
   private IvParameterSpec iv;
   
   private Timestamps ts;
   private Hash h;
   private Crypto crypto;

   private FileOperations fo = new FileOperations();
   
   public Unlock(){}
 
   public Unlock(int port, SecretKey sessionKey, User currentUser, IvParameterSpec iv, SecretKey fileKey) throws IOException {
      this.serverSocket = new ServerSocket(port);
      this.sessionKey = sessionKey;
      this.currentUser = currentUser;
      this.iv = iv;
      this.fileKey = fileKey;
      this.h = new Hash();
      this.ts = new Timestamps();
      this.crypto = new Crypto();
    }

   public void run() {
         try {
            Socket server = serverSocket.accept();
            DataInputStream in = new DataInputStream(server.getInputStream());
            DataOutputStream out = new DataOutputStream(server.getOutputStream());

            server.setSoTimeout(5000);

            //Respond to the client challenge
            long nonce = receiveAndDecryptNonce(in);

            if(nonce == 0){
                  System.out.println("Replay attack detected, closing connection!");
                  server.close();
                  return;
            }

            System.out.println("Nonce received from client -> " + nonce);
            // Calculate 
            nonce++;
            String stringNonce = Long.toString(nonce);
            System.out.println("Nonce calculated -> " + stringNonce);

            encryptAndSendNonce(stringNonce, out);


             if (Manager.isLocked == true){
               fo.unlock(currentUser, fileKey, iv);
             }

            // ------------ START CHALLENGES ----------------------------------
            while(true){
               System.out.println("Sending the continuous challenge nonce...");
               String nonceString = generateNonce();
               System.out.println("Nonce is -> " + nonceString);

               encryptAndSendNonce(nonceString, out);

               nonce = receiveAndDecryptNonce(in);

               if(nonce == 0){
                  System.out.println("Replay attack detected, closing connection!");
                  serverSocket.close();
                  return;
               }else if(nonce == 77999){
                  System.out.println("Client requested file lock, locking...");
                  fo.lock(currentUser, fileKey, iv);
                  try{
                    serverSocket.close();
                    return;
                  }catch(Exception e){
                    System.out.println("There was an error in the thread");
                    return;
                  }
               }

               System.out.println("Comparing " + nonce + " and " + (Long.valueOf(nonceString).longValue() + 1));
               if(nonce == Long.valueOf(nonceString).longValue() + 1){
                  System.out.println("Nonce is correct, sleeping...");
               }else{
                  System.out.println("Something is not right... closing the connection");
                  serverSocket.close();
                  return;
               }

               // Sleeping 5 seconds
               Thread.sleep(5000);
            }

            
         }catch(IOException s) {
            System.out.println("Client is away, locking files!");
            try{
            serverSocket.close();
            fo.lock(currentUser, fileKey, iv);
          }catch(Exception e){
            System.out.println("There was an error in the thread");
            return;
          }
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
      byte[] encryptedNonce = crypto.encryptWithSessionKey(nonce, sessionKey);
      String encryptedNonceString = Base64.getMimeEncoder().encodeToString(encryptedNonce);
      long timestamp = ts.generateTimeStamp();

      String hash = h.generateHash(String.valueOf(timestamp));

      String dataToSend = encryptedNonceString + "." + String.valueOf(timestamp) + "." + hash;
      System.out.println("Sending the nonce...");
      out.writeUTF(dataToSend);
   }
   
   public long receiveAndDecryptNonce(DataInputStream in) throws Exception{
      String nonceString = in.readUTF();

      String[] parts = nonceString.split("\\.");

      String hash = h.generateHash(parts[1]);

      if(!parts[2].equals(hash)){
        return 0;
      }

      if(!ts.isWithinRange(Long.valueOf(parts[1]).longValue())){
         return 0;
       }

      byte[] decodedNonce = Base64.getMimeDecoder().decode(parts[0]);
      byte[] decryptedNonce = crypto.decryptWithSessionKey(decodedNonce, sessionKey);
      String decryptedNonceString = new String(decryptedNonce, "UTF-8");
      long nonce = Long.valueOf(decryptedNonceString).longValue();
      return nonce;
   }
}
