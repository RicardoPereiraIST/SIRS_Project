package pt.utl.ist.sirs.t05.sirsapp.Activities;

import pt.utl.ist.sirs.t05.sirsapp.R;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


public class HomeActivity extends AppCompatActivity  {
    private SecretKey sessionKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent pairIntent = getIntent();
        String keyString = pairIntent.getStringExtra("SessionKey");
        if(keyString != null) {
            this.sessionKey = new SecretKeySpec(Base64.decode(keyString, Base64.DEFAULT), "AES");
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        toolbar.setTitle("Home");
        setSupportActionBar(toolbar);

        Button pair_button = (Button) findViewById(R.id.pair_button);
        Button unlock_button = (Button) findViewById(R.id.unlock_button);

        pair_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent changeActivity = new Intent(HomeActivity.this, PairingActivity.class);
                HomeActivity.this.startActivity(changeActivity);
            }
        });

        /*
        unlock_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent changeActivity = new Intent(HomeActivity.this, UnlockActivity.class);
            }
        });*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about:
                Intent changeActivity = new Intent(HomeActivity.this, AboutActivity.class);
                HomeActivity.this.startActivity(changeActivity);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

