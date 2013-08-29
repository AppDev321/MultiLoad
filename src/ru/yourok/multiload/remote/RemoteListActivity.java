package ru.yourok.multiload.remote;

import android.app.ListActivity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import ru.yourok.multiload.R;
import ru.yourok.multiload.ServiceUtils;
import ru.yourok.multiload.service.utils.Settings;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: yourok
 * Date: 30.10.12
 * Time: 13:24
 */
public class RemoteListActivity extends ListActivity
{
    RemoteManager remoteManager = null;
    ServiceUtils serviceUtils = null;
    MySimpleArrayAdapter adapter;
    ArrayList<UrlPath> urls;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        urls = new ArrayList<UrlPath>();
        adapter = new MySimpleArrayAdapter(this, urls);
        this.setListAdapter(adapter);

        serviceUtils = new ServiceUtils(this, new Handler(new ServiceHandlerCallBack()));
        serviceUtils.StartService();
        serviceUtils.BindService();
    }

    private class ServiceHandlerCallBack implements Handler.Callback
    {
        @Override
        public boolean handleMessage(Message message)
        {
            if (message.what == 1)
            {
                remoteManager = serviceUtils.getBind().getRemoteManager();
                new LoadUrlsTask().execute();
            } else if (message.what == 0)
            {
                remoteManager = null;
            }
            return false;
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        if (urls != null || !urls.isEmpty())
        {
            try
            {

            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    private class MySimpleArrayAdapter extends ArrayAdapter<UrlPath>
    {
        private final Context context;
        private final ArrayList<UrlPath> values;

        public MySimpleArrayAdapter(Context context, ArrayList<UrlPath> values)
        {
            super(context, R.layout.remote_list_item, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.remote_list_item, parent, false);

            TextView textViewUrl = (TextView) rowView.findViewById(R.id.textViewUrl);
            TextView textViewPath = (TextView) rowView.findViewById(R.id.textViewPath);

            if (values != null || !values.isEmpty())
            {
                textViewUrl.setText(values.get(position).getUrl());
                textViewPath.setText(values.get(position).getPath());
            }

            return rowView;
        }
    }

    private class LoadUrlsTask extends AsyncTask<Void, String, Void>
    {
        @Override
        protected Void doInBackground(Void... voids)
        {
            try
            {
                if (remoteManager != null)
                {
                    if (!remoteManager.isConnected())
                        if (!remoteManager.Connect())
                        {
                            publishProgress("Error connection to server");
                            return null;
                        }
                    remoteManager.setNamePass(Settings.getInstance().getUserName(), Settings.getInstance().getPassword());
                    urls.clear();
                    ArrayList<UrlPath> tmp = remoteManager.getUrls();
                    if (tmp != null)
                        urls.addAll(tmp);
                    else
                        publishProgress(remoteManager.getErrString());
                    publishProgress("", "");
                }
            } catch (Exception e)
            {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values)
        {
            super.onProgressUpdate(values);
            if (values.length == 1)
                Toast.makeText(Settings.getInstance().getServiceContext(), values[0], Toast.LENGTH_SHORT).show();
            else
                adapter.notifyDataSetChanged();
        }
    }
}
