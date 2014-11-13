package beaconeggs.android;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by william on 13/11/14.
 */
public class BaseActivity extends Activity {

    protected App app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app = (App) getApplication();
    }
}
