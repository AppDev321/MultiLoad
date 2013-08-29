package ru.yourok.multiload.remote;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import ru.yourok.multiload.R;
import ru.yourok.multiload.ServiceUtils;
import ru.yourok.multiload.service.utils.Settings;

/**
 * Created with IntelliJ IDEA.
 * User: yourok
 * Date: 10/26/12
 * Time: 5:25 PM
 */
public class RemoteActivity extends Activity
{
    TextView errorView;

    EditText editLogin;
    EditText editPassword;

    Button btnRegister;
    Button btnLoadUrls;

    RemoteManager remoteManager = null;
    ServiceUtils serviceUtils = null;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.remote);

        errorView = (TextView) findViewById(R.id.textViewError);

        editLogin = (EditText) findViewById(R.id.editUserName);
        editPassword = (EditText) findViewById(R.id.editPassword);

        btnRegister = (Button) findViewById(R.id.button_register);
        btnLoadUrls = (Button) findViewById(R.id.button_load_urls);

        serviceUtils = new ServiceUtils(this, new Handler(new ServiceHandlerCallBack()));
        serviceUtils.StartService();
        serviceUtils.BindService();

        btnRegister.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                new RegisterTask().execute();
            }
        });

        btnLoadUrls.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(RemoteActivity.this, RemoteListActivity.class);
                startActivity(intent);
            }
        });
    }

    private class RegisterTask extends AsyncTask<Void, String, Boolean>
    {
        @Override
        protected Boolean doInBackground(Void... voids)
        {
            if (remoteManager != null)
            {
                publishProgress(new String[]{"Connecting..."});
                if (!remoteManager.isConnected())
                    if (!remoteManager.Connect())
                    {
                        publishProgress(new String[]{"Error connecting"});
                        Log.v("MultiLoad", remoteManager.getErrString());
                        return false;
                    }

                publishProgress(new String[]{"Registration"});
                String user = editLogin.getText().toString();
                String pass = editPassword.getText().toString();
                if (user.isEmpty() || pass.isEmpty())
                {
                    publishProgress(new String[]{"User name or password wrong"});
                    return false;
                }

                remoteManager.setNamePass(user, pass);
                boolean isRegister = remoteManager.registerUser();
                if (isRegister)
                {
                    publishProgress(new String[]{"Registration success"});
                    return true;
                } else
                    publishProgress(new String[]{remoteManager.getErrString()});
            }
            return false;
        }


        @Override
        protected void onProgressUpdate(String... values)
        {
            super.onProgressUpdate(values);
            errorView.setText(values[0]);
        }
    }

    @Override
    protected void onPause()
    {
        setSettings();
        super.onPause();
    }

    private void disableAll()
    {
        editLogin.setEnabled(false);
        editPassword.setEnabled(false);
        btnRegister.setEnabled(false);
    }

    private void enableAll()
    {
        editLogin.setEnabled(true);
        editPassword.setEnabled(true);
        btnRegister.setEnabled(true);
    }

    private void getSettings()
    {
        editLogin.setText(Settings.getInstance().getUserName());
        editPassword.setText(Settings.getInstance().getPassword());
    }

    private void setSettings()
    {
        Settings.getInstance().setUserName(editLogin.getText().toString());
        Settings.getInstance().setPassword(editPassword.getText().toString());
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
                remoteManager = serviceUtils.getBind().getRemoteManager();
            } else if (message.what == 0)
            {
                disableAll();
            }
            return false;
        }
    }
}
