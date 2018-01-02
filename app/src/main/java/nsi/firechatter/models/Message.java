package nsi.firechatter.models;

import com.google.firebase.database.Exclude;

public class Message {

    @Exclude
    public String id;

//    public String chatId;

    public String senderId;
    public String senderName;
    public String avatarUrl;
    public Object dateTime;
    public String content;
    public String type;

    public Message() {
        // Default constructor required for calls to DataSnapshot.getValue(Message.class)
    }

    public Message(String senderId, String senderName, String avatarUrl, String content, String type) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.avatarUrl = avatarUrl;
        this.content = content;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setDateTime(Object dateTime) {
        this.dateTime = dateTime;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSenderName() {
        return senderName;
    }

    public Object getDateTime() {
        return dateTime;
    }

    public String getContent() {
        return content;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }
}
