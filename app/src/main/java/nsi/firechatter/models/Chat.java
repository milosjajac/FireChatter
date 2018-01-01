package nsi.firechatter.models;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

public class Chat {

    @Exclude
    public String id;

    @Exclude
    public String name;

    @Exclude
    public String avatarUrl;

    public String lastMsgId;
    public Object lastMsgDate;
    public Map<String, Boolean> members = new HashMap<>();

    public Chat() {
        // Default constructor required for calls to DataSnapshot.getValue(Chat.class)
    }

}
