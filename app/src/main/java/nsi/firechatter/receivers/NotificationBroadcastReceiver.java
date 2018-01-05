package nsi.firechatter.receivers;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.HashMap;

import nsi.firechatter.R;
import nsi.firechatter.activities.ChatActivity;
import nsi.firechatter.models.MessageTypeEnum;


public class NotificationBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        HashMap<String, String> data = (HashMap<String, String>) intent.getSerializableExtra("data");
        new FetchImageAndSendNotificationTask(context, data).execute();
    }

    public class FetchImageAndSendNotificationTask extends AsyncTask<Void, Void, Void> {

        private final WeakReference<Context> weakContext;
        private HashMap<String, String> data;

        public FetchImageAndSendNotificationTask(Context context, HashMap<String, String> data) {
            this.weakContext = new WeakReference<>(context);
            this.data = data;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            String chatId = data.get("chatId");
            String chatName = data.get("chatName");
            String senderName = data.get("senderName");
            String content = data.get("content");
            MessageTypeEnum type = MessageTypeEnum.valueOf(data.get("type"));
            String iconUrl = data.get("iconUrl");
            Bitmap image;

            String notificationContent = "";

            if (!chatName.isEmpty()) {
                if (type == MessageTypeEnum.TEXT) {
                    notificationContent = senderName + ": " + content;
                } else if (type == MessageTypeEnum.IMAGE) {
                    notificationContent = senderName + " sent a photo.";
                }
            } else {
                if (type == MessageTypeEnum.TEXT) {
                    notificationContent = content;
                } else if (type == MessageTypeEnum.IMAGE) {
                    notificationContent = "Sent a photo.";
                }
            }

            if (chatName.isEmpty() && !iconUrl.isEmpty()) {
                try {
                    URL url = new URL(iconUrl);
                    image = BitmapFactory.decodeStream(url.openStream());
                } catch (IOException e) {
                    e.printStackTrace();
                    image = BitmapFactory.decodeResource(weakContext.get().getResources(), R.drawable.ic_group_24dp);
                }
            } else {
                image = BitmapFactory.decodeResource(weakContext.get().getResources(), R.drawable.ic_group_24dp);
            }

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(weakContext.get(), "MY_CHANNEL")
                            .setSmallIcon(R.drawable.firechatter_logo)
                            .setLargeIcon(image)
                            .setContentTitle(!chatName.isEmpty() ? chatName : senderName)
                            .setContentText(notificationContent)
                            .setAutoCancel(true);

            Intent notifIntent = new Intent(weakContext.get().getApplicationContext(), ChatActivity.class);
            notifIntent.setAction(chatId);
            notifIntent.putExtra("chatId", chatId);
            PendingIntent resultPendingIntent = PendingIntent.getActivity(
                    weakContext.get().getApplicationContext(), 0, notifIntent, PendingIntent.FLAG_ONE_SHOT);

            builder.setContentIntent(resultPendingIntent);

            NotificationManager notificationManager = (NotificationManager) weakContext.get().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(chatId.hashCode(), builder.build());
            return null;
        }
    }
}
