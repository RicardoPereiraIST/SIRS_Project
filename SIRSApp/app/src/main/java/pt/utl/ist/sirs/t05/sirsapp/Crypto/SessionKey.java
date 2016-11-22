package pt.utl.ist.sirs.t05.sirsapp.Crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/**
 * Created by Diogo on 11/20/2016.
 */

public class SessionKey {

    public byte[] encryptWithSessionKey(String nonce, SecretKey key)throws Exception{
        String ivStr = "Randominitvector";
        IvParameterSpec iv = new IvParameterSpec(ivStr.getBytes());

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        return cipher.doFinal(nonce.getBytes("UTF-8"));
    }

    public byte[] decryptWithSessionKey(byte[] nonce, SecretKey key)throws Exception{
        String ivStr = "Randominitvector";
        IvParameterSpec iv = new IvParameterSpec(ivStr.getBytes());

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        return cipher.doFinal(nonce);
    }
}
