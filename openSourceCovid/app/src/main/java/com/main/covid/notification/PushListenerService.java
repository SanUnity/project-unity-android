package com.main.covid.notification;

import android.app.Notification;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.main.covid.App;
import com.main.covid.BuildConfig;
import com.main.covid.network.DataUploader;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class PushListenerService extends FirebaseMessagingService {
    public static final String TAG = PushListenerService.class.getSimpleName();

    // Intent keys
    public static final String INTENT_SNS_NOTIFICATION_FROM = "from";
    public static final String INTENT_SNS_NOTIFICATION_DATA = "data";


    OriginalNotificationManager notificationManager;
    private static int counter = 0;

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        DataUploader.INSTANCE.uploadDeviceToken(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        super.onMessageReceived(remoteMessage);

        if (BuildConfig.FLAVOR == "cam") {
            createNotificationMadrid(remoteMessage);
        } else if (remoteMessage.getNotification() != null) {
            createNotification(remoteMessage);
        }

    }


    private void createNotificationMadrid(RemoteMessage remoteMessage) {
        Intent intent = remoteMessage.toIntent();
        Notification notification;
        notificationManager = OriginalNotificationManager.getInstance(App.AppContext);


        String title = intent.getExtras().getString("title");
        String message = intent.getExtras().getString("message");

        try {
            JSONObject json =
                    new JSONObject(intent.getExtras().getString("infoapp"));

            notification = OriginalNotificationManager.createUrlNotification(App.AppContext,
                    title,
                    message,
                    json.getString("element"));

        } catch (JSONException ex) {
            notification = OriginalNotificationManager.createBasicNotification(App.AppContext,
                    title, message);
        }

        notificationManager.notifyExec(notification, counter++, App.AppContext);

    }

    private void createNotification(RemoteMessage remoteMessage) {
        Intent intent = remoteMessage.toIntent();
        notificationManager = OriginalNotificationManager.getInstance(App.AppContext);

        Notification notification;

        String type = intent.getStringExtra("gcm.notification.type");

        if (type.equals("url")) {
            notification = OriginalNotificationManager.createUrlNotification(App.AppContext,
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody(),
                    Objects.requireNonNull(intent.getStringExtra("gcm.notification.element")));
        } else {
            notification = OriginalNotificationManager.createBasicNotification(App.AppContext,
                    remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody());
        }

        notificationManager.notifyExec(notification, counter++, App.AppContext);
    }

}