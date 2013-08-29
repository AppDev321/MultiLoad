package ru.yourok.multiload.service;

import android.os.Handler;
import android.os.Message;
import ru.yourok.multiload.service.utils.FileSaver;
import ru.yourok.multiload.service.utils.Settings;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created with IntelliJ IDEA.
 * User: yourok
 * Date: 31.07.12
 * Time: 21:41
 */
public class HttpDownLoad extends Thread
{
    public static final int Connect = 100;
    public static final int Loading = 101;
    public static final int Error = 102;
    public static final int Complete = 103;
    public static final int Stoped = 104;

    private long startOffset;
    private long endOffset;
    private long currentOffset;
    private long bufferSize;
    private String errorString;
    private Handler stateHandler;
    private int state;

    private URL url;
    private FileSaver fileSaver;
    private boolean doStop;


    public HttpDownLoad(URL url, FileSaver fileSaver, long bufferSize, Handler handler)
    {
        this.url = url;
        this.fileSaver = fileSaver;
        this.bufferSize = bufferSize;
        doStop = false;
        stateHandler = handler;
        state = -1;
    }

    public int Start(long start, long end, int priority)
    {
        if (doStop)
        {
            errorString = "Already starting...";
            sendState(Error);
            return -1;
        }

        startOffset = start;
        endOffset = end;
        currentOffset = start;
        this.setPriority(priority);
        this.start();
        return 0;
    }

    public int Start(long start, long end)
    {
        return Start(start, end, MIN_PRIORITY);
    }

    public int Start(long start, long current, long end)
    {
        if (doStop)
        {
            errorString = "Already starting...";
            sendState(Error);
            return -1;
        }

        startOffset = start;
        endOffset = end;
        currentOffset = current;
        this.setPriority(MIN_PRIORITY);
        this.start();
        return 0;
    }

    public void Stop()
    {
        doStop = true;
    }

    @Override
    public void run()
    {
        try
        {
            sendState(Connect);
            URLConnection conn = url.openConnection();
            //conn.setUseCaches(false);
            //conn.setDefaultUseCaches(false);
            conn.setConnectTimeout(Settings.getInstance().getTimeOut());
            conn.setReadTimeout(Settings.getInstance().getTimeOut());
            conn.setRequestProperty("Range", "bytes=" + String.valueOf(currentOffset) + "-");
            conn.connect();
            InputStream readStream = conn.getInputStream();
            byte[] buffer = new byte[(int) bufferSize];
            int readBuffer;
            fileSaver.seek((int) getId(), currentOffset);
            while (!doStop && ((readBuffer = readStream.read(buffer)) != -1))
            {
                if ((currentOffset >= endOffset) && endOffset > 0)
                    break;

                if (fileSaver.writeBuffer((int) getId(), buffer, readBuffer) != 0)
                {
                    errorString = fileSaver.getErrorString();
                    sendState(Error);
                    break;
                } else
                {
                    currentOffset += readBuffer;
                    sendState(Loading);

                    //TODO
                    Thread.sleep(50);
                }
            }

            fileSaver.close((int) getId());

            if (currentOffset > endOffset && endOffset > 0)
                currentOffset = endOffset;

            readStream.close();
            if (state != Error)
            {
                if (doStop)
                    sendState(Stoped);
                else
                    sendState(Complete);
            }

            doStop = true;
        } catch (FileNotFoundException e)
        {
            errorString = "Error, file not found: " + e.getMessage();
            sendState(Error);
            e.printStackTrace();

        } catch (Exception e)
        {
            errorString = e.getMessage();
            sendState(Error);
            e.printStackTrace();
        }
    }

    public long getStartOffset()
    {
        return startOffset;
    }

    public long getEndOffset()
    {
        return endOffset;
    }

    public void setEndOffset(long endOffset)
    {
        this.endOffset = endOffset;
    }

    public long getCurrentOffset()
    {
        return currentOffset;
    }

    public long getBufferSize()
    {
        return bufferSize;
    }

    public String getErrorString()
    {
        return errorString;
    }

    public int getLoadState()
    {
        return state;
    }

    private void sendState(int state)
    {
        this.state = state;
        Message msg = stateHandler.obtainMessage(0, (int) getId(), state);
        stateHandler.sendMessage(msg);
    }
}
