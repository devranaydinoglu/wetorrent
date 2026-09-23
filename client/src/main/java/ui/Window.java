package ui;

import base.Session;

import javax.swing.*;
import java.awt.*;

public class Window {

    private final Session session;

    public Window(Session session) {
        this.session = session;
    }

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

        session.setErrorListener(((msg, cause) -> {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                    frame,
                    msg + "\n" + cause.getMessage(),
                    "Torrent Error",
                    JOptionPane.ERROR_MESSAGE
                );
            });
        }));

        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int width = gd.getDisplayMode().getWidth() / 2;
        int height = gd.getDisplayMode().getHeight() / 2;
        Dimension windowDimensions = new Dimension(width, height);
        frame.setPreferredSize(windowDimensions);

        TopPanel topPanel = new TopPanel(session);
        frame.getContentPane().add(topPanel.getContentPane());

        // Display the window
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

}
