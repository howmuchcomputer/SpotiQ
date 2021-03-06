package se.zinokader.spotiq.feature.party.tracklist;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import jp.wasabeef.recyclerview.animators.FadeInDownAnimator;
import se.zinokader.spotiq.R;
import se.zinokader.spotiq.constant.ApplicationConstants;
import se.zinokader.spotiq.databinding.FragmentTracklistBinding;
import se.zinokader.spotiq.model.Song;
import se.zinokader.spotiq.repository.TracklistRepository;
import se.zinokader.spotiq.util.di.Injector;
import se.zinokader.spotiq.util.view.CustomDividerItemDecoration;

public class TracklistFragment extends Fragment {

    FragmentTracklistBinding binding;

    @Inject
    TracklistRepository tracklistRepository;

    private CompositeDisposable subscriptions = new CompositeDisposable();

    private FadeInDownAnimator itemAnimator = new FadeInDownAnimator();

    private List<Song> songs = new ArrayList<>();

    public static TracklistFragment newInstance(String partyTitle) {
        TracklistFragment tracklistFragment = new TracklistFragment();
        Bundle newInstanceArguments = new Bundle();
        newInstanceArguments.putString(ApplicationConstants.PARTY_NAME_EXTRA, partyTitle);
        tracklistFragment.setArguments(newInstanceArguments);
        return tracklistFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        ((Injector) getContext().getApplicationContext()).inject(this);
        super.onCreate(savedInstanceState);
        String partyTitle = getArguments().getString(ApplicationConstants.PARTY_NAME_EXTRA);

        itemAnimator.setInterpolator(new DecelerateInterpolator());
        itemAnimator.setAddDuration(ApplicationConstants.DEFAULT_ITEM_ADD_DURATION_MS);
        itemAnimator.setRemoveDuration(ApplicationConstants.DEFAULT_ITEM_REMOVE_DURATION_MS);
        itemAnimator.setMoveDuration(ApplicationConstants.DEFAULT_ITEM_MOVE_DURATION_MS);

        subscriptions.add(tracklistRepository.listenToTracklistChanges(partyTitle)
            .delay(ApplicationConstants.DEFAULT_NEW_ITEM_DELAY_MS, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(childEvent -> {
                Song song = childEvent.getDataSnapshot().getValue(Song.class);
                switch (childEvent.getChangeType()) {
                    case ADDED:
                        addSong(song);
                        break;
                    case REMOVED:
                        removeSong(song);
                        break;
                }}));
    }

    @Override
    public void onDestroy() {
        subscriptions.clear();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putString(ApplicationConstants.PARTY_NAME_EXTRA, getArguments().getString(ApplicationConstants.PARTY_NAME_EXTRA));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parentContainer, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_tracklist, parentContainer, false);

        binding.tracklistRecyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        binding.tracklistRecyclerView.setEmptyView(binding.tracklistEmptyView);
        binding.tracklistRecyclerView.setItemAnimator(itemAnimator);
        binding.tracklistRecyclerView.addItemDecoration(new CustomDividerItemDecoration(
            getResources().getDrawable(R.drawable.track_list_padding_divider), true, true));
        binding.tracklistRecyclerView.setAdapter(new TracklistRecyclerAdapter(songs));

        return binding.getRoot();
    }

    public void scrollToTop() {
        binding.tracklistRecyclerView.smoothScrollToPosition(0);
    }

    private void addSong(Song song) {
        songs.add(song);
        int songPosition = getSongPosition(song);
        binding.tracklistRecyclerView.getAdapter().notifyItemInserted(songPosition);
    }

    private void removeSong(Song song) {
        int songPosition = getSongPosition(song);
        songs.remove(songPosition);
        binding.tracklistRecyclerView.getAdapter().notifyItemRemoved(songPosition);
    }

    private int getSongPosition(Song song) {
        for (int position = 0; position < songs.size(); position++) {
            if (song.getSongSpotifyId().equals(songs.get(position).getSongSpotifyId())) {
                return position;
            }
        }
        return -1;
    }

}
