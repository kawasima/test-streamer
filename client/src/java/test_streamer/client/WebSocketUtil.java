package test_streamer.client;

import com.ning.http.client.websocket.WebSocket;
import us.bpsm.edn.printer.Printer;
import us.bpsm.edn.printer.Printers;
import us.bpsm.edn.protocols.Protocol;

import java.io.StringWriter;

/**
 * @author kawasima
 */
public class WebSocketUtil {
    public static void send(WebSocket websocket, Object msg) {
        if(msg instanceof String) {
            websocket.sendTextMessage((String)msg);
        } else {
            final Protocol<Printer.Fn<?>> protocol = Printers
                    .prettyProtocolBuilder()
                    .put(Object.class,
                            new BeanPrinterFn()).build();
            StringWriter sw = new StringWriter();
            Printers.newPrinter(protocol, sw)
                    .printValue(msg);
            sw.flush();
            send(websocket, sw.toString());

        }
    }
}
