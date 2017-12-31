package nsi.firechatter.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import nsi.firechatter.R;
import nsi.firechatter.adapters.UsersRecyclerViewAdapter;
import nsi.firechatter.models.User;

public class UsersActivity extends AppCompatActivity implements UsersRecyclerViewAdapter.OnUsersInteractionListener {

    private RecyclerView usersRecyclerView;
    private ProgressBar usersProgressBar;
    private TextView usersEmptyText;

    private List<User> users = new ArrayList<>();
    private UsersRecyclerViewAdapter usersAdapter;

    private DatabaseReference usersDbRef = FirebaseDatabase.getInstance().getReference().child("users");

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
}
