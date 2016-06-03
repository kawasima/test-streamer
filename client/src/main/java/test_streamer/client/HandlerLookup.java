package test_streamer.client;

import us.bpsm.edn.Keyword;

import java.util.HashMap;
import java.util.Map;

/**
 * @author kawasima
 */
public class HandlerLookup {
    private final Map<Keyword, Handler> handlers = new HashMap<Keyword, Handler>();

    public Handler getHandler(Keyword command) {
        return handlers.get(command);
    }

    public void registerHandler(Keyword command, Handler handler) {
        handlers.put(command, handler);
    }

    public void dispose() {
        for (Map.Entry<Keyword, Handler> entry : handlers.entrySet()) {
            entry.getValue().dispose();
        }

    }
}
