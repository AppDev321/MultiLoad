package ru.yourok.multiload;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import ru.yourok.multiload.service.HttpDownLoadManager;
import ru.yourok.multiload.service.utils.Settings;


/**
 * Created with IntelliJ IDEA.
 * User: yourok
 * Date: 02.08.12
 * Time: 13:45
 */
public class AddDownLoad extends Activity
{
    ServiceUtils serviceUtils = null;

    EditText urlEdit = null, fileEdit = null;
    boolean isAdd;
    boolean editType;
    int id;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.adddownload);
        editType = false;

        serviceUtils = new ServiceUtils(this, new Handler(new ServiceHandlerCallBack()));
        serviceUtils.StartService();
        serviceUtils.BindService();

        urlEdit = (EditText) findViewById(R.id.urlEdit);
        fileEdit = (EditText) findViewById(R.id.filenameEdit);

        Intent intent = getIntent();

        if (intent.getExtras() != null)
        {
            editType = intent.getExtras().getBoolean("EDIT");
            if (editType)
            {
                ((Button) findViewById(R.id.btnAdd)).setText(R.string.btn_edit);
                id = intent.getExtras().getInt("ID");
                urlEdit.setText(intent.getExtras().getString("URLNAME"));
                fileEdit.setText(intent.getExtras().getString("FILENAME"));
            }
        }

        isAdd = false;

        if (intent.getData() != null)
        {
            urlEdit.setText(intent.getDataString());
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        serviceUtils.UnBindService();
    }

    public void addBtnClick(View view)
    {
        if (isAdd)
        {
            finish();
            return;
        }

        isAdd = true;
        String url = "";
        String filename = "";

        if (urlEdit != null)
            url = urlEdit.getText().toString();
        if (url.isEmpty())
        {
            Toast.makeText(this, R.string.error_url_empty, Toast.LENGTH_SHORT).show();
            isAdd = false;
            return;
        }
        if (fileEdit != null)
            filename = fileEdit.getText().toString();

        if (editType)
        {
            HttpDownLoadManager manager = serviceUtils.getBind().getAllHttpDownLoad().get(id);
            manager.editUrlFileName(url, filename);
            finish();
            return;

        } else
        {
            serviceUtils.getBind().addHttpDownLoad(url, filename);
            finish();
            return;
        }
    }

    public void cancelBtnClick(View view)
    {
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString("UrlString", urlEdit.getText().toString());
        outState.putString("FileNameString", fileEdit.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        urlEdit.setText(savedInstanceState.getString("UrlString"));
        fileEdit.setText(savedInstanceState.getString("FileNameString"));
    }

    private class ServiceHandlerCallBack implements Handler.Callback
    {
        @Override
        public boolean handleMessage(Message message)
        {
            if (message.what == 1)
            {
                if (getIntent().getData() != null && !Settings.getInstance().showAddActivity() && !editType)
                {
                    serviceUtils.getBind().addHttpDownLoad(getIntent().getDataString(), "");
                    finish();
                }
            }
            return false;
        }
    }
}