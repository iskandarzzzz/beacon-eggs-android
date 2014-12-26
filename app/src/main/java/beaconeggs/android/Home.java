package beaconeggs.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.TextHttpResponseHandler;

import org.apache.http.Header;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import beaconeggs.android.editorModel.EditorLayout;
import beaconeggs.android.editorModel.EditorWidget;
import beaconeggs.android.editorModel.EditorWidgetAdapter;
import beaconeggs.android.service.BeaconHistory;
import beaconeggs.android.service.BeaconMonitorService;
import beaconeggs.android.service.ExecutorListener;
import beaconeggs.android.service.RestClient;
import beaconeggs.core.ComputedPoint;


public class Home extends BaseActivity {

    private static final String TAG = "Home";
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
                    computed_position.setText(computedPoint.getX() + "\n" + computedPoint.getY());
                }

                @Override
                public void onDistance(String msg) {
                    distances.setText(msg);
                }

                @Override
                public void onProcessedDistance(String msg) {
                    processedDistances.setText(msg);
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBound = false;
        }
    };
    private TextView computed_position;
    private TextView distances;
    private TextView processedDistances;
    private Spinner layouts;
    private Spinner filter_method;
    private NumberPicker foregroundScanPeriod;
    private Switch switch_start;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // find
        computed_position = (TextView) findViewById(R.id.computed_position);
        distances = (TextView) findViewById(R.id.distances);
        processedDistances = (TextView) findViewById(R.id.processedDistances);
        layouts = (Spinner) findViewById(R.id.layouts);
        filter_method = (Spinner) findViewById(R.id.filter_method);
        foregroundScanPeriod = (NumberPicker) findViewById(R.id.foreground_scan_period);
        switch_start = (Switch) findViewById(R.id.switch_start);

        // set
        layouts.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                app.editorLayout = app.editorLayouts.get(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        filter_method.setAdapter(new FilterMethodSpinnerAdapter(this));
        filter_method.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                app.filterMethod = BeaconHistory.FilterMethod.values()[i];
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        filter_method.setSelection(BeaconHistory.FilterMethod.WAverage.ordinal(), true);
        setNumberPicker(foregroundScanPeriod, 50, 60 * 1000, 50);
        switch_start.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    startBeaconManagerService();
                } else {
                    stopBeaconManagerService();
                }
            }
        });
    }

    private void setNumberPicker(NumberPicker picker, final int min, int max, final int step) {
        picker.setMinValue(min / step - 1);
        picker.setMaxValue((max / step) - 1);
        String[] valueSet = new String[max / min];
        for (int i = min; i <= max; i += step) {
            valueSet[(i / step) - 1] = Integer.toString(i);
        }
        picker.setDisplayedValues(valueSet);
        picker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int oldVal, int newVal) {
                app.foregroundScanPeriod = min + newVal * step;
            }
        });
        picker.setValue(BeaconMonitorService.DEFAULT_FOREGROUND_SCAN_PERIOD / step - 1);
    }

    @Override
    protected void onStart() {
        super.onStart();

        RestClient.getLayouts(new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                Gson gson = new GsonBuilder().registerTypeAdapter(EditorWidget.class, new EditorWidgetAdapter()).setPrettyPrinting().create();
                Type type = new TypeToken<ArrayList<EditorLayout>>() {
                }.getType();

                List<EditorLayout> editorLayouts = gson.fromJson(responseString, type);
                app.editorLayouts = editorLayouts;

                LayoutsSpinnerAdapter spinnerAdapter = new LayoutsSpinnerAdapter(Home.this, editorLayouts);
                layouts.setAdapter(spinnerAdapter);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopBeaconManagerService();
    }

    private void startBeaconManagerService() {
        // start service
        if (App.hasBluetooth()) {
            if (mBound == false) {
                // Bind to BeaconMonitorService
                Intent intent = new Intent(this, BeaconMonitorService.class);
                bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            }
        } else {
            Toast.makeText(this, "No BluetoothAdapter found", Toast.LENGTH_LONG).show();
        }
    }

    private void stopBeaconManagerService() {
        // Unbind from BeaconMonitorService
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.home, menu);
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
