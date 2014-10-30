package beaconeggs.android.service;

import android.util.Log;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.Utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import beaconeggs.android.App;
import beaconeggs.android.editorModel.EditorBeacon;
import beaconeggs.android.editorModel.EditorWidget;
import beaconeggs.core.ComputedPoint;
import beaconeggs.core.ResolutionType;

class Executor {
    private Queue<List<Beacon>> queue;
    private boolean isExecuting = false;
    private ResolutionSelector resolutionSelector;
    private App app;
    private ExecutorListener executorListener;
    private List<EditorWidget> editorWidgets;

    public Executor(ResolutionSelector resolutionSelector, App app) {
        this.resolutionSelector = resolutionSelector;
        this.app = app;

        queue = new LinkedList<List<Beacon>>();
    }

    public void setExecutorListener(ExecutorListener executorListener) {
        this.executorListener = executorListener;
    }

    public void addJob(List<Beacon> beacons) {
        if (beacons.isEmpty())
            return;

        queue.add(beacons);

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
        while (!queue.isEmpty()) {
            List<Beacon> beacons = queue.poll();

            List<LayoutBeacon> layoutBeacons = processBeacons(beacons);
            ResolutionType type = resolutionSelector.selectType(layoutBeacons);

            if (type != null) {
                ComputedPoint computedPoint = type.compute();
                Log.d("+++++++++++++++++++", "computedPoint: " + computedPoint);

                resolutionSelector.addComputedPoint(computedPoint);
                if (executorListener != null) {
                    executorListener.onExecute(computedPoint);
                }
            }

        }
        isExecuting = false;
    }

    private List<LayoutBeacon> processBeacons(List<Beacon> beacons) {
        List<LayoutBeacon> layoutBeacons = new ArrayList<LayoutBeacon>(beacons.size());

        for (Beacon beacon : beacons) {
            LayoutBeacon layoutBeacon = makeLayoutBeacon(beacon);
            if (layoutBeacon != null)
                layoutBeacons.add(layoutBeacon);
        }

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
    private LayoutBeacon makeLayoutBeacon(Beacon beacon) {
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
                    double distance = Utils.computeAccuracy(beacon);
                    Log.d("==================", "distance:" + distance);
                    layoutBeacon = new LayoutBeacon(beacon, editorBeacon.getPos(), editorBeacon.getRadius(), distance);
                }
            }
        }

        return layoutBeacon;
    }
}