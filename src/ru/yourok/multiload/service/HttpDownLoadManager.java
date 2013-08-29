package ru.yourok.multiload.service;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import org.json.JSONObject;
import ru.yourok.multiload.service.utils.FileSaver;
import ru.yourok.multiload.service.utils.ManagerSaver;
import ru.yourok.multiload.service.utils.Range;
import ru.yourok.multiload.service.utils.Settings;

import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: yourok
 * Date: 01.08.12
 * Time: 11:22
 */
public class HttpDownLoadManager
{
    public static final int Connect = 200;
    public static final int Paused = 201;
    public static final int Loading = 202;
    public static final int Error = 203;
    public static final int Complete = 204;
    public static final int CreateFile = 205;
    public static final int Wait = 206;

    private ConnectionsHandlerCallBack loadHandlerCallBack = new ConnectionsHandlerCallBack();
    private Handler connectionsHandler = new Handler(loadHandlerCallBack);
    private HashMap<Integer, HttpDownLoad> connections = new HashMap<Integer, HttpDownLoad>();

    private URL url;
    private FileSaver fileSaver;
    private Handler stateHandler;
    private int ID;
    private int state;
    private int maxConnections;
    private int loadingThreads;
    //private int errorCount;
    private long lastTime;
    private long loadingTime;
    private long downLoadSize;
    private long contentSize;
    private String errorString;
    private String mimeType;
    private boolean isMultiLoad;
    private float speed;
    private ArrayList<Range> ranges;

    public HttpDownLoadManager(URL url, String fileName, Handler handler, int id)
    {
        this.url = url;
        fileSaver = new FileSaver(fileName);
        stateHandler = handler;
        state = Paused;
        errorString = "";
        mimeType = "";
        ID = id;
        speed = 0;
        loadingTime = 0;
        //errorCount = 0;
        isMultiLoad = true;
        ranges = new ArrayList<Range>();
        initParam();
    }

    public void setRanges(ArrayList<Range> ranges, boolean compute)
    {
        synchronized (this.ranges)
        {
            if (compute && state == Loading)
            {
                long timeToLoad = System.currentTimeMillis() - lastTime;
                long lastLoad = getLoadedSize(this.ranges);
                downLoadSize = getLoadedSize(ranges);
                if (timeToLoad > 0 && lastLoad != downLoadSize)
                {
                    if (downLoadSize > contentSize && contentSize != 0)
                        downLoadSize = contentSize;
                    lastTime = System.currentTimeMillis();
                    loadingTime += timeToLoad;
                    speed = (((float) Math.abs(downLoadSize) / loadingTime));
                }

                loadingThreads = 0;
                for (HttpDownLoad loadThread : connections.values())
                {
                    if (loadThread.getLoadState() == HttpDownLoad.Loading)
                        loadingThreads++;
                }
            }
            this.ranges = ranges;
        }
    }

    public ArrayList<Range> getRanges()
    {
        synchronized (ranges)
        {
            return (ArrayList<Range>) ranges.clone();
        }
    }

    public int getID()
    {
        return ID;
    }

    public String getFileName()
    {
        return fileSaver.getFileName();
    }

    public String getUrlString()
    {
        try
        {
            return url.toURI().toString();
        } catch (Exception e)
        {
            e.printStackTrace();
            return url.toString();
        }
    }

    public void setState(int state)
    {
        sendState(state);
    }

    public int getState()
    {
        return state;
    }

    public String getErrorMessage()
    {
        return errorString;
    }

    public String getMimeType()
    {
        return mimeType;
    }

    public long getLoadingTime()
    {
        return loadingTime;
    }

    public int getLoadingThreads()
    {
        return loadingThreads;
    }

    public long getDownLoadSize()
    {
        return downLoadSize;
    }

    public long getContentSize()
    {
        return contentSize;
    }

    public float getSpeed()
    {
        return speed;
    }

    public boolean isMultiLoad()
    {
        return isMultiLoad;
    }

