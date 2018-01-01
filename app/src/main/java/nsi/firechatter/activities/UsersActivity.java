package nsi.firechatter.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nsi.firechatter.R;
import nsi.firechatter.adapters.UsersRecyclerViewAdapter;
import nsi.firechatter.models.Chat;
import nsi.firechatter.models.User;

public class UsersActivity extends AppCompatActivity implements UsersRecyclerViewAdapter.OnUsersInteractionListener {

    private RecyclerView usersRecyclerView;
    private ProgressBar usersProgressBar;
    private TextView usersEmptyText;

    private List<User> users = new ArrayList<>();
    private UsersRecyclerViewAdapter usersAdapter;

    private DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
    private DatabaseReference usersDbRef = FirebaseDatabase.getInstance().getReference().child("users");
    private DatabaseReference chatsDbRef = FirebaseDatabase.getInstance().getReference().child("chats");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);
        Toolbar toolbar = findViewById(R.id.users_activity_toolbar);
        setSupportActionBar(toolbar);

        usersRecyclerView = findViewById(R.id.users_activity_users_list);
        usersProgressBar = findViewById(R.id.users_activity_users_progress);
        usersEmptyText = findViewById(R.id.users_activity_users_empty_txt);

        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        divider.setDrawable(getDrawable(R.drawable.list_divider));
        usersRecyclerView.addItemDecoration(divider);

        usersAdapter = new UsersRecyclerViewAdapter(this, this, users);
        usersRecyclerView.setAdapter(usersAdapter);

        getUsersAndSetupUI();
    }

    private void getUsersAndSetupUI() {
        final String loggedUserId = FirebaseAuth.getInstance().getUid();

        usersDbRef.orderByChild("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    if (!userId.equals(loggedUserId)) {
                        User user = userSnapshot.getValue(User.class);
                        user.id = userId;
                        users.add(user);
                    }
                }

                if (users.size() == 0) {
                    usersProgressBar.setVisibility(View.GONE);
                    usersEmptyText.setVisibility(View.VISIBLE);
                } else {
                    usersAdapter.notifyDataSetChanged();

                    usersRecyclerView.setVisibility(View.VISIBLE);
                    usersProgressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_users, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_users_finish:
                onFinish();
                break;
        }
        return true;
    }

    private void onFinish() {
        List<String> selectedUserIds = usersAdapter.getSelectedUserIds();
        if (selectedUserIds.size() > 0) {
            initiateChatWithUsers(selectedUserIds);
        } else {
            Toast.makeText(this, getString(R.string.users_activity_none_selected_msg), Toast.LENGTH_SHORT).show();
        }
    }

    private void initiateChatWithUsers(final List<String> userIds) {
        final String loggedUserId = FirebaseAuth.getInstance().getUid();
        final List<Chat> activeChats = new ArrayList<>();

        usersDbRef.child(loggedUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                user.id = dataSnapshot.getKey();

                if (user.chats.size() == 0) {
                    createAndOpenNewChat(userIds);
                } else {
                    final int[] counter = { user.chats.size() };
                    for (String userChatId : user.chats.keySet()) {
                        chatsDbRef.child(userChatId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Chat chat = dataSnapshot.getValue(Chat.class);
                                chat.id = dataSnapshot.getKey();

                                activeChats.add(chat);
                                counter[0] -= 1;

                                if (counter[0] == 0) {
                                    openExistingOrCreateNewChat(userIds, activeChats);
                                }

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void openExistingOrCreateNewChat(List<String> userIds, List<Chat> chats) {
        List<String> userIdsWithLoggedUser = new ArrayList<>(userIds);
        userIdsWithLoggedUser.add(FirebaseAuth.getInstance().getUid());

        Set userIdsSet = new HashSet<>(userIdsWithLoggedUser);
        for (Chat chat : chats) {
            if (userIdsSet.equals(new HashSet<>(chat.members.keySet()))) {
                openChatWithId(chat.id);
                return;
            }
        }

        createAndOpenNewChat(userIds);
    }

    private void openChatWithId(String chatId) {
        Intent intent = new Intent(UsersActivity.this, ChatActivity.class);
        intent.putExtra("chatId", chatId);
        startActivity(intent);
        finish();
    }

    private void createAndOpenNewChat(final List<String> userIds) {
        userIds.add(FirebaseAuth.getInstance().getUid());

        Chat chat = new Chat();
        chat.lastDate = ServerValue.TIMESTAMP;
        for (String userId : userIds) {
            chat.members.put(userId, true);
        }

        Map<String, Object> updates = new HashMap<>();

        final String newChatKey = chatsDbRef.push().getKey();
        updates.put("chats/" + newChatKey, chat);

        for (String userId : userIds) {
            updates.put("users/" + userId + "/chats/" + newChatKey, true);
        }

        dbRef.updateChildren(updates).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                openChatWithId(newChatKey);
            }
        });
    }
}
