package beaconeggs.android.service;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import beaconeggs.core.Point;

/**
 * Created by william on 07/01/15.
 */
public class SocketIOClient {

    private static Socket socket;

    public static void connect() {
        try {
            socket = IO.socket("http://beacon.egg.ovh");
            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void disconnect() {
        socket.disconnect();
    }

    public static void sendComputedPosition(Point point, HashMap<Integer, Double> distancesToSend) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("x", point.getX());
            obj.put("y", point.getY());

            JSONObject distances = new JSONObject();
            for (Map.Entry<Integer, Double> integerDoubleEntry : distancesToSend.entrySet()) {
                distances.put(integerDoubleEntry.getKey().toString(), integerDoubleEntry.getValue());
            }
            obj.put("distances", distances);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        emit("computedPosition", obj);
    }

    private static void emit(String event, JSONObject object) {
        if (socket.connected()) socket.emit(event, object);
    }
}
