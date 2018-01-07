package nsi.firechatter.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseUser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import nsi.firechatter.R;
import nsi.firechatter.activities.ChatActivity;
import nsi.firechatter.models.Chat;

public class ChatsRecyclerViewAdapter extends RecyclerView.Adapter<ChatsRecyclerViewAdapter.ChatViewHolder> {

    private Context context;
    private List<Chat> chats;
    private OnChatInteractionListener chatInteractionListener;
    private DateFormat df;
    private String currentUserId;


    public interface OnChatInteractionListener {
        void onChatClick(Chat chat);
    }

    public ChatsRecyclerViewAdapter(Context context, OnChatInteractionListener listener, List<Chat> chats) {
        this.context = context;
        this.chatInteractionListener = listener;
        this.chats = chats;
    }

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
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

        if(holder.chat.lastMsgId!=null) {
            holder.chatLastMsgTv.setText(holder.chat.lastMsg);

            int daysDiff = (int) (TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())
                    - TimeUnit.MILLISECONDS.toDays((long) holder.chat.lastMsgDate));

            switch (daysDiff) {
                case 0:
                    df = new SimpleDateFormat("HH:mm");
                    break;
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                    df = new SimpleDateFormat("E");
                    break;
                default:
                    df = new SimpleDateFormat("dd. MMM ''yy");
                    break;
            }

            holder.chatLastTimeTv.setText(df.format(holder.chat.lastMsgDate));

            long lastActivity = (long) holder.chat.members.get(currentUserId);

            if (lastActivity != ChatActivity.SECRET_DATE && lastActivity < (long) holder.chat.lastMsgDate) {
                holder.chatLastMsgTv.setTextColor(Color.BLACK);
                holder.chatLastMsgTv.setTypeface(null, Typeface.BOLD);
                holder.chatLastTimeTv.setTextColor(Color.BLACK);
                holder.chatLastTimeTv.setTypeface(null, Typeface.BOLD);
            }
        }
        else
        {
            holder.chatLastMsgTv.setText("This is new conversation");
            holder.chatLastTimeTv.setText("");
        }
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
        public final TextView chatLastMsgTv;
        public final TextView chatLastTimeTv;
        public Chat chat;

        public ChatViewHolder(View chatView, ViewGroup parent) {
            super(chatView);
            this.chatView = chatView;

            chatAvatarImg = chatView.findViewById(R.id.chats_item_avatar);
            chatNameTv = chatView.findViewById(R.id.chats_item_name);
            chatLastMsgTv = chatView.findViewById(R.id.chats_item_last_message);
            chatLastTimeTv = chatView.findViewById(R.id.chats_item_time);
        }
    }

}
