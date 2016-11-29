package pt.utl.ist.sirs.t05.sirsapp.Activities;

import pt.utl.ist.sirs.t05.sirsapp.AsyncTasks.Unlock;
import pt.utl.ist.sirs.t05.sirsapp.R;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.View;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class UnlockActivity extends AppCompatActivity {

    private SecretKey sessionKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unlock);

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        toolbar.setTitle("Unlock Files");
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        Intent intent = getIntent();
        String keyString = intent.getStringExtra("SessionKey");

        this.sessionKey = new SecretKeySpec(Base64.decode(keyString, Base64.DEFAULT), "AES");

        new Unlock(sessionKey).execute(UnlockActivity.this);
    }
}
