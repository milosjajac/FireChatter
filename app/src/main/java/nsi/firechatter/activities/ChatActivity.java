package nsi.firechatter.activities;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
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

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nsi.firechatter.R;
import nsi.firechatter.adapters.MessagesRecyclerViewAdapter;
import nsi.firechatter.models.Chat;
import nsi.firechatter.models.Message;
import nsi.firechatter.models.MessageTypeEnum;
import nsi.firechatter.models.User;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_CHAT_ID = "chatId";
    private static final int RC_STORAGE_PERMISSION = 1;
    private static final int RC_CHOOSE_IMAGE = 2;
    public static final long SECRET_DATE = 766630659564L; //18.04.1994.

    private ImageView imageBtn;
    private ImageView sendBtn;
    private EditText messageEt;
    private ProgressBar messagesProgressBar;
    private TextView messagesEmptyText;
    private TextView typingIndicatorText;
    private TextView seenIndicatorText;

    private String chatId;
    private String selectedImageLocalPath;
    private long lastMessageTime;
    private String lastMessageSenderId;


    private FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
    private final DatabaseReference usersDbRef = dbRef.child("users");
    private DatabaseReference chatsDbRef = dbRef.child("chats");
    private DatabaseReference messagesDbRef;

    private List<Message> messages = new ArrayList<>();
    private RecyclerView messagesRecyclerView;
    private MessagesRecyclerViewAdapter messagesAdapter;
    private boolean isTyping;
    private Map<String, String> usersTyping = new LinkedHashMap<>();
    private Map<String, Long> usersSeen = new LinkedHashMap<>();
    private HashMap<String, User> members = new HashMap<>();
    private int lastScrolledToPosition = 0;

    private BroadcastReceiver newMessageNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            HashMap<String, String> data = (HashMap<String, String>) intent.getSerializableExtra("data");
            if (data.get("chatId").equals(chatId)) {
                abortBroadcast();
            }
        }
    };

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
        seenIndicatorText = findViewById(R.id.chat_activity_seen_indicator);

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
        messagesRecyclerView.addOnChildAttachStateChangeListener(newMessageAttachedListener);

        chatId = getIntent().getStringExtra(EXTRA_CHAT_ID);
        messagesDbRef = dbRef.child("messages").child(chatId);
        messageEt.requestFocus();
        messageEt.addTextChangedListener(typingIndicator);

        getChatAndSetupUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter("dofijghdoflkghdflk");
        intentFilter.setPriority(100);
        registerReceiver(newMessageNotificationReceiver, intentFilter);

        chatsDbRef.child(chatId).child("members").child(currentUser.getUid()).setValue(SECRET_DATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(newMessageNotificationReceiver);

        chatsDbRef.child(chatId).child("members").child(currentUser.getUid()).setValue(ServerValue.TIMESTAMP);
    }

    private void getChatAndSetupUI() {
        final String loggedUserId = FirebaseAuth.getInstance().getUid();

        chatsDbRef.child(chatId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final Chat chat = dataSnapshot.getValue(Chat.class);

                if (chat.name != null && !chat.name.isEmpty()) {
                    setTitle(chat.name);
                }

                if(chat.lastMsgId!=null) {
                    messagesDbRef.child(chat.lastMsgId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot1) {
                            Message message = dataSnapshot1.getValue(Message.class);
                            lastMessageSenderId = message.senderId;
                            lastMessageTime = (long) message.dateTime;
                            //updateSeenIndicatorText();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }

                final int[] counter = {chat.members.size() - 1};
                for (final String memberId : chat.members.keySet()) {
                    if (memberId.equals(loggedUserId)) {
                        continue;
                    }

                    usersDbRef.child(memberId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot1) {
                            User member = dataSnapshot1.getValue(User.class);
                            members.put(memberId, member);

                            counter[0] -= 1;
                            if (counter[0] == 0) {
                                if (members.size() == 1) {
                                    setTitle(members.entrySet().iterator().next().getValue().name);
                                }

                                startTrackingMessages();
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

    private void startTrackingMessages() {
        messagesAdapter.setMembers(members);

        messagesDbRef.orderByChild("dateTime").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                final Message mMessage = dataSnapshot.getValue(Message.class);
                messages.add(mMessage);

                messagesAdapter.notifyDataSetChanged();
                messagesProgressBar.setVisibility(View.GONE);
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

        chatsDbRef.child(chatId).child("members").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                String currentUserId = currentUser.getUid();
                if (!dataSnapshot.getKey().equals(currentUserId)) {
                    usersSeen.put(dataSnapshot.getKey(), (long) dataSnapshot.getValue());
                    updateSeenIndicatorText();
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if (!dataSnapshot.getKey().equals(currentUser.getUid())) {
                    usersSeen.put(dataSnapshot.getKey(), (long) dataSnapshot.getValue());
                    updateSeenIndicatorText();
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

    private void updateSeenIndicatorText() {
        String text = "";

        if(lastMessageSenderId != null) {
            int size = usersSeen.size();
            if (size == 1) {
                if (lastMessageSenderId.equals(currentUser.getUid())) {
                    for (String userId : usersSeen.keySet()) {
                        long lastUserActivity = usersSeen.get(userId);
                        if (lastUserActivity == SECRET_DATE || lastUserActivity > lastMessageTime) {
//                            DateFormat df = new SimpleDateFormat("HH:mm");
//                            if (lastUserActivity == SECRET_DATE) {
//                                lastUserActivity = lastMessageTime;
//                            }
//                            text = "Seen" + df.format(lastUserActivity);
                            text = "Seen";
                        }
                    }
                }
            } else if (size > 1) {
                    for (String userId : usersSeen.keySet()) {
                        long lastUserActivity = usersSeen.get(userId);
                        if (lastUserActivity == SECRET_DATE || lastUserActivity > lastMessageTime) {
                            if (text.isEmpty()) {
                                text = members.get(userId).name;
                            } else {
                                text = text + ", " + members.get(userId).name;
                            }
                        }
                    }
                    if(!text.isEmpty()){
                        int count = text.length() - text.replace(",", "").length();
                        if (count==size)
                        {
                            text = "everyone";
                        }
                        text = "Seen by " + text;
                    }
                }
            }

        seenIndicatorText.setText(text);
    }


    private void onImageClick() {
        checkReadStoragePermission();
    }

    private void checkReadStoragePermission() {
        final String storagePermission = Manifest.permission.READ_EXTERNAL_STORAGE;
        int userPermission = ContextCompat.checkSelfPermission(ChatActivity.this, storagePermission);
        boolean permissionGranted = userPermission == PackageManager.PERMISSION_GRANTED;

        if (!permissionGranted) {
            ActivityCompat.requestPermissions(ChatActivity.this, new String[]{storagePermission},
                    RC_STORAGE_PERMISSION);
        } else {
            onStoragePermissionGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == RC_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onStoragePermissionGranted();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void onStoragePermissionGranted() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");

        startActivityForResult(galleryIntent, RC_CHOOSE_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_CHOOSE_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                messagesProgressBar.setVisibility(View.VISIBLE);

                final Message newMessage = new Message(currentUser.getUid(), "", MessageTypeEnum.IMAGE);
                newMessage.id = messagesDbRef.push().getKey();
                newMessage.dateTime = ServerValue.TIMESTAMP;

                StorageReference imagesStorageRef = FirebaseStorage.getInstance().getReference().child("images").child(chatId);
                String imageFileName = newMessage.id + ".jpg";
                selectedImageLocalPath = getRealPathFromURI(data.getData());

                imagesStorageRef.child(imageFileName).putFile(Uri.fromFile(new File(selectedImageLocalPath)))
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                messagesProgressBar.setVisibility(View.GONE);

                                final Uri imageUrl = taskSnapshot.getDownloadUrl();
                                newMessage.content = imageUrl.toString();

                                Map<String, Object> updates = new HashMap<>();

                                updates.put("messages/"+chatId+"/"+newMessage.id, newMessage);
                                updates.put("chats/"+chatId+"/lastMsgId", newMessage.id);

                                dbRef.updateChildren(updates).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        messageEt.setText("");
                                    }
                                });
                            }
                        });
            }
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = this.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void onSendClick() {
        String message = messageEt.getText().toString().trim();
        if(!message.isEmpty()) {
            Map<String, Object> updates = new HashMap<>();

            Message newMessage = new Message(currentUser.getUid(), message, MessageTypeEnum.TEXT);

            newMessage.id = messagesDbRef.push().getKey();
            newMessage.dateTime = ServerValue.TIMESTAMP;

            updates.put("messages/"+chatId+"/"+newMessage.id, newMessage);
            updates.put("chats/"+chatId+"/lastMsgId", newMessage.id);

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

    private RecyclerView.OnChildAttachStateChangeListener newMessageAttachedListener = new RecyclerView.OnChildAttachStateChangeListener() {
        @Override
        public void onChildViewAttachedToWindow(View view) {
            if (messagesRecyclerView.getAdapter().getItemCount() > lastScrolledToPosition + 1) {
                messagesRecyclerView.smoothScrollToPosition(messagesRecyclerView.getAdapter().getItemCount() - 1);
                lastScrolledToPosition = messagesRecyclerView.getAdapter().getItemCount() - 1;
            }
        }

        @Override
        public void onChildViewDetachedFromWindow(View view) {

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
