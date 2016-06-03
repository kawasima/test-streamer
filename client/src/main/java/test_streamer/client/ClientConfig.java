package test_streamer.client;

import java.util.HashMap;

/**
 * @author kawasima
 */
public class ClientConfig {
    private HashMap<ClientConfigKey, Object> config = new HashMap<ClientConfigKey, Object>();

    public ClientConfig() {
        //config.put(ClientConfigKey.UI, "test_streamer.client.ui.TrayNotification");
    }

    public String getString(ClientConfigKey key) {
        return config.get(key).toString();
    }

    public Object getObject(ClientConfigKey key) {
        return config.get(key);
    }

    public void setString(ClientConfigKey key, String value) {
        config.put(key, value);
    }

    public void setObject(ClientConfigKey key, Object value) {
        config.put(key, value);
    }

    public enum ClientConfigKey {
        CLASS_PROVIDER_URL,
        SERVER_HOST,
        UI
    }
}
