package se.zinokader.spotiq;

import javax.inject.Singleton;
import dagger.Component;
import se.zinokader.spotiq.activity.BaseActivity;
import se.zinokader.spotiq.activity.LoginActivity;
import se.zinokader.spotiq.activity.LobbyActivity;
import se.zinokader.spotiq.activity.PartyActivity;
import se.zinokader.spotiq.activity.SearchActivity;

@Singleton
@Component(modules = { AppModule.class })
public interface AppComponent {
    void inject(LoginActivity target);
    void inject(LobbyActivity target);
    void inject(PartyActivity target);
    void inject(SearchActivity target);
}