    public void editUrlFileName(String url, String fileName)
    {
        if (getUrlString().equalsIgnoreCase(url) && getFileName().equalsIgnoreCase(fileName))
            return;
        initParam();
        int tmpState = state;
        if (state != Paused)
        {
            Pause();
        }
        try
        {
            if (!getUrlString().equalsIgnoreCase(url))
                this.url = new URL(url);

            if (!getFileName().equalsIgnoreCase(fileName))
            {
                if (fileSaver.copy(fileName) != 0)
                {
                    errorString = fileSaver.getErrorString();
                    sendState(Error);
                    return;
                }
            }
            if (tmpState != Paused)
                Resume();
        } catch (Exception e)
        {
            e.printStackTrace();
            errorString = e.toString();
            sendState(Error);
        }
    }

    public int Start()
    {
        try
        {
            Stop();
            sendState(Connect);
            maxConnections = Settings.getInstance().getMaxConnections();
            ConnectManager connectManager = new ConnectManager();
            connectManager.start();
            connectManager.join();
            if (state == Error)
                return -1;

            if (state != Connect)
                return -1;

            sendState(CreateFile);
            fileSaver.Create(contentSize);
            initParam();

            if (state != CreateFile)
                return -1;
            loadingTime = 0;

            sendState(Loading);
            if (state != Loading)
                return -1;

            ranges.clear();
            ranges.add(new Range(0, 0, contentSize));

            HttpDownLoad downLoad = new HttpDownLoad(url, fileSaver, getBufferSize(), connectionsHandler);
            connections.put((int) downLoad.getId(), downLoad);
            downLoad.Start(0, contentSize);

            while (divLoadThread()) Thread.sleep(100);

        } catch (Exception e)
        {
            e.printStackTrace();
            errorString = e.toString();
            sendState(Error);
            return -1;
        }
        return 0;
    }

