package nsi.firechatter.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import nsi.firechatter.R;
import nsi.firechatter.activities.ChatActivity;


public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        String chatId = data.get("chatId");
        String chatName = data.get("chatName");
        String senderName = data.get("senderName");
        String content = data.get("content");
        String type = data.get("type");
        String iconUrl = data.get("iconUrl");
        Bitmap image;

        String notificationContent;

        if (!chatName.isEmpty()) {
            if (type.equals("text")) {
                notificationContent = senderName + ": " + content;
            } else {
                notificationContent = senderName + " sent a photo.";
            }
        } else {
            if (type.equals("text")) {
                notificationContent = content;
            } else {
                notificationContent = "Sent a photo.";
            }
        }

        if (!iconUrl.isEmpty()) {
            try {
                URL url = new URL(iconUrl);
                image = BitmapFactory.decodeStream(url.openStream());
            } catch (IOException e) {
                e.printStackTrace();
                image = BitmapFactory.decodeResource(getResources(), R.drawable.ic_group_24dp);
            }
        } else {
            image = BitmapFactory.decodeResource(getResources(), R.drawable.ic_group_24dp);
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(MyFirebaseMessagingService.this, "MY_CHANNEL")
                        .setSmallIcon(R.drawable.firechatter_logo)
                        .setLargeIcon(image)
                        .setContentTitle(!chatName.isEmpty() ? chatName : senderName)
                        .setContentText(notificationContent)
                        .setAutoCancel(true);

        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.setAction(chatId);
        intent.putExtra("chatId", chatId);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                getApplicationContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT);

        builder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(chatId.hashCode(), builder.build());
    }

}
