package ru.yourok.multiload.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import ru.yourok.multiload.remote.RemoteManager;
import ru.yourok.multiload.service.utils.Settings;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: yourok
 * Date: 25.07.12
 * Time: 14:26
 */
public class MultiLoadService extends Service
{
    MultiLoadServiceBinder binder = new MultiLoadServiceBinder();
    MultiLoadManager loadManager;
    RemoteManager remoteManager;

    @Override
    public void onCreate()
    {
        super.onCreate();
        Settings.getInstance().Init(this);

        /*
        String extStr = "zip,gz,gzip,tgz,bz2,bzip2,tbz2,tbz,tar,rar,cab,arj,taz,cpio,rpm,deb,lzh,lha,iso,cab," +
                "acr,air,apk,app,bat,bin,elf,exe,pif,scr,dll,drv,sys," +
                "doc,docx,odt,rtf,wpd,wps,xlr,xls,xlsx,indd,pct,pdf,accdb,dbf,mdb,pdb,dwg,dxf,torrent,txt," +
                "fb2,chm,epub,pdb,prc,pml,mobi,azw,tcr," +
                "fnt,fon,otf,ttf," +
                "ai,eps,ps,svg,bmp,dds,dng,gif,jpg,png,psd,pspimage,tga,thm,tif,yuv,3dm,3ds,max,obj," +
                "aac,aif,iff,m3u,m4a,mid,mp3,mpa,ra,wav,wma,flac,ogg,3g2,3gp,asf,asx,avi,flv,mov,mp4,mpg,rm,swf,vob,wmv,mkv,";
        String[] extensions = extStr.split(",");
        String manifest = "";
        for (String ext : extensions)
        {
            manifest += "<data android:pathPattern=\".*\\\\." + ext + "\"/>";
            String tt = ".*\\\\.";
            String buf = "";
            for (int i = 0; i < 10; i++)
            {
                buf += tt;
                manifest += "<data android:pathPattern=\"" + buf + ext + ".*\"/>";
            }
        }
        */

        loadManager = new MultiLoadManager(this);
        remoteManager = new RemoteManager();
    }

    @Override
    public IBinder onBind(Intent arg0)
    {
        return binder;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    public class MultiLoadServiceBinder extends Binder
    {
        public int addHttpDownLoad(String urlStr, String fileName)
        {
            return loadManager.addHttpUrl(urlStr, fileName);
        }

        public ArrayList<HttpDownLoadManager> getAllHttpDownLoad()
        {
            return loadManager.getAllLoadThread();
        }

        public void deleteHttpDownLoad(int id)
        {
            loadManager.deleteLoadThread(id);
        }

        public void clearHttpDownLoads()
        {
            loadManager.clearLoadThreads();
        }

        public RemoteManager getRemoteManager()
        {
            return remoteManager;
        }
    }
}
