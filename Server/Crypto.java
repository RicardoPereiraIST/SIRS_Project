import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import java.io.FileOutputStream;
import java.io.FileInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;

public class Crypto {
	
	  public SecretKey generateEncryptionKey(String password, byte[] salt) throws Exception{
		  SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		  KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 1024, 128);
		  SecretKey tmp = factory.generateSecret(spec);
		  SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES"); 
		  return secret;
	  }
	
	 public String encryptPassword(String password) throws Exception{
		    MessageDigest cript = MessageDigest.getInstance("SHA-256");
		    cript.reset();
		    cript.update(password.getBytes("utf8"));
		    String hex = String.format("%040x", new BigInteger(1,cript.digest()));
		    for(int i = 0; i<2; i++){
		      cript.reset();
		      cript.update(hex.getBytes("utf8"));
		      hex = String.format("%040x", new BigInteger(1,cript.digest()));
		    }
		    return hex;
		  }

		  public String generateSalt() throws Exception{
		    SecureRandom r = new SecureRandom();
		    String salt = new BigInteger(130, r).toString(32);
		      
		    File f = new File(".Users.txt");
		    BufferedReader br = new BufferedReader(new FileReader(f));

		    String line = br.readLine();
		    while(line != null){
		      String[] parts = line.split(" ");
		      if(parts[1].equals(salt)){
		    	br.close();
		        return generateSalt();
		      }
		      line = br.readLine();
		    }
		    br.close();

		    return salt;
		  }

		  public void encryptFile(File f, SecretKey key, IvParameterSpec iv) throws Exception{
        	FileInputStream file = new FileInputStream(f.getPath().toString());

        	String[] dirs = f.getPath().toString().split("/");
        	String[] files = dirs[2].split("\\.");

        	FileOutputStream outStream = new FileOutputStream(dirs[0]+"/"+dirs[1]+"/"+files[0]+"_enc."+files[1]);
        	Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		    cipher.init(Cipher.ENCRYPT_MODE, key, iv);
		    CipherOutputStream cos = new CipherOutputStream(outStream, cipher);
		    byte[] buf = new byte[1024];
		    int read;
		    while((read=file.read(buf))!=-1){
		    	cos.write(buf, 0, read);
		    }
		    file.close();
		    outStream.flush();
		    cos.close();

		    f.delete();
		  }

		  public void decryptFile(File f, SecretKey key, IvParameterSpec iv) throws Exception{
		  	String[] dirs = f.getPath().toString().split("/");
        	String[] files = dirs[2].split("\\.");
        	String[] enc = files[0].split("\\_");
        	FileInputStream file = new FileInputStream(f.getPath().toString());

        	FileOutputStream outStream = new FileOutputStream(dirs[0]+"/"+dirs[1]+"/"+enc[0]+"."+files[1]);


        	Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		    cipher.init(Cipher.DECRYPT_MODE, key, iv);
		    CipherOutputStream cos = new CipherOutputStream(outStream, cipher);
		    byte[] buf = new byte[1024];
		    int read;
		    while((read=file.read(buf))!=-1){
		    	cos.write(buf, 0, read);
		    }
		    file.close();
		    outStream.flush();
		    cos.close();

		    f.delete();
		  }
		  
		   // ------- Initial key -------------------------

		   public SecretKey generateInitialKey(String token){
		      try{
		         byte[] saltBytes = "1234561234567812".getBytes();

		         SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

		         KeySpec spec = new PBEKeySpec(token.toCharArray(), saltBytes, 1024, 256);
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
		   
		// ------------ RSA --------------------

		   public String encryptWithPublicKey(byte[] plaintext, PublicKey key)throws Exception{
		      Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		      cipher.init(Cipher.ENCRYPT_MODE, key);
		      byte[] ciphertext = cipher.doFinal(plaintext);
		      return DatatypeConverter.printBase64Binary(ciphertext);
		   }

}
