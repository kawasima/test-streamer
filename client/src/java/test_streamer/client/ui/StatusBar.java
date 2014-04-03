package test_streamer.client.ui;

import javax.swing.*;
import java.awt.*;

/**
 * @author kawasima
 */
public class StatusBar extends JLabel {
    public StatusBar(String text){
        super(text);
    }

    public void setText(String text, Color color) {
        setText(text);
        setForeground(color);
    }
}
