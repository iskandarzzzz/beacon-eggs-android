package beaconeggs.android.editorModel;

import java.util.List;

/**
 * Created by william on 18/10/14.
 */
public class EditorLayout {

    private String name;
    private List<EditorWidget> widgets;

    public String getName() {
        return name;
    }

    public List<EditorWidget> getEditorWidgets() {
        return widgets;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('\n')
                .append("name").append('\t').append(name).append('\n')
                .append("wdgets").append('\t').append(widgets).append('\n');
        return sb.toString();
    }
}
