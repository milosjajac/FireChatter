package nsi.firechatter.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import nsi.firechatter.R;
import nsi.firechatter.activities.ChatActivity;
import nsi.firechatter.models.Message;
import nsi.firechatter.models.User;

public class MessagesRecyclerViewAdapter extends RecyclerView.Adapter {
    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

    private Context context;
    private List<Message> messages;
    private Map<String, User> members;

    private DateFormat df = new SimpleDateFormat("dd. MMM ''yy HH:mm");

    public MessagesRecyclerViewAdapter(Context context, List<Message> messageList) {
        this.context = context;
        messages = messageList;
    }

    public void setMembers(Map<String, User> members) {
        this.members = members;
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // Determines the appropriate ViewType according to the sender of the message.
    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);

        if (message.senderId.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            // If the current user is the sender of the message
            return VIEW_TYPE_MESSAGE_SENT;
        } else {
            // If some other user sent the message
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }
    }

    // Inflates the appropriate layout according to the ViewType.
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;

        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.sent_message_list_item, parent, false);
            return new SentMessageHolder(view);
        } else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.recived_message_list_item, parent, false);
            return new ReceivedMessageHolder(view);
        }

        return null;
    }

    // Passes the message object to a ViewHolder so that the contents can be bound to UI.
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);

        switch (holder.getItemViewType()) {
            case VIEW_TYPE_MESSAGE_SENT:
                ((SentMessageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MESSAGE_RECEIVED:
                User sender = members.get(message.senderId);
                ((ReceivedMessageHolder) holder).bind(message, sender);
        }
    }

    private class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;
        ImageView messageImage;

        SentMessageHolder(View itemView) {
            super(itemView);

            messageText = (TextView) itemView.findViewById(R.id.my_message_text_body);
            messageImage = (ImageView) itemView.findViewById(R.id.my_message_image_body);
            Glide.with(context).load(R.drawable.loading).into(messageImage);
            timeText = (TextView) itemView.findViewById(R.id.my_message_time);
        }

        void bind(Message message) {
            switch (message.type) {
                case TEXT:
                    messageImage.setVisibility(View.GONE);
                    messageText.setVisibility(View.VISIBLE);
                    messageText.setText(message.content);
                    break;
                case IMAGE:
                    messageImage.setVisibility(View.VISIBLE);
                    messageText.setVisibility(View.GONE);
                    String messageUrl = message.content;
                    if ( messageUrl!= null && !messageUrl.isEmpty()) {
                        Glide.with(context)
                                .load(messageUrl)
                                .apply(new RequestOptions()
                                        .fitCenter())
                                .into(messageImage);
                    }
                    break;
            }

            // Format the stored timestamp into a readable String using method.
            timeText.setText(df.format(message.dateTime));
        }
    }

    private class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText, nameText;
        ImageView profileImage, messageImage;

        ReceivedMessageHolder(View itemView) {
            super(itemView);

            messageText = (TextView) itemView.findViewById(R.id.sender_message_text_body);
            messageImage = (ImageView) itemView.findViewById(R.id.sender_message_image_body);
            Glide.with(context).load(R.drawable.loading).into(messageImage);
            timeText = (TextView) itemView.findViewById(R.id.sender_message_time);
            nameText = (TextView) itemView.findViewById(R.id.sender_message_name);
            profileImage = (ImageView) itemView.findViewById(R.id.sender_message_avatar);
        }

        void bind(Message message, User sender) {
            switch (message.type) {
                case TEXT:
                    messageImage.setVisibility(View.GONE);
                    messageText.setVisibility(View.VISIBLE);
                    messageText.setText(message.content);
                    break;
                case IMAGE:
                    messageImage.setVisibility(View.VISIBLE);
                    messageText.setVisibility(View.GONE);
                    String messageUrl = message.content;
                    if ( messageUrl!= null && !messageUrl.isEmpty()) {
                        Glide.with(context)
                                .load(messageUrl)
                                .apply(new RequestOptions()
                                        .fitCenter())
                                .into(messageImage);
                    }
                    break;
            }

            // Format the stored timestamp into a readable String using method.
            timeText.setText(df.format(message.dateTime));
            nameText.setText(sender.name);

            // Insert the profile image from the URL into the ImageView.
            String avatarUrl = sender.avatarUrl;
            if ( avatarUrl!= null && !avatarUrl.isEmpty()) {
                Glide.with(context)
                        .load(avatarUrl)
                        .into(profileImage);
            }
        }
    }
}
