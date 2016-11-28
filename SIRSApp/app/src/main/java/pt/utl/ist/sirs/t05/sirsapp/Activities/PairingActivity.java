package pt.utl.ist.sirs.t05.sirsapp.Activities;

import pt.utl.ist.sirs.t05.sirsapp.Pair;
import pt.utl.ist.sirs.t05.sirsapp.R;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import javax.crypto.SecretKey;


public class PairingActivity extends AppCompatActivity  {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pairing_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        toolbar.setTitle("Pairing");
        setSupportActionBar(toolbar);

        Button saveButton = (Button)findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                EditText tokenText = (EditText)findViewById(R.id.token_box);
                if(!tokenText.getText().toString().equals("")){
                    Toast.makeText(view.getContext(), "Token saved!", Toast.LENGTH_SHORT).show();

                    Pair p = new Pair(tokenText.getText().toString());
                    p.execute();

                    try {
                        SecretKey sessionKey = p.get();
                        String keyString = Base64.encodeToString(sessionKey.getEncoded(), Base64.DEFAULT);

                        Intent changeActivity = new Intent(PairingActivity.this, HomeActivity.class);
                        changeActivity.putExtra("SessionKey", keyString);
                        PairingActivity.this.startActivity(changeActivity);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                else{
                    Toast.makeText(view.getContext(), "Token is empty!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}

