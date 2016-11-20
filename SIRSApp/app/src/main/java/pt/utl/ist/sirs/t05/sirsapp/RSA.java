package pt.utl.ist.sirs.t05.sirsapp;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.PrivateKey;
import android.util.Base64;
import javax.crypto.Cipher;

/**
 * Created by JoseDias on 19/11/2016.
 */

public class RSA
{
    private KeyPair keyPair;

    public RSA() throws Exception
    {
        Initialize();
    }

    public void Initialize() throws Exception
    {
        KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
        keygen.initialize(512);
        keyPair = keygen.generateKeyPair();
    }

    public String encrypt(String plaintext, PublicKey key)  throws Exception
    {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF8"));
        return encodeBASE64(ciphertext);
    }
    public byte[] decrypt(String ciphertext, PrivateKey key)  throws Exception
    {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] plaintext = cipher.doFinal(decodeBASE64(ciphertext));
        return plaintext;
    }
    private String encodeBASE64(byte[] bytes)
    {
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private byte[] decodeBASE64(String text) throws Exception
    {
        byte[] data = Base64.decode(text, Base64.DEFAULT);
        return data;
    }

    public KeyPair getKeyPair(){
        return keyPair;
    }
}