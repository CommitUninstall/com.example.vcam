package com.example.vcam;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.InputConfiguration;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.widget.Toast;

import com.example.vcam.OutputImageFormat;
import com.example.vcam.VideoToFrames;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookMain implements IXposedHookLoadPackage {
    public static Surface mSurface;
    public static SurfaceTexture mSurfacetexture;
    public static MediaPlayer mMediaPlayer;
    public static SurfaceTexture fake_SurfaceTexture;
    public static Camera origin_preview_camera;

    public static Camera camera_onPreviewFrame;
    public static Camera start_preview_camera;
    public static volatile byte[] data_buffer = {0};
    public static byte[] input;
    public static int mhight;
    public static int mwidth;
    public static boolean is_someone_playing;
    public static boolean is_hooked;
    public static VideoToFrames hw_decode_obj;
    public static VideoToFrames c2_hw_decode_obj;
    public static VideoToFrames c2_hw_decode_obj_1;
    public static SurfaceTexture c1_fake_texture;
    public static Surface c1_fake_surface;
    public static SurfaceHolder ori_holder;
    public static MediaPlayer mplayer1;
    public static Camera mcamera1;
    public int imageReaderFormat = 0;
    public static boolean is_first_hook_build = true;

    public static int onemhight;
    public static int onemwidth;
    public static Class camera_callback_calss;

    public static String video_path = "/storage/emulated/0/DCIM/Camera1/";

    public static Surface c2_preview_Surfcae;
    public static Surface c2_preview_Surfcae_1;
    public static Surface c2_reader_Surfcae;
    public static Surface c2_reader_Surfcae_1;
    public static MediaPlayer c2_player;
    public static MediaPlayer c2_player_1;
    public static Surface c2_virtual_surface;
    public static SurfaceTexture c2_virtual_surfaceTexture;
    public boolean need_recreate;
    public static CameraDevice.StateCallback c2_state_cb;
    public static CaptureRequest.Builder c2_builder;
    public static SessionConfiguration fake_sessionConfiguration;
    public static SessionConfiguration sessionConfiguration;
    public static OutputConfiguration outputConfiguration;
    public boolean need_to_show_toast = true;
    private static boolean isSettingUpMediaPlayer = false;

    public int c2_ori_width = 1280;
    public int c2_ori_height = 720;

    public static Class c2_state_callback;
    public Context toast_content;

    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Exception {
        // Debug logging for package initialization
        XposedBridge.log("[VCAM] [DEBUG] Loading hooks for package: " + lpparam.packageName);

        // Special debug mode for Snapchat
        boolean isSnapchat = lpparam.packageName.equals("com.snapchat.android");
        if (isSnapchat) {
            XposedBridge.log("[VCAM] [SNAPCHAT] Initializing hooks for Snapchat - enabling enhanced debugging");
        }
        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "setPreviewTexture", SurfaceTexture.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                XposedBridge.log("[VCAM] [DEBUG] " + lpparam.packageName + " called setPreviewTexture");
                File file = new File(video_path + "virtual.mp4");
                XposedBridge.log("[VCAM] [DEBUG] " + lpparam.packageName + " checking virtual.mp4 at: " + file.getAbsolutePath() + " exists: " + file.exists() + " canRead: " + file.canRead());
                if (file.exists()) {
                    File control_file = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "disable.jpg");
                    if (control_file.exists()){
                        return;
                    }
                    if (is_hooked) {
                        is_hooked = false;
                        return;
                    }
                    if (param.args[0] == null) {
                        return;
                    }
                    if (param.args[0].equals(c1_fake_texture)) {
                        return;
                    }
                    if (origin_preview_camera != null && origin_preview_camera.equals(param.thisObject)) {
                        param.args[0] = fake_SurfaceTexture;
                        XposedBridge.log("[VCAM] Found duplicate " + origin_preview_camera.toString());
                        return;
                    } else {
                        XposedBridge.log("[VCAM] Creating preview");
                    }

                    origin_preview_camera = (Camera) param.thisObject;
                    mSurfacetexture = (SurfaceTexture) param.args[0];
                    if (fake_SurfaceTexture == null) {
                        fake_SurfaceTexture = new SurfaceTexture(10);
                    } else {
                        fake_SurfaceTexture.release();
                        fake_SurfaceTexture = new SurfaceTexture(10);
                    }
                    param.args[0] = fake_SurfaceTexture;
                } else {
                    File toast_control = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "no_toast.jpg");
                    need_to_show_toast = !toast_control.exists();
                    if (toast_content != null && need_to_show_toast) {
                        try {
                            Toast.makeText(toast_content, "Virtual video file not found\n" + lpparam.packageName + "\nPath: " + video_path, Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("[VCAM] [toast]" + ee.toString());
                        }
                    }
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.camera2.CameraManager", lpparam.classLoader, "openCamera", String.class, CameraDevice.StateCallback.class, Handler.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("[VCAM] [DEBUG]" + lpparam.packageName + " called Camera2 openCamera (3-param)");
                if (param.args[1] == null) {
                    return;
                }
                if (param.args[1].equals(c2_state_cb)) {
                    return;
                }
                c2_state_cb = (CameraDevice.StateCallback) param.args[1];
                c2_state_callback = param.args[1].getClass();
                File control_file = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "disable.jpg");
                if (control_file.exists()) {
                    return;
                }
                File file = new File(video_path + "virtual.mp4");
                File toast_control = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "no_toast.jpg");
                need_to_show_toast = !toast_control.exists();
                if (!file.exists()) {
                    if (toast_content != null && need_to_show_toast) {
                        try {
                            Toast.makeText(toast_content, "Virtual video file not found\n" + lpparam.packageName + "\nPath: " + video_path, Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("[VCAM] [toast]" + ee.toString());
                        }
                    }
                    return;
                }
                XposedBridge.log("[VCAM][DEBUG] " + lpparam.packageName + " - 1-param camera initialization, class: " + c2_state_callback.toString());
                is_first_hook_build = true;
                process_camera2_init(c2_state_callback);
            }
        });


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            XposedHelpers.findAndHookMethod("android.hardware.camera2.CameraManager", lpparam.classLoader, "openCamera", String.class, Executor.class, CameraDevice.StateCallback.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("[VCAM] [DEBUG]" + lpparam.packageName + " called Camera2 openCamera (3-param with Executor)");
                    if (param.args[2] == null) {
                        return;
                    }
                    if (param.args[2].equals(c2_state_cb)) {
                        return;
                    }
                    c2_state_cb = (CameraDevice.StateCallback) param.args[2];
                    File control_file = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "disable.jpg");
                    if (control_file.exists()) {
                        return;
                    }
                    File file = new File(video_path + "virtual.mp4");
                    File toast_control = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "no_toast.jpg");
                    need_to_show_toast = !toast_control.exists();
                    if (!file.exists()) {
                        if (toast_content != null && need_to_show_toast) {
                            try {
                                Toast.makeText(toast_content, "Virtual video file not found\n" + lpparam.packageName + "\nPath: " + video_path, Toast.LENGTH_SHORT).show();
                            } catch (Exception ee) {
                                XposedBridge.log("[VCAM] [toast]" + ee.toString());
                            }
                        }
                        return;
                    }
                    c2_state_callback = param.args[2].getClass();
                    XposedBridge.log("[VCAM] [DEBUG]" + lpparam.packageName + " - 2-param camera initialization, class: " + c2_state_callback.toString());
                    is_first_hook_build = true;
                    process_camera2_init(c2_state_callback);
                }
            });
        }


        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "setPreviewCallbackWithBuffer", Camera.PreviewCallback.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args[0] != null) {
                    process_callback(param);
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "addCallbackBuffer", byte[].class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args[0] != null) {
                    param.args[0] = new byte[((byte[]) param.args[0]).length];
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "setPreviewCallback", Camera.PreviewCallback.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args[0] != null) {
                    process_callback(param);
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "setOneShotPreviewCallback", Camera.PreviewCallback.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args[0] != null) {
                    process_callback(param);
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "takePicture", Camera.ShutterCallback.class, Camera.PictureCallback.class, Camera.PictureCallback.class, Camera.PictureCallback.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                XposedBridge.log("[VCAM] 4-param photo capture");
                if (param.args[1] != null) {
                    process_a_shot_YUV(param);
                }

                if (param.args[3] != null) {
                    process_a_shot_jpeg(param, 3);
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.media.MediaRecorder", lpparam.classLoader, "setCamera", Camera.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                File toast_control = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "no_toast.jpg");
                need_to_show_toast = !toast_control.exists();
                XposedBridge.log("[VCAM] [record]" + lpparam.packageName);
                if (toast_content != null && need_to_show_toast) {
                    try {
                        Toast.makeText(toast_content, "App: " + lpparam.appInfo.name + " (" + lpparam.packageName + ")" + " started recording, but interception is not currently supported", Toast.LENGTH_SHORT).show();
                    }catch (Exception ee){
                        XposedBridge.log("[VCAM] [toast]" + Arrays.toString(ee.getStackTrace()));
                    }
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.app.Instrumentation", lpparam.classLoader, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (param.args[0] instanceof Application) {
                    try {
                        toast_content = ((Application) param.args[0]).getApplicationContext();
                    } catch (Exception ee) {
                        XposedBridge.log("[VCAM] " + ee.toString());
                    }
                    File force_private = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/Camera1/private_dir.jpg");
                    if (toast_content != null) {
                        int auth_statue = 0;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            try {
                                auth_statue += (toast_content.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) + 1);
                            } catch (Exception ee) {
                                XposedBridge.log("[VCAM] [permission-check]" + ee.toString());
                            }
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    auth_statue += (toast_content.checkSelfPermission(Manifest.permission.MANAGE_EXTERNAL_STORAGE) + 1);
                                }
                            } catch (Exception ee) {
                                XposedBridge.log("[VCAM] [permission-check]" + ee.toString());
                            }
                        }else {
                            if (toast_content.checkCallingPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ){
                                auth_statue = 2;
                            }
                        }
                        //Permission check completed
                        if (auth_statue < 1 || force_private.exists()) {
                            File shown_file = new File(toast_content.getExternalFilesDir(null).getAbsolutePath() + "/Camera1/");
                            if ((!shown_file.isDirectory()) && shown_file.exists()) {
                                shown_file.delete();
                            }
                            if (!shown_file.exists()) {
                                shown_file.mkdir();
                            }
                            shown_file = new File(toast_content.getExternalFilesDir(null).getAbsolutePath() + "/Camera1/" + "has_shown");
                            File toast_force_file = new File(Environment.getExternalStorageDirectory().getPath()+ "/DCIM/Camera1/force_show.jpg");
//                            if ((!lpparam.packageName.equals(BuildConfig.APPLICATION_ID)) && ((!shown_file.exists()) || toast_force_file.exists())) {
//                                try {
//                                    Toast.makeText(toast_content, lpparam.packageName + " has not granted local directory read permission, please check permissions\nCamera1 is currently redirected to " + toast_content.getExternalFilesDir(null).getAbsolutePath() + "/Camera1/", Toast.LENGTH_SHORT).show();
//                                    FileOutputStream fos = new FileOutputStream(toast_content.getExternalFilesDir(null).getAbsolutePath() + "/Camera1/" + "has_shown");
//                                    String info = "shown";
//                                    fos.write(info.getBytes());
//                                    fos.flush();
//                                    fos.close();
//                                } catch (Exception e) {
//                                    XposedBridge.log("[VCAM] [switch-dir]" + e.toString());
//                                }
//                            }
                            video_path = toast_content.getExternalFilesDir(null).getAbsolutePath() + "/Camera1/";
                        }else {
                            video_path = Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/";
                        }
                    } else {
                        video_path = Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/";
                        File uni_DCIM_path = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/");
                        if (uni_DCIM_path.canWrite()) {
                            File uni_Camera1_path = new File(video_path);
                            if (!uni_Camera1_path.exists()) {
                                uni_Camera1_path.mkdir();
                            }
                        }
                    }
                    XposedBridge.log("[VCAM] [DEBUG]" + lpparam.packageName + " final video_path resolved to: " + video_path);
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "startPreview", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                File file = new File(video_path + "virtual.mp4");
                File toast_control = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "no_toast.jpg");
                need_to_show_toast = !toast_control.exists();
                if (!file.exists()) {
                    if (toast_content != null && need_to_show_toast) {
                        try {
                            Toast.makeText(toast_content, "Virtual video file not found\n" + lpparam.packageName + "\nPath: " + video_path, Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("[VCAM] [toast]" + ee.toString());
                        }
                    }
                    return;
                }
                File control_file = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "disable.jpg");
                if (control_file.exists()) {
                    return;
                }
                is_someone_playing = false;
                XposedBridge.log("[VCAM] Starting preview");
                start_preview_camera = (Camera) param.thisObject;
                if (ori_holder != null) {

                    if (mplayer1 == null) {
                        mplayer1 = new MediaPlayer();
                    } else {
                        mplayer1.release();
                        mplayer1 = null;
                        mplayer1 = new MediaPlayer();
                    }
                    if (!ori_holder.getSurface().isValid() || ori_holder == null) {
                        return;
                    }
                    mplayer1.setSurface(ori_holder.getSurface());
                    File sfile = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "no-silent.jpg");
                    if (!(sfile.exists() && (!is_someone_playing))) {
                        mplayer1.setVolume(0, 0);
                        is_someone_playing = false;
                    } else {
                        is_someone_playing = true;
                    }
                    mplayer1.setLooping(true);

                    mplayer1.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mplayer1.start();
                        }
                    });

                    try {
                        mplayer1.setDataSource(video_path + "virtual.mp4");
                        mplayer1.prepare();
                    } catch (IOException e) {
                        XposedBridge.log("[VCAM] " + e.toString());
                    }
                }


                if (mSurfacetexture != null) {
                    if (mSurface == null) {
                        mSurface = new Surface(mSurfacetexture);
                    } else {
                        mSurface.release();
                        mSurface = new Surface(mSurfacetexture);
                    }

                    if (mMediaPlayer == null) {
                        mMediaPlayer = new MediaPlayer();
                    } else {
                        mMediaPlayer.release();
                        mMediaPlayer = new MediaPlayer();
                    }

                    mMediaPlayer.setSurface(mSurface);

                    File sfile = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "no-silent.jpg");
                    if (!(sfile.exists() && (!is_someone_playing))) {
                        mMediaPlayer.setVolume(0, 0);
                        is_someone_playing = false;
                    } else {
                        is_someone_playing = true;
                    }
                    mMediaPlayer.setLooping(true);

                    mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mMediaPlayer.start();
                        }
                    });

                    try {
                        mMediaPlayer.setDataSource(video_path + "virtual.mp4");
                        mMediaPlayer.prepare();
                    } catch (IOException e) {
                        XposedBridge.log("[VCAM]" + e.toString());
                    }
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "setPreviewDisplay", SurfaceHolder.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("[VCAM] Adding SurfaceView preview");
                File file = new File(video_path + "virtual.mp4");
                File toast_control = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "no_toast.jpg");
                need_to_show_toast = !toast_control.exists();
                if (!file.exists()) {
                    if (toast_content != null && need_to_show_toast) {
                        try {
                            Toast.makeText(toast_content, "Virtual video file not found\n" + lpparam.packageName + "\nPath: " + video_path, Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("【VCAM】[toast]" + ee.toString());
                        }
                    }
                    return;
                }
                File control_file = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "disable.jpg");
                if (control_file.exists()) {
                    return;
                }
                mcamera1 = (Camera) param.thisObject;
                ori_holder = (SurfaceHolder) param.args[0];
                if (c1_fake_texture == null) {
                    c1_fake_texture = new SurfaceTexture(11);
                } else {
                    c1_fake_texture.release();
                    c1_fake_texture = null;
                    c1_fake_texture = new SurfaceTexture(11);
                }

                if (c1_fake_surface == null) {
                    c1_fake_surface = new Surface(c1_fake_texture);
                } else {
                    c1_fake_surface.release();
                    c1_fake_surface = null;
                    c1_fake_surface = new Surface(c1_fake_texture);
                }
                is_hooked = true;
                mcamera1.setPreviewTexture(c1_fake_texture);
                param.setResult(null);
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.camera2.CaptureRequest.Builder", lpparam.classLoader, "addTarget", Surface.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                XposedBridge.log("[VCAM] [DEBUG]" + lpparam.packageName + " called addTarget: " + (param.args[0] != null ? param.args[0].toString() : "null"));

                if (param.args[0] == null) {
                    return;
                }
                if (param.thisObject == null) {
                    return;
                }
                File file = new File(video_path + "virtual.mp4");
                File toast_control = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "no_toast.jpg");
                need_to_show_toast = !toast_control.exists();
                if (!file.exists()) {
                    if (toast_content != null && need_to_show_toast) {
                        try {
                            Toast.makeText(toast_content, "Virtual video file not found\n" + lpparam.packageName + "\nPath: " + video_path, Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("[VCAM] [toast]" + ee.toString());
                        }
                    }
                    return;
                }
                if (c2_virtual_surface == null) {
                    create_virtual_surface();
                }

                if (param.args[0].equals(c2_virtual_surface)) {
                    return;
                }
                File control_file = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "disable.jpg");
                if (control_file.exists()) {
                    return;
                }
                String surfaceInfo = param.args[0].toString();
                XposedBridge.log("[VCAM] [DEBUG] Surface analysis: " + surfaceInfo + " - imageReaderFormat: " + imageReaderFormat);

                // For apps that use JPEG format (256), treat surfaces as preview surfaces
                if (surfaceInfo.contains("Surface(name=null)") && imageReaderFormat != 256) {
                    if (c2_reader_Surfcae == null) {
                        c2_reader_Surfcae = (Surface) param.args[0];
                        XposedBridge.log("[VCAM] [DEBUG] Assigned to c2_reader_Surfcae");
                    } else {
                        if ((!c2_reader_Surfcae.equals(param.args[0])) && c2_reader_Surfcae_1 == null) {
                            c2_reader_Surfcae_1 = (Surface) param.args[0];
                            XposedBridge.log("[VCAM] [DEBUG] Assigned to c2_reader_Surfcae_1");
                        }
                    }
                } else {
                    // Treat as preview surface
                    if (c2_preview_Surfcae == null) {
                        c2_preview_Surfcae = (Surface) param.args[0];
                        XposedBridge.log("[VCAM] [DEBUG] Assigned to c2_preview_Surfcae (JPEG or named surface)");
                    } else {
                        if ((!c2_preview_Surfcae.equals(param.args[0])) && c2_preview_Surfcae_1 == null) {
                            c2_preview_Surfcae_1 = (Surface) param.args[0];
                            XposedBridge.log("[VCAM] [DEBUG] Assigned to c2_preview_Surfcae_1");
                        }
                    }
                }
                XposedBridge.log("[VCAM] Adding target: " + param.args[0].toString());
                param.args[0] = c2_virtual_surface;

                // Process camera2 play with a slight delay to ensure surfaces are properly set up
                // This ensures MediaPlayer setup happens at the right time
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        XposedBridge.log("[VCAM] [DEBUG] Delayed triggering process_camera2_play from addTarget");
                        process_camera2_play();
                    }
                }, 100); // 100ms delay

            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.camera2.CaptureRequest.Builder", lpparam.classLoader, "removeTarget", Surface.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {

                if (param.args[0] == null) {
                    return;
                }
                if (param.thisObject == null) {
                    return;
                }
                File file = new File(video_path + "virtual.mp4");
                File toast_control = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "no_toast.jpg");
                need_to_show_toast = !toast_control.exists();
                if (!file.exists()) {
                    if (toast_content != null && need_to_show_toast) {
                        try {
                            Toast.makeText(toast_content, "Virtual video file not found\n" + lpparam.packageName + "\nPath: " + video_path, Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("[VCAM] [toast]" + ee.toString());
                        }
                    }
                    return;
                }
                File control_file = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "disable.jpg");
                if (control_file.exists()) {
                    return;
                }
                Surface rm_surf = (Surface) param.args[0];
                if (rm_surf.equals(c2_preview_Surfcae)) {
                    c2_preview_Surfcae = null;
                }
                if (rm_surf.equals(c2_preview_Surfcae_1)) {
                    c2_preview_Surfcae_1 = null;
                }
                if (rm_surf.equals(c2_reader_Surfcae_1)) {
                    c2_reader_Surfcae_1 = null;
                }
                if (rm_surf.equals(c2_reader_Surfcae)) {
                    c2_reader_Surfcae = null;
                }

                XposedBridge.log("[VCAM] Removing target: " + param.args[0].toString());
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.camera2.CaptureRequest.Builder", lpparam.classLoader, "build", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("[VCAM] [DEBUG]" + lpparam.packageName + " called CaptureRequest.Builder.build()");
                if (param.thisObject == null) {
                    return;
                }
                if (param.thisObject.equals(c2_builder)) {
                    return;
                }
                c2_builder = (CaptureRequest.Builder) param.thisObject;
                File file = new File(video_path + "virtual.mp4");
                File toast_control = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "no_toast.jpg");
                need_to_show_toast = !toast_control.exists();
                if (!file.exists() && need_to_show_toast) {
                    if (toast_content != null) {
                        try {
                            Toast.makeText(toast_content, "Virtual video file not found\n" + lpparam.packageName + "\nPath: " + video_path, Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("[VCAM] [toast]" + ee.toString());
                        }
                    }
                    return;
                }

                File control_file = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "disable.jpg");
                if (control_file.exists()) {
                    return;
                }
                XposedBridge.log("[VCAM][DEBUG] " + lpparam.packageName + " - Starting build request with video path: " + video_path);
                process_camera2_play();
            }
        });

