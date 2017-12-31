package nsi.firechatter.activities;

import android.content.Intent;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import nsi.firechatter.R;
import nsi.firechatter.adapters.ChatsRecyclerViewAdapter;
import nsi.firechatter.models.Chat;

public class MainActivity extends AppCompatActivity implements ChatsRecyclerViewAdapter.OnChatInteractionListener {

    private RecyclerView chatsRecyclerView;
    private ProgressBar chatsProgressBar;
    private TextView chatsEmptyText;
    private FloatingActionButton newBtn;

    private List<Chat> chats = new ArrayList<>();
    private ChatsRecyclerViewAdapter chatsAdapter;

    private DatabaseReference usersDbRef = FirebaseDatabase.getInstance().getReference().child("users");
    private DatabaseReference chatsDbRef = FirebaseDatabase.getInstance().getReference().child("chats");

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
        usersDbRef.child(userId).child("chats").orderByChild("lastDate")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getValue() == null) {
                            chatsProgressBar.setVisibility(View.GONE);
                            chatsEmptyText.setVisibility(View.VISIBLE);
                        } else {
                            for (DataSnapshot chatSnapshot : dataSnapshot.getChildren()) {
                                Chat newChat = chatSnapshot.getValue(Chat.class);
                                newChat.id = chatSnapshot.getKey();
                                chats.add(0, newChat);
                            }

                            chatsAdapter.notifyDataSetChanged();

                            chatsRecyclerView.setVisibility(View.VISIBLE);
                            chatsProgressBar.setVisibility(View.GONE);
                        }


                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    @Override
    public void onChatClick(Chat chat) {

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
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
                break;
        }
        return true;
    }
}
