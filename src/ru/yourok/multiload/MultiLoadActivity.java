package ru.yourok.multiload;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.*;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import ru.yourok.multiload.remote.RemoteActivity;
import ru.yourok.multiload.service.HttpDownLoadManager;

import java.io.File;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MultiLoadActivity extends Activity
{
    private static final String TAG = "MultiLoad";
    ServiceUtils serviceUtils = null;
    DownLoadListAdapter li_adapter = null;
    ListView listView;

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        MultiLoadApp.activityResumed();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        MultiLoadApp.activityPaused();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.main);

        listView = (ListView) findViewById(R.id.listView);
        listView.setLongClickable(true);
        OnItemClick onItemClick = new OnItemClick();
        listView.setOnItemClickListener(onItemClick);
        listView.setOnItemLongClickListener(onItemClick);

        serviceUtils = new ServiceUtils(this, new Handler());
        if (savedInstanceState != null)
        {
            if (savedInstanceState.getBoolean("ServiceBound"))
            {
                serviceUtils.BindService();
            } else
            {
                serviceUtils.StartService();
                serviceUtils.BindService();
            }
        } else
        {
            serviceUtils.StartService();
            serviceUtils.BindService();
        }

        final Handler uiHandler = new Handler();
        new Timer().schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                uiHandler.post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (serviceUtils.getBind() != null)
                        {
                            if (li_adapter == null && serviceUtils.getBind() != null)
                            {
                                li_adapter = new DownLoadListAdapter(MultiLoadActivity.this, serviceUtils);
                                listView.setAdapter(li_adapter);
                            }

                            if (li_adapter != null)
                                li_adapter.notifyDataSetChanged();
                        }
                    }
                });
            }

        }, 0L, 1000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.add_download:
                addDownLoad();
                return true;
            case R.id.pause_all:
                pauseAll();
                return true;
            case R.id.resume_all:
                resumeAll();
                return true;
            case R.id.delete_all:
                deleteAll();
                return true;
            case R.id.settings:
                ShowSettings();
                return true;
            case R.id.stopservice:
                StopService();
                return true;
            case R.id.remotectrl:
                RemoteCtrl();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putBoolean("ServiceBound", serviceUtils.isBound());
    }


    private class OnItemClick implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener
    {
        private boolean sendContent(int position)
        {
            if (serviceUtils.getBind() != null)
            {
                HttpDownLoadManager manager = serviceUtils.getBind().getAllHttpDownLoad().get(position);
                if (manager.getState() == HttpDownLoadManager.Complete)
                {
                    try
                    {
                        String mime = manager.getMimeType();
                        if (mime == "")
                            mime = "*/*";

                        Uri uri = Uri.fromFile(new File(manager.getFileName()));

                        Intent sendIntent = new Intent(Intent.ACTION_VIEW);
                        sendIntent.setDataAndType(uri, mime);
                        return startActivityIfNeeded(sendIntent, -1);
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }

        private void showMenu(int position)
        {
            if (serviceUtils.getBind() != null)
            {
                ContextMenuActivity contextMenuActivity = new ContextMenuActivity(MultiLoadActivity.this, serviceUtils, position);
                contextMenuActivity.show();
            }
        }

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
        {
            HttpDownLoadManager manager = serviceUtils.getBind().getAllHttpDownLoad().get(position);
            if (manager.getState() == HttpDownLoadManager.Complete)
            {
                if (!sendContent(position))
                    showMenu(position);
            } else
            {
                showMenu(position);
            }
        }

        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id)
        {
            HttpDownLoadManager manager = serviceUtils.getBind().getAllHttpDownLoad().get(position);
            if (manager.getState() != HttpDownLoadManager.Complete)
            {
                return sendContent(position);
            } else
            {
                showMenu(position);
                return true;
            }
        }
    }

    private void addDownLoad()
    {
        Intent intent = new Intent(this, AddDownLoad.class);
        startActivity(intent);
    }

    private void pauseAll()
    {
        if (serviceUtils.getBind() != null)
        {
            ArrayList<HttpDownLoadManager> managers = serviceUtils.getBind().getAllHttpDownLoad();
            for (HttpDownLoadManager manager : managers)
                manager.Pause();
        }
    }

    private void resumeAll()
    {
        if (serviceUtils.getBind() != null)
        {
            ArrayList<HttpDownLoadManager> managers = serviceUtils.getBind().getAllHttpDownLoad();
            for (HttpDownLoadManager manager : managers)
                if (manager.getState() != HttpDownLoadManager.Loading)
                    manager.Resume();
        }
    }

    private void deleteAll()
    {
        if (serviceUtils.getBind() != null)
        {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder
                    .setMessage(R.string.label_deletefiles)
                    .setCancelable(false)
                    .setPositiveButton(R.string.label_yes, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            ArrayList<HttpDownLoadManager> managers = serviceUtils.getBind().getAllHttpDownLoad();
                            for (HttpDownLoadManager manager : managers)
                            {
                                manager.Stop();
                                new File(manager.getFileName()).delete();
                            }
                            serviceUtils.getBind().clearHttpDownLoads();
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(R.string.label_no, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            ArrayList<HttpDownLoadManager> managers = serviceUtils.getBind().getAllHttpDownLoad();
                            for (HttpDownLoadManager manager : managers)
                                manager.Stop();
                            serviceUtils.getBind().clearHttpDownLoads();
                            dialog.cancel();
                        }
                    });

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
    }

    private void ShowSettings()
    {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void StopService()
    {
        serviceUtils.UnBindService();
        serviceUtils.StopService();
        finish();
    }

    private void RemoteCtrl()
    {
        Intent intent = new Intent(this, RemoteActivity.class);
        startActivity(intent);
    }

    private class DownLoadListAdapter extends BaseAdapter
    {
        private ServiceUtils serviceUtils;
        private Context context;

        public DownLoadListAdapter(Context context, ServiceUtils serviceUtils)
        {
            super();
            this.serviceUtils = serviceUtils;
            this.context = context;
        }

        @Override
        public int getCount()
        {
            if (serviceUtils != null && serviceUtils.getBind() != null && serviceUtils.getBind().getAllHttpDownLoad() != null)
                return serviceUtils.getBind().getAllHttpDownLoad().size();
            else
                return 0;
        }

        @Override
        public Object getItem(int position)
        {
            if (serviceUtils != null && serviceUtils.getBind() != null && serviceUtils.getBind().getAllHttpDownLoad() != null)
                return serviceUtils.getBind().getAllHttpDownLoad().get(position);
            else
                return null;
        }

        @Override
        public long getItemId(int arg0)
        {
            return arg0;
        }

        private String trimFilePath(String fileName, TextView textView, int viewWidth)
        {
            try
            {
                fileName = URLDecoder.decode(fileName);

                Rect bounds = new Rect();
                Paint textPaint = textView.getPaint();
                textPaint.getTextBounds(fileName, 0, fileName.length(), bounds);
                int width = bounds.width();
                if (width > viewWidth)
                {
                    String trim = "";
                    for (int i = fileName.length() / 2 - 1; i > 0; i--)
                    {
                        trim = fileName.substring(0, i) + "..." + fileName.substring(fileName.length() - i);
                        bounds = new Rect();
                        textPaint = textView.getPaint();
                        textPaint.getTextBounds(trim, 0, trim.length(), bounds);
                        width = bounds.width();
                        if (width < viewWidth)
                            return trim;
                    }
                    return trim;
                }
                return fileName;

            } catch (Exception e)
            {
                e.printStackTrace();
                return fileName;
            }
        }

        private String getTime(long milliseconds)
        {
            int seconds = (int) ((milliseconds / 1000) % 60);
            int minutes = (int) ((milliseconds / 1000) / 60);
            String sec = String.valueOf(seconds);
            if (seconds < 10)
                sec = "0" + sec;
            String min = String.valueOf(minutes);
            if (minutes < 10)
                min = "0" + min;
            return min + ":" + sec;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View v = convertView;
            if (v == null)
            {
                LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.listitem, null);
            }

            if (serviceUtils != null && serviceUtils.getBind() != null && serviceUtils.getBind().getAllHttpDownLoad() != null)
            {
                HttpDownLoadManager downLoadManager = serviceUtils.getBind().getAllHttpDownLoad().get(position);
                if (downLoadManager != null)
                {
                    String progress = "";
                    String detail = "";
                    TextView turl = (TextView) v.findViewById(R.id.li_loadurl);
                    TextView tfile = (TextView) v.findViewById(R.id.li_filesave);
                    ThreadedProgress tprogressbar = (ThreadedProgress) v.findViewById(R.id.li_threadprogress);
                    if (turl != null)
                    {
                        DisplayMetrics displaymetrics = new DisplayMetrics();
                        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
                        int viewWidth = displaymetrics.widthPixels - 20;
                        turl.setText(trimFilePath(downLoadManager.getUrlString(), turl, viewWidth));
                    }
                    if (tfile != null)
                    {
                        tfile.setText(downLoadManager.getFileName());
                    }

                    if (downLoadManager.getState() == HttpDownLoadManager.Error)
                    {
                        tfile.setText(downLoadManager.getErrorMessage());
                    } else if (downLoadManager.getState() == HttpDownLoadManager.CreateFile)
                    {
                        progress = getString(R.string.label_openfile) + " " + new File(downLoadManager.getFileName()).getName();
                    } else if (downLoadManager.getState() == HttpDownLoadManager.Connect)
                    {
                        progress = getString(R.string.label_connecting);
                    } else if (downLoadManager.getState() == HttpDownLoadManager.Complete)
                    {
                        progress = getString(R.string.label_downloadcomplete);
                        DecimalFormat df = new DecimalFormat();
                        long loadedSize = downLoadManager.getDownLoadSize();
                        long loadingTime = downLoadManager.getLoadingTime();
                        detail = df.format(loadedSize) + " bytes | " + getTime(loadingTime);
                    } else if (downLoadManager.getState() == HttpDownLoadManager.Paused)
                    {
                        progress = getString(R.string.label_downloadpaused);
                    } else if (downLoadManager.getState() == HttpDownLoadManager.Wait)
                    {
                        progress = getString(R.string.label_downloadwait);
                    } else if (downLoadManager.getState() == HttpDownLoadManager.Loading)
                    {
                        DecimalFormat dfp = new DecimalFormat("#.00");
                        long loadedSize = downLoadManager.getDownLoadSize();
                        long allSize = downLoadManager.getContentSize();
                        long percent = 0;

                        String loadedSizeStr = "";
                        if (loadedSize < 1000)
                            loadedSizeStr = dfp.format(loadedSize) + " b";
                        if (loadedSize < 1000000)
                            loadedSizeStr = dfp.format((float) loadedSize / 1000) + " kb";
                        if (loadedSize >= 1000000)
                            loadedSizeStr = dfp.format((float) loadedSize / 1000000) + " mb";

                        if (allSize > 0)
                        {
                            String allSizeStr = "";
                            if (allSize < 1000)
                                allSizeStr = dfp.format(allSize) + "b";
                            if (allSize < 1000000)
                                allSizeStr = dfp.format((float) allSize / 1000) + "kb";
                            if (allSize > 1000000)
                                allSizeStr = dfp.format((float) allSize / 1000000) + "mb";

                            if (allSize > 0)
                                percent = loadedSize * 100 / allSize;
                            progress = String.valueOf(percent) + " % | " + loadedSizeStr + " / " + allSizeStr + " | " + String.valueOf(downLoadManager.getLoadingThreads());
                        } else
                            progress = loadedSizeStr + " bytes";

                        DecimalFormat dfd = new DecimalFormat("#.00");
                        float speed = downLoadManager.getSpeed();
                        long remain = 0;
                        if ((long) speed > 0)
                            remain = (allSize - loadedSize) / (long) speed;
                        long loadingTime = downLoadManager.getLoadingTime();

                        if (allSize > 0)
                            detail = getString(R.string.label_speed) + " " + dfd.format(speed) + " KB/S | " + getTime(remain) + " / " + getTime(loadingTime);
                        else
                            detail = getString(R.string.label_speed) + " " + dfd.format(speed) + " KB/S | " + String.valueOf(loadingTime);
                    }

                    if (tprogressbar != null)
                    {
                        tprogressbar.setParams(downLoadManager, progress, detail);
                    }
                } else
                {
                    TextView tname = (TextView) v.findViewById(R.id.li_loadurl);
                    tname.setText("");
                }
            }
            return v;
        }
    }
}
