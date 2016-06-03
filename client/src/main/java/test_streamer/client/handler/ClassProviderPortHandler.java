package test_streamer.client.handler;

import test_streamer.client.ClientConfig;
import test_streamer.client.Handler;
import test_streamer.client.WebSocketUtil;
import test_streamer.client.dto.ReadyCommand;
import us.bpsm.edn.Keyword;

import javax.websocket.Session;
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
    public void handle(Map<Keyword, Object> msg, Session session) {
        config.setString(ClientConfig.ClientConfigKey.CLASS_PROVIDER_URL,
                new StringBuilder(256)
                        .append("ws://")
                        .append(config.getString(ClientConfig.ClientConfigKey.SERVER_HOST))
                        .append(":")
                        .append(msg.get(Keyword.newKeyword("port")))
                        .toString());
        WebSocketUtil.send(session, new ReadyCommand());
    }

    @Override
    public void dispose() {

    }
}
