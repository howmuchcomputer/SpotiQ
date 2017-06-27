package se.zinokader.spotiq.service;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.session.MediaSession;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Metadata;
import com.spotify.sdk.android.player.PlaybackBitrate;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.PlayerInitializationException;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;
import com.valdesekamdem.library.mdtoast.MDToast;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import se.zinokader.spotiq.R;
import se.zinokader.spotiq.constant.ApplicationConstants;
import se.zinokader.spotiq.constant.LogTag;
import se.zinokader.spotiq.constant.ServiceConstants;
import se.zinokader.spotiq.constant.SpotifyConstants;
import se.zinokader.spotiq.repository.TracklistRepository;
import se.zinokader.spotiq.util.di.Injector;
import se.zinokader.spotiq.util.exception.EmptyTracklistException;

public class SpotiqPlayerService extends Service implements ConnectionStateCallback, Player.NotificationCallback {

    @Inject
    TracklistRepository tracklistRepository;

    @Inject
    SpotifyCommunicatorService spotifyCommunicatorService;

    private CompositeDisposable disposableActions = new CompositeDisposable();
    private Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private Config playerConfig;
    private SpotifyPlayer spotifyPlayer;
    private boolean isTracklistEmpty = false;
    private String partyTitle;

    private boolean changingConfiguration;

    private NotificationManager notificationManager;
    private static final int ONGOING_NOTIFICATION_ID = 821;

    private IBinder binder = new PlayerServiceBinder();

    public class PlayerServiceBinder extends Binder {
        public SpotiqPlayerService getService() {
            return SpotiqPlayerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        stopForeground(true);
        changingConfiguration = false;
        return binder;
    }

    @Override
    public void onRebind(Intent intent) {
        stopForeground(true);
        changingConfiguration = false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (!changingConfiguration) {
            Log.d(LogTag.LOG_PLAYER_SERVICE, "Starting foreground player service");
            sendNotification(true);
        }
        return true;
    }

    @Override
    public void onCreate() {
        ((Injector) getApplicationContext()).inject(this);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Log.d(LogTag.LOG_PLAYER_SERVICE, "SpotiQ Player service created");
    }

    @Override
    public void onDestroy() {
        Log.d(LogTag.LOG_PLAYER_SERVICE, "SpotiQ Player service destroyed");
        disposableActions.clear();
        if (spotifyPlayer != null) {
            try {
                Spotify.awaitDestroyPlayer(this, 5, TimeUnit.SECONDS);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void onTaskRemoved(Intent rootIntent) {
        //Called when application closes by user input
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        changingConfiguration = true;
    }

    private void sendNotification(boolean shouldStartForeground) {
        AsyncTask.execute(() -> {
            String title;
            String description;
            Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.image_album_placeholder);
            boolean shouldBeOngoing = serviceIsRunningInForeground(this);

            if (isPlaying() && !isTracklistEmpty) {
                Metadata.Track currentTrack = spotifyPlayer.getMetadata().currentTrack;
                title = currentTrack.name;
                description = currentTrack.artistName + " - " + currentTrack.albumName;
                try {
                    largeIcon = Glide.with(this)
                        .load(currentTrack.albumCoverWebUrl)
                        .asBitmap()
                        .into(250, 250)
                        .get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            else {
                title = "Hosting party";
                description = "You're currently the host of " + partyTitle;
            }

            Notification notification;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notification = new Notification.Builder(this, ApplicationConstants.MEDIA_NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_logo)
                    .setLargeIcon(largeIcon)
                    .setStyle(new Notification.MediaStyle()
                        .setMediaSession(new MediaSession(this, ApplicationConstants.MEDIA_SESSSION_TAG).getSessionToken()))
                    .setColorized(true)
                    .setContentTitle(title)
                    .setContentText(description)
                    .setOngoing(shouldBeOngoing)
                    .build();
            }
            else {
                notification = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_notification_logo)
                    .setLargeIcon(largeIcon)
                    .setStyle(new NotificationCompat.MediaStyle()
                        .setMediaSession(new MediaSessionCompat(this, ApplicationConstants.MEDIA_SESSSION_TAG).getSessionToken()))
                    .setColorized(true)
                    .setContentTitle(title)
                    .setContentText(description)
                    .setOngoing(shouldBeOngoing)
                    .setChannel(ApplicationConstants.MEDIA_NOTIFICATION_CHANNEL_ID)
                    .setDefaults(4)
                    .build();
            }

            if (shouldStartForeground) {
                startForeground(ONGOING_NOTIFICATION_ID, notification);
            }
            else {
                notificationManager.notify(ONGOING_NOTIFICATION_ID, notification);
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LogTag.LOG_PLAYER_SERVICE, "Player service initialization started");

        partyTitle = intent.getStringExtra(ApplicationConstants.PARTY_NAME_EXTRA);
        if (partyTitle.isEmpty()) {
            Log.d(LogTag.LOG_PLAYER_SERVICE, "Player service could not be initialized - Received empty party title");
            stopSelf();
        }

        if (intent.getAction().equals(ServiceConstants.ACTION_INIT)) {
            initPlayer().subscribe(didInit -> {
                Log.d(LogTag.LOG_PLAYER_SERVICE, "Player initialization status: " + didInit);
                if (!didInit) stopSelf();
            });
        }

        return START_STICKY;
    }

    private Single<Boolean> initPlayer() {
        return Single.create(subscriber -> {
            if (playerConfig == null) {
                playerConfig = new Config(this,
                    spotifyCommunicatorService.getAuthenticator().getAccessToken(),
                    SpotifyConstants.CLIENT_ID);
                playerConfig.useCache(false);
            }

            spotifyPlayer = Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
                @Override
                public void onInitialized(SpotifyPlayer player) {
                    spotifyPlayer.addConnectionStateCallback(SpotiqPlayerService.this);
                    spotifyPlayer.addNotificationCallback(SpotiqPlayerService.this);
                    spotifyPlayer.setPlaybackBitrate(new Player.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            subscriber.onSuccess(true);
                            Log.d(LogTag.LOG_PLAYER_SERVICE, "Set Spotify Player custom playback bitrate successfully");
                        }

                        @Override
                        public void onError(Error error) {
                            subscriber.onError(new PlayerInitializationException(error.name()));
                            Log.d(LogTag.LOG_PLAYER_SERVICE, "Failed to set Spotify Player playback bitrate. Cause: " + error.toString());

                        }
                    }, PlaybackBitrate.BITRATE_HIGH);

                }

                @Override
                public void onError(Throwable throwable) {
                    subscriber.onError(throwable);
                    Log.d(LogTag.LOG_PLAYER_SERVICE, "Could not initialize Spotify Player: " + throwable.getMessage());
                    stopSelf();
                }

            });
        });
    }

