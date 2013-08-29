package ru.yourok.multiload;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import ru.yourok.multiload.service.utils.Settings;

/**
 * Created with IntelliJ IDEA.
 * User: yourok
 * Date: 15.08.12
 * Time: 18:10
 */
public class SettingsActivity extends Activity
{
    ServiceUtils serviceUtils = null;

    EditText editMaxLoads, editMaxThreads, editTimeout, editMaxBuf;
    CheckBox checkAutoSizeBuf, checkWifiCheck, checkShowAdd;
    Button btnResetSet;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.setting);

        editMaxLoads = (EditText) findViewById(R.id.editTextMaxLoads);
        editMaxThreads = (EditText) findViewById(R.id.editTextMaxThreads);
        editTimeout = (EditText) findViewById(R.id.editTextTimeOut);
        editMaxBuf = (EditText) findViewById(R.id.editTextBufSize);

        checkAutoSizeBuf = (CheckBox) findViewById(R.id.checkBoxAutoSizeBuf);
        checkWifiCheck = (CheckBox) findViewById(R.id.checkBoxCheckWifi);
        checkShowAdd = (CheckBox) findViewById(R.id.checkBoxShowAdd);

        btnResetSet = (Button) findViewById(R.id.buttonResetSettings);

        btnResetSet.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                editMaxLoads.setText("3");
                editMaxThreads.setText("5");
                editTimeout.setText("15000");
                editMaxBuf.setText("1048576");

                checkAutoSizeBuf.setChecked(true);
                checkWifiCheck.setChecked(true);
                checkShowAdd.setChecked(false);
            }
        });

        serviceUtils = new ServiceUtils(this, new Handler(new ServiceHandlerCallBack()));
        serviceUtils.StartService();
        serviceUtils.BindService();
    }

    @Override
    protected void onPause()
    {
        setSettings();
        super.onPause();
    }

    private void disableAll()
    {
        editMaxLoads.setEnabled(false);
        editMaxThreads.setEnabled(false);
        editTimeout.setEnabled(false);
        editMaxBuf.setEnabled(false);
        checkAutoSizeBuf.setEnabled(false);
        checkWifiCheck.setEnabled(false);
        checkShowAdd.setEnabled(false);
    }

    private void enableAll()
    {
        editMaxLoads.setEnabled(true);
        editMaxThreads.setEnabled(true);
        editTimeout.setEnabled(true);
        editMaxBuf.setEnabled(true);
        checkAutoSizeBuf.setEnabled(true);
        checkWifiCheck.setEnabled(true);
        checkShowAdd.setEnabled(true);
    }

    private void getSettings()
    {
        editMaxLoads.setText(String.valueOf(Settings.getInstance().getMaxLoads()));
        editMaxThreads.setText(String.valueOf(Settings.getInstance().getMaxConnections()));
        editTimeout.setText(String.valueOf(Settings.getInstance().getTimeOut()));
        editMaxBuf.setText(String.valueOf(Settings.getInstance().getMaxSizeBuffer()));

        checkAutoSizeBuf.setChecked(Settings.getInstance().getAutoSizeBuffer());
        checkWifiCheck.setChecked(Settings.getInstance().getLoadOnWifi());
        checkShowAdd.setChecked(Settings.getInstance().showAddActivity());
    }

    private void setSettings()
    {
        if (editMaxLoads.getText().toString().isEmpty())
            editMaxLoads.setText("0");
        if (editMaxThreads.getText().toString().isEmpty())
            editMaxThreads.setText("0");
        if (editTimeout.getText().toString().isEmpty())
            editTimeout.setText("0");
        if (editMaxBuf.getText().toString().isEmpty())
            editMaxBuf.setText("0");
        Settings.getInstance().setMaxLoads(Integer.valueOf(editMaxLoads.getText().toString()));
        Settings.getInstance().setMaxConnections(Integer.valueOf(editMaxThreads.getText().toString()));
        Settings.getInstance().setTimeOut(Integer.valueOf(editTimeout.getText().toString()));
        Settings.getInstance().setMaxSizeBuffer(Integer.valueOf(editMaxBuf.getText().toString()));

        Settings.getInstance().setAutoSizeBuffer(checkAutoSizeBuf.isChecked());
        Settings.getInstance().setLoadOnWifi(checkWifiCheck.isChecked());
        Settings.getInstance().setShowAddActivity(checkShowAdd.isChecked());
    }

    private class ServiceHandlerCallBack implements Handler.Callback
    {
        @Override
        public boolean handleMessage(Message message)
        {
            if (message.what == 1)
            {
                enableAll();
                getSettings();
            } else if (message.what == 0)
            {
                disableAll();
            }
            return false;
        }
    }
}
