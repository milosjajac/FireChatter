package nsi.firechatter.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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

    private ImageView imageBtn;
    private ImageView sendBtn;
    private EditText messageEt;
    private ProgressBar messagesProgressBar;
    private TextView messagesEmptyText;
    private TextView typingIndicatorText;

    private String chatId;
    private String selectedImageLocalPath;


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
                String imageFileName = newMessage.getId() + ".jpg";
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
                                updates.put("chats/"+chatId+"/lastMsgDate", newMessage.dateTime);
                                updates.put("chats/"+chatId+"/lastMsg", "Sent photo.");

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
            updates.put("chats/"+chatId+"/lastMsgDate", newMessage.dateTime);
            updates.put("chats/"+chatId+"/lastMsg", newMessage.content);

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
