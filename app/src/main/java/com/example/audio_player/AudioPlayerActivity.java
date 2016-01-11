package com.example.audio_player;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.audio_player.AudioPlayerService;

import java.util.List;

public class AudioPlayerActivity extends Activity {
    private final String TAG = this.getClass().getName();
    private Button btnPlay;
    private TextView statusLabel;
    private ProgressBar progressBar;
    private ProgressReceiver progressReceiver;
    private IntentFilter intentFilter;
    private Intent intent;
    private int status;
    private int progress;
    private ServiceConnection mServiceConnection;
    private AudioPlayerService.MPBinder binder;
    private boolean bind = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);
        initAppComponents(savedInstanceState);
    }

    private void initAppComponents(Bundle savedInstanceState) {
        btnPlay = (Button) findViewById(R.id.btnPlay);
        statusLabel = (TextView) findViewById(R.id.status_label);
        progressBar = (ProgressBar) findViewById(R.id.progress);
        progressBar.setVisibility(View.INVISIBLE);

        progressReceiver = new ProgressReceiver();
        intentFilter = new IntentFilter(AudioPlayerService.ACTION_PROGRESS);
        this.registerReceiver(progressReceiver, intentFilter);

        mServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                binder = (AudioPlayerService.MPBinder) service;
                bind = true;
                status = binder.getService().getStatus();
                if (binder.getService().isPlaying()) {
                    progress = binder.getService().getProgress();
                }
                updateUIStatus(status, progress);
            }

            public void onServiceDisconnected(ComponentName className) {
                binder = null;
                bind = false;
                Log.d(TAG, "Service disconnected");
            }
        };
        intent = new Intent(this, AudioPlayerService.class);
        startService(intent);

        btnPlay.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View v) {
                                           if (bind) {
                                               changeStatus();
                                               updateUIStatus(status, progress);
                                               updateServiceStatus(status);
                                           }
                                       }
                                   }
        );
        Log.d(TAG, "activity created");
    }

    @Override
    protected void onResume() {
        bindService(intent, mServiceConnection, 0);
        super.onResume();
        Log.d(TAG, "activity resumed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bind)
            unbindService(mServiceConnection);
    }

    private void changeStatus() {
        switch (status) {
            case AudioPlayerService.IDLE_STATUS:
                status = AudioPlayerService.PLAYING_STATUS;
                break;

            case AudioPlayerService.PLAYING_STATUS:
                status = AudioPlayerService.PAUSE_STATUS;
                break;

            case AudioPlayerService.PAUSE_STATUS:
                status = AudioPlayerService.PLAYING_STATUS;
                break;

            case AudioPlayerService.ERROR_STATUS:
                status = AudioPlayerService.IDLE_STATUS;
                break;
        }
    }

    private void updateUIStatus(int status, int progress) {
        switch (status) {
            case AudioPlayerService.IDLE_STATUS: {
                btnPlay.setText(R.string.play);

                progressBar.setVisibility(View.INVISIBLE);
                progressBar.setProgress(0);

                statusLabel.setText(R.string.status_idle);
            }
            break;

            case AudioPlayerService.PLAYING_STATUS: {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(progress);
                btnPlay.setText(R.string.pause);
                statusLabel.setText(R.string.status_playing);
            }
            break;

            case AudioPlayerService.PAUSE_STATUS: {
                progressBar.setVisibility(View.VISIBLE);
                btnPlay.setEnabled(true);
                btnPlay.setText(R.string.play);
                progressBar.setProgress(progress);

                statusLabel.setText(R.string.status_paused);
            }
            break;

            case AudioPlayerService.ERROR_STATUS: {
                Toast.makeText(AudioPlayerActivity.this,
                        R.string.playing_error, Toast.LENGTH_SHORT)
                        .show();
                stopService(intent);
                startService(intent);
                bindService(intent, mServiceConnection, 0);
                this.status = AudioPlayerService.IDLE_STATUS;
            }
            break;
        }
    }

    private void updateServiceStatus(int status) {
        binder.getService().onChangeState(status);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(progressReceiver);
    }

    class ProgressReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            progress = intent.getIntExtra(AudioPlayerService.PROGRESS_EXTRA, 0);
            status = intent.getIntExtra(AudioPlayerService.STATUS_EXTRA, AudioPlayerService.ERROR_STATUS);
            updateUIStatus(status, progress);
        }
    }

}
