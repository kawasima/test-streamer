package test_streamer.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test_streamer.client.dto.ReadyCommand;
import test_streamer.client.event.ConnectEvent;
import test_streamer.client.handler.ClassProviderPortHandler;
import test_streamer.client.handler.DoTestHandler;
import test_streamer.client.ui.PanelNotification;
import test_streamer.client.ui.TrayNotification;
import us.bpsm.edn.Keyword;
import us.bpsm.edn.parser.Parseable;
import us.bpsm.edn.parser.Parser;
import us.bpsm.edn.parser.Parsers;

import javax.websocket.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.*;
import java.util.*;
import java.util.List;

import static test_streamer.client.ClientConfig.ClientConfigKey.CLASS_PROVIDER_URL;
import static test_streamer.client.ClientConfig.ClientConfigKey.SERVER_HOST;

/**
 * Starts a client application.
 *
 * @author kawasima
 */
public class Main extends Application {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private Session session;
    private Parser ednParser = Parsers.newParser(Parsers.defaultConfiguration());
    private final HandlerLookup handlerLookup = new HandlerLookup();
    private List<ClientUI> uiList;
    private ClientConfig config;
    private boolean isTerminate = false;

    @Override
    public void init() {
        uiList = new ArrayList<>();
        this.config = new ClientConfig();
        handlerLookup.registerHandler(
                Keyword.newKeyword("do-test"),
                new DoTestHandler(config, uiList));
        handlerLookup.registerHandler(
                Keyword.newKeyword("class-provider-port"),
                new ClassProviderPortHandler(config));
    }

    public Session connect(final String testServerUrl) throws IOException, DeploymentException {
        URI uri = URI.create(testServerUrl);
        WebSocketContainer wsContainer = ContainerProvider.getWebSocketContainer();
        ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
        session = wsContainer.connectToServer(new Endpoint() {
            @Override
            public void onOpen(final Session session, EndpointConfig endpointConfig) {
                session.addMessageHandler(String.class, message -> {
                    Parseable psb = Parsers.newParseable(new String(message));
                    Map<Keyword, Object> command = (Map)ednParser.nextValue (psb);
                    Handler handler = handlerLookup.getHandler((Keyword)command.get(Keyword.newKeyword("command")));
                    handler.handle(command, session);
                });
                uiList.forEach(ClientUI::standby);
                String serverHost = config.getString(SERVER_HOST);
                config.setObject(CLASS_PROVIDER_URL,
                        "ws://" + serverHost + "/wscl");
                WebSocketUtil.send(session, new ReadyCommand());
            }

            @Override
            public void onClose(final Session session, CloseReason closeReason) {
                uiList.forEach(ClientUI::disconnect);
            }
        }, cec, uri);

        return session;
    }

    public void start(Stage stage, String testServerUrl) throws IOException, DeploymentException {
        try {
            if (SystemTray.isSupported()) {
                uiList.add(new TrayNotification());
            }
            PanelNotification panelUi = new PanelNotification();
            uiList.add(panelUi);
            Scene scene = new Scene(panelUi);
            scene.getStylesheets().add("css/client.css");
            stage.setOnCloseRequest(event -> {
                Platform.runLater(() -> destroy());
                stage.close();
            });
            stage.setScene(scene);
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }

        URI serverUri = URI.create(testServerUrl);
        int port = serverUri.getPort();
        config.setString(SERVER_HOST, serverUri.getHost() + (port > 0 ? ":" + port : "")
                + Optional.ofNullable(serverUri.getPath()).orElse(""));
        uiList.forEach(ui -> ui.addEventHandler(ConnectEvent.CONNECT_SERVER, e -> {
            try {
                connect(testServerUrl + "/join");
            } catch (Exception ex) {
                ui.disconnect();
            }
        }));
        try {
            connect(testServerUrl);
        } catch (Exception ex) {
            uiList.forEach(ClientUI::disconnect);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Platform.runLater(() -> {
                destroy();
                stage.close();
            });
        }));
    }

    public void destroy() {
        isTerminate = true;
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (IOException ignore) {

            }
        }
        handlerLookup.dispose();

    }

    protected String getTestStreamerUrl() {
        Properties props = new Properties();
        InputStream is = getClass().getResourceAsStream("/app.properties");
        if (is == null) return null;
        try {
            props.load(is);
            return props.getProperty("teststreamer.url");
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        Policy.setPolicy(new Policy() {
            @Override
            public PermissionCollection getPermissions(CodeSource codesource) {
                Permissions p = new Permissions();
                p.add(new AllPermission());
                return p;
            }
        });
        Platform.setImplicitExit(false);

        String testStreamerUrl = Optional.ofNullable(getTestStreamerUrl())
                .orElse("ws://localhost:5000");
        LOG.info("teststreamer url = {}", testStreamerUrl);
        start(stage, testStreamerUrl);
        stage.show();
    }

    public static void main(String[] args) throws IOException, DeploymentException {
        launch(args);
    }
}
