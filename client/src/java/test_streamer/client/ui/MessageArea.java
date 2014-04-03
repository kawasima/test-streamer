package test_streamer.client.ui;

import javax.swing.*;
import java.awt.*;

/**
 * @author kawasima
 */
public class MessageArea extends JTextArea {
    public MessageArea() {
        super();
        setBackground(new Color(253, 246, 227));
        setAutoscrolls(true);
        setEditable(false);
    }
}
