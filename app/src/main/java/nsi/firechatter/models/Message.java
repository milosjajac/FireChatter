package nsi.firechatter.models;

import com.google.firebase.database.Exclude;

public class Message {

    @Exclude
    private String id;

    private String senderId;
    private Object dateTime;
    private String content;
    private MessageTypeEnum type;

    public Message() {
        // Default constructor required for calls to DataSnapshot.getValue(Message.class)
    }

    public Message(String senderId, String content, MessageTypeEnum type) {
        this.senderId = senderId;
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

    public void setDateTime(Object dateTime) {
        this.dateTime = dateTime;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MessageTypeEnum getType() {
        return type;
    }

    public void setType(MessageTypeEnum type) {
        this.type = type;
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
}
