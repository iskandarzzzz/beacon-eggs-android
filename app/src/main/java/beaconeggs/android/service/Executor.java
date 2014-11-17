package beaconeggs.android.service;

import android.util.Log;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.Utils;
import com.google.common.collect.EvictingQueue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

    // key: beacon minor
    // value: history of distances
    private HashMap<Integer, EvictingQueue<Double>> distanceHistory;

    public Executor(ResolutionSelector resolutionSelector, App app) {
        this.resolutionSelector = resolutionSelector;
        this.app = app;

        queue = new LinkedList<List<Beacon>>();

        distanceHistory = new HashMap<Integer, EvictingQueue<Double>>();
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

                HashMap<Integer, Double> distancesToSend = new HashMap<Integer, Double>();
                for (Map.Entry<Integer, EvictingQueue<Double>> evictingQueueEntry : distanceHistory.entrySet()) {
                    Integer key = evictingQueueEntry.getKey();
                    double avg = computeAvgDistance(key);

                    distancesToSend.put(key, avg);
                }

                // send position to server
                RestClient.postPosition(computedPoint, distancesToSend);
            }

        }
        isExecuting = false;
    }

    private List<LayoutBeacon> processBeacons(List<Beacon> beacons) {
        List<LayoutBeacon> layoutBeacons = new ArrayList<LayoutBeacon>(beacons.size());

        String distancesString = "";
        String avgDistancesString = "";
        for (Beacon beacon : beacons) {
            double distance = Utils.computeAccuracy(beacon);
            addDistanceToHistory(beacon.getMinor(), distance);

            double avgDistance = computeAvgDistance(beacon.getMinor());

            // prepare distance string
            distancesString += beacon.getMinor() + " distance:" + distance + "\n";
            avgDistancesString += beacon.getMinor() + " avgDistance:" + avgDistance + "\n";

            LayoutBeacon layoutBeacon = makeLayoutBeacon(beacon, avgDistance);
            if (layoutBeacon != null)
                layoutBeacons.add(layoutBeacon);
        }

        // show distances on activity
        executorListener.onDistance(distancesString);
        executorListener.onProcessedDistance(avgDistancesString);

        return layoutBeacons;
    }

    private double computeAvgDistance(int minor) {
        EvictingQueue<Double> distances = distanceHistory.get(minor);

        double avgDistance = 0;
        for (Double distance : distances) {
            avgDistance += distance;
        }
        avgDistance /= distances.size();

        return avgDistance;
    }

    /**
     * Put the new estimate distance into a list
     * replacing the oldest value if the list is full
     *
     * @param minor
     * @param distance
     */
    private void addDistanceToHistory(int minor, double distance) {
        boolean containsKey = distanceHistory.containsKey(minor);

        if (!containsKey) {
            EvictingQueue<Double> distances = EvictingQueue.create(10);
            distanceHistory.put(minor, distances);
        }

        distanceHistory.get(minor).add(distance);
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