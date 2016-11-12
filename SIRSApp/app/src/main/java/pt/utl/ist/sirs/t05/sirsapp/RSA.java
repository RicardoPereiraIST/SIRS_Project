package pt.utl.ist.sirs.t05.sirsapp;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;


/**
 * Created by Diogo on 11/11/2016.
 */

public class RSA {

    private Context context;

    public RSA(Context context){
        this.context = context;
    }

    public String readKeyFile(String filename) throws IOException{
        InputStream input = context.getAssets().open(filename);

        java.util.Scanner s = new java.util.Scanner(input).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static PrivateKey getPrivateKey(String fileContent) throws Exception{

        String keyString = fileContent.replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "");

        byte[] keyBytes = Base64.decode(keyString, Base64.DEFAULT);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        PrivateKey key = KeyFactory.getInstance("RSA", "BC").generatePrivate(keySpec);

        return key;
    }

    public static PublicKey getPublicKey(String fileContent) throws Exception{

        String keyString = fileContent.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "");

        byte[] keyBytes = Base64.decode(keyString, Base64.DEFAULT);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        PublicKey key = KeyFactory.getInstance("RSA").generatePublic(keySpec);

        return key;
    }

}