/*        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "stopPreview", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.thisObject.equals(HookMain.origin_preview_camera) || param.thisObject.equals(HookMain.camera_onPreviewFrame) || param.thisObject.equals(HookMain.mcamera1)) {
                    if (hw_decode_obj != null) {
                        hw_decode_obj.stopDecode();
                    }
                    if (mplayer1 != null) {
                        mplayer1.release();
                        mplayer1 = null;
                    }
                    if (mMediaPlayer != null) {
                        mMediaPlayer.release();
                        mMediaPlayer = null;
                    }
                    is_someone_playing = false;

                    XposedBridge.log("[VCAM] Stop preview");
                }
            }
        });*/

        XposedHelpers.findAndHookMethod("android.media.ImageReader", lpparam.classLoader, "newInstance", int.class, int.class, int.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                XposedBridge.log("[VCAM][DEBUG] " + lpparam.packageName + " created ImageReader: Width: " + param.args[0] + " Height: " + param.args[1] + " Format: " + param.args[2] + " MaxImages: " + param.args[3]);
                c2_ori_width = (int) param.args[0];
                c2_ori_height = (int) param.args[1];
                imageReaderFormat = (int) param.args[2];
                File toast_control = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "no_toast.jpg");
                need_to_show_toast = !toast_control.exists();
                if (toast_content != null && need_to_show_toast) {
                    try {
                        Toast.makeText(toast_content, "App created image reader:\nWidth: " + param.args[0] + "\nHeight: " + param.args[1] + "\nUsually only width-height ratio needs to match video", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        XposedBridge.log("[VCAM] [toast]" + e.toString());
                    }
                }
            }
        });


        XposedHelpers.findAndHookMethod("android.hardware.camera2.CameraCaptureSession.CaptureCallback", lpparam.classLoader, "onCaptureFailed", CameraCaptureSession.class, CaptureRequest.class, CaptureFailure.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log("[VCAM] onCaptureFailed" + " Reason: " + ((CaptureFailure) param.args[2]).getReason());

                    }
                });

        // Add hook to detect Camera.open() calls that might be missed
        try {
            XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "open", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("[VCAM] [DEBUG]" + lpparam.packageName + " called Camera.open() - result: " + (param.getResult() != null ? "success" : "null"));
                }
            });
        } catch (Exception e) {
            XposedBridge.log("[VCAM] [DEBUG] Could not hook Camera.open(): " + e.getMessage());
        }

        // Add hook to detect Camera.open(int) calls
        try {
            XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "open", int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("[VCAM] [DEBUG]" + lpparam.packageName + " called Camera.open(int) with cameraId: " + param.args[0] + " - result: " + (param.getResult() != null ? "success" : "null"));
                }
            });
        } catch (Exception e) {
            XposedBridge.log("[VCAM] [DEBUG] Could not hook Camera.open(int): " + e.getMessage());
        }

        // Add hook to detect CameraManager.getCameraIdList() calls
        try {
            XposedHelpers.findAndHookMethod("android.hardware.camera2.CameraManager", lpparam.classLoader, "getCameraIdList", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String[] cameraIds = (String[]) param.getResult();
                    XposedBridge.log("[VCAM] [DEBUG]" + lpparam.packageName + " called getCameraIdList() - found " + (cameraIds != null ? cameraIds.length : 0) + " cameras");
                }
            });
        } catch (Exception e) {
            XposedBridge.log("[VCAM] [DEBUG] Could not hook getCameraIdList(): " + e.getMessage());
        }

        // Hook Surface.release() to detect if Snapchat is releasing surfaces unexpectedly
        try {
            XposedHelpers.findAndHookMethod("android.view.Surface", lpparam.classLoader, "release", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("[VCAM] [DEBUG]" + lpparam.packageName + " is releasing surface: " + param.thisObject.toString());
                    if (lpparam.packageName.equals("com.snapchat.android")) {
                        XposedBridge.log("[VCAM] [SNAPCHAT] Surface being released - this might cause issues if it's the virtual surface");
                    }
                }
            });
        } catch (Exception e) {
            XposedBridge.log("[VCAM] [DEBUG] Could not hook Surface.release(): " + e.getMessage());
        }
    }

    private void process_camera2_play() {
        if (isSettingUpMediaPlayer) {
            XposedBridge.log("[VCAM] [DEBUG] MediaPlayer setup already in progress, skipping");
            return;
        }

        XposedBridge.log("[VCAM] [DEBUG] process_camera2_play() called - c2_reader_Surfcae: " + (c2_reader_Surfcae != null) +
                ", c2_reader_Surfcae_1: " + (c2_reader_Surfcae_1 != null) +
                ", c2_preview_Surfcae: " + (c2_preview_Surfcae != null) +
                ", c2_preview_Surfcae_1: " + (c2_preview_Surfcae_1 != null));

        isSettingUpMediaPlayer = true;

        if (c2_reader_Surfcae != null) {
            if (c2_hw_decode_obj != null) {
                c2_hw_decode_obj.stopDecode();
                c2_hw_decode_obj = null;
            }

            c2_hw_decode_obj = new VideoToFrames();
            try {
                if (imageReaderFormat == 256) {
                    c2_hw_decode_obj.setSaveFrames("null", OutputImageFormat.JPEG);
                } else {
                    c2_hw_decode_obj.setSaveFrames("null", OutputImageFormat.NV21);
                }
                c2_hw_decode_obj.set_surfcae(c2_reader_Surfcae);
                c2_hw_decode_obj.decode(video_path + "virtual.mp4");
            } catch (Throwable throwable) {
                XposedBridge.log("[VCAM]" + throwable);
            }
        }

        if (c2_reader_Surfcae_1 != null) {
            if (c2_hw_decode_obj_1 != null) {
                c2_hw_decode_obj_1.stopDecode();
                c2_hw_decode_obj_1 = null;
            }

            c2_hw_decode_obj_1 = new VideoToFrames();
            try {
                if (imageReaderFormat == 256) {
                    c2_hw_decode_obj_1.setSaveFrames("null", OutputImageFormat.JPEG);
                } else {
                    c2_hw_decode_obj_1.setSaveFrames("null", OutputImageFormat.NV21);
                }
                c2_hw_decode_obj_1.set_surfcae(c2_reader_Surfcae_1);
                c2_hw_decode_obj_1.decode(video_path + "virtual.mp4");
            } catch (Throwable throwable) {
                XposedBridge.log("[VCAM]" + throwable);
            }
        }


        if (c2_preview_Surfcae != null) {
            XposedBridge.log("[VCAM] [DEBUG] Setting up video decode for c2_preview_Surfcae: " + c2_preview_Surfcae.toString());

            // For JPEG format surfaces, use VideoToFrames instead of MediaPlayer
            // since MediaPlayer can't directly output to ImageReader surfaces
            if (imageReaderFormat == 256) {
                XposedBridge.log("[VCAM] [DEBUG] Using VideoToFrames for JPEG format surface");

                if (c2_hw_decode_obj != null) {
                    c2_hw_decode_obj.stopDecode();
                    c2_hw_decode_obj = null;
                }

                c2_hw_decode_obj = new VideoToFrames();
                try {
                    File videoFile = new File(video_path + "virtual.mp4");
                    XposedBridge.log("[VCAM] [DEBUG] Video file for VideoToFrames - exists: " + videoFile.exists() +
                            " canRead: " + videoFile.canRead() + " length: " + videoFile.length() +
                            " path: " + videoFile.getAbsolutePath());

                    c2_hw_decode_obj.setSaveFrames("null", OutputImageFormat.JPEG);
                    XposedBridge.log("[VCAM] [DEBUG] VideoToFrames setSaveFrames completed");

                    c2_hw_decode_obj.set_surfcae(c2_preview_Surfcae);
                    XposedBridge.log("[VCAM] [DEBUG] VideoToFrames surface set completed");

                    c2_hw_decode_obj.decode(video_path + "virtual.mp4");
                    XposedBridge.log("[VCAM] [DEBUG] VideoToFrames decode started successfully");
                } catch (Throwable throwable) {
                    XposedBridge.log("[VCAM] [DEBUG] VideoToFrames error for c2_preview_Surfcae: " + throwable.getClass().getSimpleName() + " - " + throwable.getMessage());

                    // Print stack trace for debugging
                    StackTraceElement[] stackTrace = throwable.getStackTrace();
                    for (int i = 0; i < Math.min(stackTrace.length, 8); i++) {
                        XposedBridge.log("[VCAM] [Stack]" + stackTrace[i].toString());
                    }
                }
            } else {
                // Use MediaPlayer for non-JPEG surfaces
                XposedBridge.log("[VCAM] [DEBUG] Using MediaPlayer for non-JPEG format surface");
                try {
                    if (c2_player != null) {
                        try {
                            if (c2_player.isPlaying()) {
                                c2_player.stop();
                            }
                            c2_player.reset();
                            c2_player.release();
                        } catch (Exception e) {
                            XposedBridge.log("[VCAM] [DEBUG] Error cleaning up old MediaPlayer: " + e.getMessage());
                        }
                        c2_player = null;
                    }

                    // Check if surface is valid before setting it
                    if (!c2_preview_Surfcae.isValid()) {
                        XposedBridge.log("[VCAM] [DEBUG] c2_preview_Surfcae is not valid, skipping MediaPlayer setup");
                    } else {
                        // Create new MediaPlayer
                        c2_player = new MediaPlayer();
                        XposedBridge.log("[VCAM] [DEBUG] Created new MediaPlayer instance");

                        // Set up error listener first
                        c2_player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                            public boolean onError(MediaPlayer mp, int what, int extra) {
                                XposedBridge.log("[VCAM] [DEBUG] MediaPlayer error - what: " + what + " extra: " + extra);
                                return false;
                            }
                        });

                        // Set data source first, then surface
                        File videoFile = new File(video_path + "virtual.mp4");
                        XposedBridge.log("[VCAM] [DEBUG] Video file exists: " + videoFile.exists() + " canRead: " + videoFile.canRead() + " length: " + videoFile.length());

                        c2_player.setDataSource(video_path + "virtual.mp4");
                        XposedBridge.log("[VCAM] [DEBUG] DataSource set successfully");

                        c2_player.setSurface(c2_preview_Surfcae);
                        XposedBridge.log("[VCAM] [DEBUG] Surface set successfully");

                        File sfile = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "no-silent.jpg");
                        if (!sfile.exists()) {
                            c2_player.setVolume(0, 0);
                        }
                        c2_player.setLooping(true);

                        XposedBridge.log("[VCAM] [DEBUG] About to call prepare() for c2_player");

                        c2_player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            public void onPrepared(MediaPlayer mp) {
                                XposedBridge.log("[VCAM] [DEBUG] c2_player prepared and starting");
                                try {
                                    c2_player.start();
                                    XposedBridge.log("[VCAM] [DEBUG] c2_player started successfully");
                                } catch (Exception e) {
                                    XposedBridge.log("[VCAM] [DEBUG] Failed to start MediaPlayer: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        });

                        // Try synchronous prepare instead of async
                        c2_player.prepare();
                        XposedBridge.log("[VCAM] [DEBUG]MediaPlayer prepare() completed for c2_preview_Surfcae");
                    }
                } catch (Exception e) {
                    XposedBridge.log("[VCAM] [ERROR] c2player error [" + c2_preview_Surfcae.toString() + "]: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    XposedBridge.log("[VCAM] [DEBUG]Video file path: " + video_path + "virtual.mp4");
                    XposedBridge.log("[VCAM] [DEBUG] Surface valid: " + c2_preview_Surfcae.isValid());
                    XposedBridge.log("[VCAM] [DEBUG] MediaPlayer state when error occurred: " + (c2_player != null ? "exists" : "null"));

                    // Print full stack trace for debugging
                    StackTraceElement[] stackTrace = e.getStackTrace();
                    for (int i = 0; i < Math.min(stackTrace.length, 10); i++) {
                        XposedBridge.log("[VCAM] [STACK]" + stackTrace[i].toString());
                    }
                }
            }
        }

        if (c2_preview_Surfcae_1 != null) {
            XposedBridge.log("[VCAM] [DEBUG] Setting up video decode for c2_preview_Surfcae_1: " + c2_preview_Surfcae_1.toString());

            // For JPEG format surfaces, use VideoToFrames instead of MediaPlayer
            if (imageReaderFormat == 256) {
                XposedBridge.log("[VCAM] [DEBUG] Using VideoToFrames for JPEG format surface_1");

                if (c2_hw_decode_obj_1 != null) {
                    c2_hw_decode_obj_1.stopDecode();
                    c2_hw_decode_obj_1 = null;
                }

                c2_hw_decode_obj_1 = new VideoToFrames();
                try {
                    File videoFile = new File(video_path + "virtual.mp4");
                    XposedBridge.log("[VCAM] [DEBUG]Video file for VideoToFrames_1 - exists: " + videoFile.exists() +
                            " canRead: " + videoFile.canRead() + " length: " + videoFile.length());

                    c2_hw_decode_obj_1.setSaveFrames("null", OutputImageFormat.JPEG);
                    XposedBridge.log("[VCAM] [DEBUG] VideoToFrames_1 setSaveFrames completed");

                    c2_hw_decode_obj_1.set_surfcae(c2_preview_Surfcae_1);
                    XposedBridge.log("[VCAM] [DEBUG] VideoToFrames_1 surface set completed");

                    c2_hw_decode_obj_1.decode(video_path + "virtual.mp4");
                    XposedBridge.log("[VCAM] [DEBUG] VideoToFrames_1 decode started successfully");
                } catch (Throwable throwable) {
                    XposedBridge.log("[VCAM] [DEBUG] VideoToFrames error for c2_preview_Surfcae_1: " + throwable.getClass().getSimpleName() + " - " + throwable.getMessage());

                    // Print stack trace for debugging
                    StackTraceElement[] stackTrace = throwable.getStackTrace();
                    for (int i = 0; i < Math.min(stackTrace.length, 8); i++) {
                        XposedBridge.log("[VCAM] [DEBUG] " + stackTrace[i].toString());
                    }
                }
            } else {
                // Use MediaPlayer for non-JPEG surfaces
                XposedBridge.log("[VCAM] [DEBUG] Using MediaPlayer for non-JPEG format surface_1");
                try {
                    // Properly clean up existing MediaPlayer
                    if (c2_player_1 != null) {
                        try {
                            if (c2_player_1.isPlaying()) {
                                c2_player_1.stop();
                            }
                            c2_player_1.reset();
                            c2_player_1.release();
                        } catch (Exception e) {
                            XposedBridge.log("[VCAM] [DEBUG] Error cleaning up old MediaPlayer_1: " + e.getMessage());
                        }
                        c2_player_1 = null;
                    }

                    if (!c2_preview_Surfcae_1.isValid()) {
                        XposedBridge.log("[VCAM] [DEBUG] c2_preview_Surfcae_1 is not valid, skipping MediaPlayer setup");
                    } else {
                        // Create new MediaPlayer
                        c2_player_1 = new MediaPlayer();
                        XposedBridge.log("[VCAM] [DEBUG] Created new MediaPlayer_1 instance");

                        // Set up error listener first
                        c2_player_1.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                            public boolean onError(MediaPlayer mp, int what, int extra) {
                                XposedBridge.log("[VCAM] [DEBUG] MediaPlayer_1 error - what: " + what + " extra: " + extra);
                                return false;
                            }
                        });

                        // Set data source first, then surface
                        c2_player_1.setDataSource(video_path + "virtual.mp4");
                        XposedBridge.log("[VCAM] [DEBUG] DataSource set successfully for MediaPlayer_1");

                        c2_player_1.setSurface(c2_preview_Surfcae_1);
                        XposedBridge.log("[VCAM] [DEBUG] Surface set successfully for MediaPlayer_1");

                        File sfile = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "no-silent.jpg");
                        if (!sfile.exists()) {
                            c2_player_1.setVolume(0, 0);
                        }
                        c2_player_1.setLooping(true);

                        XposedBridge.log("[VCAM] [DEBUG] About to call prepare() for c2_player_1");

                        c2_player_1.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            public void onPrepared(MediaPlayer mp) {
                                XposedBridge.log("[VCAM] [DEBUG] c2_player_1 prepared and starting");
                                try {
                                    c2_player_1.start();
                                    XposedBridge.log("[VCAM] [DEBUG] c2_player_1 started successfully");
                                } catch (Exception e) {
                                    XposedBridge.log("[VCAM] [DEBUG] Failed to start MediaPlayer_1: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        });

                        // Try synchronous prepare instead of async
                        c2_player_1.prepare();
                        XposedBridge.log("[VCAM] [DEBUG] MediaPlayer_1 prepare() completed for c2_preview_Surfcae_1");
                    }
                } catch (Exception e) {
                    XposedBridge.log("[VCAM] [ERROR] c2player1 error [" + c2_preview_Surfcae_1.toString() + "]: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    XposedBridge.log("[VCAM] [DEBUG] Video file path: " + video_path + "virtual.mp4");
                    XposedBridge.log("[VCAM] [DEBUG] Surface valid: " + c2_preview_Surfcae_1.isValid());
                    XposedBridge.log("[VCAM] [DEBUG] MediaPlayer_1 state when error occurred: " + (c2_player_1 != null ? "exists" : "null"));

                    // Print full stack trace for debugging
                    StackTraceElement[] stackTrace = e.getStackTrace();
                    for (int i = 0; i < Math.min(stackTrace.length, 10); i++) {
                        XposedBridge.log("【VCAM】【STACK】" + stackTrace[i].toString());
                    }
                }
            }
        }
        XposedBridge.log("[VCAM] Camera2 processing fully executed\"");
        isSettingUpMediaPlayer = false;
    }

    private Surface create_virtual_surface() {
        if (need_recreate) {
            if (c2_virtual_surfaceTexture != null) {
                c2_virtual_surfaceTexture.release();
                c2_virtual_surfaceTexture = null;
            }
            if (c2_virtual_surface != null) {
                c2_virtual_surface.release();
                c2_virtual_surface = null;
            }
            c2_virtual_surfaceTexture = new SurfaceTexture(15);
            c2_virtual_surface = new Surface(c2_virtual_surfaceTexture);
            need_recreate = false;
            XposedBridge.log("[VCAM] [DEBUG] Created new virtual surface: " + c2_virtual_surface.toString());
        } else {
            if (c2_virtual_surface == null) {
                need_recreate = true;
                c2_virtual_surface = create_virtual_surface();
            }
        }
        XposedBridge.log("[VCAM] [Rebuilding virtual surface]" + c2_virtual_surface.toString());
        return c2_virtual_surface;
    }

    private void process_camera2_init(Class hooked_class) {

        XposedHelpers.findAndHookMethod(hooked_class, "onOpened", CameraDevice.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                need_recreate = true;
                create_virtual_surface();
                if (c2_player != null) {
                    c2_player.stop();
                    c2_player.reset();
                    c2_player.release();
                    c2_player = null;
                }
                if (c2_hw_decode_obj_1 != null) {
                    c2_hw_decode_obj_1.stopDecode();
                    c2_hw_decode_obj_1 = null;
                }
                if (c2_hw_decode_obj != null) {
                    c2_hw_decode_obj.stopDecode();
                    c2_hw_decode_obj = null;
                }
                if (c2_player_1 != null) {
                    c2_player_1.stop();
                    c2_player_1.reset();
                    c2_player_1.release();
                    c2_player_1 = null;
                }
                c2_preview_Surfcae_1 = null;
                c2_reader_Surfcae_1 = null;
                c2_reader_Surfcae = null;
                c2_preview_Surfcae = null;
                is_first_hook_build = true;
                XposedBridge.log("[VCAM] Opening camera C2");

                File file = new File(video_path + "virtual.mp4");
                File toast_control = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "no_toast.jpg");
                need_to_show_toast = !toast_control.exists();
                if (!file.exists()) {
                    if (toast_content != null && need_to_show_toast) {
                        try {
                            Toast.makeText(toast_content, "Virtual video file not found\n" + toast_content.getPackageName() + "\nPath: " + video_path, Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("[VCAM] [toast]" + ee.toString());
                        }
                    }
                    return;
                }
                XposedHelpers.findAndHookMethod(param.args[0].getClass(), "createCaptureSession", List.class, CameraCaptureSession.StateCallback.class, Handler.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam paramd) throws Throwable {
                        if (paramd.args[0] != null) {
                            XposedBridge.log("[VCAM] createCaptureSession create capture, Original: " + paramd.args[0].toString() + " Virtual: " + c2_virtual_surface.toString());
                            paramd.args[0] = Arrays.asList(c2_virtual_surface);
                            if (paramd.args[1] != null) {
                                process_camera2Session_callback((CameraCaptureSession.StateCallback) paramd.args[1]);
                            }
                        }
                    }
                });

/*                XposedHelpers.findAndHookMethod(param.args[0].getClass(), "close", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam paramd) throws Throwable {
                        XposedBridge.log("C2终止预览");
                        if (c2_hw_decode_obj != null) {
                            c2_hw_decode_obj.stopDecode();
                            c2_hw_decode_obj = null;
                        }
                        if (c2_hw_decode_obj_1 != null) {
                            c2_hw_decode_obj_1.stopDecode();
                            c2_hw_decode_obj_1 = null;
                        }
                        if (c2_player != null) {
                            c2_player.release();
                            c2_player = null;
                        }
                        if (c2_player_1 != null){
                            c2_player_1.release();
                            c2_player_1 = null;
                        }
                        c2_preview_Surfcae_1 = null;
                        c2_reader_Surfcae_1 = null;
                        c2_reader_Surfcae = null;
                        c2_preview_Surfcae = null;
                        need_recreate = true;
                        is_first_hook_build= true;
                    }
                });*/

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    XposedHelpers.findAndHookMethod(param.args[0].getClass(), "createCaptureSessionByOutputConfigurations", List.class, CameraCaptureSession.StateCallback.class, Handler.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            if (param.args[0] != null) {
                                outputConfiguration = new OutputConfiguration(c2_virtual_surface);
                                param.args[0] = Arrays.asList(outputConfiguration);

                                XposedBridge.log("[VCAM] Executed createCaptureSessionByOutputConfigurations-144777");
                                if (param.args[1] != null) {
                                    process_camera2Session_callback((CameraCaptureSession.StateCallback) param.args[1]);
                                }
                            }
                        }
                    });
                }


                XposedHelpers.findAndHookMethod(param.args[0].getClass(), "createConstrainedHighSpeedCaptureSession", List.class, CameraCaptureSession.StateCallback.class, Handler.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        super.beforeHookedMethod(param);
                        if (param.args[0] != null) {
                            param.args[0] = Arrays.asList(c2_virtual_surface);
                            XposedBridge.log("[VCAM] Executed createConstrainedHighSpeedCaptureSession -5484987");
                            if (param.args[1] != null) {
                                process_camera2Session_callback((CameraCaptureSession.StateCallback) param.args[1]);
                            }
                        }
                    }
                });


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    XposedHelpers.findAndHookMethod(param.args[0].getClass(), "createReprocessableCaptureSession", InputConfiguration.class, List.class, CameraCaptureSession.StateCallback.class, Handler.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            if (param.args[1] != null) {
                                param.args[1] = Arrays.asList(c2_virtual_surface);
                                XposedBridge.log("[VCAM] Executed createReprocessableCaptureSession");
                                if (param.args[2] != null) {
                                    process_camera2Session_callback((CameraCaptureSession.StateCallback) param.args[2]);
                                }
                            }
                        }
                    });
                }


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    XposedHelpers.findAndHookMethod(param.args[0].getClass(), "createReprocessableCaptureSessionByConfigurations", InputConfiguration.class, List.class, CameraCaptureSession.StateCallback.class, Handler.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            if (param.args[1] != null) {
                                outputConfiguration = new OutputConfiguration(c2_virtual_surface);
                                param.args[0] = Arrays.asList(outputConfiguration);
                                XposedBridge.log("[VCAM] Executed createReprocessableCaptureSessionByConfigurations");
                                if (param.args[2] != null) {
                                    process_camera2Session_callback((CameraCaptureSession.StateCallback) param.args[2]);
                                }
                            }
                        }
                    });
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    XposedHelpers.findAndHookMethod(param.args[0].getClass(), "createCaptureSession", SessionConfiguration.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            if (param.args[0] != null) {
                                XposedBridge.log("[VCAM] Executed createCaptureSession -5484987");
                                sessionConfiguration = (SessionConfiguration) param.args[0];
                                outputConfiguration = new OutputConfiguration(c2_virtual_surface);
                                fake_sessionConfiguration = new SessionConfiguration(sessionConfiguration.getSessionType(),
                                        Arrays.asList(outputConfiguration),
                                        sessionConfiguration.getExecutor(),
                                        sessionConfiguration.getStateCallback());
                                param.args[0] = fake_sessionConfiguration;
                                process_camera2Session_callback(sessionConfiguration.getStateCallback());
                            }
                        }
                    });
                }
            }
        });


        XposedHelpers.findAndHookMethod(hooked_class, "onError", CameraDevice.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("[VCAM] Camera error onError: " + (int) param.args[1]);
            }

        });


        XposedHelpers.findAndHookMethod(hooked_class, "onDisconnected", CameraDevice.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("[VCAM] Camera disconnected onDisconnected:");
            }

        });


    }

    private void process_a_shot_jpeg(XC_MethodHook.MethodHookParam param, int index) {
        try {
            XposedBridge.log("[VCAM] Second jpeg:" + param.args[index].toString());
        } catch (Exception eee) {
            XposedBridge.log("[VCAM]" + eee);

        }
        Class callback = param.args[index].getClass();

        XposedHelpers.findAndHookMethod(callback, "onPictureTaken", byte[].class, android.hardware.Camera.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam paramd) throws Throwable {
                try {
                    Camera loaclcam = (Camera) paramd.args[1];
                    onemwidth = loaclcam.getParameters().getPreviewSize().width;
                    onemhight = loaclcam.getParameters().getPreviewSize().height;
                    XposedBridge.log("[VCAM] JPEG photo callback initialized: Width: " + onemwidth + " Height: " + onemhight + " Corresponding class: " + loaclcam.toString());
                    File toast_control = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "no_toast.jpg");
                    need_to_show_toast = !toast_control.exists();
                    if (toast_content != null && need_to_show_toast) {
                        try {
                            Toast.makeText(toast_content, "Photo capture detected\nWidth: " + onemwidth + "\nHeight: " + onemhight + "\nFormat: JPEG", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            XposedBridge.log("[VCAM] [toast]" + e.toString());
                        }
                    }
                    File control_file = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "disable.jpg");
                    if (control_file.exists()) {
                        return;
                    }

                    Bitmap pict = getBMP(video_path + "1000.bmp");
                    ByteArrayOutputStream temp_array = new ByteArrayOutputStream();
                    pict.compress(Bitmap.CompressFormat.JPEG, 100, temp_array);
                    byte[] jpeg_data = temp_array.toByteArray();
                    paramd.args[0] = jpeg_data;
                } catch (Exception ee) {
                    XposedBridge.log("[VCAM] " + ee.toString());
                }
            }
        });
    }

    private void process_a_shot_YUV(XC_MethodHook.MethodHookParam param) {
        try {
            XposedBridge.log("[VCAM] Found photo YUV:" + param.args[1].toString());
        } catch (Exception eee) {
            XposedBridge.log("[VCAM] " + eee);
        }
        Class callback = param.args[1].getClass();
        XposedHelpers.findAndHookMethod(callback, "onPictureTaken", byte[].class, android.hardware.Camera.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam paramd) throws Throwable {
                try {
                    Camera loaclcam = (Camera) paramd.args[1];
                    onemwidth = loaclcam.getParameters().getPreviewSize().width;
                    onemhight = loaclcam.getParameters().getPreviewSize().height;
                    XposedBridge.log("[VCAM] YUV photo callback initialized: Width: " + onemwidth + " Height: " + onemhight + " Corresponding class: " + loaclcam.toString());
                    File toast_control = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "no_toast.jpg");
                    need_to_show_toast = !toast_control.exists();
                    if (toast_content != null && need_to_show_toast) {
                        try {
                            Toast.makeText(toast_content, "Photo capture detected\nWidth: " + onemwidth + "\nHeight: " + onemhight + "\nFormat: YUV_420_888", Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("[VCAM] [toast]" + ee.toString());
                        }
                    }
                    File control_file = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "disable.jpg");
                    if (control_file.exists()) {
                        return;
                    }
                    input = getYUVByBitmap(getBMP(video_path + "1000.bmp"));
                    paramd.args[0] = input;
                } catch (Exception ee) {
                    XposedBridge.log("[VCAM] " + ee.toString());
                }
            }
        });
    }

    private void process_callback(XC_MethodHook.MethodHookParam param) {
        Class preview_cb_class = param.args[0].getClass();
        int need_stop = 0;
        File control_file = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "disable.jpg");
        if (control_file.exists()) {
            need_stop = 1;
        }
        File file = new File(video_path + "virtual.mp4");
        File toast_control = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "no_toast.jpg");
        need_to_show_toast = !toast_control.exists();
        if (!file.exists()) {
            if (toast_content != null && need_to_show_toast) {
                try {
                    Toast.makeText(toast_content, "Virtual video file not found\n" + toast_content.getPackageName() + "\nPath: " + video_path, Toast.LENGTH_SHORT).show();
                } catch (Exception ee) {
                    XposedBridge.log("[VCAM] [toast]" + ee);
                }
            }
            need_stop = 1;
        }
        int finalNeed_stop = need_stop;
        XposedHelpers.findAndHookMethod(preview_cb_class, "onPreviewFrame", byte[].class, android.hardware.Camera.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam paramd) throws Throwable {
                Camera localcam = (android.hardware.Camera) paramd.args[1];
                if (localcam.equals(camera_onPreviewFrame)) {
                    while (data_buffer == null) {
                    }
                    System.arraycopy(data_buffer, 0, paramd.args[0], 0, Math.min(data_buffer.length, ((byte[]) paramd.args[0]).length));
                } else {
                    camera_callback_calss = preview_cb_class;
                    camera_onPreviewFrame = (android.hardware.Camera) paramd.args[1];
                    mwidth = camera_onPreviewFrame.getParameters().getPreviewSize().width;
                    mhight = camera_onPreviewFrame.getParameters().getPreviewSize().height;
                    int frame_Rate = camera_onPreviewFrame.getParameters().getPreviewFrameRate();
                    XposedBridge.log("[VCAM] Frame preview callback initialized: Width: " + mwidth + " Height: " + mhight + " Frame Rate: " + frame_Rate);
                    File toast_control = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/Camera1/" + "no_toast.jpg");
                    need_to_show_toast = !toast_control.exists();
                    if (toast_content != null && need_to_show_toast) {
                        try {
                            Toast.makeText(toast_content, "Preview detected\nWidth: " + mwidth + "\nHeight: " + mhight + "\n" + "Video resolution must match exactly", Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("[VCAM] [toast]" + ee.toString());
                        }
                    }
                    if (finalNeed_stop == 1) {
                        return;
                    }
                    if (hw_decode_obj != null) {
                        hw_decode_obj.stopDecode();
                    }
                    hw_decode_obj = new VideoToFrames();
                    hw_decode_obj.setSaveFrames("", OutputImageFormat.NV21);
                    hw_decode_obj.decode(video_path + "virtual.mp4");
                    while (data_buffer == null) {
                    }
                    System.arraycopy(data_buffer, 0, paramd.args[0], 0, Math.min(data_buffer.length, ((byte[]) paramd.args[0]).length));
                }

            }
        });

    }

    private void process_camera2Session_callback(CameraCaptureSession.StateCallback callback_calss){
        if (callback_calss == null){
            return;
        }
//        XposedHelpers.findAndHookMethod(callback_calss.getClass(), "onConfigureFailed", CameraCaptureSession.class, new XC_MethodHook() {
//            @Override
//            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                XposedBridge.log("[VCAM] onConfigureFailed: " + param.args[0].toString());
//            }
//
//        });
//
//        XposedHelpers.findAndHookMethod(callback_calss.getClass(), "onConfigured", CameraCaptureSession.class, new XC_MethodHook() {
//            @Override
//            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                XposedBridge.log("[VCAM] onConfigured: " + param.args[0].toString());
//            }
//        });
//
//        XposedHelpers.findAndHookMethod( callback_calss.getClass(), "onClosed", CameraCaptureSession.class, new XC_MethodHook() {
//            @Override
//            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                XposedBridge.log("[VCAM] onClosed: " + param.args[0].toString());
//            }
//        });
    }



    // The following code is from: https://blog.csdn.net/jacke121/article/details/73888732
    private Bitmap getBMP(String file) throws Throwable {
        return BitmapFactory.decodeFile(file);
    }

    private static byte[] rgb2YCbCr420(int[] pixels, int width, int height) {
        int len = width * height;
        // YUV format array size: Y luminance occupies len length, U and V each occupy len/4 length.
        byte[] yuv = new byte[len * 3 / 2];
        int y, u, v;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int rgb = (pixels[i * width + j]) & 0x00FFFFFF;
                int r = rgb & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb >> 16) & 0xFF;
                // Apply formula
                y = ((66 * r + 129 * g + 25 * b + 128) >> 8) + 16;
                u = ((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128;
                v = ((112 * r - 94 * g - 18 * b + 128) >> 8) + 128;
                y = y < 16 ? 16 : (Math.min(y, 255));
                u = u < 0 ? 0 : (Math.min(u, 255));
                v = v < 0 ? 0 : (Math.min(v, 255));
                // Assignment
                yuv[i * width + j] = (byte) y;
                yuv[len + (i >> 1) * width + (j & ~1)] = (byte) u;
                yuv[len + +(i >> 1) * width + (j & ~1) + 1] = (byte) v;
            }
        }
        return yuv;
    }

    private static byte[] getYUVByBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = width * height;
        int[] pixels = new int[size];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        return rgb2YCbCr420(pixels, width, height);
    }
}

