package nsi.firechatter.services;

import android.content.Intent;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;

import nsi.firechatter.activities.ChatActivity;


public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        HashMap<String, String> data = new HashMap<>(remoteMessage.getData());

        Intent intent = new Intent("dofijghdoflkghdflk");
        intent.putExtra("data", data);
        sendOrderedBroadcast(intent, null);
    }

}
