package beaconeggs.android.service;

import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;

import beaconeggs.core.Point;

/**
 * Created by william on 18/10/14.
 */
public class RestClient {
    private static final String TAG = "RestClient";
    private static final String BASE_URL = "http://beacon.egg.ovh/api";

    private static final AsyncHttpClient client = new AsyncHttpClient();

    public static void getBeacons(AsyncHttpResponseHandler handler) {
        client.get(BASE_URL + "/beacon", handler);
    }

    public static void getLayouts(AsyncHttpResponseHandler handler) {
        client.get(BASE_URL + "/layout", handler);
    }

    public static void postPosition(Point point) {
        RequestParams params = new RequestParams();
        params.put("x", point.getX());
        params.put("y", point.getY());

        client.post(BASE_URL + "/user", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.d(TAG, "onSuccess");
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d(TAG, "onFailure");
            }
        });
    }
}
