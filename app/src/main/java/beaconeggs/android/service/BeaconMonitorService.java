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

import java.util.List;

import beaconeggs.android.App;

public class BeaconMonitorService extends Service {

    public static final int DEFAULT_FOREGROUND_SCAN_PERIOD = 500;
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

        // Set scan period
//        beaconManager.setBackgroundScanPeriod(10000, 0);
        beaconManager.setForegroundScanPeriod(app.foregroundScanPeriod, 0);

        // Set listener
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, List<Beacon> beacons) {
                Log.d(TAG, "onBeaconsDiscovered: " + beacons.size());
                executor.addJob(beacons);
            }
        });

        connectBeaconService();
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
