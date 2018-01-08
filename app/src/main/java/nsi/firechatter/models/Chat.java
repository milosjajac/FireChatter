package nsi.firechatter.models;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

public class Chat {

    @Exclude
    public String id;

    @Exclude
    public String avatarUrl;

    @Exclude
    public Message lastMsg;

    @Exclude
    public String  lastMsgSenderName;

    public String name;
    public String lastMsgId;
    public Map<String, Object> members = new HashMap<>();

    public Chat() {
        lastMsg = new Message();
        // Default constructor required for calls to DataSnapshot.getValue(Chat.class)
    }

    public long getLastMsgTime()
    {
        return lastMsg.dateTime == null ?  0L : (long) lastMsg.dateTime;
    }

}
