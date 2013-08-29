package ru.yourok.multiload.service;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;
import ru.yourok.multiload.service.utils.ManagerSaver;
import ru.yourok.multiload.service.utils.NotificationUtils;
import ru.yourok.multiload.service.utils.Settings;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: yourok
 * Date: 25.07.12
 * Time: 21:24
 */
public class MultiLoadManager
{
    Context context;
    Handler statusHandler;
    NotificationUtils notificationUtils;
    HashMap<Integer, HttpDownLoadManager> downLoadManagers = new HashMap<Integer, HttpDownLoadManager>();
    String loadStatesDir;

    public MultiLoadManager(Context context)
    {
        this.context = context;
        notificationUtils = new NotificationUtils(context);
        statusHandler = new Handler(new CheckStatus());
        loadStatesDir = context.getFilesDir() + "/LoadStates/";
        if (!new File(loadStatesDir).exists())
            new File(loadStatesDir).mkdir();
        loadStates();
    }

    public int addHttpUrl(String urlStr, String fileName)
    {
        try
        {
            for (HttpDownLoadManager downLoadManager : downLoadManagers.values())
            {
                if (downLoadManager.getFileName().equalsIgnoreCase(fileName))
                {
                    return -2;
                }
            }
            int id = getEmptyID();
            URL url = new URL(urlStr);
            new addUrl(url, fileName, id).start();

            notificationUtils.createNotification(id);

            return id;
        } catch (Exception e)
        {
            e.printStackTrace();
            Toast.makeText(context, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            return -1;
        }
    }

    public ArrayList<HttpDownLoadManager> getAllLoadThread()
    {
        return new ArrayList<HttpDownLoadManager>(downLoadManagers.values());
    }

    public void deleteLoadThread(int id)
    {
        downLoadManagers.remove(id);
        new File(loadStatesDir + String.valueOf(id)).delete();
        notificationUtils.removeNotification(id);
    }

    public void clearLoadThreads()
    {
        downLoadManagers.clear();
        for (File file : new File(loadStatesDir).listFiles())
            file.delete();
        notificationUtils.clearNotifications();
    }

    private int getEmptyID()
    {
        int id = 0;
        for (HttpDownLoadManager provider : downLoadManagers.values())
        {
            if (provider.getID() == id)
                id++;
        }
        return id;
    }

    private void loadStates()
    {
        try
        {
            for (String fileName : new File(loadStatesDir).list())
            {
                FileInputStream inputStream = new FileInputStream(loadStatesDir + fileName);
                byte buf[] = new byte[1024];
                String json = "";
                while ((inputStream.read(buf)) != -1)
                    json += new String(buf);
                inputStream.close();
                if (json.isEmpty())
                {
                    new File(fileName).delete();
                    continue;
                }
                HttpDownLoadManager manager = new ManagerSaver().startManager(json, statusHandler);
                downLoadManagers.put(manager.getID(), manager);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private int getLoadingThread()
    {
        int loadingThreads = 0;
        for (HttpDownLoadManager dl : downLoadManagers.values())
        {
            if (dl.getState() == HttpDownLoadManager.Connect || dl.getState() == HttpDownLoadManager.CreateFile || dl.getState() == HttpDownLoadManager.Loading)
                loadingThreads++;
        }
        return loadingThreads;
    }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private class addUrl extends Thread
    {
        HttpDownLoadManager download;
        URL url;
        int id;

        addUrl(URL url, String fileName, int id)
        {
            this.url = url;
            this.id = id;
            download = new HttpDownLoadManager(url, fileName, statusHandler, id);
            downLoadManagers.put(id, download);
            this.setPriority(MIN_PRIORITY);
        }

        @Override
        public void run()
        {
            if (getLoadingThread() >= Settings.getInstance().getMaxLoads())
                download.setState(HttpDownLoadManager.Wait);
            else
                download.Start();
        }
    }

    private class startUrl extends Thread
    {
        private HttpDownLoadManager dl;

        private startUrl(HttpDownLoadManager dl)
        {
            this.dl = dl;
            this.setPriority(MIN_PRIORITY);
        }

        @Override
        public void run()
        {
            dl.Start();
        }
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private class CheckStatus implements Handler.Callback
    {
        @Override
        public boolean handleMessage(Message message)
        {
            notificationUtils.updateNotification((HttpDownLoadManager) message.obj);
            if (getLoadingThread() < Settings.getInstance().getMaxLoads())
            {
                for (HttpDownLoadManager dl : downLoadManagers.values())
                {
                    if (dl.getState() == HttpDownLoadManager.Wait)
                    {
                        new startUrl(dl).start();
                        break;
                    }
                }
            }
            return false;
        }
    }
}
