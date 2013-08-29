package ru.yourok.multiload.service.utils;

import android.os.Handler;
import org.json.JSONObject;
import ru.yourok.multiload.service.HttpDownLoadManager;

import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: yourok
 * Date: 02.08.12
 * Time: 17:09
 */
public class ManagerSaver
{
    public static final String formatVersion = "beta";

    public String manager2String(HttpDownLoadManager manager)
    {
        try
        {
            JSONObject jsonObject = new JSONObject();

            String fn = manager.getFileName();
            if (fn == null)
                fn = "";

            jsonObject.put("Version", formatVersion);
            jsonObject.put("URL", manager.getUrlString());
            jsonObject.put("FileName", fn);
            jsonObject.put("ErrorMessage", manager.getErrorMessage());
            jsonObject.put("LoadingTime", manager.getLoadingTime());
            jsonObject.put("LoadedSize", manager.getDownLoadSize());
            jsonObject.put("State", manager.getState());
            jsonObject.put("Id", manager.getID());
            jsonObject.put("MIME", manager.getMimeType());

            jsonObject.put("Connections", manager.getRanges().size());
            for (int i = 0; i < manager.getRanges().size(); i++)
            {
                Range range = manager.getRanges().get(i);
                JSONObject jsonRange = new JSONObject();
                jsonRange.put("OffsetStart", range.offset);
                jsonRange.put("OffsetEnd", range.end);
                jsonObject.put("Range" + String.valueOf(i), jsonRange);
            }

            return jsonObject.toString();

        } catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public HttpDownLoadManager startManager(String params, Handler handler)
    {
        try
        {
            JSONObject jsonObject = new JSONObject(params);
            URL url = new URL(jsonObject.getString("URL"));
            String fileName = jsonObject.getString("FileName");
            int id = jsonObject.getInt("Id");
            HttpDownLoadManager downLoadManager = new HttpDownLoadManager(url, fileName, handler, id);
            new startUrl(downLoadManager, jsonObject).start();
            return downLoadManager;

        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private class startUrl extends Thread
    {
        private HttpDownLoadManager dl;
        JSONObject jsonObject;

        private startUrl(HttpDownLoadManager dl, JSONObject jsonObject)
        {
            this.dl = dl;
            this.jsonObject = jsonObject;
        }

        @Override
        public void run()
        {
            dl.Start(jsonObject);
        }
    }
}
