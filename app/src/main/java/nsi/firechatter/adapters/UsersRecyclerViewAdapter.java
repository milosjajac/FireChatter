package nsi.firechatter.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import nsi.firechatter.R;
import nsi.firechatter.models.Chat;
import nsi.firechatter.models.User;

public class UsersRecyclerViewAdapter extends RecyclerView.Adapter<UsersRecyclerViewAdapter.UserViewHolder> {

    private Context context;
    private List<User> users;
    private List<String> selectedUserIds = new ArrayList<>();
    private OnUsersInteractionListener usersInteractionListener;

    public interface OnUsersInteractionListener {

    }

    public UsersRecyclerViewAdapter(Context context, OnUsersInteractionListener listener, List<User> users) {
        this.context = context;
        this.usersInteractionListener = listener;
        this.users = users;
    }

    @Override
    public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.users_list_item, parent, false);
        return new UserViewHolder(view, parent);
    }

    @Override
    public void onBindViewHolder(final UserViewHolder holder, int position) {
        holder.user = users.get(position);
        holder.userNameTv.setText(holder.user.name);

        if (holder.user.avatarUrl != null && !holder.user.avatarUrl.isEmpty()) {
            Glide.with(context)
                    .load(holder.user.avatarUrl)
                    .into(holder.userAvatarImg);
        }

        holder.userView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (usersInteractionListener != null) {
                    updateUserSelectionList(holder.user.id);
                    holder.userChk.toggle();
                }
            }
        });
    }

    private void updateUserSelectionList(String userId) {
        if (selectedUserIds.contains(userId)) {
            selectedUserIds.remove(userId);
        } else {
            selectedUserIds.add(userId);
        }
    }

    public List<String> getSelectedUserIds() {
        return this.selectedUserIds;
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public class UserViewHolder extends RecyclerView.ViewHolder {
        public final View userView;
        public final ImageView userAvatarImg;
        public final TextView userNameTv;
        public final CheckBox userChk;
        public User user;

        public UserViewHolder(View userView, ViewGroup parent) {
            super(userView);
            this.userView = userView;

            userAvatarImg = userView.findViewById(R.id.users_item_avatar);
            userNameTv = userView.findViewById(R.id.users_item_name);
            userChk = userView.findViewById(R.id.users_item_chk);
        }
    }

}
