package nsi.firechatter.models;

import com.google.firebase.database.Exclude;

public class User {

    @Exclude
    public String id;

    public String email;
    public String name;
    public String avatarUrl;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String email, String name, String avatarUrl) {
        this.email = email;
        this.name = name;
        this.avatarUrl = avatarUrl;
    }
}
