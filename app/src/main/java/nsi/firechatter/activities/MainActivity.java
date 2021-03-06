package nsi.firechatter.activities;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToLongFunction;

import nsi.firechatter.R;
import nsi.firechatter.adapters.ChatsRecyclerViewAdapter;
import nsi.firechatter.models.Chat;
import nsi.firechatter.models.Message;
import nsi.firechatter.models.MessageTypeEnum;
import nsi.firechatter.models.User;

import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingLong;
import static java.util.Comparator.nullsFirst;

public class MainActivity extends AppCompatActivity implements ChatsRecyclerViewAdapter.OnChatInteractionListener {
    @Override
    protected void onResume() {
        super.onResume();
        chatsAdapter.notifyDataSetChanged();
    }

    private RecyclerView chatsRecyclerView;
    private ProgressBar chatsProgressBar;
    private TextView chatsEmptyText;
    private FloatingActionButton newBtn;

    private List<Chat> chats = new ArrayList<>();
    private ChatsRecyclerViewAdapter chatsAdapter;
    String currentUserId;

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

        currentUserId = FirebaseAuth.getInstance().getUid();
        chatsAdapter.setCurrentUserId(currentUserId);

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

        usersDbRef.child(userId).child("chats").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null) {
                    chatsEmptyText.setVisibility(View.VISIBLE);
                    chatsProgressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getChatAndSetupUI(final String chatId) {
        chatsDbRef.child(chatId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                chatsRecyclerView.setVisibility(View.VISIBLE);
                chatsProgressBar.setVisibility(View.GONE);
                chatsEmptyText.setVisibility(View.GONE);

                final Chat chat = dataSnapshot.getValue(Chat.class);
                chat.id = dataSnapshot.getKey();
                final int ind = chatIndexOf(chat.id);

                if (chat.lastMsgId != null) {
                    dbRef.child("messages").child(chat.id).child(chat.lastMsgId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot1) {
                            Message message = dataSnapshot1.getValue(Message.class);
                            chat.lastMsg.content = " sent a photo.";
                            chat.lastMsg.type = message.type;
                            if (chat.lastMsg.type == MessageTypeEnum.TEXT) {
                                chat.lastMsg.content = message.content;
                            }
                            chat.lastMsg.dateTime = message.dateTime;
                            chat.lastMsg.senderId = message.senderId;
                            usersDbRef.child(chat.lastMsg.senderId).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot2) {
                                    chat.lastMsgSenderName = dataSnapshot2.getValue(String.class);

                                    updateChatList(chat,ind);
                                    sort();
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                } else {
                    updateChatList(chat,ind);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private int chatIndexOf(String chatId) {
        for (int i = 0; i < chats.size(); i++) {
            if (chats.get(i).id == chatId) {
                return i;
            }
        }
        return -1;
    }

    private void updateChatList(final Chat chat, final int ind) {
        if (ind == -1) {
            chats.add(chat);
        } else {
            chats.set(ind, chat);
        }

        if (chat.name == null || chat.name.isEmpty()) {
            String otherMemberId = "";
            for (String memberId : chat.members.keySet()) {
                if (!memberId.equals(currentUserId)) {
                    otherMemberId = memberId;
                }
            }

            usersDbRef.child(otherMemberId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    User otherMember = dataSnapshot.getValue(User.class);
                    chat.name = otherMember.name;
                    chat.avatarUrl = otherMember.avatarUrl;

                    startTrackingMessages(chat.id);
                    chatsAdapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        } else {
            chatsAdapter.notifyDataSetChanged();
            startTrackingMessages(chat.id);
        }
    }

    public void sort() {

//        Collections.sort(chats, new Comparator<Chat>() {
//            @Override
//            public int compare(Chat o1, Chat o2) {
//                return (int)(o2.getLastMsgTime() - o1.getLastMsgTime());
//            }
//        });

        Collections.sort(chats, new Comparator<Chat>() {
            @Override
            public int compare(Chat o1, Chat o2) {
                long lastMsgDate1 = o1.lastMsg.dateTime == null ? 0L : (long) o1.lastMsg.dateTime;
                long lastMsgDate2 = o2.lastMsg.dateTime == null ? 0L : (long) o2.lastMsg.dateTime;
                return (int)(lastMsgDate2-lastMsgDate1);
            }
        });
    }

    public void startTrackingMessages(final String chatId)
    {
        final boolean first[] = {true};
        chatsDbRef.child(chatId).child("lastMsgId").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (first[0]) {
                    first[0] = false;
                } else {
                    final String lastMsgId = (String) dataSnapshot.getValue();

                    if (lastMsgId != null) {
                        dbRef.child("messages").child(chatId).child(lastMsgId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot1) {
                                int ind = chatIndexOf(chatId);
                                final Chat chat = chats.get(ind);
                                chats.remove(ind);

                                Message message = dataSnapshot1.getValue(Message.class);

                                chat.lastMsgId = lastMsgId;
                                chat.lastMsg.content = " sent a photo.";
                                chat.lastMsg.type = message.type;
                                if (chat.lastMsg.type == MessageTypeEnum.TEXT) {
                                    chat.lastMsg.content = message.content;
                                }
                                chat.lastMsg.dateTime = message.dateTime;
                                chat.lastMsg.senderId = message.senderId;
                                usersDbRef.child(chat.lastMsg.senderId).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot2) {
                                        chat.lastMsgSenderName = dataSnapshot2.getValue(String.class);
                                        chats.add(0, chat);
                                        chatsAdapter.notifyDataSetChanged();
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });

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

        chatsDbRef.child(chatId).child("members").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if(dataSnapshot.getKey().equals(currentUserId)) {
                    int ind = chatIndexOf(chatId);
                    Chat chat = chats.get(ind);
                    chat.members.put(dataSnapshot.getKey(), dataSnapshot.getValue());
                    chats.set(ind, chat);
                    chatsAdapter.notifyDataSetChanged();
                }
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
