package com.example.camerasurface_v3;
import android.Manifest;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.camerasurface_v3.MainActivity;
import com.example.camerasurface_v3.R;

import java.util.Locale;

public class LaunchActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launch_activity);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            //check permission
            if(!hasPermissions(PERMISSIONS)){
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }else{
                Intent mainIntent = new Intent(LaunchActivity.this, MainActivity.class);
                startActivity(mainIntent);
                finish();
            }
        }
    }

    //--permission--//
    static final int PERMISSIONS_REQUEST_CODE =1000;
    String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_IMAGES};

    private boolean hasPermissions(String[] permissions){
        int result;

        //check permissions on 'PERMISSIONS[]'
        for(String perms : permissions){
            result = ContextCompat.checkSelfPermission(this,perms);

            if(result== PackageManager.PERMISSION_DENIED){
                return false;
            }
        }
        return true;
    }
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);

        switch (requestCode){
            case PERMISSIONS_REQUEST_CODE:
                if(grantResults.length > 0){
                    boolean cameraPermissionAccepted = grantResults[0]
                            == PackageManager.PERMISSION_GRANTED;
                    boolean diskPermissionAccepted = grantResults[1]
                            == PackageManager.PERMISSION_GRANTED;
                    boolean disk2PermissionAccepted = grantResults[2]
                            == PackageManager.PERMISSION_GRANTED;

                    if(!cameraPermissionAccepted || !diskPermissionAccepted || !disk2PermissionAccepted)
                        showDialogForPermission("using CameraApp need permission.");
                    else {
                        Intent mainIntent = new Intent(LaunchActivity.this, MainActivity.class);
                        startActivity(mainIntent);
                        finish();
                    }
                }
                break;
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String s) {
        AlertDialog.Builder builder = new AlertDialog.Builder(LaunchActivity.this);
        builder.setTitle("Alert");
        builder.setMessage(s);
        builder.setCancelable(false);
        builder.setPositiveButton("yes", (dialog, which) -> requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE));
        builder.setNegativeButton("no", (dialog, which) -> finish());
        builder.create().show();
    }
}
