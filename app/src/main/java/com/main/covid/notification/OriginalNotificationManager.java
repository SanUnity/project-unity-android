package com.main.covid.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;


import com.main.covid.R;
import com.main.covid.views.MainActivity;

import static com.main.covid.utils.UtilsHelper.gotDrawableRessource;


/**
 * Created by Carlos Olmedo on 31/3/17. Modified by Rubén Izquierdo
 */
class OriginalNotificationManager {


    private static OriginalNotificationManager objectInstance;
    private static NotificationCompat.Builder notificationBuilder;
    private static android.app.NotificationManager notificationManager;
//    private static String appSender;

    private OriginalNotificationManager(Context context) {

    }

    static OriginalNotificationManager getInstance(Context context) {



        if (objectInstance == null) {
            objectInstance = new OriginalNotificationManager(context);
        }

        notificationManager = (android.app.NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(new NotificationChannel("default",context.getString(R.string.app_name),android.app.NotificationManager.IMPORTANCE_DEFAULT));
        }

        return objectInstance;
    }

    static Notification createBasicNotification(Context context, String title, String body){

        Intent resultI = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, resultI, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder = new NotificationCompat.Builder(context,"default");
        notificationBuilder.setContentIntent(pendingIntent);
        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle()
                .bigText(body));
        notificationBuilder.setContentTitle(title);
        notificationBuilder.setContentText(body);

        if(gotDrawableRessource(context,"push_notification"))
        {
            notificationBuilder.setSmallIcon(context.getResources().getIdentifier("push_notification","drawable",context.getPackageName()));
        }else{
            notificationBuilder.setSmallIcon(R.drawable.ic_notifications);
        }

        return notificationBuilder.build();

    }


    static Notification createUrlNotification(Context appContext, String title, String body, String data) {
        notificationBuilder = new NotificationCompat.Builder(appContext,"default");

        Intent resultI = new Intent(appContext, MainActivity.class);
        resultI.putExtra("urlNotification", data);
        PendingIntent pendingIntent = PendingIntent.getActivity(appContext, 1, resultI, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(pendingIntent);
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setContentTitle(title);
        notificationBuilder.setContentText(body);
        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle()
                .bigText(body));
        notificationBuilder.setSmallIcon(R.drawable.ic_notifications);

        return notificationBuilder.build();
    }

    void notifyExec(Notification notification, Integer requestCode, Context context) {
        notificationManager.notify(context.getString(R.string.app_name), requestCode, notification);
    }


}

