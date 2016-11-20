package pt.utl.ist.sirs.t05.sirsapp;

import android.util.Log;

import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by Diogo on 11/17/2016.
 */

public class WeakKey {

    private SecretKey key;

    public WeakKey(String password, String salt){
        try {

            byte[] byte_salt = salt.getBytes();

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

            KeySpec spec = new PBEKeySpec(password.toCharArray(), byte_salt, 1024, 128);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");

            this.key = secret;

        }catch(Exception e){
            Log.e("ERROR", e.getMessage());
        }
    }

    public byte[] encryptWithWeakKey(byte[] plaintext, SecretKey key) throws Exception{
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(plaintext);
    }

    public String decryptWithWeakKey(byte[] ciphertext, SecretKey key) throws Exception{
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return new String(cipher.doFinal(ciphertext), "UTF-8");
    }

    public SecretKey getKey(){
        return key;
    }
}
