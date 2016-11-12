package pt.utl.ist.sirs.t05.sirsapp;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.io.IOException;
import java.net.ServerSocket;
import java.security.PrivateKey;
import java.security.PublicKey;

public class MainActivity extends AppCompatActivity {

    private ServerSocket serverSocket;
    Handler updateConversationHandler;
    private ServerThread serverThread;

    public static PrivateKey PRIVATE;
    public static PublicKey PUBLIC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        updateConversationHandler = new Handler();

        // KEYS ----------------------------------
        RSA rsa = new RSA(findViewById(R.id.activity_main).getContext());
        try {
            String privateKey = rsa.readKeyFile("private.key");
            String publicKey = rsa.readKeyFile("server_public.key");
            PRIVATE = RSA.getPrivateKey(privateKey);
            PUBLIC = RSA.getPublicKey(publicKey);
        }catch(Exception e){
            e.printStackTrace();
        }
        // ----------------------------------------

        Thread thread = null;
        this.serverThread = new ServerThread((TextView)findViewById(R.id.textView), updateConversationHandler);

        thread = new Thread(serverThread);
        thread.start();
    }

    @Override
    protected void onStop(){
        super.onStop();
        try{
            serverThread.getServerSocket().close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
