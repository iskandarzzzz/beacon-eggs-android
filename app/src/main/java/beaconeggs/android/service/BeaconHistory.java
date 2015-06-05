package beaconeggs.android.service;

import com.google.common.collect.EvictingQueue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Thomaspiotrowski on 11/20/14.
 */
public class BeaconHistory {

    private static final long DEFAULT_BEACON_TIMEOUT = 30 * 1000;
    private int queueSize = 500;

    public boolean isEmpty() {
        return beaconHistory.isEmpty();
    }

    public enum FilterMethod {
        Average,
        ExpWAverage,
        WAverage,
        Median
    }

    private EvictingQueue<LayoutBeacon> beaconHistory = EvictingQueue.create(queueSize);
    private HashMap<String, Long> lastSeenStamp = new HashMap<String, Long>();

    public BeaconHistory() {

    }

    public void addBeacons(List<LayoutBeacon> beacons) {
        beaconHistory.addAll(beacons);

        // Keep track of last seen timestamp
        long timestamp = new Date().getTime();
        for (LayoutBeacon beacon : beacons) {
            lastSeenStamp.put(getBeaconId(beacon), timestamp);
        }
    }

    /**
     * Transform global list beaconHistory into per beacon map
     * beacons must not have timed out
     *
     * @return
     */
    private HashMap<String, ArrayList<LayoutBeacon>> getBeaconMap() {
        HashMap<String, ArrayList<LayoutBeacon>> beaconMap = new HashMap<String, ArrayList<LayoutBeacon>>();

        List<String> validBeaconIds = getValidBeaconIds();

        for (LayoutBeacon b : beaconHistory) {
            String key = getBeaconId(b);

            // beacon has not timeout
            if (validBeaconIds.contains(key)) {
                // create beacon list dynamically
                if (!beaconMap.containsKey(key)) {
                    beaconMap.put(key, new ArrayList<LayoutBeacon>());
                }
                ArrayList<LayoutBeacon> beaconList = beaconMap.get(key);

                beaconList.add(b);
            }
        }

        return beaconMap;
    }

    /**
     * Build a list of BeaconIds of beacons that are still alive
     *
     * @retur
     */
    private List<String> getValidBeaconIds() {
        ArrayList<String> ids = new ArrayList<String>();
        long timestamp = new Date().getTime();
        for (Map.Entry<String, Long> entry : lastSeenStamp.entrySet()) {
            if (timestamp - entry.getValue() <= DEFAULT_BEACON_TIMEOUT) {
                ids.add(entry.getKey());
            }
        }
        return ids;
    }


    /**
     * Helper method to build the key identify a beacon with its UUID, major and minor
     *
     * @param b
     * @return
     */
    private static String getBeaconId(LayoutBeacon b) {
        return b.getUuid() + '|' + b.getMajor() + '|' + b.getMinor();
    }

