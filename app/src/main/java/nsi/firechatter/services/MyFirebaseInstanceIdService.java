package nsi.firechatter.services;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;


public class MyFirebaseInstanceIdService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        if (refreshedToken != null) {
            saveNewToken(refreshedToken);
        }
    }

    private void saveNewToken(String token) {
        DatabaseReference usersDbRef = FirebaseDatabase.getInstance().getReference().child("users");
        String currentUserId = FirebaseAuth.getInstance().getUid();

        if (currentUserId != null) {
            usersDbRef.child(currentUserId).child("deviceToken").setValue(token);
        }
    }
}
