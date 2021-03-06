package se.zinokader.spotiq.feature.search;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import se.zinokader.spotiq.R;
import se.zinokader.spotiq.constant.ApplicationConstants;
import se.zinokader.spotiq.model.Song;
import se.zinokader.spotiq.util.mapper.ArtistMapper;
import se.zinokader.spotiq.util.type.Empty;

public class SongRecyclerAdapter extends RecyclerView.Adapter<SongRecyclerAdapter.SongHolder> {

    private final PublishSubject<Song> onClickSubject = PublishSubject.create();
    private final PublishSubject<Song> onLongClickSubject = PublishSubject.create();
    private final PublishSubject<Empty> onLongClickEndSubject = PublishSubject.create();
    private boolean curentlyLongClicking = false;
    private List<Song> songs = new ArrayList<>();

    @Override
    public SongHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View inflatedView = LayoutInflater.from(viewGroup.getContext())
            .inflate(R.layout.recyclerview_row_search_song, viewGroup, false);
        inflatedView.getLayoutParams().width = viewGroup.getWidth();
        return new SongHolder(inflatedView);
    }

    @Override
    public void onBindViewHolder(SongHolder songHolder, int i) {

        Song song = songs.get(i);
        Context context = songHolder.itemView.getContext();

        songHolder.itemView.setOnClickListener(view -> onClickSubject.onNext(song));
        songHolder.itemView.setOnLongClickListener(view -> {
            curentlyLongClicking = true;
            onLongClickSubject.onNext(song);
            return true;
        });
        songHolder.itemView.setOnTouchListener((view, motionEvent) -> {
            view.onTouchEvent(motionEvent);
            if (motionEvent.getAction() == MotionEvent.ACTION_UP && curentlyLongClicking) {
                onLongClickEndSubject.onNext(new Empty());
                curentlyLongClicking = false;
            }
            return true;
        });

        String artistsName = ArtistMapper.joinArtistNames(song.getArtists());

        Glide.with(context)
            .load(song.getAlbumArtUrl())
            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
            .placeholder(R.drawable.image_album_placeholder)
            .fitCenter()
            .override(ApplicationConstants.LOW_QUALITY_ALBUM_ART_DIMENSION, ApplicationConstants.LOW_QUALITY_ALBUM_ART_DIMENSION)
            .into(songHolder.albumArt);
        songHolder.songName.setText(song.getName());
        songHolder.artistsName.setText(artistsName);
        songHolder.albumName.setText(song.getAlbum().name);
    }

    @Override
    public void onViewRecycled(SongHolder songHolder) {
        if(songHolder != null) {
            songHolder.albumArt.setImageDrawable(null);
            Glide.clear(songHolder.albumArt);
        }
        super.onViewRecycled(songHolder);
    }

    public void updateSongs(List<Song> songs) {
        this.songs.addAll(songs);
    }

    public void clearResults() {
        songs.clear();
    }

    public Observable<Song> observeClicks() {
        return onClickSubject;
    }

    public Observable<Song> observeLongClicks() {
        return onLongClickSubject;
    }

    public Observable<Empty> observeLongClickEnd() {
        return onLongClickEndSubject;
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    class SongHolder extends RecyclerView.ViewHolder {

        private ImageView albumArt;
        private TextView songName;
        private TextView artistsName;
        private TextView albumName;

        SongHolder(View view) {
            super(view);

            albumArt = view.findViewById(R.id.albumArt);
            songName = view.findViewById(R.id.songName);
            artistsName = view.findViewById(R.id.artistsName);
            albumName = view.findViewById(R.id.albumName);
        }


    }

}
