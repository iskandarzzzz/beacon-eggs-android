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

    public LayoutBeacon(String uuid, int major, int minor, String name, String macAddress, int measuredPower, int rssi, Point pos, double radius, double distance) {
        this.uuid = uuid;
        this.major = major;
        this.minor = minor;
        this.name = name;
        this.macAddress = macAddress;
        this.measuredPower = measuredPower;
        this.rssi = rssi;
        this.pos = pos;
        this.radius = radius;
        this.distance = distance;
    }

    public LayoutBeacon(Beacon beacon, double distance) {
        this(beacon.getProximityUUID(), beacon.getMajor(), beacon.getMinor(), beacon.getName(), beacon.getMacAddress(), beacon.getMeasuredPower(), beacon.getRssi(), null, 0, distance);
    }

    public LayoutBeacon(LayoutBeacon beacon, int measuredPower, int rssi) {
        this(beacon.getUuid(), beacon.getMajor(), beacon.getMinor(), beacon.getName(), beacon.getMacAddress(), measuredPower, rssi, beacon.getPos(), beacon.getRadius(), beacon.getDistance());
    }

    public LayoutBeacon(LayoutBeacon beacon, Point pos, double radius) {
        this(beacon.getUuid(), beacon.getMajor(), beacon.getMinor(), beacon.getName(), beacon.getMacAddress(), beacon.getMeasuredPower(), beacon.getRssi(), pos, radius, beacon.getDistance());
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

    public int getMajor() {
        return major;
    }

    public String getName() {
        return name;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public int getMeasuredPower() {
        return measuredPower;
    }

    public int getRssi() {
        return rssi;
    }

    @Override
    public String toString() {
        return "LayoutBeacon{" +
                "uuid='" + uuid + '\'' +
                ", major=" + major +
                ", minor=" + minor +
                ", name='" + name + '\'' +
                ", macAddress='" + macAddress + '\'' +
                ", measuredPower=" + measuredPower +
                ", rssi=" + rssi +
                ", pos=" + pos +
                ", radius=" + radius +
                ", distance=" + distance +
                '}';
    }

    public int getMinor() {
        return minor;
    }
}