package test_streamer.client;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.websocket.WebSocket;
import com.ning.http.client.websocket.WebSocketTextListener;
import com.ning.http.client.websocket.WebSocketUpgradeHandler;
import test_streamer.client.handler.ClassProviderPortHandler;
import test_streamer.client.handler.DoTestHandler;
import test_streamer.client.ui.TrayNotification;
import us.bpsm.edn.Keyword;
import us.bpsm.edn.parser.Parseable;
import us.bpsm.edn.parser.Parser;
import us.bpsm.edn.parser.Parsers;

import java.net.URI;
import java.security.*;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static test_streamer.client.ClientConfig.ClientConfigKey.SERVER_HOST;
import static test_streamer.client.ClientConfig.ClientConfigKey.UI;
/**
 * @author kawasima
 */
public class Main {
    private AsyncHttpClient client;
    private Parser ednParser = Parsers.newParser(Parsers.defaultConfiguration());
    private final HandlerLookup handlerLookup = new HandlerLookup();
    private ClientUI ui;
    private ClientConfig config;
    private boolean isTerminate = false;

    public Main(ClientConfig config) {
        this.config = config;
        handlerLookup.registerHandler(
                Keyword.newKeyword("do-test"),
                new DoTestHandler(config));
        handlerLookup.registerHandler(
                Keyword.newKeyword("class-provider-port"),
                new ClassProviderPortHandler(config));
    }
    public Main() {
        this(new ClientConfig());
    }

    public WebSocket connect(final String testServerUrl) {
        client = new AsyncHttpClient();
        WebSocket websocket;
        while(true) {
            try {
                websocket = client.prepareGet(testServerUrl)
                        .execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(
                                new WebSocketTextListener() {
                                    private WebSocket connection;
                                    @Override
                                    public void onMessage(String message) {
                                        Parseable psb = Parsers.newParseable(new String(message));
                                        Map<Keyword, Object> command = (Map)ednParser.nextValue (psb);
                                        Handler handler = handlerLookup.getHandler((Keyword)command.get(Keyword.newKeyword("command")));
                                        handler.handle(command, connection);
                                    }

                                    @Override
                                    public void onFragment(String fragment, boolean last) {
                                        throw new UnsupportedOperationException("onFragment");
                                    }

                                    @Override
                                    public void onOpen(WebSocket websocket) {
                                        connection = websocket;
                                        ui.standby();
                                        WebSocketUtil.send(websocket, "{:command :class-provider-port}");
                                    }

                                    @Override
                                    public void onClose(WebSocket websocket) {
                                        if (websocket.isOpen()) {
                                            websocket.close();

                                        }
                                        ui.disconnect();
                                        connection = null;

                                        if (!isTerminate)
                                            connect(testServerUrl);
                                    }

                                    @Override
                                    public void onError(Throwable t) {
                                        t.printStackTrace();
                                    }
                                }
                        ).build()).get(5000, TimeUnit.MILLISECONDS);
                break;
            } catch(Exception ex) {
                try {
                    Thread.sleep(5000);
                } catch(InterruptedException ignore) {}
            }
        }
        return websocket;
    }

    public void start(String testServerUrl) {
        try {
            ui = (ClientUI) config.getObject(UI);
            if (ui == null) {
                ui = new TrayNotification();
                config.setObject(UI, ui);
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException();
        }

        config.setString(SERVER_HOST, URI.create(testServerUrl).getHost());
        connect(testServerUrl);
        final Main self = this;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void start() {
                self.destroy();
            }
        });

    }

    public void destroy() {
        isTerminate = true;
        if (client != null && !client.isClosed())
            client.close();
        handlerLookup.dispose();
    }



    public static void main(String[] args) {
        Main main = new Main();
        System.setSecurityManager(null);
        Policy.setPolicy(new Policy() {
            @Override
            public PermissionCollection getPermissions(CodeSource codesource) {
                Permissions p = new Permissions();
                p.add(new AllPermission());
                return p;
            }
        });
        main.start("ws://localhost:5000/join");
    }
}
