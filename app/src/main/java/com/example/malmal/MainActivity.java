package com.example.malmal;

import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.malmal.databinding.ActivityMainBinding;
import com.google.firebase.FirebaseApp;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private SceneSegmentation sceneSegmentation = new SceneSegmentation();
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private static final int MY_PERMISSIONS_REQUEST_STORAGE = 1;
    private enum Mode { SENDER, RECEIVER }
    private Mode currentMode = Mode.SENDER;
    private SenderCameraObserver sender;
    private ReceiverCameraObserver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            FirebaseApp.initializeApp(this);
            FirebaseApp app = FirebaseApp.getInstance();
            Log.d("FirebaseInit", "FirebaseApp is initialized: " + app.getName());
        } catch (IllegalStateException e) {
            Log.e("FirebaseInit", "FirebaseApp is not initialized.", e);
        }
        sender = new SenderCameraObserver(new Handler(), this);
        receiver = new ReceiverCameraObserver(new Handler(), this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        sceneSegmentation.initialize(this);


        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.fab)
                        .setAction("Action", null).show();
            }
        });

        RadioGroup radioGroup = binding.radioGroup;
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.senderButton) {
                    setReceiverMode(Mode.SENDER);
                } else {
                    setReceiverMode(Mode.RECEIVER);
                }
            }
        });
        setReceiverMode(Mode.SENDER);
        ((RadioButton) binding.senderButton).setChecked(true);

        malmalCheckPermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_STORAGE) {
            // 권한 부여 상태 확인 및 처리
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }


    public void malmalCheckPermission() {
        List<String> permissionsNeeded = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("malmalCheckPermission", "You need READ permission");
            permissionsNeeded.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("malmalCheckPermission", "You need WRITE permission");
            permissionsNeeded.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    MY_PERMISSIONS_REQUEST_STORAGE);
        }
    }

    private void toggleMode() {
        currentMode = (currentMode == Mode.SENDER) ? Mode.RECEIVER : Mode.SENDER;
        setReceiverMode(currentMode);
    }
    private void setReceiverMode(Mode mode) {
        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        if (mode == Mode.SENDER) {
            contentResolver.registerContentObserver(uri, true, sender);
            contentResolver.unregisterContentObserver(receiver);
        } else {
            contentResolver.registerContentObserver(uri, true, receiver);
            contentResolver.unregisterContentObserver(sender);
        }
    }
}