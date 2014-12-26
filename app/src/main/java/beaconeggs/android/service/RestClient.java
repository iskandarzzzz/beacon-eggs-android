package beaconeggs.android.service;

import android.util.Log;

import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;

import beaconeggs.core.Point;

/**
 * Created by william on 18/10/14.
 */
public class RestClient {
    private static final String TAG = "RestClient";
    private static final String BASE_URL = "http://beacon.egg.ovh/api";

    private static final AsyncHttpClient client = new AsyncHttpClient();
    private static final Gson gson = new Gson();

    public static void getBeacons(AsyncHttpResponseHandler handler) {
        client.get(BASE_URL + "/beacon", handler);
    }

    public static void getLayouts(AsyncHttpResponseHandler handler) {
        client.get(BASE_URL + "/layout", handler);
    }

    public static void postPosition(Point point, HashMap<Integer, Double> distancesToSend) {
        RequestParams params = new RequestParams();
        params.put("x", point.getX());
        params.put("y", point.getY());
        params.put("distances", gson.toJson(distancesToSend));

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

    public static void uploadBeaconLog(BeaconLogger logger) {
        File logfile = logger.getFile();
        RequestParams params = new RequestParams();

        try {
            params.put("logfile", logfile);

            client.post(BASE_URL + "/log", params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    Log.d(TAG, "logfile upload success");
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    Log.d(TAG, "logfile upload failure");
                }
            });
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFound: " + logger.getFile());
        }
    }
}
