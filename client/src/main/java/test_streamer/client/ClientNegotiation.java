package test_streamer.client;

import io.undertow.websockets.WebSocketExtension;
import io.undertow.websockets.client.WebSocketClientNegotiation;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.HandshakeResponse;
import java.util.*;

/**
 * @author kawasima
 */
public class ClientNegotiation extends WebSocketClientNegotiation {

    private final ClientEndpointConfig config;

    ClientNegotiation(List<String> supportedSubProtocols, List<WebSocketExtension> supportedExtensions, ClientEndpointConfig config) {
        super(supportedSubProtocols, supportedExtensions);
        this.config = config;
    }

    @Override
    public void afterRequest(final Map<String, List<String>> headers) {

        ClientEndpointConfig.Configurator configurator = config.getConfigurator();
        if (configurator != null) {
            final Map<String, List<String>> newHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                ArrayList<String> arrayList = new ArrayList<>();
                arrayList.addAll(entry.getValue());
                newHeaders.put(entry.getKey(), arrayList);
            }
            configurator.afterResponse(new HandshakeResponse() {
                @Override
                public Map<String, List<String>> getHeaders() {
                    return newHeaders;
                }
            });
        }
    }

    @Override
    public void beforeRequest(Map<String, List<String>> headers) {
        ClientEndpointConfig.Configurator configurator = config.getConfigurator();
        if (configurator != null) {
            final Map<String, List<String>> newHeaders = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                ArrayList<String> arrayList = new ArrayList<>();
                arrayList.addAll(entry.getValue());
                newHeaders.put(entry.getKey(), arrayList);
            }
            configurator.beforeRequest(newHeaders);
            headers.clear(); //TODO: more efficient way
            for (Map.Entry<String, List<String>> entry : newHeaders.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    headers.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

}
