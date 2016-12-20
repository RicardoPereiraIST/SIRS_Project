import java.net.*;
import java.io.*;
import java.security.PublicKey;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.SecretKey;
import javax.xml.bind.DatatypeConverter;


public class Pair extends Thread {
   private ServerSocket serverSocket;

   private SecretKey initialKey;
   private SecretKey sessionKey;
   private PublicKey mobilePublicKey;

   private Timestamps ts;
   private Hash h;
   private Crypto crypto;
   
   private String token;
   
   public Pair(int port, String token) throws IOException {
      this.serverSocket = new ServerSocket(port);
      this.token = token;
      this.ts = new Timestamps();
      this.h = new Hash();
      this.crypto = new Crypto();
      }

   public SecretKey getSessionKey(){
    return sessionKey;
   }

   public void run() {
         try {
         
            // Generate the initial key based on the password
            initialKey = crypto.generateInitialKey(this.token);

            System.out.println("\nWaiting for client on port " + serverSocket.getLocalPort() + "...");
            Socket server = serverSocket.accept();
            
            System.out.println("Just connected to " + server.getRemoteSocketAddress());
            DataInputStream in = new DataInputStream(server.getInputStream());

            // Receive from client the public key
            String received = in.readUTF();

            String parts[] = received.split("\\.");

            String hash = h.generateHash(parts[1]);
            if(!parts[2].equals(hash)){
              System.out.println("Wrong hash!");
              serverSocket.close();
              return;
            }

            byte[] encrypted_public_key = DatatypeConverter.parseBase64Binary(parts[0]);
            byte[] decrypted_public_key = crypto.decryptWithInitialKey(encrypted_public_key, initialKey);

            mobilePublicKey = KeyFactory.getInstance("RSA").
                                    generatePublic(new X509EncodedKeySpec(decrypted_public_key));

            // Generate and send the enrypted session key
            System.out.println("Sending session key encrypted...");
            sessionKey = crypto.generateSessionKey();

            String sessionKeyToSend = crypto.encryptWithPublicKey(sessionKey.getEncoded(), mobilePublicKey);
            long timestamp = ts.generateTimeStamp();
            hash = h.generateHash(String.valueOf(timestamp));
            String dataToSend = sessionKeyToSend + "." + String.valueOf(timestamp) + "." + hash;

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
}   