package se.zinokader.spotiq.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.PlaybackBitrate;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import se.zinokader.spotiq.constant.ApplicationConstants;
import se.zinokader.spotiq.constant.LogTag;
import se.zinokader.spotiq.constant.SpotifyConstants;
import se.zinokader.spotiq.repository.TracklistRepository;
import se.zinokader.spotiq.util.di.Injector;

public class SpotiqPlayerService extends Service implements ConnectionStateCallback, Player.NotificationCallback {

    @Inject
    TracklistRepository tracklistRepository;

    @Inject
    SpotifyCommunicatorService spotifyCommunicatorService;

    private Config playerConfig;
    private SpotifyPlayer spotifyPlayer;
    private String partyTitle;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        ((Injector) getApplicationContext()).inject(this);
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        if (spotifyPlayer != null) spotifyPlayer.shutdown();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LogTag.LOG_PLAYER_SERVICE, "Player service initialized");
        partyTitle = intent.getStringExtra(ApplicationConstants.PARTY_NAME_EXTRA);
        if (partyTitle.isEmpty()) {
            Log.d(LogTag.LOG_PLAYER_SERVICE, "Player service could not be initialized. Received empty party title");
            stopSelf();
        }
        return START_STICKY; //don't boil me, I'm still alive!
    }

    private void initPlayer() {

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
                        Log.d(LogTag.LOG_PLAYER_SERVICE, "Set Spotify Player custom playback bitrate successfully");
                    }

                    @Override
                    public void onError(Error error) {
                        Log.d(LogTag.LOG_PLAYER_SERVICE, "Failed to set Spotify Player playback bitrate. Cause: " + error.toString());

                    }
                }, PlaybackBitrate.BITRATE_HIGH);

            }

            @Override
            public void onError(Throwable throwable) {
                Log.d(LogTag.LOG_PLAYER_SERVICE, "Could not initialize Spotify Player: " + throwable.getMessage());
                stopSelf();
            }

        });
    }

    private void musicAction() {
        /*
        if (tracklist.isEmpty()) {
            restartableFirst(EMPTY_TRACKLIST_MESSAGE_RESTARTABLE_ID,
                () -> Observable.just(new Empty()),
                (partyView, empty) -> partyView.showMessage("No songs in the tracklist, try adding some!"));
            return Observable.just(false);
        }
        else if (spotifyPlayer.getPlaybackState().isPlaying) {
            return pause();
        }
        else if(spotifyPlayer.getMetadata().currentTrack != null) {
            return resume();
        }
        else {
            return play();
        }
        */
    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        switch (playerEvent) {
            case kSpPlaybackNotifyTrackDelivered:
                tracklistRepository.removeFirstSong(partyTitle)
                    .delay(2, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(wasRemoved -> {
                        // musicAction();
                    });
        }
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
    }

    @Override
    public void onTemporaryError() {

    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d(LogTag.LOG_PLAYER_SERVICE, "Spotify connection message: " + message);
    }
}