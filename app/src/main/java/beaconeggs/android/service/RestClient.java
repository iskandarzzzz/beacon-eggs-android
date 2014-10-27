package beaconeggs.android.service;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

/**
 * Created by william on 18/10/14.
 */
public class RestClient {
    private static final String BASE_URL = "http://beacon.egg.ovh/api";

    private static final AsyncHttpClient client = new AsyncHttpClient();

    public static void getBeacons(AsyncHttpResponseHandler handler) {
        client.get(BASE_URL + "/beacon", handler);
    }

    public static void getLayouts(AsyncHttpResponseHandler handler) {
        client.get(BASE_URL + "/layout", handler);
    }
}
