package nsi.firechatter.activities;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.util.SortedList;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import nsi.firechatter.R;
import nsi.firechatter.adapters.ChatsRecyclerViewAdapter;
import nsi.firechatter.models.Chat;
import nsi.firechatter.models.User;

public class MainActivity extends AppCompatActivity implements ChatsRecyclerViewAdapter.OnChatInteractionListener {

    private RecyclerView chatsRecyclerView;
    private ProgressBar chatsProgressBar;
    private TextView chatsEmptyText;
    private FloatingActionButton newBtn;

    private List<Chat> chats = new ArrayList<>();
    private ChatsRecyclerViewAdapter chatsAdapter;

    private DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
    private DatabaseReference usersDbRef = dbRef.child("users");
    private DatabaseReference chatsDbRef = dbRef.child("chats");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.main_activity_toolbar);
        setSupportActionBar(toolbar);

        chatsRecyclerView = findViewById(R.id.main_activity_chats_list);
        chatsProgressBar = findViewById(R.id.main_activity_chats_progress);
        chatsEmptyText = findViewById(R.id.main_activity_chats_empty_txt);
        newBtn = findViewById(R.id.main_activity_new_btn);

        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        divider.setDrawable(getDrawable(R.drawable.list_divider));
        chatsRecyclerView.addItemDecoration(divider);

        chatsAdapter = new ChatsRecyclerViewAdapter(this, this, chats);
        chatsRecyclerView.setAdapter(chatsAdapter);

        newBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNewClick();
            }
        });

        getUserChatsAndSetupUI();
    }

    private void getUserChatsAndSetupUI() {
        String userId = FirebaseAuth.getInstance().getUid();
        usersDbRef.child(userId).child("chats").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                getChatAndSetupUI(dataSnapshot.getKey());
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getChatAndSetupUI(final String chatId) {
        chatsDbRef.child(chatId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                chatsRecyclerView.setVisibility(View.VISIBLE);
                chatsProgressBar.setVisibility(View.GONE);

                final Chat chat = dataSnapshot.getValue(Chat.class);
                chat.id = dataSnapshot.getKey();

                int ind = chatIndexOf(chat);
                if (ind == -1) {
                    ind = addChatToCorrectPosition(chat);
                } else {
                    chats.set(ind, chat);
                }

                String loggedUserId = FirebaseAuth.getInstance().getUid();
                Set<String> memberIds = chat.members.keySet();
                memberIds.remove(loggedUserId);

                final List<User> otherMembers = new ArrayList<>();
                final int[] counter = { memberIds.size() };

                for (String memberId : memberIds) {
                    usersDbRef.child(memberId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            User user = dataSnapshot.getValue(User.class);
                            otherMembers.add(user);
                            counter[0] -= 1;

                            if (counter[0] == 0) {
                                String oldChatName = chat.name;
                                chat.name = otherMembers.get(0).name;
                                for (int i = 1 ; i < otherMembers.size(); i++) {
                                    String nextName = otherMembers.get(i).name;

                                    if (nextName.charAt(0) > chat.name.charAt(0)) {
                                        chat.name = chat.name + ", " + nextName;
                                    } else {
                                        chat.name = nextName + ", " + chat.name;
                                    }
                                }

                                if (otherMembers.size() == 1) {
                                    chat.avatarUrl = otherMembers.get(0).avatarUrl;
                                }

                                chatsAdapter.notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private int chatIndexOf(Chat chat) {
        for (int i = 0; i < chats.size(); i++) {
            if (chats.get(i).id == chat.id) {
                return i;
            }
        }
        return -1;
    }

    private int addChatToCorrectPosition(Chat chat) {
        int ind = 0;
        while (ind < chats.size() && (long) chat.lastMsgDate < (long) chats.get(ind).lastMsgDate) {
            ind += 1;
        }
        chats.add(ind, chat);
        return ind;
    }

    @Override
    public void onChatClick(Chat chat) {
        Intent intent = new Intent(MainActivity.this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_CHAT_ID, chat.id);
        startActivity(intent);
    }

    private void onNewClick() {
        startActivity(new Intent(MainActivity.this, UsersActivity.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_main_logout:
                onLogout();
                break;
        }
        return true;
    }

    private void onLogout() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }
}
