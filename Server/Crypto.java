import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class Crypto {
	
	  public SecretKey generateEncryptionKey(String password, byte[] salt) throws Exception{
		  SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		  KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 1024, 128);
		  SecretKey tmp = factory.generateSecret(spec);
		  SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES"); 
		  return secret;
	  }
	
	 public String encryptPassword(String password) throws Exception{
		    MessageDigest cript = MessageDigest.getInstance("SHA-1");
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
		    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		    cipher.init(Cipher.ENCRYPT_MODE, key, iv);

		    BufferedReader br = new BufferedReader(new FileReader(f));


		    StringBuffer stringBuffer = new StringBuffer();
		    String line = null;

		    while((line = br.readLine())!=null){
		      stringBuffer.append(line).append("\n");
		    }

		    byte[] encryptedText = cipher.doFinal(stringBuffer.toString().getBytes());
		    br.close();
		    Files.write(f.toPath(), encryptedText);
		  }

		  public void decryptFile(File f, SecretKey key, IvParameterSpec iv) throws Exception{
		    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		    cipher.init(Cipher.DECRYPT_MODE, key, iv);

		    byte[] data = Files.readAllBytes(f.toPath());

		    String decryptedText = new String(cipher.doFinal(data), "UTF-8");
		    FileWriter fw = new FileWriter(f, false);
		    BufferedWriter bw = new BufferedWriter(fw);
		    bw.write(decryptedText);
		    bw.close();
		  }

}
