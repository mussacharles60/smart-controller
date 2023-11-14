package com.myssa.smartcontroller;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.myssa.smartcontroller.databinding.ActivitySplashBinding;

import java.io.InputStream;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    ActivitySplashBinding binding;
//    MediaPlayer mp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.imageContainer.setVisibility(View.VISIBLE);

        try {
            InputStream ims = getAssets().open("logo.png");
            // load image as Drawable
            Drawable d = Drawable.createFromStream(ims, null);
            // set image to ImageView
            binding.splashImage.setImageDrawable(d);
        } catch (Exception e) {
            e.printStackTrace();
        }

//        SurfaceHolder holder = binding.surfaceView.getHolder();
//        holder.addCallback(this);
//        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//        // holder.setFixedSize(800, 480);
//        mp = new MediaPlayer();
//        mp.setOnPreparedListener(mp -> {
//            Log.d(SplashActivity.class.getSimpleName(), "setOnPreparedListener:");
//            mp.setVolume(0f, 0f);
//            mp.setLooping(false);
//            setVideoSize();
//        });
//        mp.setOnInfoListener((mp, what, extra) -> {
//            Log.d(SplashActivity.class.getSimpleName(), "setOnInfoListener:");
//            binding.imageContainer.setVisibility(View.GONE);
//            if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
//                Log.d(SplashActivity.class.getSimpleName(), "playing: ");
//                setVideoSize();
//                // return true;
//            }
//            return false;
//        });
//        mp.setOnErrorListener((mp, what, extra) -> {
//            Log.d(SplashActivity.class.getSimpleName(), "setOnErrorListener:");
//            startMainActivity();
//            return false;
//        });
//        mp.setOnCompletionListener(mp -> {
//            Log.d(SplashActivity.class.getSimpleName(), "setOnCompletionListener:");
//            startMainActivity();
//        });
//
//        //try {
//        //    // Uri uri = Uri.parse("asset:///boot_logo.mp4");
//        //    String path = "android.resource://" + getPackageName() + "/" + R.raw.boot_logo;
//        //    Log.d(SplashActivity.class.getSimpleName(), "path: " + path);
//        //    binding.videoView.setVideoURI(Uri.parse(path));
//        //    binding.videoView.setOnPreparedListener(mp -> {
//        //        Log.d(SplashActivity.class.getSimpleName(), "setOnPreparedListener: mp: " + mp);
//        ////                mp.setVolume(0f, 0f);
//        //        mp.setLooping(true);
//        //    });
//        //    binding.videoView.setOnInfoListener((mp, what, extra) -> {
//        //        Log.d(SplashActivity.class.getSimpleName(), "setOnInfoListener: mp: " + mp);
//        //        binding.imageContainer.setVisibility(View.GONE);
//        //        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
//        //            Log.d(SplashActivity.class.getSimpleName(), "playing: ");
//        //            return true;
//        //        }
//        //        return false;
//        //    });
//        //    binding.videoView.setOnErrorListener((mp, what, extra) -> {
//        //        Log.d(SplashActivity.class.getSimpleName(), "setOnErrorListener: mp: " + mp);
//        //        //                try {
//        //        //                    Intent intent = new Intent(SplashActivity.this, MainActivity.class);
//        //        //                    startActivity(intent);
//        //        //                    finish();
//        //        //                } catch (Exception e) {
//        //        //                    e.printStackTrace();
//        //        //                }
//        //        return false;
//        //    });
//        //    binding.videoView.setOnCompletionListener(mp -> {
//        //        Log.d(SplashActivity.class.getSimpleName(), "setOnCompletionListener: mp: " + mp);
//        //        //                try {
//        //        //                    Intent intent = new Intent(SplashActivity.this, MainActivity.class);
//        //        //                    startActivity(intent);
//        //        //                    finish();
//        //        //                } catch (Exception e) {
//        //        //                    e.printStackTrace();
//        //        //                }
//        //    });
//        //    binding.videoView.requestFocus();
//        //    binding.videoView.start();
//        //    binding.videoView.setKeepScreenOn(true);
//        //    binding.videoView.setZOrderOnTop(true);
//        //    Log.d(SplashActivity.class.getSimpleName(), "start: ");
//        //} catch (Exception e) {
//        //    Log.d(SplashActivity.class.getSimpleName(), "Exception: e: " + e.getMessage());
//        //    e.printStackTrace();
//        //    //            try {
//        //    //                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
//        //    //                startActivity(intent);
//        //    //                finish();
//        //    //            } catch (Exception _e) {
//        //    //                Log.d(SplashActivity.class.getSimpleName(), "Exception: e2: " + _e.getMessage());
//        //    //                _e.printStackTrace();
//        //    //            }
//        //}
//
//
//        //        new android.os.Handler().postDelayed(() -> {
//        //            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
//        //            startActivity(intent);
//        //            finish();
//        //        }, 3000);

        new Handler().postDelayed(this::startMainActivity, 2000);
    }

//    private void setVideoSize() {
//        if (mp == null) {
//            return;
//        }
//
//        try {
//            // // Get the dimensions of the video
//            int videoWidth = mp.getVideoWidth();
//            int videoHeight = mp.getVideoHeight();
//            float videoProportion = (float) videoWidth / (float) videoHeight;
//
//            // Get the width of the screen
//            int screenWidth = getWindowManager().getDefaultDisplay().getWidth();
//            int screenHeight = getWindowManager().getDefaultDisplay().getHeight();
//            float screenProportion = (float) screenWidth / (float) screenHeight;
//
//            // Get the SurfaceView layout parameters
//            android.view.ViewGroup.LayoutParams lp = binding.surfaceView.getLayoutParams();
//            if (videoProportion > screenProportion) {
//                lp.width = screenWidth;
//                lp.height = (int) ((float) screenWidth / videoProportion);
//            } else {
//                lp.width = (int) (videoProportion * (float) screenHeight);
//                lp.height = screenHeight;
//            }
//            // Commit the layout parameters
//            binding.surfaceView.setLayoutParams(lp);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    private void startMainActivity() {
//        closePlayer();
        new android.os.Handler().postDelayed(() -> {
            try {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 500);
    }

//    private void closePlayer() {
//        try {
//            if (mp != null) {
//                mp.setOnPreparedListener(null);
//                mp.setOnInfoListener(null);
//                mp.setOnErrorListener(null);
//                mp.setOnCompletionListener(null);
//                //if (mp.isPlaying()) {
//                //    mp.pause();
//                //}
//                mp.release();
//                mp.reset();
//                mp = null;
//            }
//        } catch (IllegalStateException e) {
//            e.printStackTrace();
//        }
//    }

    @Override
    protected void onDestroy() {
//        closePlayer();
        super.onDestroy();
    }

//    @Override
//    public void surfaceCreated(@NonNull SurfaceHolder holder) {
////        String path = "android.resource://" + getPackageName() + "/" + R.raw.boot_logo;
////        Log.d(SplashActivity.class.getSimpleName(), "path: " + path);
////        mp.setDisplay(holder);
////        try {
////            mp.setDataSource(this, Uri.parse(path));
////            mp.prepare();
////        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
////            e.printStackTrace();
////            startMainActivity();
////        }
////        mp.start();
//    }
//
//    @Override
//    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
//
//    }
//
//    @Override
//    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
//
//    }
}
