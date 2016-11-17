package pt.utl.ist.sirs.t05.sirsapp;

import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.Random;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by Diogo on 11/17/2016.
 */

public class Key {

    private SecretKey key;

    public Key(String password){
        try {

            String salt_string = "client_6000_saltclient_6000_salt";
            byte[] salt = salt_string.getBytes();

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 1024, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");

            this.key = secret;

        }catch(Exception e){
            Log.e("ERROR", e.getMessage());
        }
    }

    public SecretKey getKey(){
        return key;
    }


}
