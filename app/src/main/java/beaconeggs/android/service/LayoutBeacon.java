package beaconeggs.android.service;

import com.estimote.sdk.Beacon;

import beaconeggs.core.Point;

class LayoutBeacon {

    // Estimote Beacon
    private final String uuid;
    private final int major;
    private final int minor;
    private final String name;
    private final String macAddress;
    private final int measuredPower;
    private final int rssi;
    // EditorBeacon
    private final Point pos;
    private final double radius;
    // Computed
    private final double distance;

    public LayoutBeacon(Beacon beacon, Point pos, double radius, double distance) {
        uuid = beacon.getProximityUUID();
        major = beacon.getMajor();
        minor = beacon.getMinor();
        name = beacon.getName();
        macAddress = beacon.getMacAddress();
        measuredPower = beacon.getMeasuredPower();
        rssi = beacon.getRssi();
        this.pos = pos;
        this.radius = radius;
        this.distance = distance;
    }

    public double getDistance() {
        return distance;
    }

    public Point getPos() {
        return pos;
    }

    public String getUuid() {
        return uuid;
    }

    public double getRadius() {
        return radius;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('\n')
                .append("radius").append('\t').append(radius).append('\n')
                .append("uuid").append('\t').append(uuid).append('\n')
                .append("distance").append('\t').append(distance).append('\n')
                .append("pos").append('\t').append(pos).append('\n');

        return sb.toString();
    }
}