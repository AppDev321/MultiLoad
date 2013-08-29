package ru.yourok.multiload;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import ru.yourok.multiload.service.HttpDownLoadManager;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: yourok
 * Date: 07.08.12
 * Time: 10:33
 */
public class ContextMenuActivity extends Dialog
{
    Button btnResume, btnPause, btnReload, btnEdit, btnDelete, btnDeleteFile;
    Context context;
    ServiceUtils serviceUtils;
    int position;

    public ContextMenuActivity(Context context, ServiceUtils serviceUtils, int position)
    {
        super(context);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.contextmenuactivity);
        setCanceledOnTouchOutside(true);
        setCancelable(true);

        this.context = context;
        this.position = position;
        this.serviceUtils = serviceUtils;

        btnPause = (Button) findViewById(R.id.btnPause);
        btnResume = (Button) findViewById(R.id.btnResume);
        btnReload = (Button) findViewById(R.id.btnReload);
        btnEdit = (Button) findViewById(R.id.btnEdit);
        btnDelete = (Button) findViewById(R.id.btnDelete);
        btnDeleteFile = (Button) findViewById(R.id.btnDeleteFile);

        OnClick onClick = new OnClick();

        btnResume.setOnClickListener(onClick);
        btnPause.setOnClickListener(onClick);
        btnReload.setOnClickListener(onClick);
        btnEdit.setOnClickListener(onClick);
        btnDelete.setOnClickListener(onClick);
        btnDeleteFile.setOnClickListener(onClick);
    }

    private class OnClick implements View.OnClickListener
    {
        @Override
        public void onClick(View view)
        {
            if (serviceUtils != null && serviceUtils.getBind() != null && serviceUtils.getBind().getAllHttpDownLoad() != null)
            {
                HttpDownLoadManager manager = serviceUtils.getBind().getAllHttpDownLoad().get(position);
                switch (view.getId())
                {
                    case R.id.btnPause:
                        if (manager.getState() != HttpDownLoadManager.Complete)
                        {
                            manager.Pause();
                        }
                        break;
                    case R.id.btnResume:
                    {
                        if (manager.getState() != HttpDownLoadManager.Complete && manager.getState() != HttpDownLoadManager.Loading)
                            manager.Resume();
                        break;
                    }
                    case R.id.btnReload:
                    {
                        manager.Stop();
                        new File(manager.getFileName()).delete();
                        new connect(manager).start();
                        break;
                    }
                    case R.id.btnEdit:
                    {
                        Intent intent = new Intent(context, AddDownLoad.class);
                        intent.putExtra("EDIT", true);
                        intent.putExtra("ID", manager.getID());
                        intent.putExtra("FILENAME", manager.getFileName());
                        intent.putExtra("URLNAME", manager.getUrlString());
                        context.startActivity(intent);
                        break;
                    }
                    case R.id.btnDelete:
                    {
                        manager.Stop();
                        serviceUtils.getBind().deleteHttpDownLoad(manager.getID());
                        break;
                    }
                    case R.id.btnDeleteFile:
                    {
                        manager.Stop();
                        new File(manager.getFileName()).delete();
                        serviceUtils.getBind().deleteHttpDownLoad(manager.getID());
                        break;
                    }
                }
            }
            cancel();
        }
    }

    class connect extends Thread
    {
        HttpDownLoadManager manager;

        connect(HttpDownLoadManager manager)
        {
            this.manager = manager;
        }

        @Override
        public void run()
        {
            manager.Start();
        }
    }

    ;
}
