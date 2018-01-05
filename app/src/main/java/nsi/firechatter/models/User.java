package nsi.firechatter.models;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

public class User {

    @Exclude
    public String id;

    public String email;
    public String name;
    public String avatarUrl;
    public String deviceToken;
    public Map<String, Boolean> chats = new HashMap<>();

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String email, String name, String avatarUrl, String deviceToken) {
        this.email = email;
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.deviceToken = deviceToken;
    }
}
