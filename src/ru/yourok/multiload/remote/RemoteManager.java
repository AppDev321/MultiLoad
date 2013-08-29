package ru.yourok.multiload.remote;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: yourok
 * Date: 24.10.12
 * Time: 14:16
 */
public class RemoteManager
{
    public final static int protocolVersion = 1;

    private Client client;
    private String errString;
    private String userName;
    private String password;

    public RemoteManager()
    {
        errString = "";
    }

    public String getErrString()
    {
        return errString;
    }

    public boolean Connect()
    {
        client = new Client("10.0.0.130", 21285);
        if (!client.Connect())
        {
            errString = client.getErrStr();
            return false;
        }
        return true;
    }

    public boolean isConnected()
    {
        if (client == null || !client.isConnected())
            return false;
        return true;
    }

    public void setNamePass(String userName, String password)
    {
        this.userName = userName;
        this.password = password;
    }

    private JSONObject transmitCmd(byte[] sendBytes)
    {
        if (client == null || !client.isConnected())
        {
            errString = "not connected";
            return null;
        }

        if (!client.Send(sendBytes))
        {
            errString = client.getErrStr();
            return null;
        }
        byte[] recv = client.Receive();
        if (recv == null)
        {
            errString = client.getErrStr();
            return null;
        }

        JSONObject jsonObject;
        try
        {
            jsonObject = new JSONObject(new String(recv));
            try
            {
                errString = jsonObject.getString("Error");
            } catch (Exception e)
            {
                errString = "";
            }

            if (errString == null || !errString.isEmpty())
                return null;
        } catch (Exception e)
        {
            errString = "wrong message received";
            return null;
        }

        return jsonObject;
    }

    private JSONObject getHeader(String command)
    {
        JSONObject jsonObject;
        try
        {
            jsonObject = new JSONObject();
            jsonObject.put("ProtocolVersion", protocolVersion);
            jsonObject.put("UserName", userName);
            jsonObject.put("Password", password);
            if (command != null)
                jsonObject.put("Command", command);
            return jsonObject;

        } catch (Exception e)
        {
            e.printStackTrace();
            errString = "Error connect to server";
            return null;
        }
    }

    public boolean registerUser()
    {
        JSONObject jsonObject;
        jsonObject = getHeader("Register");
        jsonObject = transmitCmd(jsonObject.toString().getBytes());
        if (jsonObject == null)
            return false;
        else
            return true;
    }

    public ArrayList<UrlPath> getUrls()
    {
        JSONObject jsonObject;
        jsonObject = getHeader("GetUrls");
        jsonObject = transmitCmd(jsonObject.toString().getBytes());
        if (jsonObject == null)
            return null;
        ArrayList<UrlPath> urls = new ArrayList<UrlPath>();
        Iterator i = jsonObject.keys();
        while (i.hasNext())
        {
            try
            {
                String key = i.next().toString();
                String val = jsonObject.getString(key);
                UrlPath urlPath = new UrlPath(key, val);
                urls.add(urlPath);
            } catch (JSONException e)
            {
                e.printStackTrace();
            }
        }
        return urls;
    }
}
