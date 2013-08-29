package ru.yourok.multiload.service.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: yourok
 * Date: 01.08.12
 * Time: 11:30
 */
public class Settings
{
    private static Settings ourInstance = new Settings();
    private static SharedPreferences.Editor editor;

    public static Settings getInstance()
    {
        return ourInstance;
    }

    private Settings()
    {
    }

    private static SharedPreferences settings = null;
    private Context context = null;

    public void Init(Context context)
    {
        this.context = context;
        settings = context.getSharedPreferences("MultiLoadServiceSettings", Context.MODE_PRIVATE);
        editor = settings.edit();
    }

    public int getMaxLoads()
    {
        return settings.getInt("MaxLoads", 3);
    }

    public int getMaxConnections()
    {
        return settings.getInt("MaxConnections", 5);
    }

    public int getTimeOut()
    {
        return settings.getInt("TimeOut", 15000);//15sec
    }

    public boolean getAutoSizeBuffer()
    {
        return settings.getBoolean("AutoSizeBuffer", true);
    }

    public int getMaxSizeBuffer()
    {
        return settings.getInt("MaxSizeBuffer", 1048576);//1mb
    }

    public boolean showAddActivity()
    {
        return settings.getBoolean("ShowAddActivity", false);
    }

    public Context getServiceContext()
    {
        return context;
    }


    public void setMaxLoads(int value)
    {
        if (value == 0)
            value = 3;
        editor.putInt("MaxLoads", value);
        editor.commit();
    }

    public void setMaxConnections(int value)
    {
        if (value == 0)
            value = 5;
        editor.putInt("MaxConnections", value);
        editor.commit();
    }

    public void setTimeOut(int value)
    {
        if (value == 0)
            value = 15000;
        editor.putInt("TimeOut", value);
        editor.commit();
    }

    public void setAutoSizeBuffer(boolean value)
    {
        editor.putBoolean("AutoSizeBuffer", value);
        editor.commit();
    }

    public void setMaxSizeBuffer(int value)
    {
        if (value == 0)
            value = 1048576;
        editor.putInt("MaxSizeBuffer", value);
        editor.commit();
    }

    public void setShowAddActivity(boolean value)
    {
        editor.putBoolean("ShowAddActivity", value);
        editor.commit();
    }

    public void setLoadOnWifi(boolean value)
    {
        editor.putBoolean("LoadOnWifi", value);
        editor.commit();
    }

    //wifi utils
    public boolean getLoadOnWifi()
    {
        return settings.getBoolean("LoadOnWifi", true);
    }

    public boolean wifiCheck()
    {
        if (!getLoadOnWifi())
            return true;

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null)
            return false;

        NetworkInfo wifiNetwork = cm.getActiveNetworkInfo();
        if (wifiNetwork != null && wifiNetwork.getType() == ConnectivityManager.TYPE_WIFI && wifiNetwork.isConnected())
            return true;

        return false;
    }

    //remote utils
    public String getUserName()
    {
        return settings.getString("UserName", null);
    }

    public String getPassword()
    {
        return settings.getString("Password", null);
    }

    public void setUserName(String value)
    {
        editor.putString("UserName", value);
        editor.commit();
    }

    public void setPassword(String value)
    {
        editor.putString("Password", value);
        editor.commit();
    }

    public String getDeviceId()
    {

        final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        final String tmDevice, tmSerial, androidId;
        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        UUID deviceUuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
        String deviceId = deviceUuid.toString();
        return deviceId;
    }
}
