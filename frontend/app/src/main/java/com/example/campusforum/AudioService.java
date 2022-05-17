package com.example.campusforum;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

public class AudioService extends Service {
    static final String ACTION_START = "START";
    static final String ACTION_PAUSE = "PAUSE";

    private MediaPlayer mediaPlayer;
    private String audioSrc;

    public AudioService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        audioSrc = "";
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle data = intent.getExtras();
        assert data.getString("action", null) != null;
        switch (data.getString("action")) {
            case ACTION_START:
                assert data.getString("audio_src", null) != null;
                if (audioSrc.equals(data.getString("audio_src"))) {
                    mediaPlayer.start();
                } else {
                    try {
                        mediaPlayer.reset();
                        audioSrc = data.getString("audio_src");
//                        mediaPlayer.setDataSource(HttpUtil.baseUrl + audioSrc);
                        mediaPlayer.setDataSource(audioSrc);
                        mediaPlayer.prepareAsync();
                        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mediaPlayer) {
                                mediaPlayer.start();
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case ACTION_PAUSE:
                mediaPlayer.pause();
                break;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaPlayer.release();
    }
}