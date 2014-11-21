package beaconeggs.android.service;

import com.estimote.sdk.Beacon;
import com.google.common.collect.EvictingQueue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Thomaspiotrowski on 11/20/14.
 */
public class BeaconHistory {

    private int queueSize = 500;

    public boolean isEmpty() {
        return beaconHistory.isEmpty();
    }

    public enum FilterMethod {
        Average,
        ExpWAverage,
        WAverage,
        Median;
    }

    private EvictingQueue<Beacon> beaconHistory = EvictingQueue.create(queueSize);

    public BeaconHistory() {

    }

    public void addBeacon(List<Beacon> b) {
        beaconHistory.addAll(b);
    }

    /**
     * Transform global list beaconHistory into per beacon map
     *
     * @return
     */
    private HashMap<String, ArrayList<Beacon>> getBeaconMap() {

        HashMap<String, ArrayList<Beacon>> beaconMap = new HashMap<String, ArrayList<Beacon>>();

        for (Beacon b : beaconHistory) {

            String key = b.getProximityUUID() + '|' + b.getMajor() + '|' + b.getMinor();

            if (!beaconMap.containsKey(key)) {
                beaconMap.put(key, new ArrayList<Beacon>());
            }
            ArrayList<Beacon> beaconList = beaconMap.get(key);
            beaconList.add(b);
        }

        return beaconMap;
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

    private Beacon computeExpWAverage(List<Beacon> list) {

        return null;
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
