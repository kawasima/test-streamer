package test_streamer.client;

import us.bpsm.edn.Keyword;

import javax.websocket.Session;
import java.util.Map;

/**
 * @author kawasima
 */
public interface Handler {
    public void handle(Map<Keyword, Object> msg, Session session);
    public void dispose();
}
