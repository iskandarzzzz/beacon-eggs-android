package beaconeggs.android.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.TextHttpResponseHandler;

import org.apache.http.Header;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import beaconeggs.android.App;
import beaconeggs.android.editorModel.EditorLayout;
import beaconeggs.android.editorModel.EditorWidget;
import beaconeggs.android.editorModel.EditorWidgetAdapter;

public class BeaconMonitorService extends Service {

    private static final String TAG = BeaconMonitorService.class.getSimpleName();
    private static final String ESTIMOTE_PROXIMITY_UUID = "B9407F30-F5F8-466E-AFF9-25556B57FE6D";
    private static final Region ALL_ESTIMOTE_BEACONS = new Region("regionId", ESTIMOTE_PROXIMITY_UUID, null, null);
    private final IBinder mBinder = new LocalBinder();
    private ResolutionSelector resolutionSelector;
    private Executor executor;
    private BeaconManager beaconManager;

    @Override
    public void onCreate() {
        super.onCreate();

        final App app = (App) getApplication();

        resolutionSelector = new ResolutionSelector();
        executor = new Executor(resolutionSelector, app);
        beaconManager = new BeaconManager(this);

        final Gson gson = new GsonBuilder().registerTypeAdapter(EditorWidget.class, new EditorWidgetAdapter()).setPrettyPrinting().create();

        // Set scan period
//        beaconManager.setBackgroundScanPeriod(10000, 0);
        beaconManager.setForegroundScanPeriod(1000, 0);

        // Set listener
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, List<Beacon> beacons) {
                Log.d(TAG, "onBeaconsDiscovered: " + beacons.size());
                executor.addJob(beacons);
            }
        });

        RestClient.getLayouts(new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                Type type = new TypeToken<ArrayList<EditorLayout>>() {
                }.getType();

                List<EditorLayout> editorLayouts = gson.fromJson(responseString, type);
                // TODO: for testing purpose use only the first layout
                app.editorLayout = editorLayouts.get(0);

                Log.d(TAG, responseString);
                Log.d(TAG, gson.toJson(editorLayouts));

                connectBeaconService();
            }
        });

//        RestClient.getBeacons(new AsyncHttpResponseHandler() {
//            @Override
//            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
//                Type type = new TypeToken<ArrayList<LayoutBeacon>>() {
//                }.getType();
//
//                List<LayoutBeacon> layoutBeacons = gson.fromJson(new String(responseBody), type);
//
//                Log.d(TAG, new String(responseBody));
//                Log.d(TAG, "" + layoutBeacons.size());
//                Log.d(TAG, layoutBeacons.toString());
//                Log.d(TAG, "" + layoutBeacons.get(0).getUuid());
//            }
//
//            @Override
//            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
//                Log.d(TAG, "Status !!" + statusCode);
//                Log.d(TAG, "FAILURE !!");
//            }
//        });
    }

    private void connectBeaconService() {
        // Connect to BeaconService
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                // Start monitoring
                try {
                    beaconManager.startRanging(ALL_ESTIMOTE_BEACONS);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Stop monitoring
        try {
            beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS);
            beaconManager.disconnect();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Return the communication channel to the service.
        return mBinder;
    }

    public void setListener(ExecutorListener listener) {
        executor.setExecutorListener(listener);
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public BeaconMonitorService getService() {
            return BeaconMonitorService.this;
        }
    }
}
