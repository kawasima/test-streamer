package test_streamer.client;

import org.junit.Test;
import test_streamer.client.dto.TestCaseResult;
import test_streamer.client.dto.TestSuiteResult;
import us.bpsm.edn.printer.Printer;
import us.bpsm.edn.printer.Printers;
import us.bpsm.edn.protocols.Protocol;

import java.io.PrintWriter;

/**
 * @author kawasima
 */
public class EdnTest {
    @Test
    public void toEdnTestCaseResult() {
        final Protocol<Printer.Fn<?>> protocol = Printers
                .defaultProtocolBuilder()
                .put(Object.class,
                        new BeanPrinterFn()).build();
        PrintWriter pw = new PrintWriter(System.out);
        Printers.newPrinter(protocol, pw)
                .printValue(new TestCaseResult());
        pw.flush();
    }

    @Test
    public void toEdnTestSuiteResult() {
        final Protocol<Printer.Fn<?>> protocol = Printers
                .prettyProtocolBuilder()
                .put(Object.class,
                        new BeanPrinterFn()).build();
        PrintWriter pw = new PrintWriter(System.out);
        TestSuiteResult res = new TestSuiteResult("example.Hoge");
        res.getTestcases().add(new TestCaseResult());
        Printers.newPrinter(protocol, pw)
                .printValue(res);
        pw.flush();

    }
}
