package se.zinokader.spotiq.activity;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.mukesh.tinydb.TinyDB;

public abstract class BaseActivity extends AppCompatActivity {

    Typeface ROBOTOLIGHT;
    TinyDB datastore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        datastore = new TinyDB(this);
        ROBOTOLIGHT = Typeface.createFromAsset(getAssets(), "fonts/robotolight.ttf");
    }

}
