package com.example.audio_player;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioPlayerService extends Service {
    private final String TAG = this.getClass().getName();
    protected final static int PLAYING_STATUS = 0;
    protected final static int PAUSE_STATUS = 1;
    protected final static int IDLE_STATUS = 2;
    protected final static int ERROR_STATUS = 3;
    protected final static String ACTION_PROGRESS = "com.example.audio_player.Status";
    protected static final String STATUS_EXTRA = "status";
    protected static final String PROGRESS_EXTRA = "progress";
    private static final int NOTIFICATION_ID = 1;
    private static final int UPDATE_DELAY = 200;
    private int status = IDLE_STATUS;
    private ExecutorService executor;
    private int duration;
    MediaPlayer mediaPlayer;
    MPBinder binder = new MPBinder();
    private int progress;
    private Timer delayTimer;

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newFixedThreadPool(1);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, String.valueOf(startId));
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service onBind" + hashCode());
        return binder;
    }

    private Notification createNotification() {
        Intent intent = new Intent(this, AudioPlayerActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Notification.Builder builder = new Notification.Builder(AudioPlayerService.this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("Audio Player");
        builder.setContentText("Music is playing");
        builder.setContentIntent(pIntent);
        builder.setWhen(System.currentTimeMillis());

        Notification notification = builder.getNotification();
        return notification;
    }

    protected void onChangeState(int newStatus) {
        status = newStatus;
        switch (status) {
            case PLAYING_STATUS: {
                if (!isPlaying()) {
                    mediaPlayer = MediaPlayer.create(this, R.raw.tfk_phenomenon);
                    duration = mediaPlayer.getDuration();
                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            try {
                                if (delayTimer != null)
                                    delayTimer.cancel();
                                releaseMP();
                            } catch (Exception e) {
                                Log.d(TAG, e.getMessage());
                            }
                        }
                    });
                }
                mediaPlayer.start();
                startUpdateProgress();
                startForeground(NOTIFICATION_ID, createNotification());
                Log.d(TAG, "START_PLAYING_STATUS Service");
            }
            break;

            case PAUSE_STATUS:
                Log.d(TAG, "PAUSE STATUS Service");
                stopForeground(true);
                mediaPlayer.pause();
                break;
        }
    }

    private void shareProgress() {
        Intent intent = new Intent(ACTION_PROGRESS);
        intent.putExtra(STATUS_EXTRA, status);
        if (isPlaying()) {
            intent.putExtra(PROGRESS_EXTRA, getProgress());
        }
        sendBroadcast(intent);
    }

    private void startUpdateProgress() {
        Runnable runUpdate = new Runnable() {
            @Override
            public void run() {
                delayTimer = new Timer();
                while (mediaPlayer.isPlaying()) {
                    TimerTask timerTask = new TimerTask() {
                        @Override
                        public void run() {
                            if (getProgress() != progress) {
                                shareProgress();
                                progress = getProgress();
                            }

                        }
                    };
                    delayTimer.schedule(timerTask, UPDATE_DELAY);
                }
                delayTimer.cancel();
            }
        };
        executor.execute(runUpdate);
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
                delayTimer = null;
            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
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
