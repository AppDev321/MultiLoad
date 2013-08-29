package ru.yourok.multiload.remote;

import java.io.ByteArrayOutputStream;
import java.net.*;

/**
 * Created with IntelliJ IDEA.
 * User: yourok
 * Date: 17.10.12
 * Time: 16:32
 */
public class Client
{
    private Socket socket;
    private int port;
    private String host;
    private String errStr;
    private boolean proxy = false;
    private boolean connected;

    public Client(String host, int port)
    {
        this.port = port;
        this.host = host;
        connected = false;
    }

    public void setProxy(boolean on)
    {
        proxy = on;
    }

    public void setTimeout(int timeout)
    {
        try
        {
            socket.setSoTimeout(timeout);
        } catch (Exception e)
        {
            e.printStackTrace();
            errStr = e.getMessage();
        }
    }

    public boolean Send(byte[] cmd)
    {
        if (!connected)
            return false;
        try
        {
            socket.getOutputStream().write(cmd);
            socket.getOutputStream().flush();
            return true;
        } catch (Exception e)
        {
            e.printStackTrace();
            errStr = e.getMessage();
            connected = false;
        }
        return false;
    }

    private byte[] readAll() throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] br = new byte[1024];
        while (true)
        {
            int readBytes = socket.getInputStream().read(br);
            if (readBytes <= 0)
                for (int i = 0; i < 10; i++)
                {
                    readBytes = socket.getInputStream().read(br);
                    if (readBytes > 0)
                        break;
                }
            if (readBytes > 0)
                baos.write(br, 0, readBytes);
            else
                break;
            if (socket.getInputStream().available() == 0)
                break;
        }

        return baos.toByteArray();
    }

    public byte[] Receive()
    {
        if (!connected)
        {
            errStr = "not connected";
            return null;
        }
        try
        {
            return readAll();

        } catch (SocketTimeoutException et)
        {
            et.printStackTrace();
            errStr = "connection timeout error";
            connected = false;
        } catch (Exception e)
        {
            e.printStackTrace();
            errStr = e.getMessage();
            connected = false;
        }

        if (errStr == null || errStr.isEmpty())
            errStr = "unknown error";
        return null;
    }

    public boolean Connect()
    {
        try
        {
            socket = null;
            if (proxy)
            {
                SocketAddress addr = new InetSocketAddress(host, port);
                Proxy proxy = new Proxy(Proxy.Type.SOCKS, addr);
                socket = new Socket(proxy);
            } else
                socket = new Socket(host, port);
            connected = true;

        } catch (Exception e)
        {
            e.printStackTrace();
            errStr = e.getMessage();
            connected = false;
            if (socket == null)
                return false;
        }
        return true;
    }

    public String getErrStr()
    {
        return errStr;
    }

    public boolean isConnected()
    {
        return connected;
    }
}