    public void play() {
        Log.d(LogTag.LOG_PLAYER_SERVICE, "PLAY CLICKED!");

        if (!isTracklistEmpty && spotifyPlayer.getMetadata().currentTrack != null) {
            resume();
        }
        else {
            playNext();
        }
    }

    public void pause() {
        Log.d(LogTag.LOG_PLAYER_SERVICE, "PAUSE CLICKED!");

        spotifyPlayer.pause(new Player.OperationCallback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onError(Error error) {

            }
        });
    }

    private void playNext() {
        tracklistRepository.getFirstSong(partyTitle)
            .subscribe(song -> {
                spotifyPlayer.playUri(new Player.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(LogTag.LOG_PLAYER_SERVICE, "Playing next song in tracklist");
                        isTracklistEmpty = false;
                        //delay sending notification until song certainly is playing (Spotify Android SDK to blame)
                        new Handler().postDelayed(() -> sendNotification(false), 2000);
                    }

                    @Override
                    public void onError(Error error) {
                        Log.d(LogTag.LOG_PLAYER_SERVICE, "Failed to play next song in tracklist: " + error.name());
                    }
                }, song.getSongUri(), 0, 0);
            }, throwable -> {
                Log.d(LogTag.LOG_PLAYER_SERVICE, "Could not play next song, reason: " + throwable.getMessage());
                isTracklistEmpty = true;
                sendNotification(false);
            });
    }

    private void resume() {
        spotifyPlayer.resume(new Player.OperationCallback() {
            @Override
            public void onSuccess() {
                Log.d(LogTag.LOG_PLAYER_SERVICE, "Resumed music successfully");
            }

            @Override
            public void onError(Error error) {
                Log.d(LogTag.LOG_PLAYER_SERVICE, "Failed to resume music");
            }
        });
    }

    public boolean isPlaying() {
        return !(spotifyPlayer == null || spotifyPlayer.getPlaybackState() == null) && spotifyPlayer.getPlaybackState().isPlaying;
    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.d(LogTag.LOG_PLAYER_SERVICE, "Playback event: " + playerEvent.name());
        switch (playerEvent) {
            case kSpPlaybackNotifyLostPermission:
                showLostPlaybackPermissionToast();
                break;
            case kSpPlaybackNotifyTrackDelivered:
                handlePlaybackEnd();
        }
    }

    private void showLostPlaybackPermissionToast() {
        mainThreadHandler.post(() -> {
            MDToast lostPlaybackPermissionToast =
                MDToast.makeText(this,
                    getString(R.string.playback_permission_lost_notice),
                    MDToast.LENGTH_LONG,
                    MDToast.TYPE_INFO);
            lostPlaybackPermissionToast.setIcon(R.drawable.ic_spotify_connect);
            lostPlaybackPermissionToast.show();
        });
    }

    private void handlePlaybackEnd() {
        tracklistRepository.removeFirstSong(partyTitle)
            .subscribe(wasRemoved -> {
                if (wasRemoved) playNext();
            }, throwable -> {
                if (throwable instanceof EmptyTracklistException) {
                    Log.d(LogTag.LOG_PLAYER_SERVICE, "Tracklist empty, next song not played");
                    isTracklistEmpty = true;
                }
            });
    }

    @Override
    public void onPlaybackError(Error error) {
        Log.d(LogTag.LOG_PLAYER_SERVICE, "Spotify playback error: " + error.toString());
    }

    @Override
    public void onLoggedIn() {

    }

    @Override
    public void onLoggedOut() {
    }

    @Override
    public void onLoginFailed(Error error) {
        Log.d(LogTag.LOG_PLAYER_SERVICE, "Spotify login failed: " + error.toString());
        switch (error) {
            case kSpErrorNeedsPremium:
        }
    }

    @Override
    public void onTemporaryError() {

    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d(LogTag.LOG_PLAYER_SERVICE, "Spotify connection message: " + message);
    }

    public boolean serviceIsRunningInForeground(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(
            Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(
            Integer.MAX_VALUE)) {
            if (getClass().getName().equals(service.service.getClassName())) {
                if (service.foreground) {
                    return true;
                }
            }
        }
        return false;
    }

}
