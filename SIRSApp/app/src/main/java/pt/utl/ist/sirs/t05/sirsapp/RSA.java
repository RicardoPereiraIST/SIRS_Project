package pt.utl.ist.sirs.t05.sirsapp;

import android.content.Context;
import android.util.Base64;

import java.io.IOException;
import java.io.InputStream;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;


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

    public PrivateKey getPrivateKey() throws Exception{
        String key = readKeyFile("private.key");
        return null;
    }

    public PublicKey getPublicKey() throws Exception{
        String key = readKeyFile("server_public.key");
        return null;
    }






}
