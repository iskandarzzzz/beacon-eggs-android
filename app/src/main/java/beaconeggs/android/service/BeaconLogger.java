package beaconeggs.android.service;

import android.content.Context;
import android.util.Log;

import com.estimote.sdk.Beacon;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import beaconeggs.core.ComputedPoint;

/**
 * Created by Thomaspiotrowski on 12/18/14.
 */
public class BeaconLogger {
    private static final String TAG = "+++++++BeaconLogger";
    public static final String FILENAME = "Beacon.log";
    private File log;
    private OutputStream out;

    public BeaconLogger(Context context) {

        log = new File(context.getExternalFilesDir(null), FILENAME);

        try {
            out = new FileOutputStream(log);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void logBeacons(List<Beacon> beacons) throws IOException {

        byte[] data;

        long timestamp = new Date().getTime();
        byte[] prefix = (Long.toString(timestamp) + '|').getBytes();

        for (Beacon beacon : beacons) {
            data = beacon.toString().getBytes();
            Log.d(TAG, beacon.toString());
            out.write(prefix);
            out.write(data);
            out.write((byte) 10);
        }

    }

    public void logLayoutBeacon(List<LayoutBeacon> layoutBeacons) throws IOException {

        byte[] data;

        long timestamp = new Date().getTime();
        byte[] prefix = (Long.toString(timestamp) + '|').getBytes();

        for (LayoutBeacon layoutBeacon : layoutBeacons) {
            data = layoutBeacon.toString().getBytes();
            Log.d(TAG, layoutBeacon.toString());
            out.write(data);
        }
    }

    public void logComputedPoint(List<ComputedPoint> computedPoints) throws IOException {
        byte[] data;

        long timestamp = new Date().getTime();
        byte[] prefix = (Long.toString(timestamp) + '|').getBytes();
        
        for (ComputedPoint computedPoint : computedPoints) {
            data = computedPoint.toString().getBytes();
            Log.d(TAG, computedPoint.toString());
            out.write(data);
        }
    }

    public void stopLogger() {
        if (out != null) {
            try {
                out.flush();
                out.close();
            } catch (IOException e) {
                Log.e(TAG, "Error while closing Beacons.log");
            }
        }
    }
}
