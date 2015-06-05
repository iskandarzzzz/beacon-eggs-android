package beaconeggs.android;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.SharedPreferences;

import java.util.List;
import java.util.UUID;

import beaconeggs.android.editorModel.EditorLayout;
import beaconeggs.android.service.BeaconHistory;
import beaconeggs.android.service.BeaconMonitorService;

/**
 * Created by william on 18/10/14.
 */
public class App extends Application {

    private static SharedPreferences pref;
    // Retrieve from server
    public List<EditorLayout> editorLayouts;
    // Set by respective Spinner
    public EditorLayout editorLayout;
    public BeaconHistory.FilterMethod filterMethod;
    // Set by NumberPicker
    public long foregroundScanPeriod = BeaconMonitorService.DEFAULT_FOREGROUND_SCAN_PERIOD;

    @Override
    public void onCreate() {
        super.onCreate();
        pref = getSharedPreferences("pref", MODE_PRIVATE);
    }

    // Helper method
    static boolean hasBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        return bluetoothAdapter != null;
    }

    public static String getUserId() {
        String userId = pref.getString("userId", null);
        if (userId == null) {
            userId = UUID.randomUUID().toString();
            pref.edit().putString("userId", userId);
        }

        return userId;
    }
}
