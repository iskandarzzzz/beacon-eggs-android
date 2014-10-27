package beaconeggs.android;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import beaconeggs.android.service.BeaconMonitorService;
import beaconeggs.android.service.ExecutorListener;
import beaconeggs.core.ComputedPoint;


public class Home extends Activity {

    BeaconMonitorService mService;
    Boolean mBound = false;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            BeaconMonitorService.LocalBinder binder = (BeaconMonitorService.LocalBinder) iBinder;
            mService = binder.getService();
            mBound = true;

            mService.setListener(new ExecutorListener() {
                @Override
                public void onExecute(ComputedPoint computedPoint) {
                    txt.setText("" + computedPoint.getX() + " : " + computedPoint.getY());
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBound = false;
        }
    };
    private TextView txt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        txt = (TextView) findViewById(R.id.txt);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Bind to BeaconMonitorService
        Intent intent = new Intent(this, BeaconMonitorService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unbind from BeaconMonitorService
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
