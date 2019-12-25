package com.brazedblue.waverly;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class DebugLogActivity extends Activity {

    private static final String TAG = "DebugLogActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.brazedblue.waverly.R.layout.activity_debug_log);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(com.brazedblue.waverly.R.menu.debug_log_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == com.brazedblue.waverly.R.id.sendLogEmailAction)
        {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(com.brazedblue.waverly.R.string.log_email_subject));
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"stevenmeyer142@gmail.com"});
            String logString = CustomLog.getLogText();

            intent.putExtra(Intent.EXTRA_TEXT, logString);
            intent.setType("plain/text");

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
            else {
                android.util.Log.e(TAG, "intent.resolveActivity failed");
            }

        }

        return super.onOptionsItemSelected(item);

    }

        @Override
    protected void onResume() {
        super.onResume();

        TextView logText = (TextView)findViewById(com.brazedblue.waverly.R.id.log_text);
        String logString = CustomLog.getLogText();
        if (logString != null)
        {
            logText.setText(logString);
        }
    }
}
