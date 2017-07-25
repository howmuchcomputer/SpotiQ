package se.zinokader.spotiq.feature.search.songsearch;

import org.cryse.widget.persistentsearch.SearchSuggestionsBuilder;

import java.util.List;

import se.zinokader.spotiq.feature.base.BaseView;
import se.zinokader.spotiq.model.Song;

public interface SongSearchView extends BaseView {
    void updateSearch(List<Song> songs);
    void updateSearchSuggestions(SearchSuggestionsBuilder searchSuggestionsBuilder);
}
