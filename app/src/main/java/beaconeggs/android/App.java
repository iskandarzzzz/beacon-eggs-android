package beaconeggs.android;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;

import java.util.List;

import beaconeggs.android.editorModel.EditorLayout;
import beaconeggs.android.service.BeaconMonitorService;

/**
 * Created by william on 18/10/14.
 */
public class App extends Application {

    public EditorLayout editorLayout;

    public List<EditorLayout> editorLayouts;
    public long foregroundScanPeriod = BeaconMonitorService.DEFAULT_FOREGROUND_SCAN_PERIOD;

    static boolean hasBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        return bluetoothAdapter != null;
    }
}
