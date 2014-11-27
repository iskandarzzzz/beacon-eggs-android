package beaconeggs.android.service;

import android.util.Log;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import beaconeggs.android.App;
import beaconeggs.android.editorModel.EditorBeacon;
import beaconeggs.android.editorModel.EditorWidget;
import beaconeggs.core.ComputedPoint;
import beaconeggs.core.ResolutionType;

class Executor {
    private BeaconHistory beaconHistory;
    private boolean isExecuting = false;
    private ResolutionSelector resolutionSelector;
    private App app;
    private ExecutorListener executorListener;
    private List<EditorWidget> editorWidgets;

    public Executor(ResolutionSelector resolutionSelector, App app) {
        this.resolutionSelector = resolutionSelector;
        this.app = app;

        beaconHistory = new BeaconHistory();
    }

    public void setExecutorListener(ExecutorListener executorListener) {
        this.executorListener = executorListener;
    }

    public void addJob(List<Beacon> beacons) {
        if (beacons.isEmpty())
            return;

        beaconHistory.addBeacons(beacons);

        // only execute when we have retrieved editor data
        if (editorWidgets == null) {
            editorWidgets = app.editorLayout.getEditorWidgets();
            if (editorWidgets != null) {
                execute();
            }
        } else {
            execute();
        }
    }

    private void execute() {
        if (isExecuting)
            return;

        isExecuting = true;
        List<Beacon> beacons = beaconHistory.getBeacons(app.filterMethod);

        List<LayoutBeacon> layoutBeacons = processBeacons(beacons);
        ResolutionType type = resolutionSelector.selectType(layoutBeacons);

        if (type != null) {
            ComputedPoint computedPoint = type.compute();
            Log.d("+++++++++++++++++++", "computedPoint: " + computedPoint);

            resolutionSelector.addComputedPoint(computedPoint);
            if (executorListener != null) {
                executorListener.onExecute(computedPoint);
            }

            HashMap<Integer, Double> distancesToSend = new HashMap<Integer, Double>();
            for (LayoutBeacon layoutBeacon : layoutBeacons) {
                distancesToSend.put(layoutBeacon.getMinor(), layoutBeacon.getDistance());
            }

            // send position to server
            RestClient.postPosition(computedPoint, distancesToSend);
        }

        isExecuting = false;
    }

    private List<LayoutBeacon> processBeacons(List<Beacon> beacons) {
        List<LayoutBeacon> layoutBeacons = new ArrayList<LayoutBeacon>(beacons.size());

        String distancesString = "";
        for (Beacon beacon : beacons) {
            double distance = Utils.computeAccuracy(beacon);

            // prepare distance string
            distancesString += beacon.getMinor() + " distance:" + distance + "\n";

            LayoutBeacon layoutBeacon = makeLayoutBeacon(beacon, distance);
            if (layoutBeacon != null)
                layoutBeacons.add(layoutBeacon);
        }

        // show distances on activity
        executorListener.onDistance(distancesString);

        return layoutBeacons;
    }

    /**
     * Create a new instance of LayoutBeacon from a given Beacon
     * if it exists in Layout
     * else null
     *
     * @param beacon
     * @param distance
     * @return
     */
    private LayoutBeacon makeLayoutBeacon(Beacon beacon, double distance) {
        LayoutBeacon layoutBeacon = null;

        for (EditorWidget editorWidget : editorWidgets) {
            boolean isBeacon = editorWidget instanceof EditorBeacon;

            if (isBeacon) {
                EditorBeacon editorBeacon = (EditorBeacon) editorWidget;
                String uuid = editorBeacon.getUuid();
                int major = editorBeacon.getMajor();
                int minor = editorBeacon.getMinor();

                boolean sameBeacon = (beacon.getProximityUUID().equalsIgnoreCase(uuid) && beacon.getMajor() == major && beacon.getMinor() == minor);
                if (sameBeacon) {
//                    Log.d("==================", "distance:" + distance);
                    layoutBeacon = new LayoutBeacon(beacon, editorBeacon.getPos(app.editorLayout.getPxPerMeter()), editorBeacon.getRadius() / app.editorLayout.getPxPerMeter(), distance);
                }
            }
        }

        return layoutBeacon;
    }
}