package nsi.firechatter.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import nsi.firechatter.R;
import nsi.firechatter.models.Message;

public class MessagesRecyclerViewAdapter extends RecyclerView.Adapter {
    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

    private Context mContext;
    private List<Message> mMessageList;

    private DateFormat df = new SimpleDateFormat("dd. MMM ''yy HH:mm");

    public MessagesRecyclerViewAdapter(Context context, List<Message> messageList) {
        mContext = context;
        mMessageList = messageList;
    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }

    // Determines the appropriate ViewType according to the sender of the message.
    @Override
    public int getItemViewType(int position) {
        Message message = mMessageList.get(position);

        if (message.getSenderId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
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
        Message message = mMessageList.get(position);

        switch (holder.getItemViewType()) {
            case VIEW_TYPE_MESSAGE_SENT:
                ((SentMessageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MESSAGE_RECEIVED:
                ((ReceivedMessageHolder) holder).bind(message);
        }
    }

    private class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;
        ImageView messageImage;

        SentMessageHolder(View itemView) {
            super(itemView);

            messageText = (TextView) itemView.findViewById(R.id.my_message_text_body);
            messageImage = (ImageView) itemView.findViewById(R.id.my_message_image_body);
            timeText = (TextView) itemView.findViewById(R.id.my_message_time);
        }

        void bind(Message message) {
            switch (message.getType()) {
                case TEXT:
                    messageImage.setVisibility(View.GONE);
                    messageText.setVisibility(View.VISIBLE);
                    messageText.setText(message.getContent());
                    break;
                case IMAGE:
                    messageImage.setVisibility(View.GONE);
                    messageText.setVisibility(View.VISIBLE);
                    String messageUrl = message.getContent();
                    if ( messageUrl!= null && !messageUrl.isEmpty()) {
                    Glide.with(mContext)
                            .load(messageUrl)
                            .into(messageImage);
                    }
                    break;
            }

            // Format the stored timestamp into a readable String using method.
            timeText.setText(df.format(message.getDateTime()));
        }
    }

    private class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText, nameText;
        ImageView profileImage, messageImage;

        ReceivedMessageHolder(View itemView) {
            super(itemView);

            messageText = (TextView) itemView.findViewById(R.id.sender_message_text_body);
            messageImage = (ImageView) itemView.findViewById(R.id.my_message_image_body);
            timeText = (TextView) itemView.findViewById(R.id.sender_message_time);
            nameText = (TextView) itemView.findViewById(R.id.sender_message_name);
            profileImage = (ImageView) itemView.findViewById(R.id.sender_message_avatar);
        }

        void bind(Message message) {
            switch (message.getType()) {
                case TEXT:
                    messageImage.setVisibility(View.GONE);
                    messageText.setVisibility(View.VISIBLE);
                    messageText.setText(message.getContent());
                    break;
                case IMAGE:
                    messageImage.setVisibility(View.GONE);
                    messageText.setVisibility(View.VISIBLE);
                    String messageUrl = message.getContent();
                    if ( messageUrl!= null && !messageUrl.isEmpty()) {
                        Glide.with(mContext)
                                .load(messageUrl)
                                .into(messageImage);
                    }
                    break;
            }

            // Format the stored timestamp into a readable String using method.
            timeText.setText(df.format(message.getDateTime()));
            //TODO set avatar & name
//            nameText.setText(message.getSenderName());
//
//            // Insert the profile image from the URL into the ImageView.
//            String avatarUrl = message.getAvatarUrl();
//            if ( avatarUrl!= null && !avatarUrl.isEmpty()) {
//                Glide.with(mContext)
//                        .load(avatarUrl)
//                        .into(profileImage);
//            }
        }
    }
}