    public void Start(JSONObject jsonObject)
    {
        try
        {
            ID = jsonObject.getInt("Id");
            Stop();

            if (jsonObject.getInt("State") != Complete)
            {
                sendState(Connect);
                ConnectManager connectManager = new ConnectManager();
                connectManager.start();
                connectManager.join();
                if (state == Error)
                    return;

                if (!fileSaver.Create(contentSize))
                {
                    sendState(Error);
                    errorString = fileSaver.getErrorString();
                    return;
                }
            }
            if (mimeType.isEmpty())
                mimeType = jsonObject.getString("MIME");
            errorString = jsonObject.getString("ErrorMessage");
            loadingTime = jsonObject.getLong("LoadingTime");
            downLoadSize = jsonObject.getLong("LoadedSize");
            maxConnections = Settings.getInstance().getMaxConnections();

            int threadsCount = jsonObject.getInt("Connections");
            ranges.clear();
            for (int i = 0; i < threadsCount; i++)
            {
                long start = jsonObject.getJSONObject("Range" + String.valueOf(i)).getLong("OffsetStart");
                Long end = jsonObject.getJSONObject("Range" + String.valueOf(i)).getLong("OffsetEnd");
                ranges.add(new Range(start, start, end));
            }

            connections.clear();

            if (jsonObject.getInt("State") == Loading || jsonObject.getInt("State") == Connect || jsonObject.getInt("State") == CreateFile)
            {
                if (ranges.size() == 0)
                    ranges.add(new Range(0, 0, contentSize));

                for (Range range : ranges)
                {
                    HttpDownLoad downLoad = new HttpDownLoad(url, fileSaver, getBufferSize(), connectionsHandler);
                    connections.put((int) downLoad.getId(), downLoad);
                    downLoad.Start(range.offset, range.end);
                }
                sendState(Loading);
            } else
                sendState(jsonObject.getInt("State"));

        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void Pause()
    {
        try
        {
            if (state == Loading || state == CreateFile || state == Connect)
            {
                initParam();
                for (HttpDownLoad downLoad : connections.values())
                    downLoad.Stop();

                ArrayList<Range> newRange = new ArrayList<Range>();
                for (HttpDownLoad loadThread : connections.values())
                    newRange.add(new Range(loadThread.getStartOffset(), loadThread.getCurrentOffset(), loadThread.getEndOffset()));
                setRanges(newRange, false);

                connections.clear();
                sendState(Paused);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void Resume()
    {
        try
        {
            if (state == Paused || state == Error)
            {
                initParam();
                if (ranges.isEmpty() || !isMultiLoad)
                {
                    ranges.clear();
                    ranges.add(new Range(0, 0, getContentSize()));
                }
                connections.clear();
                for (Range range : ranges)
                {
                    HttpDownLoad downLoad = new HttpDownLoad(url, fileSaver, getBufferSize(), connectionsHandler);
                    connections.put((int) downLoad.getId(), downLoad);
                    downLoad.Start(range.offset, range.end);
                }
                sendState(Loading);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void Stop()
    {
        try
        {
            for (HttpDownLoad downLoad : connections.values())
                downLoad.Stop();

            for (HttpDownLoad downLoad : connections.values())
                downLoad.join();

            connections.clear();
            state = Paused;
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void initParam()
    {
        lastTime = System.currentTimeMillis();
    }

    private long getLoadedSize(ArrayList<Range> ranges)
    {
        long loaded = 0;
        for (Range range : ranges)
            loaded += range.end - range.offset;
        return fileSaver.getFileSize() - loaded;
    }

    private long getBufferSize()
    {
        long bufferSize;
        long contentSize = fileSaver.getFileSize();
        if (Settings.getInstance().getAutoSizeBuffer())
        {
            bufferSize = contentSize / maxConnections;
            if (bufferSize < 65535)
                bufferSize = 65535;
            if (bufferSize > 1048576)
                bufferSize = 1048576;

        } else
        {
            bufferSize = Settings.getInstance().getMaxSizeBuffer();
        }
        return bufferSize;
    }

    private void sendState(int state)
    {
        if (state == Loading)
            lastTime = System.currentTimeMillis();
        this.state = state;
        Message msg = stateHandler.obtainMessage(0, state, ID, this);
        stateHandler.sendMessage(msg);
        saveState();
    }

    private boolean startErrThread()
    {
        if (state == Loading)
        {
            for (HttpDownLoad errThread : connections.values())
            {
                if (errThread.getLoadState() == HttpDownLoad.Error)
                {
                    HttpDownLoad downLoad = new HttpDownLoad(url, fileSaver, getBufferSize(), connectionsHandler);
                    connections.put((int) downLoad.getId(), downLoad);
                    downLoad.Start(errThread.getStartOffset(), errThread.getCurrentOffset(), errThread.getEndOffset());
                    connections.remove((int) errThread.getId());
                    return true;
                }
            }
        }
        return false;
    }

    private boolean divLoadThread()
    {
        boolean ret = false;
        HttpDownLoad divThread = null;
        long tmpMaxBuff = 0;

        if (connections.size() < maxConnections && isMultiLoad)
        {

            for (HttpDownLoad loadThread : connections.values())
            {
                long tmpOff = loadThread.getCurrentOffset();
                long tmpEndOff = loadThread.getEndOffset();
                long tmpCurrBuff = tmpEndOff - tmpOff;
                if (tmpCurrBuff < loadThread.getBufferSize() * 2)
                {
                    continue;
                }
                if (tmpCurrBuff > tmpMaxBuff)
                {
                    tmpMaxBuff = tmpCurrBuff;
                    divThread = loadThread;
                }
            }
        }

        if (divThread != null)
        {
            long tmpOff = divThread.getCurrentOffset();
            long tmpEndOff = divThread.getEndOffset();
            long tmpCurrBuff = (tmpEndOff - tmpOff) / 2;
            divThread.setEndOffset(tmpEndOff - tmpCurrBuff);

            HttpDownLoad downLoad = new HttpDownLoad(url, fileSaver, getBufferSize(), connectionsHandler);
            connections.put((int) downLoad.getId(), downLoad);
            downLoad.Start(tmpEndOff - tmpCurrBuff, tmpEndOff);
            ret = true;
        }

        ArrayList<Range> newRange = new ArrayList<Range>();
        for (HttpDownLoad loadThread : connections.values())
        {
            newRange.add(new Range(loadThread.getStartOffset(), loadThread.getCurrentOffset(), loadThread.getEndOffset()));
        }
        setRanges(newRange, true);
        return ret;
    }

    private boolean ifAllState(int state)
    {
        if (connections.isEmpty())
            return false;
        boolean ifstate = true;
        for (HttpDownLoad dl : connections.values())
        {
            if (dl.getLoadState() != state)
            {
                ifstate = false;
                break;
            }
        }
        return ifstate;
    }

    private void saveState()
    {
        try
        {
            String loadStatesDir = Settings.getInstance().getServiceContext().getFilesDir() + "/LoadStates/";
            FileOutputStream outputStream = new FileOutputStream(loadStatesDir + String.valueOf(ID));
            String params = new ManagerSaver().manager2String(this);
            outputStream.write(params.getBytes());
            outputStream.close();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private class ConnectionsHandlerCallBack implements Handler.Callback
    {
        @Override
        public boolean handleMessage(Message message)
        {
            if (message.what == 0)
            {
                if (message.arg2 == HttpDownLoad.Complete)
                {
                    connections.remove(message.arg1);
                    boolean ifErrStart = false;
                    while (startErrThread())
                        ifErrStart = true;
                    if (!ifErrStart)
                        divLoadThread();
                    if (ifAllState(HttpDownLoad.Complete) || connections.isEmpty())
                    {
                        fileSaver.close();
                        sendState(Complete);
                        Vibrator vibrator = (Vibrator) Settings.getInstance().getServiceContext().getSystemService(Context.VIBRATOR_SERVICE);
                        long[] pattern = {50, 100, 50, 100, 50, 100, 50, 100};
                        vibrator.vibrate(pattern, -1);
                    }
                    return false;
                } else if (message.arg2 == HttpDownLoad.Error)
                {
                    //errorCount++;
                    connectionsHandler.sendEmptyMessageDelayed(1, 1000);
                    if (ifAllState(HttpDownLoad.Error))
                    {
                        if (!connections.isEmpty())
                            errorString = connections.get(message.arg1).getErrorString();
                        sendState(Error);
                        Vibrator vibrator = (Vibrator) Settings.getInstance().getServiceContext().getSystemService(Context.VIBRATOR_SERVICE);
                        long[] pattern = {50, 100, 50, 100, 50, 100};
                        vibrator.vibrate(pattern, -1);
                        connectionsHandler.removeMessages(1);
                    }

                } else if (message.arg2 == HttpDownLoad.Loading)
                {
                    divLoadThread();
                    sendState(Loading);
                }
            } else if (message.what == 1)
            {
                startErrThread();
            }
            return false;
        }
    }

    private class ConnectManager extends Thread
    {
        @Override
        public void run()
        {
            try
            {
                if (!Settings.getInstance().wifiCheck())
                {
                    errorString = "Wifi is off";
                    sendState(Error);
                    return;
                }
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(Settings.getInstance().getTimeOut());
                connection.setReadTimeout(Settings.getInstance().getTimeOut());
                connection.connect();

                mimeType = connection.getContentType();
                contentSize = connection.getContentLength();

                if (contentSize == -1)
                {
                    isMultiLoad = false;
                    contentSize = 0;
                } else
                    isMultiLoad = true;

                if (fileSaver.getFileName().isEmpty())
                {
                    String redirectUrl = connection.getURL().toURI().getPath();
                    fileSaver = new FileSaver(Environment.getExternalStorageDirectory().getPath() + "/MultiLoad/" + redirectUrl.substring(redirectUrl.lastIndexOf('/') + 1));
                }

            } catch (Exception e)
            {
                e.printStackTrace();
                errorString = "Error connection to host";
                sendState(Error);
            }
        }
    }

}
