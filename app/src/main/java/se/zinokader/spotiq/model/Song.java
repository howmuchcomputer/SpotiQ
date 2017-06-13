package se.zinokader.spotiq.model;

import com.google.firebase.database.Exclude;

import java.util.List;

import kaaes.spotify.webapi.android.models.AlbumSimple;
import kaaes.spotify.webapi.android.models.ArtistSimple;
import se.zinokader.spotiq.constant.ApplicationConstants;


public class Song {

    private String addedBySpotifyId;
    private String songSpotifyId;
    private AlbumSimple album;
    private List<ArtistSimple> artists;
    private long durationMs;
    private String name;
    private String previewUrl;

    public Song() {
    }

    public Song(String addedBySpotifyId, String songSpotifyId, List<ArtistSimple> artists, AlbumSimple album, long durationMs, String name, String previewUrl) {
        this.addedBySpotifyId = addedBySpotifyId;
        this.songSpotifyId = songSpotifyId;
        this.artists = artists;
        this.album = album;
        this.durationMs = durationMs;
        this.name = name;
        this.previewUrl = previewUrl;
    }

    public String getAddedBySpotifyId() {
        return addedBySpotifyId;
    }

    public void setAddedBySpotifyId(String addedBySpotifyId) {
        this.addedBySpotifyId = addedBySpotifyId;
    }

    public String getSongSpotifyId() {
        return songSpotifyId;
    }

    public void setSongSpotifyId(String songSpotifyId) {
        this.songSpotifyId = songSpotifyId;
    }

    public List<ArtistSimple> getArtists() {
        return artists;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getName() {
        return name;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public AlbumSimple getAlbum() {
        return album;
    }

    @Exclude
    public String getSongUri() {
        return "spotify:track:" + songSpotifyId;
    }

    @Exclude
    public String getAlbumArtUrl() {
        return album.images.isEmpty()
                ? ApplicationConstants.PROFILE_IMAGE_PLACEHOLDER_URL
                : album.images.get(0).url;
    }
}
