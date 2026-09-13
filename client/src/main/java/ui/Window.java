package ui;

import javax.swing.*;
import java.awt.*;

public class Window {

    public void start() {
        // Schedule a job for the event-dispatching thread:
        // creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGui();
            }
        });
    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private void createAndShowGui() {
        JFrame frame = new JFrame("weTorrent");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int width = gd.getDisplayMode().getWidth();
        int height = gd.getDisplayMode().getHeight();
        Dimension windowDimensions = new Dimension(width, height);
        frame.setPreferredSize(windowDimensions);

        TopPanel topPanel = new TopPanel();
        frame.getContentPane().add(topPanel.getContentPane());

        // Display the window
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

}
