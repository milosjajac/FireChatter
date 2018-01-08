package nsi.firechatter.adapters;

import android.content.AsyncQueryHandler;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestFutureTarget;
import com.bumptech.glide.request.RequestOptions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import nsi.firechatter.R;
import nsi.firechatter.activities.ChatActivity;
import nsi.firechatter.models.Chat;
import nsi.firechatter.models.MessageTypeEnum;

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
        String msgPrefix = "";

        if(holder.chat.lastMsgId!=null) {
            if(holder.chat.lastMsg.senderId.equals(currentUserId))
            {
                msgPrefix = "You";
            }
            else if(holder.chat.members.size()==2) {
                msgPrefix = "";
            }else {
                msgPrefix = holder.chat.lastMsgSenderName.split(" ", 2)[0];
            }

            if(!msgPrefix.isEmpty()) {
                if (holder.chat.lastMsg.type == MessageTypeEnum.TEXT) {
                    msgPrefix = msgPrefix + ": ";
                }
            }

            holder.chatLastMsgTv.setText(msgPrefix.concat(holder.chat.lastMsg.content));


            int daysDiff = (int) (TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())
                    - TimeUnit.MILLISECONDS.toDays((long) holder.chat.lastMsg.dateTime));

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

            holder.chatLastTimeTv.setText(df.format(holder.chat.lastMsg.dateTime));

            long lastActivity = (long) holder.chat.members.get(currentUserId);

            if (lastActivity != ChatActivity.SPECIAL_TIME && lastActivity < (long) holder.chat.lastMsg.dateTime) {
                holder.chatLastMsgTv.setTextColor(Color.BLACK);
                holder.chatLastMsgTv.setTypeface(null, Typeface.BOLD);
                holder.chatLastTimeTv.setTextColor(Color.BLACK);
                holder.chatLastTimeTv.setTypeface(null, Typeface.BOLD);
            }
            else
            {
                holder.chatLastMsgTv.setTextColor(fetchPrimaryColor());
                holder.chatLastMsgTv.setTypeface(null, Typeface.NORMAL);
                holder.chatLastTimeTv.setTextColor(fetchPrimaryColor());
                holder.chatLastTimeTv.setTypeface(null, Typeface.NORMAL);
            }
        }
        else
        {
            holder.chatLastMsgTv.setText(R.string.main_activity_no_chat_messages);
            holder.chatLastTimeTv.setText("");
        }

        if (holder.chat.avatarUrl != null && !holder.chat.avatarUrl.isEmpty()) {
            Glide.with(context)
                    .load(holder.chat.avatarUrl)
                    .into(holder.chatAvatarImg);
        }else {
            Glide.with(context)
                    .load(R.drawable.firechatter_chat_group_logo)
                    .apply(new RequestOptions().fitCenter())
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

    private int fetchPrimaryColor() {
        TypedValue typedValue = new TypedValue();

        TypedArray a = context.obtainStyledAttributes(typedValue.data, new int[] { R.attr.colorPrimary });
        int color = a.getColor(0, 0);

        a.recycle();

        return color;
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
