package beaconeggs.android.service;

import beaconeggs.core.ComputedPoint;

/**
 * Created by william on 18/10/14.
 */
public interface ExecutorListener {
    public void onExecute(ComputedPoint computedPoint);

    public void onDistance(String msg);

    public void onProcessedDistance(String msg);
}
