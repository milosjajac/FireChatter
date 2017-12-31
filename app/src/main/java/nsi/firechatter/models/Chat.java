package nsi.firechatter.models;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

public class Chat {

    @Exclude
    public String id;

    public String name;
    public String avatarUrl;
    public String last;
    public Object lastDate;
    private Map<String, Boolean> members = new HashMap<>();

    public Chat() {
        // Default constructor required for calls to DataSnapshot.getValue(Chat.class)
    }

    public Chat(String name, String avatarUrl, String last) {
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.last = last;
    }
}
