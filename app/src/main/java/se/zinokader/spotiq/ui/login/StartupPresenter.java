package se.zinokader.spotiq.ui.login;

import android.os.Bundle;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import se.zinokader.spotiq.service.SpotifyService;
import se.zinokader.spotiq.ui.base.BasePresenter;


public class StartupPresenter extends BasePresenter<StartupActivity> {

    private static final int LOG_IN_DELAY = 1;
    private static final int FINISH_DELAY = 1;

    @Inject
    SpotifyService spotifyService;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
    }

    void logIn() {
        getView().startProgress();
        Observable.just(LOG_IN_DELAY)
                .delay(LOG_IN_DELAY, TimeUnit.SECONDS)
                .subscribe( success -> getView().goToAuthentication());
    }

    void logInFinished() {
        getView().finishProgress();
        Observable.just(FINISH_DELAY)
                .delay(FINISH_DELAY, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( success -> getView().goToLobby());
    }

    void logInFailed() {
        getView().resetProgress();
    }

}
