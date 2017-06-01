package se.zinokader.spotiq.model;

import java.util.List;

import kaaes.spotify.webapi.android.models.AlbumSimple;
import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.Image;

public class Song {

    private String addedBySpotifyId;
    private AlbumSimple album;
    private List<ArtistSimple> artists;
    private long durationMs;
    private String name;
    private String previewUrl;

    public Song() {
    }

    public Song(String addedBySpotifyId, List<ArtistSimple> artists, AlbumSimple album, long durationMs, String name, String previewUrl) {
        this.addedBySpotifyId = addedBySpotifyId;
        this.album = album;
        this.artists = artists;
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

    public Image getAlbumArt() {
        return this.album.images.get(0);
    }
}
