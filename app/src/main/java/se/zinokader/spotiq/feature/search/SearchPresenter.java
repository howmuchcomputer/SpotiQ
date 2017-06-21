package se.zinokader.spotiq.feature.search;

import android.os.Bundle;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.TracksPager;
import se.zinokader.spotiq.constant.LogTag;
import se.zinokader.spotiq.constant.SpotifyConstants;
import se.zinokader.spotiq.feature.base.BasePresenter;
import se.zinokader.spotiq.feature.search.preview.PreviewPlayer;
import se.zinokader.spotiq.model.Song;
import se.zinokader.spotiq.model.User;
import se.zinokader.spotiq.repository.PartiesRepository;
import se.zinokader.spotiq.repository.SpotifyRepository;
import se.zinokader.spotiq.repository.TracklistRepository;
import se.zinokader.spotiq.service.SpotifyCommunicatorService;
import se.zinokader.spotiq.util.mapper.TrackMapper;

public class SearchPresenter extends BasePresenter<SearchView> {

    @Inject
    SpotifyCommunicatorService spotifyCommunicatorService;

    @Inject
    PartiesRepository partiesRepository;

    @Inject
    TracklistRepository tracklistRepository;

    @Inject
    SpotifyRepository spotifyRepository;

    private PreviewPlayer songPreviewPlayer = new PreviewPlayer();
    private String partyTitle;
    private User user;

    private static final int LOAD_USER_RESTARTABLE_ID = 764;

    void setPartyTitle(String partyTitle) {
        this.partyTitle = partyTitle;
    }

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        //load user
        restartableLatestCache(LOAD_USER_RESTARTABLE_ID,
            () -> spotifyRepository.getMe(spotifyCommunicatorService.getWebApi())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()),
            (lobbyView, userPrivate) -> {
                user = new User(userPrivate.id, userPrivate.display_name, userPrivate.images);
            }, (lobbyView, throwable) -> {
                Log.d(LogTag.LOG_LOBBY, "Error when getting user Spotify data");
                throwable.printStackTrace();
            });

        if (savedState == null) {
            start(LOAD_USER_RESTARTABLE_ID);
        }
    }

    @Override
    protected void onDestroy() {
        songPreviewPlayer.release();
        super.onDestroy();
    }

    void startPreview(String previewUrl) {
        songPreviewPlayer.playPreview(previewUrl);
    }

    void stopPreview() {
        songPreviewPlayer.stopPreview();
    }

    void requestSong(Song song) {
        tracklistRepository.checkSongInDbPlaylist(song, partyTitle)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap(songExists -> {
                if (songExists) {
                    view().subscribe(searchViewOptionalView -> {
                        if (searchViewOptionalView.view != null) {
                            searchViewOptionalView.view.showMessage("This song is already in the queue");
                        }
                    });
                    return Observable.just(false);
                }
                else {
                    return tracklistRepository.addSong(song, partyTitle);
                }
            })
            .map(didSucceed -> {
                if (didSucceed) partiesRepository.incrementUserSongRequestCount(user, partyTitle);
                return didSucceed;
            })
            .subscribe(songWasAdded -> view().subscribe(searchViewOptionalView -> {
                if (searchViewOptionalView.view != null) {
                    if (songWasAdded) {
                        searchViewOptionalView.view.finishWithSuccess("Song added to the tracklist!");
                    }
                    else {
                        searchViewOptionalView.view.showMessage("Song could not be added to the tracklist");
                    }
                }
            }));
    }

    void searchTracks(String query) {

        Map<String, Object> searchOptions = new HashMap<>();
        searchOptions.put(SpotifyService.LIMIT, SpotifyConstants.SEARCH_QUERY_RESPONSE_LIMIT);
        searchOptions.put(SpotifyService.OFFSET, 0);

        searchTracksRecursively(query, searchOptions)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .concatMap(tracksPager -> Observable.fromArray(TrackMapper.tracksToSongs(tracksPager.tracks.items, user)))
            .subscribe(new Observer<List<Song>>() {
                @Override
                public void onSubscribe(Disposable d) {

                }

                @Override
                public void onNext(List<Song> songs) {
                    view().subscribe(searchViewOptionalView -> {
                        if (searchViewOptionalView.view != null) {
                            searchViewOptionalView.view.updateSearch(songs, false);
                        }
                    });
                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onComplete() {

                }
            });

    }

    private Observable<TracksPager> searchTracksRecursively(String query, Map<String, Object> searchOptions) {

        int lastOffset = (int) searchOptions.get(SpotifyService.OFFSET);

        return spotifyRepository.searchTracks(query, searchOptions, spotifyCommunicatorService.getWebApi())
            .concatMap(tracksPager -> {
                if (lastOffset + tracksPager.tracks.limit >= SpotifyConstants.TOTAL_ITEMS_LIMIT) {
                    return Observable.just(tracksPager);
                }
                searchOptions.put(SpotifyService.OFFSET, lastOffset + tracksPager.tracks.limit);
                return Observable.just(tracksPager)
                    .concatWith(searchTracksRecursively(query, searchOptions));
            });
    }

}
