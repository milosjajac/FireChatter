package nsi.firechatter.activities;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nsi.firechatter.R;
import nsi.firechatter.adapters.MessagesRecyclerViewAdapter;
import nsi.firechatter.models.Chat;
import nsi.firechatter.models.Message;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_CHAT_ID = "chatId";

    private ImageView imageBtn;
    private ImageView sendBtn;
    private EditText messageEt;
    private ProgressBar messagesProgressBar;
    private TextView messagesEmptyText;
    private TextView typingIndicatorText;

    private String chatId;

    private FirebaseUser currentUser;
    private DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
    private DatabaseReference usersDbRef = dbRef.child("users");
    private DatabaseReference chatsDbRef = dbRef.child("chats");
    private DatabaseReference messagesDbRef;

    private List<Message> messages = new ArrayList<>();
    private RecyclerView messagesRecyclerView;
    private MessagesRecyclerViewAdapter messagesAdapter;
    private boolean isTyping;
    private Map<String, String> usersTyping = new LinkedHashMap<>();

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
        typingIndicatorText = findViewById(R.id.chat_activity_typing_indicator);

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

        messagesRecyclerView = findViewById(R.id.chat_activity_message_list);
        messagesAdapter = new MessagesRecyclerViewAdapter(this, messages);
        messagesRecyclerView.setAdapter(messagesAdapter);
        messagesRecyclerView.addOnLayoutChangeListener(scrollMesagesOnOpenKeyboard);

        chatId = getIntent().getStringExtra(EXTRA_CHAT_ID);
        messagesDbRef = dbRef.child("messages").child(chatId);

        messageEt.addTextChangedListener(typingIndicator);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        getChatAndSetupUI();
    }

    private void getChatAndSetupUI() {
        final String loggedUserId = FirebaseAuth.getInstance().getUid();

        chatsDbRef.child(chatId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Chat chat = dataSnapshot.getValue(Chat.class);

                if (chat.name != null && !chat.name.isEmpty()) {
                    setTitle(chat.name);
                } else {
                    String otherMemberId = "";
                    for (String memberId : chat.members.keySet()) {
                        if (!memberId.equals(loggedUserId)) {
                            otherMemberId = memberId;
                        }
                    }

                    usersDbRef.child(otherMemberId).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            String otherMemberName = (String) dataSnapshot.getValue();
                            if (otherMemberName != null) {
                                setTitle(otherMemberName);
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
                messagesProgressBar.setVisibility(View.GONE);

                messagesRecyclerView.smoothScrollToPosition(messagesRecyclerView.getAdapter().getItemCount() - 1);
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

        chatsDbRef.child(chatId).child("typing").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (!dataSnapshot.getKey().equals(currentUser.getUid())) {
                    usersTyping.put(dataSnapshot.getKey(), (String) dataSnapshot.getValue());
                    updateTypingIndicatorText();
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.getKey().equals(currentUser.getUid())) {
                    usersTyping.remove(dataSnapshot.getKey());
                    updateTypingIndicatorText();
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void updateTypingIndicatorText() {
        String text = "";

        for (String userId : usersTyping.keySet()) {
            if (text.isEmpty()) {
                text = usersTyping.get(userId);
            } else {
                text = text + ", " + usersTyping.get(userId);
            }
        }

        if (usersTyping.size() == 1) {
            text = text + " is typing...";
        } else if (usersTyping.size() > 1) {
            text = text + " are typing...";
        }

        typingIndicatorText.setText(text);
    }

    private void onImageClick() {

    }

    private void onSendClick() {
        String message = messageEt.getText().toString().trim();
        if(!message.isEmpty()) {
            Map<String, Object> updates = new HashMap<>();

            String image = currentUser.getPhotoUrl() == null ? null : currentUser.getPhotoUrl().toString();
            Message newMessage = new Message(currentUser.getUid(), currentUser.getDisplayName(),
                    image, message, "text");

            newMessage.setId(messagesDbRef.push().getKey());
            newMessage.setDateTime(ServerValue.TIMESTAMP);

            updates.put("messages/"+chatId+"/"+newMessage.getId(), newMessage);
            updates.put("chats/"+chatId+"/lastMsgDate", newMessage.getDateTime());
            updates.put("chats/"+chatId+"/lastMsg", newMessage.getContent());

            dbRef.updateChildren(updates).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    messageEt.setText("");
                }
            });
        }
    }

    private View.OnLayoutChangeListener scrollMesagesOnOpenKeyboard = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View view, int left, int top, int right, int bottom, int newLeft, int newTop, int newRight, int newBottom) {
            if (bottom < newBottom) {
                messagesRecyclerView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        messagesRecyclerView.scrollToPosition(
                                messagesRecyclerView.getAdapter().getItemCount() - 1);
                    }
                }, 0);
            }
        }
    };

    private TextWatcher typingIndicator = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            if (charSequence.length() > 0) {
                if (!isTyping) {
                    setTypingIndicator();
                    isTyping = true;
                }
            } else {
                if (isTyping) {
                    removeTypingIndicator();
                    isTyping = false;
                }
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

    private void setTypingIndicator() {
        chatsDbRef.child(chatId).child("typing").child(currentUser.getUid()).onDisconnect().removeValue();
        chatsDbRef.child(chatId).child("typing").child(currentUser.getUid()).setValue(currentUser.getDisplayName());
    }

    private void removeTypingIndicator() {
        chatsDbRef.child(chatId).child("typing").child(currentUser.getUid()).removeValue();
    }
}
