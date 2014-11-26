package beaconeggs.android;

import android.content.Context;
import android.widget.ArrayAdapter;

import java.util.List;

/**
 * Created by william on 13/11/14.
 */
public class LayoutsSpinnerAdapter extends ArrayAdapter {
    public LayoutsSpinnerAdapter(Context context, List objects) {
        super(context, android.R.layout.simple_spinner_dropdown_item, objects);
    }
}
