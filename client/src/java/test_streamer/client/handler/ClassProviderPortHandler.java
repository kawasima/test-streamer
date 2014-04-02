package test_streamer.client.handler;

import com.ning.http.client.websocket.WebSocket;
import test_streamer.client.ClientConfig;
import test_streamer.client.Handler;
import test_streamer.client.WebSocketUtil;
import test_streamer.client.dto.ReadyCommand;
import us.bpsm.edn.Keyword;

import java.util.Map;

/**
 * @author kawasima
 */
public class ClassProviderPortHandler implements Handler {
    private ClientConfig config;
    public ClassProviderPortHandler(ClientConfig config) {
        this.config = config;
    }
    @Override
    public void handle(Map<Keyword, Object> msg, WebSocket websocket) {
        config.setString(ClientConfig.ClientConfigKey.CLASS_PROVIDER_URL,
                new StringBuilder(256)
                        .append("ws://")
                        .append(config.getString(ClientConfig.ClientConfigKey.SERVER_HOST))
                        .append(":")
                        .append(msg.get(Keyword.newKeyword("port")))
                        .toString());
        WebSocketUtil.send(websocket, new ReadyCommand());
    }

    @Override
    public void dispose() {

    }
}
