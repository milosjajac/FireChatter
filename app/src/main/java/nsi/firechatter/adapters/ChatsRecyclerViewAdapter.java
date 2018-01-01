package nsi.firechatter.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import nsi.firechatter.R;
import nsi.firechatter.models.Chat;

public class ChatsRecyclerViewAdapter extends RecyclerView.Adapter<ChatsRecyclerViewAdapter.ChatViewHolder> {

    private Context context;
    private List<Chat> chats;
    private OnChatInteractionListener chatInteractionListener;

    public interface OnChatInteractionListener {
        void onChatClick(Chat chat);
    }

    public ChatsRecyclerViewAdapter(Context context, OnChatInteractionListener listener, List<Chat> chats) {
        this.context = context;
        this.chatInteractionListener = listener;
        this.chats = chats;
    }

    @Override
    public ChatViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.chats_list_item, parent, false);
        return new ChatViewHolder(view, parent);
    }

    @Override
    public void onBindViewHolder(final ChatViewHolder holder, int position) {
        holder.chat = chats.get(position);
        holder.chatNameTv.setText(holder.chat.name);
        holder.chatLastTv.setText(holder.chat.lastMsgId);

        if (holder.chat.avatarUrl != null && !holder.chat.avatarUrl.isEmpty()) {
            Glide.with(context)
                    .load(holder.chat.avatarUrl)
                    .into(holder.chatAvatarImg);
        }

        holder.chatView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (chatInteractionListener != null) {
                    chatInteractionListener.onChatClick(holder.chat);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    public class ChatViewHolder extends RecyclerView.ViewHolder {
        public final View chatView;
        public final ImageView chatAvatarImg;
        public final TextView chatNameTv;
        public final TextView chatLastTv;
        public Chat chat;

        public ChatViewHolder(View chatView, ViewGroup parent) {
            super(chatView);
            this.chatView = chatView;

            chatAvatarImg = chatView.findViewById(R.id.chats_item_avatar);
            chatNameTv = chatView.findViewById(R.id.chats_item_name);
            chatLastTv = chatView.findViewById(R.id.chats_item_last);
        }
    }

}
