package test_streamer.client;

import us.bpsm.edn.printer.Printer;
import us.bpsm.edn.printer.Printers;
import us.bpsm.edn.protocols.Protocol;

import javax.websocket.Session;
import java.io.StringWriter;

/**
 * @author kawasima
 */
public class WebSocketUtil {
    public static void send(Session session, Object msg) {
        if(msg instanceof String) {
            session.getAsyncRemote().sendText((String) msg);
        } else {
            final Protocol<Printer.Fn<?>> protocol = Printers
                    .prettyProtocolBuilder()
                    .put(Object.class,
                            new BeanPrinterFn()).build();
            StringWriter sw = new StringWriter();
            Printers.newPrinter(protocol, sw)
                    .printValue(msg);
            sw.flush();
            send(session, sw.toString());
        }
    }
}
