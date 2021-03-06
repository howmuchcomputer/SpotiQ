package se.zinokader.spotiq.feature.base;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.inputmethod.InputMethodManager;

import com.github.andrewlord1990.snackbarbuilder.SnackbarBuilder;

import java.util.List;

import nucleus5.factory.PresenterFactory;
import nucleus5.presenter.Presenter;
import nucleus5.view.NucleusAppCompatActivity;
import se.zinokader.spotiq.R;
import se.zinokader.spotiq.constant.ApplicationConstants;
import se.zinokader.spotiq.service.authentication.SpotifyAuthenticationService;
import se.zinokader.spotiq.util.di.Injector;

public abstract class BaseActivity<P extends Presenter> extends NucleusAppCompatActivity<P> implements BaseView {

    private boolean snackbarShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        final PresenterFactory<P> superFactory = super.getPresenterFactory();
        setPresenterFactory(superFactory == null ? null : (PresenterFactory<P>) () -> {
            P presenter = superFactory.createPresenter();
            ((Injector) getApplication()).inject(presenter);
            return presenter;
        });
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    public void onPause() {
        hideKeyboard();
        super.onPause();
    }


    @Override
    public void onBackPressed() {
        List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
        boolean handled = false;
        for (Fragment fragment : fragmentList) {
            if (fragment instanceof BaseFragment) {
                handled = ((BaseFragment) fragment).onBackPressed();
                if (handled) {
                    break;
                }
            }
        }

        if(!handled) {
            super.onBackPressed();
        }
    }

    public void showMessage(String message) {
        if (snackbarShowing) {
            deferMessage(message);
        }
        else {
            snackbarShowing = true;
            new SnackbarBuilder(getRootView())
                .message(message)
                .dismissCallback((snackbar, i) -> snackbarShowing = false)
                .build()
                .show();
        }
    }

    private void deferMessage(String message) {
        new Handler().postDelayed(() -> {
            snackbarShowing = true;
            new SnackbarBuilder(getRootView())
                .message(message)
                .dismissCallback((snackbar, i) -> snackbarShowing = false)
                .build()
                .show();
        }, ApplicationConstants.DEFER_SNACKBAR_DELAY);
    }

    public void finishWithSuccess(String message) {
        new SnackbarBuilder(getRootView())
            .duration(Snackbar.LENGTH_SHORT)
            .message(message)
            .dismissCallback((snackbar, i) -> finish())
            .build()
            .show();
    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getRootView().getWindowToken(), 0);
    }

    public void startForegroundTokenRenewalService() {
        startService(new Intent(this, SpotifyAuthenticationService.class));
    }

    public void stopForegroundTokenRenewalService() {
        stopService(new Intent(this, SpotifyAuthenticationService.class));
    }

}
