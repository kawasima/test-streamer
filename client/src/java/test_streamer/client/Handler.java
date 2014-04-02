package test_streamer.client;

import com.ning.http.client.websocket.WebSocket;
import us.bpsm.edn.Keyword;

import java.util.Map;

/**
 * @author kawasima
 */
public interface Handler {
    public void handle(Map<Keyword, Object> msg, WebSocket websocket);
    public void dispose();
}
