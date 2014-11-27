package beaconeggs.android.service;

import com.estimote.sdk.Beacon;
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

    private EvictingQueue<Beacon> beaconHistory = EvictingQueue.create(queueSize);
    private HashMap<String, Long> lastSeenStamp = new HashMap<String, Long>();

    public BeaconHistory() {

    }

    public void addBeacons(List<Beacon> beacons) {
        beaconHistory.addAll(beacons);

        // Keep track of last seen timestamp
        long timestamp = new Date().getTime();
        for (Beacon beacon : beacons) {
            lastSeenStamp.put(getBeaconId(beacon), timestamp);
        }
    }

    /**
     * Transform global list beaconHistory into per beacon map
     * beacons must not have timed out
     *
     * @return
     */
    private HashMap<String, ArrayList<Beacon>> getBeaconMap() {
        HashMap<String, ArrayList<Beacon>> beaconMap = new HashMap<String, ArrayList<Beacon>>();

        List<String> validBeaconIds = getValidBeaconIds();

        for (Beacon b : beaconHistory) {
            String key = getBeaconId(b);

            // beacon has not timeout
            if (validBeaconIds.contains(key)) {
                // create beacon list dynamically
                if (!beaconMap.containsKey(key)) {
                    beaconMap.put(key, new ArrayList<Beacon>());
                }
                ArrayList<Beacon> beaconList = beaconMap.get(key);

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
    private static String getBeaconId(Beacon b) {
        return b.getProximityUUID() + '|' + b.getMajor() + '|' + b.getMinor();
    }

    /**
     * Applies FilterMethod m to per beacon lists and returns list of LayoutBeacons
     *
     * @param m
     * @return
     */
    public List<Beacon> getBeacons(FilterMethod m) {
        HashMap<String, ArrayList<Beacon>> beaconMap = getBeaconMap();
        List<Beacon> beacons = new ArrayList<Beacon>();

        for (ArrayList<Beacon> sameBeacons : beaconMap.values()) {
            Method method;
            try {
                method = this.getClass().getDeclaredMethod("compute" + m.toString(), List.class);
                Beacon filteredBeacon = (Beacon) method.invoke(this, sameBeacons);
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

    private Beacon computeAverage(List<Beacon> list) {

        int avgRssi = 0;
        int avgMeasuredPower = 0;
        for (Beacon beacon : list) {
            avgRssi += beacon.getRssi();
            avgMeasuredPower += beacon.getMeasuredPower();
        }
        avgRssi /= list.size();
        avgMeasuredPower /= list.size();

        Beacon beacon = list.get(0);

        return new Beacon(beacon.getProximityUUID(), beacon.getName(), beacon.getMacAddress(), beacon.getMajor(), beacon.getMinor(), avgMeasuredPower, avgRssi);
    }

    private Beacon computeWAverage(List<Beacon> list) {
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

        Beacon beacon = list.get(0);

        return new Beacon(beacon.getProximityUUID(), beacon.getName(), beacon.getMacAddress(), beacon.getMajor(), beacon.getMinor(), avgMeasuredPower, avgRssi);
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
    private Beacon computeExpWAverage(List<Beacon> list) {

        double alpha = 2 / (list.size() + 1);

        int avgRssi;
        int avgMeasuredPower;

        avgRssi = (int) expWAverageRssi(list, alpha);
        avgMeasuredPower = (int) expWAverageMeasuredPower(list, alpha);

        Beacon beacon = list.get(0);

        return new Beacon(beacon.getProximityUUID(), beacon.getName(), beacon.getMacAddress(), beacon.getMajor(), beacon.getMinor(), avgMeasuredPower, avgRssi);
    }

    private double expWAverageMeasuredPower(List<Beacon> list, double alpha) {
        if (list.size() < 2) {
            // The initial value by default is the last element of the list
            return list.get(0).getMeasuredPower();
        } else {
            // We remove latest beacon of the list
            Beacon head = list.remove(list.size());

            // And we start recursion
            return alpha * head.getMeasuredPower() + (1 - alpha) * expWAverageMeasuredPower(list, alpha);
        }
    }

    private double expWAverageRssi(List<Beacon> list, double alpha) {
        if (list.size() < 2) {
            // The initial value by default is the last element of the list
            return list.get(0).getRssi();
        } else {
            // We remove latest beacon of the list
            Beacon head = list.remove(list.size());

            // And we start recursion
            return alpha * head.getRssi() + (1 - alpha) * expWAverageRssi(list, alpha);
        }
    }

    private Beacon computeMedian(List<Beacon> list) {

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

        Beacon beacon = list.get(0);

        return new Beacon(beacon.getProximityUUID(), beacon.getName(), beacon.getMacAddress(), beacon.getMajor(), beacon.getMinor(), medianMeasuredPower, medianRssi);
    }
}
