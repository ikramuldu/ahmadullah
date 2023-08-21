package bn.tazkir.notifier;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipInputStream;

public class NotificationTask extends Thread {
    static final String NOTIFICATION_URL = "https://pub-1dc4b16f4af04413adec9c555452b89f.r2.dev/app/notification/";
    private final File zipFile;
    private int notified_id;
    private final NotificationListener notificationListener;

    public NotificationTask(File zipFile, int id, NotificationListener notificationListener) {
        this.zipFile = zipFile;
        this.notified_id = id;
        this.notificationListener = notificationListener;
    }

    @Override
    public void run() {
        downloadDatabase();
        try {
            SQLiteDatabase database = SQLiteDatabase.openDatabase(zipFile.getPath() + ".db", null, SQLiteDatabase.OPEN_READONLY);
            Cursor cursor = database.rawQuery("select * from notification where id>? limit 1", new String[]{String.valueOf(notified_id)});
            Log.d("EEE", "C:" + cursor.getCount());
            if (cursor.moveToFirst()) {
                notified_id = cursor.getInt(0);
                JSONObject notification = new JSONObject(cursor.getString(1));
                if (notification.has("picture")) {
                    downloadImage();
                }
                new Handler(Looper.getMainLooper()).post(() -> notificationListener.notify(notified_id, notification));
            }
            cursor.close();
            database.close();
        } catch (Exception e) {
            Log.d("EEE", "error: " + e.getMessage());
        }
    }

    private void downloadImage() {
        File image = new File(zipFile.getParent(), "image" + notified_id);
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(NOTIFICATION_URL + zipFile.getName() + notified_id).openConnection();
            InputStream inputStream = connection.getInputStream();
            FileOutputStream fileOutputStream = new FileOutputStream(image);
            int len;
            byte[] b = new byte[8192];
            while ((len = inputStream.read(b)) != -1) fileOutputStream.write(b, 0, len);
            fileOutputStream.flush();
            fileOutputStream.close();
            inputStream.close();
            connection.disconnect();
        } catch (Exception e) {
            image.delete();
            Log.d("EEE", "error: " + e.getMessage());
        }
    }

    private void downloadDatabase() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(NOTIFICATION_URL + zipFile.getName()).openConnection();
            int status = connection.getResponseCode();
            int len;
            byte[] b = new byte[8192];
            if (status == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                FileOutputStream outputStream = new FileOutputStream(zipFile, false);
                while ((len = inputStream.read(b)) != -1) outputStream.write(b, 0, len);
                outputStream.flush();
                outputStream.close();
                inputStream.close();
            }
            connection.disconnect();
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile));
            zipInputStream.getNextEntry();
            File dbFile = new File(zipFile.getPath() + ".db");
            FileOutputStream outputStream = new FileOutputStream(dbFile);
            while ((len = zipInputStream.read(b)) != -1) outputStream.write(b, 0, len);
            outputStream.flush();
            outputStream.close();
            zipInputStream.close();
            zipFile.delete();
        } catch (Exception e) {
            Log.d("EEE", "ERROR: " + e.getMessage());
        }
    }

    public interface NotificationListener {
        public void notify(int id, JSONObject notification);
    }
}