    /**
     * Applies FilterMethod m to per beacon lists and returns list of LayoutBeacons
     *
     * @param m
     * @return
     */
    public List<LayoutBeacon> getBeacons(FilterMethod m) {
        HashMap<String, ArrayList<LayoutBeacon>> beaconMap = getBeaconMap();
        List<LayoutBeacon> beacons = new ArrayList<LayoutBeacon>();

        for (ArrayList<LayoutBeacon> sameBeacons : beaconMap.values()) {
            Method method;
            try {
                method = this.getClass().getDeclaredMethod("compute" + m.toString(), List.class);
                LayoutBeacon filteredBeacon = (LayoutBeacon) method.invoke(this, sameBeacons);
                beacons.add(filteredBeacon);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return beacons;
    }

    private LayoutBeacon computeAverage(List<LayoutBeacon> list) {

        int avgRssi = 0;
        int avgMeasuredPower = 0;
        for (LayoutBeacon beacon : list) {
            avgRssi += beacon.getRssi();
            avgMeasuredPower += beacon.getMeasuredPower();
        }
        avgRssi /= list.size();
        avgMeasuredPower /= list.size();

        LayoutBeacon beacon = list.get(0);

        return new LayoutBeacon(beacon, avgMeasuredPower, avgRssi);
    }

    private LayoutBeacon computeWAverage(List<LayoutBeacon> list) {
        int avgRssi = 0;
        int avgMeasuredPower = 0;
        int denominator = 0;

        for (int i = 1; i <= list.size(); i++) {
            avgRssi += i * list.get(i - 1).getRssi();
            avgMeasuredPower += i * list.get(i - 1).getMeasuredPower();
            denominator += i;
        }

        avgRssi /= denominator;
        avgMeasuredPower /= denominator;

        LayoutBeacon beacon = list.get(0);

        return new LayoutBeacon(beacon, avgMeasuredPower, avgRssi);
    }

    /*
        The simplest form of exp. moving average is given by
        S(t) = α * X(t-1) + (1-α) * S(t-1)
        where S(n) is a calculated exp. moving average
        and X(n) is data retrieved in the list
        and α is a weighted coefficient  0 < α < 1.

        There is no formally correct procedure for choosing α.
        Some will take 2/(Period + 1).

        Since at this point we will already have a list
        filtered by beacon, Period = List size
    */
    private LayoutBeacon computeExpWAverage(List<LayoutBeacon> list) {
        double beta = 1 - (2 / (list.size() + 1));
        double coefficient;

        int avgRssi = 0;
        int avgMeasuredPower = 0;
        double denominator = 0;

        List<LayoutBeacon> list_rssi = new ArrayList<LayoutBeacon>(list);
        List<LayoutBeacon> list_measuredPower = new ArrayList<LayoutBeacon>(list);

        int n = list.size() - 1;
        for (int i = 0; i < list.size(); i++) {
            coefficient = Math.pow(beta, n);
            avgRssi += coefficient * list.get(i).getRssi();
            avgMeasuredPower += coefficient * list.get(i).getMeasuredPower();
            denominator += coefficient;
            n--;
        }

        avgRssi /= denominator;
        avgMeasuredPower /= denominator;

        //avgRssi = (int) expWAverageRssi(list_rssi, alpha);
        //avgMeasuredPower = (int) expWAverageMeasuredPower(list_measuredPower, alpha);

        LayoutBeacon beacon = list.get(0);

        return new LayoutBeacon(beacon, avgMeasuredPower, avgRssi);
    }

    private double expWAverageMeasuredPower(List<LayoutBeacon> list, double alpha) {
        if (list.size() < 2) {
            // The initial value by default is the last element of the list
            return list.get(0).getMeasuredPower();
        } else {
            // We remove latest beacon of the list
            LayoutBeacon head = list.remove(list.size() - 1);

            // And we start recursion
            return alpha * head.getMeasuredPower() + (1 - alpha) * expWAverageMeasuredPower(list, alpha);
        }
    }

    private double expWAverageRssi(List<LayoutBeacon> list, double alpha) {
        if (list.size() < 2) {
            // The initial value by default is the last element of the list
            return list.get(0).getRssi();
        } else {
            // We remove latest beacon of the list
            LayoutBeacon head = list.remove(list.size() - 1);

            // And we start recursion
            return alpha * head.getRssi() + (1 - alpha) * expWAverageRssi(list, alpha);
        }
    }

    private LayoutBeacon computeMedian(List<LayoutBeacon> list) {

        int medianRssi;
        int medianMeasuredPower;
        int[] tabRssi = new int[list.size()];
        int[] tabMeasuredPower = new int[list.size()];

        for (int i = 0; i < tabRssi.length; i++) {
            tabRssi[i] = list.get(i).getRssi();
            tabMeasuredPower[i] = list.get(i).getMeasuredPower();
        }

        Arrays.sort(tabRssi);
        Arrays.sort(tabMeasuredPower);

        int size = list.size();
        int lowerBound = (size / 2) - 1;
        if (list.size() % 2 == 0) {
            int upperBound = lowerBound + 1;
            medianRssi = (tabRssi[lowerBound] + tabRssi[upperBound]) / 2;
            medianMeasuredPower = (tabMeasuredPower[lowerBound] + tabMeasuredPower[upperBound]) / 2;
        } else {
            medianRssi = tabRssi[lowerBound];
            medianMeasuredPower = tabMeasuredPower[lowerBound];
        }

        LayoutBeacon beacon = list.get(0);

        return new LayoutBeacon(beacon, medianMeasuredPower, medianRssi);
    }
}
