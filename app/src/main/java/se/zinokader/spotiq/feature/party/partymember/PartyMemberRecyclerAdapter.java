package se.zinokader.spotiq.feature.party.partymember;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import se.zinokader.spotiq.R;
import se.zinokader.spotiq.model.User;

public class PartyMemberRecyclerAdapter extends RecyclerView.Adapter<PartyMemberRecyclerAdapter.UserHolder> {

    private List<User> partyMembers;

    PartyMemberRecyclerAdapter(List<User> partyMembers) {
        this.partyMembers = partyMembers;
    }

    @Override
    public UserHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View inflatedView = LayoutInflater.from(viewGroup.getContext())
            .inflate(R.layout.recyclerview_row_party_members, viewGroup, false);
        inflatedView.getLayoutParams().width = viewGroup.getWidth();
        return new UserHolder(inflatedView);
    }

    @Override
    public void onBindViewHolder(UserHolder userHolder, int i) {
        User partyMember = partyMembers.get(i);
        Glide.with(userHolder.itemView.getContext())
            .load(partyMember.getUserImageUrl())
            .into(userHolder.userImage);
        userHolder.userName.setText(partyMember.getUserName());
        userHolder.songsRequested.setText(String.format(
            partyMember.getSongsRequested() == 1
                ? "Requested %d song"
                : "Requested %d songs",
            partyMember.getSongsRequested()));
        userHolder.memberType.setText(partyMember.getHasHostPrivileges() ? "Host" : "Member");
    }

    @Override
    public long getItemId(int position) {
        return partyMembers.get(position).getUserId().hashCode();
    }

    @Override
    public int getItemCount() {
        return partyMembers.size();
    }

    public static class UserHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ImageView userImage;
        private TextView userName;
        private TextView songsRequested;
        private TextView memberType;

        UserHolder(View view) {
            super(view);

            userImage = view.findViewById(R.id.userImage);
            userName = view.findViewById(R.id.userName);
            songsRequested = view.findViewById(R.id.songsRequested);
            memberType = view.findViewById(R.id.memberType);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
        }
    }

}
