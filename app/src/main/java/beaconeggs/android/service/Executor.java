package beaconeggs.android.service;

import android.util.Log;

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

    public void addJob(List<LayoutBeacon> beacons) {
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
        List<LayoutBeacon> beacons = beaconHistory.getBeacons(app.filterMethod);

        List<LayoutBeacon> layoutBeacons = filterBeacons(beacons);
        ResolutionType type = resolutionSelector.selectType(layoutBeacons);

        if (type != null) {
            ComputedPoint computedPoint = type.compute();

            resolutionSelector.addComputedPoint(computedPoint);
            if (executorListener != null) {
                executorListener.onExecute(computedPoint);
            }

            HashMap<Integer, Double> distancesToSend = new HashMap<Integer, Double>();
            for (LayoutBeacon layoutBeacon : layoutBeacons) {
                distancesToSend.put(layoutBeacon.getMinor(), layoutBeacon.getDistance());
            }

            // send position to server
            SocketIOClient.sendComputedPosition(computedPoint, distancesToSend);
        }

        isExecuting = false;
    }

    private List<LayoutBeacon> filterBeacons(List<LayoutBeacon> beacons) {
        List<LayoutBeacon> layoutBeacons = new ArrayList<LayoutBeacon>(beacons.size());
        List<LayoutBeacon> mobileBeacons = new ArrayList<LayoutBeacon>(beacons.size());

        String distancesString = "";
        for (LayoutBeacon beacon : beacons) {
            LayoutBeacon mergedBeacon = mergeWithEditorBeacon(beacon);
            if (mergedBeacon != null) {
                layoutBeacons.add(mergedBeacon);

                // prepare distance string
                distancesString += beacon.getMinor() + " distance:" + mergedBeacon.getDistance() + "\n";
            } else {
                mobileBeacons.add(beacon);

            }
        }

        //
        if (resolutionSelector.getLastComputedPoint() != null)
            SocketIOClient.sendMobileBeacons(resolutionSelector.getLastComputedPoint(), mobileBeacons);

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
     * @return
     */
    private LayoutBeacon mergeWithEditorBeacon(LayoutBeacon beacon) {
        LayoutBeacon layoutBeacon = null;

        for (EditorWidget editorWidget : editorWidgets) {
            boolean isBeacon = editorWidget instanceof EditorBeacon;

            if (isBeacon) {
                EditorBeacon editorBeacon = (EditorBeacon) editorWidget;
                String uuid = editorBeacon.getUuid();
                int major = editorBeacon.getMajor();
                int minor = editorBeacon.getMinor();

                boolean sameBeacon = (beacon.getUuid().equalsIgnoreCase(uuid) && beacon.getMajor() == major && beacon.getMinor() == minor);
                if (sameBeacon) {
                    layoutBeacon = new LayoutBeacon(beacon, editorBeacon.getPos(app.editorLayout.getPxPerMeter()), editorBeacon.getRadius() / app.editorLayout.getPxPerMeter());
                }
            }
        }

        return layoutBeacon;
    }
}