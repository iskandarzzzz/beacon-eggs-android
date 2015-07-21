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
import com.estimote.sdk.Utils;
import com.perples.recosdk.RECOBeacon;
import com.perples.recosdk.RECOBeaconManager;
import com.perples.recosdk.RECOBeaconRegion;
import com.perples.recosdk.RECOErrorCode;
import com.perples.recosdk.RECORangingListener;
import com.perples.recosdk.RECOServiceConnectListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import beaconeggs.android.App;

public class BeaconMonitorService extends Service {

    public static final int DEFAULT_FOREGROUND_SCAN_PERIOD = 500;
    private static final String TAG = BeaconMonitorService.class.getSimpleName();
    private static final String ESTIMOTE_PROXIMITY_UUID = "B9407F30-F5F8-466E-AFF9-25556B57FE6D";
    private static final String RECO_PROXIMITY_UUID = "24DDF411-8CF1-440C-87CD-E368DAF9C93E";
    private static final Region ALL_ESTIMOTE_BEACONS = new Region("regionId", ESTIMOTE_PROXIMITY_UUID, null, null);
    private final IBinder mBinder = new LocalBinder();
    private ResolutionSelector resolutionSelector;
    private Executor executor;
    private BeaconManager beaconManager;
    private BeaconLogger beaconLogger;
    private RECOBeaconManager recoBeaconManager;

    @Override
    public void onCreate() {
        super.onCreate();

        final App app = (App) getApplication();

        SocketIOClient.connect();
        resolutionSelector = new ResolutionSelector();
        executor = new Executor(resolutionSelector, app);
        beaconManager = new BeaconManager(this);
        beaconLogger = new BeaconLogger(this);

        // Reco
        recoBeaconManager = RECOBeaconManager.getInstance(this);
        recoBeaconManager.setScanPeriod(app.foregroundScanPeriod);
        //recoBeaconManager.setSleepPeriod(0);
        recoBeaconManager.setRangingListener(new RECORangingListener() {
            @Override
            public void didRangeBeaconsInRegion(Collection<RECOBeacon> collection, RECOBeaconRegion recoBeaconRegion) {
                ArrayList<LayoutBeacon> convertedBeacons = new ArrayList<LayoutBeacon>(collection.size());
                for (RECOBeacon beacon : collection) {
                    Beacon b = new Beacon(Utils.normalizeProximityUUID(beacon.getProximityUuid()), "RECO", "", beacon.getMajor(), beacon.getMinor(), beacon.getTxPower(), beacon.getRssi());
                    double distance = Utils.computeAccuracy(b);
                    convertedBeacons.add(new LayoutBeacon(b, distance));
                }

                executor.addJob(convertedBeacons);
            }

            @Override
            public void rangingBeaconsDidFailForRegion(RECOBeaconRegion recoBeaconRegion, RECOErrorCode recoErrorCode) {

            }
        });

        // Set scan period
//        beaconManager.setBackgroundScanPeriod(10000, 0);
        beaconManager.setForegroundScanPeriod(app.foregroundScanPeriod, 0);

        // Set listener
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, List<Beacon> beacons) {
                Log.d(TAG, "onBeaconsDiscovered: " + beacons.size());

                // Convert Estimote beacons to LayoutBeacon
                ArrayList<LayoutBeacon> convertedBeacons = new ArrayList<LayoutBeacon>(beacons.size());
                for (Beacon beacon : beacons) {
                    double distance = Utils.computeAccuracy(beacon);
                    convertedBeacons.add(new LayoutBeacon(beacon, distance));
                }

                try {
                    beaconLogger.logBeacons(beacons);
                } catch (IOException e) {
                    Log.e(TAG, "Failed to write beacons to file");
                    e.printStackTrace();
                }
                executor.addJob(convertedBeacons);
            }
        });

        connectBeaconService();
    }

    private void connectBeaconService() {
        // Reco
        recoBeaconManager.bind(new RECOServiceConnectListener() {
            @Override
            public void onServiceConnect() {
                try {
                    recoBeaconManager.startRangingBeaconsInRegion(new RECOBeaconRegion(RECO_PROXIMITY_UUID, "reco"));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceFail(RECOErrorCode recoErrorCode) {

            }
        });

        // Connect to BeaconService
        //beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
        //    @Override
        //    public void onServiceReady() {
        //        // Start monitoring
        //        try {
        //            beaconManager.startRanging(ALL_ESTIMOTE_BEACONS);
        //        } catch (RemoteException e) {
        //            e.printStackTrace();
        //        }
        //    }
        //});
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Stop monitoring
        try {
            // Reco
            recoBeaconManager.unbind();

            beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS);
            beaconManager.disconnect();

            beaconLogger.stopLogger();
            RestClient.uploadBeaconLog(beaconLogger);

            SocketIOClient.disconnect();
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
