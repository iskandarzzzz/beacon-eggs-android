package beaconeggs.android.service;

import com.google.common.collect.EvictingQueue;

import java.util.ArrayList;
import java.util.List;

import beaconeggs.core.ComputedPoint;
import beaconeggs.core.Point;
import beaconeggs.core.ResolutionType;
import beaconeggs.core.ThreeBorderN;

class ResolutionSelector {

    private EvictingQueue<ComputedPoint> computedPoints = EvictingQueue.create(100);
    private Class<ResolutionType> forcedType = null;

    public ResolutionType selectType(List<LayoutBeacon> beacons) {
        // TODO: force return type
        if (forcedType != null) {
            return null;
        }

        if (beacons.size() >= 3) {
            return new ThreeBorderN(computePoints(beacons), computeDistances(beacons));
        }

        return null;
    }

    private List<Point> computePoints(List<LayoutBeacon> beacons) {
        List<Point> points = new ArrayList<Point>(beacons.size());

        for (LayoutBeacon beacon : beacons) {
            points.add(beacon.getPos());
        }

        return points;
    }

    private List<Double> computeDistances(List<LayoutBeacon> beacons) {
        List<Double> distances = new ArrayList<Double>(beacons.size());

        for (LayoutBeacon beacon : beacons) {
            distances.add(beacon.getDistance());
        }

        return distances;
    }

    public void addComputedPoint(ComputedPoint point) {
        computedPoints.add(point);
    }

    public void useType(Class<ResolutionType> c) {
        forcedType = c;
    }
}