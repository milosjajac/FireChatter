package nsi.firechatter.activities;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import nsi.firechatter.R;
import nsi.firechatter.adapters.MessagesRecyclerViewAdapter;
import nsi.firechatter.models.Message;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_CHAT_ID = "chatId";

    private ImageView imageBtn;
    private ImageView sendBtn;
    private EditText messageEt;
    private ProgressBar messagesProgressBar;
    private TextView messagesEmptyText;
    private ScrollView scrollView;

    private String chatId;

    private FirebaseUser currentUser;
    private DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
    private DatabaseReference usersDbRef = dbRef.child("users");
    private DatabaseReference chatsDbRef = dbRef.child("chats");
    private DatabaseReference messagesDbRef;

    private List<Message> messages = new ArrayList<>();
    private RecyclerView messagesRecyclerView;
    private MessagesRecyclerViewAdapter messagesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Toolbar toolbar = findViewById(R.id.chat_activity_toolbar);
        setSupportActionBar(toolbar);
        setTitle(null);

        imageBtn = findViewById(R.id.chat_activity_image_btn);
        sendBtn = findViewById(R.id.chat_activity_send_btn);
        messageEt = findViewById(R.id.chat_activity_message_text);

        imageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onImageClick();
            }
        });
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSendClick();
            }
        });

        messagesProgressBar = findViewById(R.id.chat_activity_message_progress);
        messagesEmptyText = findViewById(R.id.chat_activity_message_empty_txt);
        scrollView = findViewById(R.id.chat_activity_scroll);

        messagesRecyclerView = findViewById(R.id.chat_activity_message_list);
        messagesAdapter = new MessagesRecyclerViewAdapter(this, messages);
        messagesRecyclerView.setAdapter(messagesAdapter);

        chatId = getIntent().getStringExtra(EXTRA_CHAT_ID);
        messagesDbRef = dbRef.child("messages").child(chatId);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        getChatAndSetupUI();
    }

    private void getChatAndSetupUI() {
        final String loggedUserId = FirebaseAuth.getInstance().getUid();

        chatsDbRef.child(chatId).child("members").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final List<String> otherMemberNames = new ArrayList<>();
                final long[] counter = { dataSnapshot.getChildrenCount() - 1 };

                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    String memberId = childSnapshot.getKey();

                    if (memberId.equals(loggedUserId)) {
                        continue;
                    }

                    usersDbRef.child(memberId).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            otherMemberNames.add((String) dataSnapshot.getValue());
                            counter[0] -= 1;

                            if (counter[0] == 0) {
                                // create chat name
                                String chatName = otherMemberNames.get(0);
                                for (int i = 1; i < otherMemberNames.size(); i++) {
                                    String nextName = otherMemberNames.get(i);

                                    if (nextName.charAt(0) > chatName.charAt(0)) {
                                        chatName = chatName + ", " + nextName;
                                    } else {
                                        chatName = nextName + ", " + chatName;
                                    }
                                }
                                setTitle(chatName);
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

        messagesDbRef.orderByChild("dateTime").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                final Message mMessage = dataSnapshot.getValue(Message.class);
                messages.add(mMessage);

                messagesAdapter.notifyDataSetChanged();
                messagesRecyclerView.setVisibility(View.VISIBLE);
                messagesProgressBar.setVisibility(View.GONE);
//                scrollView.scrollTo(0, scrollView.getHeight());
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

    private void onImageClick() {

    }

    private void onSendClick() {
        String image = currentUser.getPhotoUrl() == null ? null:currentUser.getPhotoUrl().toString();
        Message newMessage = new Message(currentUser.getUid(), currentUser.getDisplayName(),
                image, messageEt.getText().toString(), "text");
        String key = messagesDbRef.push().getKey();
//        newMessage.setId(key);
        newMessage.setDateTime(ServerValue.TIMESTAMP);
        messagesDbRef.child(key).setValue(newMessage);
        messageEt.setText("");
    }
}
