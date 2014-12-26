package beaconeggs.android.editorModel;

import java.util.List;

/**
 * Created by william on 18/10/14.
 */
public class EditorLayout {

    private String name;
    private List<EditorWidget> widgets;
    private int pxPerMeter;

    public String getName() {
        return name;
    }

    public List<EditorWidget> getEditorWidgets() {
        return widgets;
    }

    public String toString() {
        return name;
    }

    public int getPxPerMeter() {
        return pxPerMeter;
    }
}
