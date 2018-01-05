package nsi.firechatter.models;

import com.google.firebase.database.Exclude;

public class Message {

    @Exclude
    public String id;

    public String senderId;
    public Object dateTime;
    public String content;
    public MessageTypeEnum type;

    public Message() {
        // Default constructor required for calls to DataSnapshot.getValue(Message.class)
    }

    public Message(String senderId, String content, MessageTypeEnum type) {
        this.senderId = senderId;
        this.content = content;
        this.type = type;
    }

}
