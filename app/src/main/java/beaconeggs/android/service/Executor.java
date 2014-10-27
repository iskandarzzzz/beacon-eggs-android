package beaconeggs.android.service;

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
    private ExecutorListener executorListener;

    public Executor(ResolutionSelector resolutionSelector) {
        this.resolutionSelector = resolutionSelector;

        queue = new LinkedList<List<Beacon>>();
    }

    public void setExecutorListener(ExecutorListener executorListener) {
        this.executorListener = executorListener;
    }

    public void addJob(List<Beacon> beacons) {
        queue.add(beacons);
        execute();
    }

    private void execute() {
        if (isExecuting)
            return;

        isExecuting = true;
        while (!queue.isEmpty()) {
            List<Beacon> beacons = queue.poll();

            List<LayoutBeacon> layoutBeacons = processBeacons(beacons);
            ResolutionType type = resolutionSelector.selectType(layoutBeacons);
            ComputedPoint computedPoint = type.compute();

            resolutionSelector.addComputedPoint(computedPoint);
            if (executorListener != null) {
                executorListener.onExecute(computedPoint);
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
        List<EditorWidget> editorWidgets = App.editorLayout.getEditorWidgets();
        LayoutBeacon layoutBeacon = null;

        for (EditorWidget editorWidget : editorWidgets) {
            boolean isBeacon = editorWidget instanceof EditorBeacon;

            if (isBeacon) {
                EditorBeacon editorBeacon = (EditorBeacon) editorWidget;
                String uuid = editorBeacon.getUuid();
                int major = editorBeacon.getMajor();
                int minor = editorBeacon.getMinor();

                boolean sameBeacon = (uuid == beacon.getProximityUUID() && major == beacon.getMajor() && minor == beacon.getMinor());
                if (sameBeacon) {
                    double distance = Utils.computeAccuracy(beacon);

                    layoutBeacon = new LayoutBeacon(beacon, editorBeacon.getPos(), editorBeacon.getRadius(), distance);
                }
            }
        }

        return layoutBeacon;
    }
}