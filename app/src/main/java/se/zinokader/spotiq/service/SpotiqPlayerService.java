package se.zinokader.spotiq.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
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
import se.zinokader.spotiq.util.NotificationUtil;
import se.zinokader.spotiq.util.ServiceUtil;
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

    private MediaSession mediaSession;
    private MediaSessionCompat mediaSessionCompat;
    private PlaybackState playbackStatePlaying;
    private PlaybackState playbackStatePaused;
    private PlaybackStateCompat playbackStateCompatPlaying;
    private PlaybackStateCompat playbackStateCompatPaused;
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
        mediaSession = new MediaSession(this, ApplicationConstants.MEDIA_SESSSION_TAG);
        mediaSessionCompat = new MediaSessionCompat(this, ApplicationConstants.MEDIA_SESSSION_TAG);
        playbackStatePlaying = new PlaybackState.Builder()
            .setState(PlaybackState.STATE_PLAYING, 0, 1).build();
        playbackStatePaused = new PlaybackState.Builder()
            .setState(PlaybackState.STATE_PAUSED, 0, 1).build();
        playbackStateCompatPlaying = new PlaybackStateCompat.Builder()
            .setState(PlaybackState.STATE_PLAYING, 0, 1).build();
        playbackStateCompatPaused = new PlaybackStateCompat.Builder()
            .setState(PlaybackState.STATE_PAUSED, 0, 1).build();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Log.d(LogTag.LOG_PLAYER_SERVICE, "SpotiQ Player service created");
    }

    @Override
    public void onDestroy() {
        Log.d(LogTag.LOG_PLAYER_SERVICE, "SpotiQ Player service destroyed");
        mediaSession.release();
        mediaSessionCompat.release();
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
        String title;
        String description;
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.image_album_placeholder);
        boolean shouldBeOngoing = ServiceUtil.isPlayerServiceInForeground(this);

        if (isPlaying() && !isTracklistEmpty) {
            Metadata.Track currentTrack = spotifyPlayer.getMetadata().currentTrack;
            title = currentTrack.name;
            description = currentTrack.artistName + " - " + currentTrack.albumName;
            Glide.with(this)
                .load(currentTrack.albumCoverWebUrl)
                .asBitmap()
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap albumArt, GlideAnimation<? super Bitmap> glideAnimation) {

                        Notification playerNotification;

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            mediaSession.setMetadata(NotificationUtil.buildMediaMetadata(currentTrack, albumArt));
                            playerNotification =
                                NotificationUtil.createPlayerNotification(SpotiqPlayerService.this, mediaSession,
                                    shouldBeOngoing, title, description, albumArt);
                        }
                        else {
                            mediaSessionCompat.setMetadata(NotificationUtil.buildMediaMetadataCompat(currentTrack, albumArt));
                            playerNotification = NotificationUtil.createPlayerNotificationCompat(SpotiqPlayerService.this, mediaSessionCompat,
                                shouldBeOngoing, title, description, albumArt);
                        }

                        if (shouldStartForeground) {
                            startForeground(ONGOING_NOTIFICATION_ID, playerNotification);
                        }
                        else {
                            notificationManager.notify(ONGOING_NOTIFICATION_ID, playerNotification);
                        }
                    }
                });
        }
        else {
            title = "Hosting party " + partyTitle;
            description = "Player is idle";
            Notification playerNotification;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                playerNotification =
                    NotificationUtil.createPlayerNotification(SpotiqPlayerService.this, mediaSession,
                        shouldBeOngoing, title, description, largeIcon);
            }
            else {
                playerNotification = NotificationUtil.createPlayerNotificationCompat(SpotiqPlayerService.this, mediaSessionCompat,
                    shouldBeOngoing, title, description, largeIcon);
            }

            if (shouldStartForeground) {
                startForeground(ONGOING_NOTIFICATION_ID, playerNotification);
            }
            else {
                notificationManager.notify(ONGOING_NOTIFICATION_ID, playerNotification);
            }
        }
    }

    private void sendPlayingStatusBroadcast(boolean isPlaying) {
        Intent playingStatusIntent = new Intent(ServiceConstants.PLAYING_STATUS_BROADCAST_NAME);
        playingStatusIntent.putExtra(ServiceConstants.PLAYING_STATUS_ISPLAYING_EXTRA, isPlaying);
        LocalBroadcastManager.getInstance(this).sendBroadcast(playingStatusIntent);
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
                            sendNotification(ServiceUtil.isPlayerServiceInForeground(SpotiqPlayerService.this));
                            sendPlayingStatusBroadcast(false); //make sure all listeners are up to sync with an inititally paused status
                            mediaSession.setActive(true);
                            mediaSessionCompat.setActive(true);
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
        if (!isTracklistEmpty && spotifyPlayer.getMetadata().currentTrack != null) {
            resume();
        }
        else {
            playNext();
        }
    }

    public void pause() {
        spotifyPlayer.pause(new Player.OperationCallback() {
            @Override
            public void onSuccess() {
                Log.d(LogTag.LOG_PLAYER_SERVICE, "Paused music successfully");
                sendPlayingStatusBroadcast(false);
                mediaSession.setPlaybackState(playbackStatePaused);
                mediaSessionCompat.setPlaybackState(playbackStateCompatPaused);
            }

            @Override
            public void onError(Error error) {
                Log.d(LogTag.LOG_PLAYER_SERVICE, "Failed to pause music");
            }
        });
    }

    private void resume() {
        spotifyPlayer.resume(new Player.OperationCallback() {
            @Override
            public void onSuccess() {
                Log.d(LogTag.LOG_PLAYER_SERVICE, "Resumed music successfully");
                sendPlayingStatusBroadcast(true);
                mediaSession.setPlaybackState(playbackStatePlaying);
                mediaSessionCompat.setPlaybackState(playbackStateCompatPlaying);
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

    private void playNext() {
        tracklistRepository.getFirstSong(partyTitle)
            .subscribe(song -> {
                spotifyPlayer.playUri(new Player.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(LogTag.LOG_PLAYER_SERVICE, "Playing next song in tracklist");
                        isTracklistEmpty = false;
                        //delay sending notification until song certainly is playing (Spotify Android SDK to blame)
                        new Handler().postDelayed(() -> {
                            sendNotification(ServiceUtil.isPlayerServiceInForeground(SpotiqPlayerService.this));
                        }, ServiceConstants.NOTIFICATION_SEND_DELAY_MS);
                        sendPlayingStatusBroadcast(true);
                        mediaSession.setPlaybackState(playbackStatePlaying);
                        mediaSessionCompat.setPlaybackState(playbackStateCompatPlaying);
                    }

                    @Override
                    public void onError(Error error) {
                        Log.d(LogTag.LOG_PLAYER_SERVICE, "Failed to play next song in tracklist: " + error.name());
                        sendPlayingStatusBroadcast(false);
                        mediaSession.setPlaybackState(playbackStatePaused);
                        mediaSessionCompat.setPlaybackState(playbackStateCompatPaused);
                    }
                }, song.getSongUri(), 0, 0);
            }, throwable -> {
                Log.d(LogTag.LOG_PLAYER_SERVICE, "Could not play next song, reason: " + throwable.getMessage());
                isTracklistEmpty = true;
                sendNotification(ServiceUtil.isPlayerServiceInForeground(SpotiqPlayerService.this));
                sendPlayingStatusBroadcast(false);
                mediaSession.setPlaybackState(playbackStatePaused);
                mediaSessionCompat.setPlaybackState(playbackStateCompatPaused);
            });
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

}
