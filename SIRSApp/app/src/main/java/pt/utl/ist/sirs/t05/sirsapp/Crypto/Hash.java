package pt.utl.ist.sirs.t05.sirsapp.Crypto;

import java.security.MessageDigest;

/**
 * Created by Ricardo on 05/12/2016.
 */

public class Hash {
    public String generateHash(String stringToEncrypt) throws Exception{
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(stringToEncrypt.getBytes());
        String encryptedString = new String(messageDigest.digest());
        return encryptedString;
    }
}
