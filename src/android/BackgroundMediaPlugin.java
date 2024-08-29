package com.example;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.core.app.NotificationCompat;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;

import java.io.IOException;
import java.net.URL;

public class BackgroundMediaPlugin extends CordovaPlugin {

    private ExoPlayer player;
    private PlayerNotificationManager playerNotificationManager;
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "media_playback_channel";
    private String currentTitle;
    private String currentArtist;
    private Bitmap currentArtwork;
    private CallbackContext eventCallback;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        switch (action) {
            case "play":
                String url = args.getString(0);
                String title = args.getString(1);
                String artist = args.getString(2);
                String artworkUrl = args.getString(3);
                boolean isVideo = args.getBoolean(4);
                long startTime = args.getLong(5);
                this.play(url, title, artist, artworkUrl, isVideo, startTime, callbackContext);
                return true;
            case "pause":
                this.pause(callbackContext);
                return true;
            case "stop":
                this.stop(callbackContext);
                return true;
            case "setVolume":
                float volume = (float) args.getDouble(0);
                this.setVolume(volume, callbackContext);
                return true;
            case "setPlaybackTime":
                long time = args.getLong(0);
                this.setPlaybackTime(time, callbackContext);
                return true;
            case "registerEventCallback":
                this.eventCallback = callbackContext;
                return true;
            case "destroy":
                this.destroy(callbackContext);
                return true;
        }
        return false;
    }

    private void play(String url, String title, String artist, String artworkUrl, boolean isVideo, long startTime, CallbackContext callbackContext) {
        cordova.getThreadPool().execute(() -> {
            // Stop any existing playback
            if (player != null) {
                player.stop();
                player.release();
                player = null;
            }

            currentTitle = title;
            currentArtist = artist;
            currentArtwork = loadBitmapFromUrl(artworkUrl);

            cordova.getActivity().runOnUiThread(() -> {
                player = new ExoPlayer.Builder(cordova.getActivity()).build();
                MediaItem mediaItem = MediaItem.fromUri(url);
                player.setMediaItem(mediaItem);
                player.prepare();
                player.seekTo(startTime);
                player.play();

                setupNotification();
                setupPlayerListeners();

                callbackContext.success("Playback started");
            });
        });
    }

    private void pause(CallbackContext callbackContext) {
        if (player != null) {
            player.pause();
            callbackContext.success("Playback paused");
        } else {
            callbackContext.error("No active playback");
        }
    }

    private void stop(CallbackContext callbackContext) {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
            playerNotificationManager.setPlayer(null);
            callbackContext.success("Playback stopped");
        } else {
            callbackContext.error("No active playback");
        }
    }

    private void setVolume(float volume, CallbackContext callbackContext) {
        if (player != null) {
            player.setVolume(volume);
            callbackContext.success("Volume set to " + volume);
        } else {
            callbackContext.error("No active playback");
        }
    }

    private void setPlaybackTime(long time, CallbackContext callbackContext) {
        if (player != null) {
            player.seekTo(time);
            callbackContext.success("Playback time set to " + time);
        } else {
            callbackContext.error("No active playback");
        }
    }

    private void destroy(CallbackContext callbackContext) {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
        if (playerNotificationManager != null) {
            playerNotificationManager.setPlayer(null);
            playerNotificationManager = null;
        }
        callbackContext.success("Plugin destroyed");
    }

    private void setupNotification() {
        if (playerNotificationManager == null) {
            playerNotificationManager = new PlayerNotificationManager.Builder(
                    cordova.getActivity(),
                    NOTIFICATION_ID,
                    CHANNEL_ID
            )
            .setChannelName("Media Playback")
            .setChannelDescription("Controls for media playback")
            .setMediaDescriptionAdapter(new PlayerNotificationManager.MediaDescriptionAdapter() {
                @Override
                public CharSequence getCurrentContentTitle(Player player) {
                    return currentTitle;
                }

                @Override
                public CharSequence getCurrentContentText(Player player) {
                    return currentArtist;
                }

                @Override
                public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
                    return currentArtwork;
                }

                @Override
                public PendingIntent createCurrentContentIntent(Player player) {
                    Intent intent = new Intent(cordova.getActivity(), cordova.getActivity().getClass());
                    return PendingIntent.getActivity(cordova.getActivity(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                }
            })
            .setNotificationListener(new PlayerNotificationManager.NotificationListener() {
                @Override
                public void onNotificationPosted(int notificationId, Notification notification, boolean ongoing) {
                    cordova.getActivity().startForeground(notificationId, notification);
                }

                @Override
                public void onNotificationCancelled(int notificationId, boolean dismissedByUser) {
                    cordova.getActivity().stopForeground(true);
                }
            })
            .build();

            playerNotificationManager.setPlayer(player);
            playerNotificationManager.setUseNextAction(true);
            playerNotificationManager.setUsePreviousAction(true);
        }
    }

    private void setupPlayerListeners() {
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED) {
                    sendEvent("onPlaybackEnd", "Playback ended");
                } else if (state == Player.STATE_READY) {
                    sendEvent("onPlaybackStart", "Playback started");
                }
            }

            @Override
            public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                    if (newPosition.positionMs > oldPosition.positionMs) {
                        sendEvent("onSkipForward", "Skipped forward");
                    } else {
                        sendEvent("onSkipBackward", "Skipped backward");
                    }
                }
            }
        });
    }

    private void sendEvent(String eventName, String message) {
        if (eventCallback != null) {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, eventName + ": " + message);
            pluginResult.setKeepCallback(true);
            eventCallback.sendPluginResult(pluginResult);
        }
    }

    private Bitmap loadBitmapFromUrl(String url) {
        try {
            return BitmapFactory.decodeStream(new URL(url).openConnection().getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onDestroy() {
        destroy(null);
        super.onDestroy();
    }
}