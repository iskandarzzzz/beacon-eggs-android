package beaconeggs.android;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;

import java.util.List;

import beaconeggs.android.editorModel.EditorLayout;
import beaconeggs.android.service.BeaconHistory;
import beaconeggs.android.service.BeaconMonitorService;

/**
 * Created by william on 18/10/14.
 */
public class App extends Application {

    // Retrieve from server
    public List<EditorLayout> editorLayouts;

    // Set by respective Spinner
    public EditorLayout editorLayout;
    public BeaconHistory.FilterMethod filterMethod;

    // Set by NumberPicker
    public long foregroundScanPeriod = BeaconMonitorService.DEFAULT_FOREGROUND_SCAN_PERIOD;

    // Helper method
    static boolean hasBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        return bluetoothAdapter != null;
    }
}
