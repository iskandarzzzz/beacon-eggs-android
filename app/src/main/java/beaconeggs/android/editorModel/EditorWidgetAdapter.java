package beaconeggs.android.editorModel;

import android.util.Log;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * Created by william on 18/10/14.
 */
public class EditorWidgetAdapter implements JsonDeserializer<EditorWidget> {
    private static final String TAG = "EditorWidgetAdapter";

    @Override
    public EditorWidget deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String type = json.getAsJsonObject().get("type").getAsString();

        try {
            Class<?> c = Class.forName("beaconeggs.android.editorModel.Editor" + type);
            return context.deserialize(json, c);
        } catch (ClassNotFoundException e) {
            Log.w(TAG, e.toString());
        }

        return null;
    }
}
