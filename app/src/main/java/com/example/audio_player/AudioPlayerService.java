package com.example.audio_player;

import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioPlayerService extends Service {
    private final String TAG = this.getClass().getName();
    protected final static int PLAYING_STATUS = 0;
    protected final static int PAUSE_STATUS = 1;
    protected final static int IDLE_STATUS = 2;
    protected final static int ERROR_STATUS = 3;
    protected final static int START_PLAYING_STATUS = 4;
    protected final static String ACTION_PROGRESS = "com.example.audio_player.Status";
    protected static final String STATUS_EXTRA = "status";
    protected static final String PROGRESS_EXTRA = "progress";
    private static final int UPDATE_DELAY = 200;
    private int status = IDLE_STATUS;
    private ExecutorService executor;
    private int duration;
    MediaPlayer mediaPlayer;
    MPBinder binder = new MPBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newFixedThreadPool(1);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, String.valueOf(startId));
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service onBind" + hashCode());
        return binder;
    }

    protected void onChangeState(int newStatus) {
        status = newStatus;
        switch (status) {
            case START_PLAYING_STATUS: {
                mediaPlayer = MediaPlayer.create(this, R.raw.tfk_phenomenon);
                duration = mediaPlayer.getDuration();
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    releaseMP();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                });
                mediaPlayer.start();
                startUpdateProgress();
                Log.d(TAG, "START_PLAYING_STATUS Service");
            }
            break;
            case PLAYING_STATUS:
                mediaPlayer.start();
                startUpdateProgress();
                Log.d(TAG, "PLAYING STATUS Service");
                break;
            case PAUSE_STATUS:
                Log.d(TAG, "PAUSE STATUS Service");
                mediaPlayer.pause();
                break;
        }
    }

    private void shareProgress() {
        Intent intent = new Intent(ACTION_PROGRESS);
        intent.putExtra(STATUS_EXTRA, status);
        if (status == START_PLAYING_STATUS || status == PAUSE_STATUS || status == PLAYING_STATUS) {
            intent.putExtra(PROGRESS_EXTRA, getProgress());
        }
        sendBroadcast(intent);
    }

    private void startUpdateProgress() {
        Runnable updateProgress = new Runnable() {
            @Override
            public void run() {
                try {
                    while (mediaPlayer.isPlaying()) {
                        Thread.sleep(UPDATE_DELAY);
                        shareProgress();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        executor.execute(updateProgress);
    }

    protected int getProgress() {
        return mediaPlayer.getCurrentPosition() * 100 / duration;
    }

    protected int getStatus() {
        return status;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "destroy service " + this.hashCode());
    }

    private void releaseMP() {
        if (mediaPlayer != null) {
            try {
                status = IDLE_STATUS;
                mediaPlayer.release();
                mediaPlayer = null;
                shareProgress();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isPlaying() {
        if (mediaPlayer == null)
            return false;
        return true;
    }

    class MPBinder extends Binder {
        AudioPlayerService getService() {
            return AudioPlayerService.this;
        }
    }
}
