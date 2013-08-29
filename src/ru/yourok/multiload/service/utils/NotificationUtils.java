package ru.yourok.multiload.service.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;
import ru.yourok.multiload.MultiLoadActivity;
import ru.yourok.multiload.R;
import ru.yourok.multiload.service.HttpDownLoadManager;

import java.io.File;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: yourok
 * Date: 09.08.12
 * Time: 11:35
 */
public class NotificationUtils
{
    private static final String TAG = NotificationUtils.class.getSimpleName();
    private NotificationManager manager;
    private Context context;
    private HashMap<Integer, Notification> notifications;

    public NotificationUtils(Context context)
    {
        this.context = context;
        manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notifications = new HashMap<Integer, Notification>();
    }

    public void createNotification(int id)
    {
        Intent activityIntent = new Intent(context, MultiLoadActivity.class);

        RemoteViews mRemoteView = new RemoteViews(context.getPackageName(), R.layout.notification_download_layout);
        mRemoteView.setTextViewText(R.id.notification_download_layout_title, "Starting download");
        mRemoteView.setProgressBar(R.id.notification_download_layout_progressbar, 100, 0, false);

        Notification notification;
        notification = new Notification(android.R.drawable.stat_sys_download, "Starting download", System.currentTimeMillis());
        notification.contentIntent = PendingIntent.getActivity(context, 0, activityIntent, 0);
        notification.contentView = mRemoteView;
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        notifications.put(id, notification);
        manager.notify(id, notification);
    }

    private Notification getDownloadError(String errMsg)
    {
        Intent notificationIntent = new Intent(context, MultiLoadActivity.class);
        Notification notification;
        notification = new Notification(android.R.drawable.stat_sys_warning, "Error downloading file", System.currentTimeMillis());
        notification.setLatestEventInfo(context, "Error downloading file", errMsg, PendingIntent.getActivity(context, 0, notificationIntent, 0));
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        return notification;
    }

    private Notification getDownloadComplete(String completeStr, HttpDownLoadManager manager)
    {

        String mime = manager.getMimeType();
        if (mime == "")
            mime = "*/*";

        Uri uri = Uri.fromFile(new File(manager.getFileName()));

        Intent notificationIntent = new Intent(Intent.ACTION_VIEW);
        notificationIntent.setDataAndType(uri, mime);

        Notification notification;
        String strComplete = context.getString(R.string.label_downloadcomplete);
        notification = new Notification(android.R.drawable.stat_sys_download_done, strComplete, System.currentTimeMillis());
        notification.setLatestEventInfo(context, strComplete, completeStr, PendingIntent.getActivity(context, 0, notificationIntent, 0));
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        return notification;
    }

    public void updateNotification(HttpDownLoadManager manager)
    {
        Notification notification = notifications.get(manager.getID());

        if (notification != null)
        {
            String msg = "";
            if (manager.getState() == HttpDownLoadManager.Error)
            {
                notification = getDownloadError(manager.getErrorMessage());
                this.manager.notify(manager.getID(), notification);
                return;

            } else if (manager.getState() == HttpDownLoadManager.Complete)
            {
                String completeStr = context.getString(R.string.label_downloadcomplete) + " : " + manager.getFileName();
                notification = getDownloadComplete(completeStr, manager);
                this.manager.notify(manager.getID(), notification);
                return;

            } else if (manager.getState() == HttpDownLoadManager.Connect)
            {
                msg = context.getString(R.string.label_connecting);
            } else if (manager.getState() == HttpDownLoadManager.CreateFile)
            {
                msg = context.getString(R.string.label_openfile) + new File(manager.getFileName()).getName();
            } else if (manager.getState() == HttpDownLoadManager.Paused)
            {
                msg = context.getString(R.string.label_downloadpaused);
            } else if (manager.getState() == HttpDownLoadManager.Wait)
            {
                msg = context.getString(R.string.label_downloadwait);
            } else if (manager.getState() == HttpDownLoadManager.Loading)
            {
                msg = context.getString(R.string.label_loading) + ": " + new File(manager.getFileName()).getName();
            }

            notification.contentView.setTextViewText(R.id.notification_download_layout_title, msg);
            long loadedSize = manager.getDownLoadSize();
            long allSize = manager.getContentSize();
            long percent = 0;
            if (allSize > 0)
                percent = loadedSize * 100 / allSize;
            notification.contentView.setProgressBar(R.id.notification_download_layout_progressbar, 100, (int) percent, false);
            this.manager.notify(manager.getID(), notification);
        }
    }

    public void removeNotification(int id)
    {
        manager.cancel(id);
        notifications.remove(id);
    }

    public void clearNotifications()
    {
        manager.cancelAll();
        notifications.clear();
    }
}
