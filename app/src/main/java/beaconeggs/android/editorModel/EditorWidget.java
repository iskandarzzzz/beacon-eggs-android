package beaconeggs.android.editorModel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import beaconeggs.core.Point;

/**
 * Created by william on 18/10/14.
 */
public abstract class EditorWidget {
    private String type;
    private double x;
    private double y;

    public String getType() {
        return type;
    }

    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this).toString();
    }
//    public String toString() {
//        StringBuilder sb = new StringBuilder();
//        sb.append('\n')
//                .append("type: ").append(type).append('\t')
//                .append("x: ").append(x).append('\t')
//                .append("y: ").append(y).append('\t');
//        return sb.toString();
//    }

    public Point getPos() {
        return new Point(x, y);
    }
}
