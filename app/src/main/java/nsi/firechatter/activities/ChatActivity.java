package nsi.firechatter.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import nsi.firechatter.R;
import nsi.firechatter.models.User;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_CHAT_ID = "chatId";

    private ImageView imageBtn;
    private ImageView sendBtn;
    private EditText messageEt;

    private DatabaseReference usersDbRef = FirebaseDatabase.getInstance().getReference().child("users");
    private DatabaseReference chatsDbRef = FirebaseDatabase.getInstance().getReference().child("chats");
    private DatabaseReference messagesDbRef = FirebaseDatabase.getInstance().getReference().child("messages");

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

        getChatAndSetupUI();
    }

    private void getChatAndSetupUI() {
        final String loggedUserId = FirebaseAuth.getInstance().getUid();
        String chatId = getIntent().getStringExtra(EXTRA_CHAT_ID);

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
    }

    private void onImageClick() {

    }

    private void onSendClick() {

    }
}
