import java.math.BigInteger;
import java.security.MessageDigest;

public class Hash {
	
    public String generateHash(String stringToEncrypt) throws Exception{
      MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      messageDigest.reset();
      messageDigest.update(stringToEncrypt.getBytes("utf8"));
      String encrypted = String.format("%040x", new BigInteger(1, messageDigest.digest()));
      return encrypted;
    }

}
