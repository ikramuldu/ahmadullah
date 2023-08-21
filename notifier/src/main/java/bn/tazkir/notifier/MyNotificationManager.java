package bn.tazkir.notifier;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class MyNotificationManager implements NotificationTask.NotificationListener {
    private static final String PREF_NAME = "notif.store";
    private static final String NOTIFIED_ID = "a";
    private static final String CHANNEL_NAME = "info";
    private final Activity activity;

    public MyNotificationManager(Activity activity) {
        this.activity = activity;
        NotificationManager manager = (android.app.NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_NAME, CHANNEL_NAME, android.app.NotificationManager.IMPORTANCE_LOW);
            mChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            manager.createNotificationChannel(mChannel);
        }
        SharedPreferences store = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        new NotificationTask(new File(activity.getFilesDir(), activity.getPackageName()
        ), store.getInt(NOTIFIED_ID, -1), this).start();
    }


    @Override
    public void notify(int id, JSONObject notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            return;
        }
        Log.d("EEE", "ID" + id);
        SharedPreferences preferences = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        preferences.edit().putInt(NOTIFIED_ID, id).apply();
        android.app.NotificationManager manager = (android.app.NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(activity, CHANNEL_NAME).setSmallIcon(R.drawable.ic_notif).setContentTitle(notification.getString("title")).setContentText(notification.getString("msg"));
            if (notification.has("picture")) {
                builder.setLargeIcon(BitmapFactory.decodeFile(new File(activity.getFilesDir(), "image" + id).getPath()));
            }
            manager.notify(id, builder.build());
        } catch (JSONException e) {
            Log.d("EEE", "error:" + e.getMessage());
        }
    }
}
