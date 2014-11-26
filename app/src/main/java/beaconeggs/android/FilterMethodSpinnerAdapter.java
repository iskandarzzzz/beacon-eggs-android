package beaconeggs.android;

import android.content.Context;
import android.widget.ArrayAdapter;

import beaconeggs.android.service.BeaconHistory;

/**
 * Created by william on 26/11/14.
 */
public class FilterMethodSpinnerAdapter extends ArrayAdapter {
    public FilterMethodSpinnerAdapter(Context context) {
        super(context, android.R.layout.simple_spinner_dropdown_item, BeaconHistory.FilterMethod.values());
    }
}
