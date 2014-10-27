package beaconeggs.android.editorModel;

/**
 * Created by william on 18/10/14.
 */
public class EditorBeacon extends EditorWidget {
    private String uuid;
    private int major;
    private int minor;
    private double radius;

    public String getUuid() {
        return uuid;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public double getRadius() {
        return radius;
    }
}
