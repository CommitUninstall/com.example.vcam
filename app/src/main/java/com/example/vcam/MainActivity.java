package com.example.vcam;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.example.vcam.R;

import java.io.File;
import java.io.IOException;

public class MainActivity extends Activity {

    private Switch force_show_switch;
    private Switch disable_switch;
    private Switch play_sound_switch;
    private Switch force_private_dir;
    private Switch disable_toast_switch;

    private boolean permissionRequestInProgress = false;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionRequestInProgress = false;

        if (grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(MainActivity.this, R.string.permission_lack_warn, Toast.LENGTH_SHORT).show();
            }else {
                File camera_dir = new File (Environment.getExternalStorageDirectory().getAbsolutePath()+"/DCIM/Camera1/");
                if (!camera_dir.exists()){
                    camera_dir.mkdir();
                }
            }
        }

        sync_statue_with_files();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        permissionRequestInProgress = false; // Reset the flag

        if (requestCode == 1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "Storage permission granted!", Toast.LENGTH_SHORT).show();
                    File camera_dir = new File (Environment.getExternalStorageDirectory().getAbsolutePath()+"/DCIM/Camera1/");
                    if (!camera_dir.exists()){
                        camera_dir.mkdir();
                    }
                } else {
                    Toast.makeText(this, R.string.permission_lack_warn, Toast.LENGTH_SHORT).show();
                }
            }
        }

        // Sync the UI state after activity result
        sync_statue_with_files();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sync_statue_with_files();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button repo_button = findViewById(R.id.button);
        force_show_switch = findViewById(R.id.switch1);
        disable_switch = findViewById(R.id.switch2);
        play_sound_switch = findViewById(R.id.switch3);
        force_private_dir = findViewById(R.id.switch4);
        disable_toast_switch = findViewById(R.id.switch5);



        sync_statue_with_files();

        repo_button.setOnClickListener(v -> {
            // Add some debug info
            Log.d("[VCAM] ", "Android version: " + Build.VERSION.SDK_INT);
            Log.d("[VCAM] ", "Has permission: " + has_permission());

            Uri uri = Uri.parse("https://github.com/w2016561536/android_virtual_cam");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        });

        Button repo_button_chinamainland = findViewById(R.id.button2);
        repo_button_chinamainland.setOnClickListener(view -> {
            Uri uri = Uri.parse("https://gitee.com/w2016561536/android_virtual_cam");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        });

        disable_switch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                if (!has_permission()) {
                    request_permission();
                } else {
                    File disable_file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera1/disable.jpg");
                    if (disable_file.exists() != b){
                        if (b){
                            try {
                                disable_file.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }else {
                            disable_file.delete();
                        }
                    }
                }
                sync_statue_with_files();
            }
        });

        force_show_switch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                if (!has_permission()) {
                    request_permission();
                } else {
                    File force_show_switch = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera1/force_show.jpg");
                    if (force_show_switch.exists() != b){
                        if (b){
                            try {
                                force_show_switch.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }else {
                            force_show_switch.delete();
                        }
                    }
                }
                sync_statue_with_files();
            }
        });

        play_sound_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (compoundButton.isPressed()) {
                    if (!has_permission()) {
                        request_permission();
                    } else {
                        File play_sound_switch = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera1/no-silent.jpg");
                        if (play_sound_switch.exists() != b){
                            if (b){
                                try {
                                    play_sound_switch.createNewFile();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }else {
                                play_sound_switch.delete();
                            }
                        }
                    }
                    sync_statue_with_files();
                }
            }
        });

        force_private_dir.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                if (!has_permission()) {
                    request_permission();
                } else {
                    File force_private_dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera1/private_dir.jpg");
                    if (force_private_dir.exists() != b){
                        if (b){
                            try {
                                force_private_dir.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }else {
                            force_private_dir.delete();
                        }
                    }
                }
                sync_statue_with_files();
            }
        });


        disable_toast_switch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                if (!has_permission()) {
                    request_permission();
                } else {
                    File disable_toast_file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera1/no_toast.jpg");
                    if (disable_toast_file.exists() != b){
                        if (b){
                            try {
                                disable_toast_file.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }else {
                            disable_toast_file.delete();
                        }
                    }
                }
                sync_statue_with_files();
            }
        });

    }

    private void request_permission() {
        if (permissionRequestInProgress) {
            return; // Prevent multiple concurrent permission requests
        }

        permissionRequestInProgress = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            // Request media permissions
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(R.string.permission_lack_warn);
            builder.setMessage(R.string.permission_description);

            builder.setNegativeButton(R.string.negative, (dialogInterface, i) -> {
                permissionRequestInProgress = false;
                Toast.makeText(MainActivity.this, R.string.permission_lack_warn, Toast.LENGTH_SHORT).show();
            });

            builder.setPositiveButton(R.string.positive, (dialogInterface, i) -> {
                requestPermissions(new String[]{
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO
                }, 1);
            });

            builder.setOnCancelListener(dialog -> permissionRequestInProgress = false);
            builder.show();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11+
            // Request MANAGE_EXTERNAL_STORAGE permission
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(R.string.permission_lack_warn);
            builder.setMessage("This app needs access to manage external storage to function properly. You'll be taken to the settings page.");

            builder.setNegativeButton(R.string.negative, (dialogInterface, i) -> {
                permissionRequestInProgress = false;
                Toast.makeText(MainActivity.this, R.string.permission_lack_warn, Toast.LENGTH_SHORT).show();
            });

            builder.setPositiveButton(R.string.positive, (dialogInterface, i) -> {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, 1);
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivityForResult(intent, 1);
                }
            });

            builder.setOnCancelListener(dialog -> permissionRequestInProgress = false);
            builder.show();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android 6+
            // Request legacy storage permissions
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(R.string.permission_lack_warn);
            builder.setMessage(R.string.permission_description);

            builder.setNegativeButton(R.string.negative, (dialogInterface, i) -> {
                permissionRequestInProgress = false;
                Toast.makeText(MainActivity.this, R.string.permission_lack_warn, Toast.LENGTH_SHORT).show();
            });

            builder.setPositiveButton(R.string.positive, (dialogInterface, i) -> {
                requestPermissions(new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, 1);
            });

            builder.setOnCancelListener(dialog -> permissionRequestInProgress = false);
            builder.show();
        } else {
            permissionRequestInProgress = false;
        }
    }

    private boolean has_permission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+ (API 33)
            // Check for media permissions
            return checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11+ (API 30)
            // For broad file access, prefer MANAGE_EXTERNAL_STORAGE
            return Environment.isExternalStorageManager() ||
                    (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                            && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android 6+ (API 23)
            // Check for legacy storage permissions
            return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Pre-Android 6
    }


    private void sync_statue_with_files() {
        Log.d(this.getApplication().getPackageName(), "[VCAM] [sync] Synchronizing switch states");
        Log.d("[VCAM] ", "sync_statue_with_files: has_permission=" + has_permission() + ", permissionRequestInProgress=" + permissionRequestInProgress);

        if (!has_permission() && !permissionRequestInProgress){
            Log.d("[VCAM] ", "Requesting permission from sync_statue_with_files");
            request_permission();
            return;
        }

        if (has_permission()) {
            File camera_dir = new File (Environment.getExternalStorageDirectory().getAbsolutePath()+"/DCIM/Camera1");
            if (!camera_dir.exists()){
                camera_dir.mkdir();
            }
        }

        try {
            File disable_file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera1/disable.jpg");
            disable_switch.setChecked(disable_file.exists());

            File force_show_file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera1/force_show.jpg");
            force_show_switch.setChecked(force_show_file.exists());

            File play_sound_file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera1/no-silent.jpg");
            play_sound_switch.setChecked(play_sound_file.exists());

            File force_private_dir_file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera1/private_dir.jpg");
            force_private_dir.setChecked(force_private_dir_file.exists());

            File disable_toast_file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera1/no_toast.jpg");
            disable_toast_switch.setChecked(disable_toast_file.exists());
        } catch (Exception e) {
            Log.w(this.getApplication().getPackageName(), "[VCAM] [sync] Cannot access external storage: " + e.getMessage());
        }
    }


}



