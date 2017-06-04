package se.zinokader.spotiq.ui.party;

import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import icepick.State;
import nucleus5.factory.RequiresPresenter;
import se.zinokader.spotiq.R;
import se.zinokader.spotiq.constants.ApplicationConstants;
import se.zinokader.spotiq.databinding.ActivityPartyBinding;
import se.zinokader.spotiq.ui.base.BaseActivity;
import se.zinokader.spotiq.util.helper.GlideRequestOptions;

@RequiresPresenter(PartyPresenter.class)
public class PartyActivity extends BaseActivity<PartyPresenter> {

    ActivityPartyBinding binding;

    @State
    Bundle partyInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        postponeEnterTransition();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_party);
        binding.setPresenter(getPresenter());
        partyInfo = getIntent().getExtras();
        if(partyInfo != null) {
            getPresenter().setPartyName(partyInfo.getString(ApplicationConstants.PARTY_NAME_EXTRA));
        }
        getPresenter().loadParty();
        getPresenter().loadUser();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPresenter().resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPresenter().pause();
    }

    public void updatePartyDetails() {

    }

    public void setUserDetails(String userName, String userImageUrl) {
        binding.userName.setText(userName);
        Glide.with(this)
                .load(userImageUrl)
                .apply(GlideRequestOptions.getProfileImageOptions())
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        startPostponedEnterTransition();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        startPostponedEnterTransition();
                        return false;
                    }
                })
                .into(binding.userImage);
    }

    public void setHostDetails(String hostName) {

    }

    public void setHostPriviliges() {

    }

    @Override
    public View getRootView() {
        return binding.getRoot();
    }
}