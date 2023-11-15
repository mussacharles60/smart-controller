package com.myssa.smartcontroller;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.myssa.smartcontroller.databinding.ActivityMainBinding;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@SuppressLint("CustomSplashScreen")
public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    public static final String TAG = "MainActivity";
    public static final int PERMISSION_REQUEST_CODE = 101;
    public final static int REQUEST_ENABLE_BT = 99;
    ActivityMainBinding binding;
    private BluetoothHelper helper;
    private MediaPlayer mp;
    private SurfaceHolder surfaceHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        try {
            InputStream ims = getAssets().open("home_logo.png");
            // load image as Drawable
            Drawable d = Drawable.createFromStream(ims, null);
            // set image to ImageView
            binding.logics.logoImg.setImageDrawable(d);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            InputStream ims = getAssets().open("launch_bg.png");
            // load image as Drawable
            Drawable d = Drawable.createFromStream(ims, null);
            // set image to ImageView
            binding.logics.relaunchBgImg.setImageDrawable(d);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            InputStream ims = getAssets().open("launch_logo.png");
            // load image as Drawable
            Drawable d = Drawable.createFromStream(ims, null);
            // set image to ImageView
            binding.logics.relaunchImg.setImageDrawable(d);
        } catch (Exception e) {
            e.printStackTrace();
        }

        binding.notSupportedText.setVisibility(View.GONE);
        binding.settingsBtn.setVisibility(View.GONE);
        binding.connectButton.setVisibility(View.GONE);
        binding.logicsContainer.setVisibility(View.GONE);
        binding.reConnectButton.setVisibility(View.GONE);
        binding.disconnectButton.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.GONE);
        binding.infoContainer.setVisibility(View.GONE);

        binding.infoBtn.setOnClickListener(v -> {
            binding.infoContainer.setVisibility(
                    binding.infoContainer.getVisibility() == View.VISIBLE ?
                            View.GONE : View.VISIBLE);
            binding.settingsBtn.setVisibility(
                    binding.settingsBtn.getVisibility() == View.VISIBLE ?
                            View.GONE : View.VISIBLE);
        });

        binding.settingsBtn.setOnClickListener(v -> {
            // open bluetooth settings
            Intent intent = new Intent();
            intent.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
        });

        SurfaceHolder holder = binding.logics.surfaceView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        // holder.setFixedSize(800, 480);

        // check if device has bluetooth
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            // check self permission for location and bluetooth. If not granted, request permission
            final List<String> permissions = new ArrayList<>();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
                }
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.BLUETOOTH_SCAN);
                }
            }
            if (permissions.size() > 0) {
                ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
            } else {
                // permission granted, init logics
                initLogics();
            }
        } else {
            binding.notSupportedText.setVisibility(View.VISIBLE);
            binding.connectButton.setVisibility(View.GONE);
            binding.logicsContainer.setVisibility(View.GONE);
            // binding.logicButtonsContainer.setVisibility(View.GONE);
            binding.reConnectButton.setVisibility(View.GONE);
            binding.disconnectButton.setVisibility(View.GONE);
            binding.progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        surfaceHolder = holder;
        initPlayer();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

    }

    private void initPlayer() {
        startPlayer();
        new Handler().postDelayed(() -> {
            if (mp != null) {
                mp.pause();
            }
        }, 500);
    }

    private void startPlayer() {
        if (surfaceHolder == null) {
            return;
        }

        stopPlayer();

        if (mp == null) {
            mp = new MediaPlayer();
            mp.setOnPreparedListener(mp -> {
                Log.d(MainActivity.class.getSimpleName(), "setOnPreparedListener:");
                // mp.setVolume(0f, 0f);
                mp.setLooping(false);
                setVideoSize();
            });
            mp.setOnInfoListener((mp, what, extra) -> {
                Log.d(MainActivity.class.getSimpleName(), "setOnInfoListener:");
                // binding.logics.startBtn.setVisibility(View.GONE);
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                    Log.d(MainActivity.class.getSimpleName(), "playing: ");
                    setVideoSize();
                    // return true;
                }
                return false;
            });
            mp.setOnErrorListener((mp, what, extra) -> {
                Log.d(MainActivity.class.getSimpleName(), "setOnErrorListener:");
                launch();
                return false;
            });
            mp.setOnCompletionListener(mp -> {
                Log.d(MainActivity.class.getSimpleName(), "setOnCompletionListener:");
                launch();
                initPlayer();
            });

            String path = "android.resource://" + getPackageName() + "/" + R.raw.scanner;
            Log.d(SplashActivity.class.getSimpleName(), "path: " + path);
            mp.setDisplay(surfaceHolder);
            try {
                mp.setDataSource(this, Uri.parse(path));
                mp.prepare();
            } catch (IllegalArgumentException | IllegalStateException | IOException e) {
                e.printStackTrace();
                launch();
            }
        }
        mp.start();
    }

    private void setVideoSize() {
        if (mp == null) {
            return;
        }
        try {
            // // Get the dimensions of the video
            int videoWidth = mp.getVideoWidth();
            int videoHeight = mp.getVideoHeight();
            float videoProportion = (float) videoWidth / (float) videoHeight;

            // Get the width of the screen
            int screenWidth = getWindowManager().getDefaultDisplay().getWidth();
            int screenHeight = getWindowManager().getDefaultDisplay().getHeight();
            float screenProportion = (float) screenWidth / (float) screenHeight;

            // Get the SurfaceView layout parameters
            android.view.ViewGroup.LayoutParams lp = binding.logics.surfaceView.getLayoutParams();
            if (videoProportion > screenProportion) {
                lp.width = screenWidth;
                lp.height = (int) ((float) screenWidth / videoProportion);
            } else {
                lp.width = (int) (videoProportion * (float) screenHeight);
                lp.height = screenHeight;
            }
            // Commit the layout parameters
            binding.logics.surfaceView.setLayoutParams(lp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closePlayer() {
        try {
            if (mp != null) {
                stopPlayer();
                mp.setOnPreparedListener(null);
                mp.setOnInfoListener(null);
                mp.setOnErrorListener(null);
                mp.setOnCompletionListener(null);

                mp.release();
                // mp.setDisplay(null);
//                try {
//                    mp.reset();
//                } catch (IllegalStateException e) {
//                    Log.e(TAG, "player: closePlayer: catch 1: ", e);
//                    e.printStackTrace();
//                }
//                mp = null;
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "player: closePlayer: catch: ", e);
            e.printStackTrace();
        }
    }

    private void stopPlayer() {
        if (mp != null) {
            if (mp.isPlaying()) {
                mp.seekTo(500);
                mp.pause();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            if (grantResults.length > 0) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        Log.e(TAG, "onRequestPermissionsResult: Permission not granted");
                        allGranted = false;
                        break;
                    }
                }
            }
            if (allGranted) {
                initLogics();
            } else {
                binding.notSupportedText.setVisibility(View.VISIBLE);
                binding.connectButton.setVisibility(View.GONE);
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initLogics() {
        binding.connectButton.setVisibility(View.VISIBLE);
        binding.connectButton.setOnClickListener(v -> {
            // check if bluetooth is enabled
            if (BluetoothHelper.isBluetoothEnabled()) {
                Log.d(TAG, "initLogics: bluetooth enabled");
                // start scanning
                Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivityForResult(intent, DeviceListActivity.GET_DEVICE_REQUEST_CODE);
            } else {
                Log.d(TAG, "initLogics: bluetooth not enabled");
                // ask user to enable bluetooth
                BluetoothHelper.enableBluetooth(this);
            }
        });

        binding.logics.startBtn.setOnClickListener(v -> {
            binding.logics.imageContainer.setVisibility(View.GONE);
        });
        binding.logics.relaunchBtn.setOnClickListener(v -> {
            binding.logics.relaunchContainer.setVisibility(View.GONE);
            binding.logics.imageContainer.setVisibility(View.VISIBLE);
        });

        binding.logics.surfaceView.setOnTouchListener((view, motionEvent) -> {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.d(TAG, "player: onTouch: MotionEvent.ACTION_DOWN");
                    startPlayer();
                    break;
                case MotionEvent.ACTION_MOVE:
                    // Handle touch move
                    break;
                case MotionEvent.ACTION_UP:
                    Log.d(TAG, "player: onTouch: MotionEvent.ACTION_UP, position: " + mp.getCurrentPosition());
                    binding.logics.imageContainer.setVisibility(View.VISIBLE);
                    if (mp != null && mp.getCurrentPosition() >= 5500) {
                        launch();
                    }
                    stopPlayer();
                    break;
            }
            return true;
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: " + requestCode + " " + resultCode);
        if (requestCode == DeviceListActivity.GET_DEVICE_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                String deviceName = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_NAME);
                String deviceAddress = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // connect to device
                // BluetoothHelper.connectToDevice(deviceAddress);
                Log.d(TAG, "onActivityResult: " + deviceName + " " + deviceAddress);
                this.initViews(deviceName, deviceAddress);
            }
        } else if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "onActivityResult: bluetooth enabled");
                // start scanning
                Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivityForResult(intent, DeviceListActivity.GET_DEVICE_REQUEST_CODE);
            } else {
                Log.d(TAG, "onActivityResult: bluetooth not enabled");
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void initViews(String deviceName, String deviceAddress) {
        binding.notSupportedText.setVisibility(View.GONE);
        binding.connectButton.setVisibility(View.GONE);
        binding.logicsContainer.setVisibility(View.VISIBLE);
        // binding.logicButtonsContainer.setVisibility(View.VISIBLE);
        binding.infoText.setVisibility(View.VISIBLE);
        binding.infoText.setText("Status: connecting to " + deviceName + "...");
        binding.disconnectButton.setVisibility(View.VISIBLE);

        helper = new BluetoothHelper(this, deviceAddress, new BluetoothHelper.BluetoothHelperListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onConnectionStatusChanged(BluetoothHelper.ConnectionStatus status) {
                Log.d(TAG, "onConnectionStatusChanged: " + status);
                runOnUiThread(() -> {
                    switch (status) {
                        case CONNECTING:
                            binding.infoText.setText("Status: connecting to " + deviceName + "...");
                            binding.disconnectButton.setVisibility(View.GONE);
                            binding.progressBar.setVisibility(View.VISIBLE);
                            // binding.logicsContainer.setVisibility(View.GONE);
                            // binding.logicButtonsContainer.setVisibility(View.GONE);
                            break;
                        case CONNECTED:
                            binding.infoText.setText("Status: connected to " + deviceName);
                            binding.disconnectButton.setVisibility(View.VISIBLE);
                            binding.progressBar.setVisibility(View.GONE);
                            binding.logicsContainer.setVisibility(View.VISIBLE);
                            // binding.logicButtonsContainer.setVisibility(View.VISIBLE);
                            // new android.os.Handler().postDelayed(() -> {
                            //     syncDevice();
                            //     new android.os.Handler().postDelayed(device::requestDeviceInfo, 1500);
                            // }, 1000);
                            break;
                        case DISCONNECTED:
                            binding.infoText.setText("Status: disconnected from " + deviceName);
                            binding.reConnectButton.setVisibility(View.VISIBLE);
                            binding.disconnectButton.setVisibility(View.VISIBLE);
                            binding.progressBar.setVisibility(View.GONE);
                            // binding.logicsContainer.setVisibility(View.GONE);
                            // binding.logicButtonsContainer.setVisibility(View.GONE);
                            break;
                        //                        case DISCONNECTING:
                        //                            binding.infoText.setText("Disconnecting from " + deviceName + "...");
                        //                            break;
                    }
                });
            }

            @Override
            public void onMessageReceived(String message) {
                Log.d(TAG, "onMessageReceived: " + message);
                 runOnUiThread(() -> handleMessage(message));
            }
        });

        binding.reConnectButton.setOnClickListener(v -> {
            helper.setAddressAndInitialize(deviceAddress);
            binding.reConnectButton.setVisibility(View.GONE);
            binding.disconnectButton.setVisibility(View.GONE);
            binding.progressBar.setVisibility(View.VISIBLE);
        });

        binding.disconnectButton.setOnClickListener(v -> {
            if (helper != null) {
                helper.stop();
                helper = null;
            }
            binding.infoText.setText("Status: not connected");
            binding.notSupportedText.setVisibility(View.GONE);
            binding.connectButton.setVisibility(View.VISIBLE);
            binding.logicsContainer.setVisibility(View.GONE);
            // binding.logicButtonsContainer.setVisibility(View.GONE);
            binding.reConnectButton.setVisibility(View.GONE);
            binding.disconnectButton.setVisibility(View.GONE);
            binding.progressBar.setVisibility(View.GONE);
        });
    }

//    private void syncDevice() {
//        final Calendar c = Calendar.getInstance();
//        int dayOfMonth = c.get(Calendar.DAY_OF_MONTH);
//        int month = c.get(Calendar.MONTH);
//        int year = c.get(Calendar.YEAR);
//        int hourOfDay = c.get(Calendar.HOUR_OF_DAY);
//        int minute = c.get(Calendar.MINUTE);
//        int second = c.get(Calendar.SECOND);
//        device.setCurrentDate(year, month + 1, dayOfMonth, hourOfDay, minute, second);
//        device.syncDevice();
//    }

    private void handleMessage(String message) {
        if (message != null && message.contains("finish")) {
            binding.logics.imageContainer.setVisibility(View.GONE);
            binding.logics.relaunchContainer.setVisibility(View.VISIBLE);
            stopPlayer();
        }
//        // current date
//        int cyear = -1;
//        int cmon = -1;
//        int cday = -1;
//        int chour = -1;
//        int cmin = -1;
//        int csec = -1;
//        // last accident
//        int lyear = -1;
//        int lmon = -1;
//        int lday = -1;
//        // days work far
//        int dwf = -1;
//        // first aid cases
//        int fac = -1;
//        // recorded incident
//        int ri = -1;
//        // days without accident
//        int dwa = -1;
//        // mode 0: clear, 1: test, 2: on
//        int mode = -1;
//        String msg = null;
//        try {
//            JSONObject obj = new JSONObject(message);
//            if (obj.has("cyear")) {
//                cyear = obj.getInt("cyear");
//            }
//            if (obj.has("cmon")) {
//                cmon = obj.getInt("cmon");
//            }
//            if (obj.has("cday")) {
//                cday = obj.getInt("cday");
//            }
//            if (obj.has("chour")) {
//                chour = obj.getInt("chour");
//            }
//            if (obj.has("cmin")) {
//                cmin = obj.getInt("cmin");
//            }
//            if (obj.has("csec")) {
//                csec = obj.getInt("csec");
//            }
//            if (obj.has("lyear")) {
//                lyear = obj.getInt("lyear");
//            }
//            if (obj.has("lmon")) {
//                lmon = obj.getInt("lmon");
//            }
//            if (obj.has("lday")) {
//                lday = obj.getInt("lday");
//            }
//            if (obj.has("dwf")) {
//                dwf = obj.getInt("dwf");
//            }
//            if (obj.has("fac")) {
//                fac = obj.getInt("fac");
//            }
//            if (obj.has("ri")) {
//                ri = obj.getInt("ri");
//            }
//            if (obj.has("dwa")) {
//                dwa = obj.getInt("dwa");
//            }
//            if (obj.has("mode")) {
//                mode = obj.getInt("mode");
//            }
//            if (obj.has("message")) {
//                msg = obj.getString("message");
//            }
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//        if (cyear != -1 && cmon != -1 && cday != -1 && chour != -1 && cmin != -1 && csec != -1) {
//            device.setCurrentDate(cyear, cmon, cday, chour, cmin, csec);
//        }
//        if (lyear != -1 && lmon != -1 && lday != -1) {
//            device.setLastAccidentDate(lyear, lmon, lday);
//        }
//        if (dwf != -1) {
//            device.setDaysWorkSoFar(dwf);
//        }
//        if (fac != -1) {
//            device.setFirstAidCases(fac);
//        }
//        if (ri != -1) {
//            device.setRecordedIncident(ri);
//        }
//        if (dwa != -1) {
//            device.setDaysWithoutAccident(dwa);
//        }
//        if (mode != -1) {
//            device.setMode(mode);
//        }
//        if (msg != null) {
//            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
//        }
    }

    private void launch() {
        binding.logics.imageContainer.setVisibility(View.GONE);
        binding.logics.relaunchContainer.setVisibility(View.VISIBLE);
        if (helper != null) {
            Log.d(TAG, "player: launch");
            helper.send("start\r\n");
        }
    }

    // close on back pressed
    @Override
    public void onBackPressed() {
        if (helper != null) {
            helper.stop();
            helper = null;
        }
        closePlayer();
        super.onBackPressed();
        finish();
    }

    // close on destroy
    @Override
    protected void onDestroy() {
        if (helper != null) {
            helper.stop();
            helper = null;
        }
        closePlayer();
        super.onDestroy();
    }
}
