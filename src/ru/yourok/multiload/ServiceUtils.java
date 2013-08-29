package ru.yourok.multiload;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import ru.yourok.multiload.service.MultiLoadService;

/**
 * Created with IntelliJ IDEA.
 * User: yourok
 * Date: 07.08.12
 * Time: 9:54
 */
public class ServiceUtils
{
    private MLServiceConnection sConn = null;
    private boolean bound = false;
    private MultiLoadService.MultiLoadServiceBinder multiLoadServiceBinder = null;
    private Context context;
    private Handler connectHandler;

    private class MLServiceConnection implements ServiceConnection
    {
        public void onServiceConnected(ComponentName name, IBinder binder)
        {
            multiLoadServiceBinder = (MultiLoadService.MultiLoadServiceBinder) binder;
            connectHandler.sendMessage(connectHandler.obtainMessage(1));
            bound = true;
        }

        public void onServiceDisconnected(ComponentName name)
        {
            connectHandler.sendMessage(connectHandler.obtainMessage(0));
            bound = false;
        }
    }

    public ServiceUtils(Context context, Handler connectHandler)
    {
        this.context = context;
        this.connectHandler = connectHandler;
        sConn = new MLServiceConnection();
    }

    public void StartService()
    {
        context.startService(new Intent(context, MultiLoadService.class));
    }

    public void StopService()
    {
        context.stopService(new Intent(context, MultiLoadService.class));
    }

    public void BindService()
    {
        context.bindService(new Intent(context, MultiLoadService.class), sConn, 0);
    }

    public void UnBindService()
    {
        if (bound)
            context.unbindService(sConn);
    }

    public MultiLoadService.MultiLoadServiceBinder getBind()
    {
        return multiLoadServiceBinder;
    }

    public boolean isBound()
    {
        return bound;
    }
}