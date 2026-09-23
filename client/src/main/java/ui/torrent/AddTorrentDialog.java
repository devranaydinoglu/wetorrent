package ui.torrent;

import base.Session;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.concurrent.ExecutionException;

public class AddTorrentDialog {

    private Window window;
    private final Session session;

    private final JDialog dialog;
    private JPanel contentPane;
    private JPanel formPanel;
    private GridBagConstraints gbc;

    private File selectedTorrent;
    private File torrentDestination;

    public AddTorrentDialog(Window window, Session session) {
        this.window = window;
        this.session = session;
        dialog = new JDialog(window, "Create New Torrent", Dialog.ModalityType.APPLICATION_MODAL);

        contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout(10, 10));
        contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Form
        formPanel = new JPanel();
        formPanel.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.0;

        JButton selectTorrentBtn = new JButton("Select Torrent");
        selectTorrentBtn.addActionListener(this::selectTorrent);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        formPanel.add(selectTorrentBtn, gbc);
        gbc.anchor = GridBagConstraints.CENTER;

        JButton downloadDestinationBtn = new JButton("Download Destination");
        downloadDestinationBtn.addActionListener(this::selectDownloadDestination);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        formPanel.add(downloadDestinationBtn, gbc);

        contentPane.add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(this::cancelTorrentFile);
        buttonPanel.add(cancelBtn);

        JButton addBtn = new JButton("Add");
        addBtn.addActionListener(this::addTorrent);
        buttonPanel.add(addBtn);

        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPane);
        dialog.pack();
        dialog.setLocationRelativeTo(window);
    }

    public void showDialog() {
        dialog.setVisible(true);
    }

    public void selectTorrent(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        int returnVal = fileChooser.showOpenDialog(contentPane);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String fileName = fileChooser.getSelectedFile().getName();
            int lastIndexOf = fileName.lastIndexOf(".");

            if (lastIndexOf == -1) {
                JOptionPane.showMessageDialog(
                    fileChooser,
                    "Couldn't process file.",
                    "File Processing Failed",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            String fileExtension = fileName.substring(lastIndexOf + 1);
            if (!fileExtension.equals("torrent")) {
                JOptionPane.showMessageDialog(
                    fileChooser,
                    "Selected file isn't a .torrent.",
                    "File Not a Torrent",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            selectedTorrent = fileChooser.getSelectedFile();
        }
    }

    public void selectDownloadDestination(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        int returnVal = fileChooser.showDialog(contentPane, "Select");

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selected = fileChooser.getSelectedFile();
            torrentDestination = selected != null && selected.isDirectory()
                ? selected
                : fileChooser.getCurrentDirectory();
        }
    }

    public void addTorrent(ActionEvent e) {
        if (selectedTorrent == null) {
            JOptionPane.showMessageDialog(
                dialog,
                "Please select a torrent.",
                "Missing Torrent",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (torrentDestination == null) {
            JOptionPane.showMessageDialog(
                dialog,
                "Please select a download destination.",
                "Missing Destination",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (!selectedTorrent.exists()) {
            JOptionPane.showMessageDialog(
                dialog,
                "Selected torrent doesn't exist.",
                "Torrent Doesn't Exist",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        SwingWorker<Void, Void> torrentWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                session.addTorrent(selectedTorrent, torrentDestination.toPath());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    System.out.println("Torrent added");
                    // TODO: Update UI

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    JOptionPane.showMessageDialog(
                        window,
                        "Couldn't process torrent:\n" + ex.getCause().getMessage(),
                        "Torrent Processing Failed",
                        JOptionPane.WARNING_MESSAGE
                    );
                }
            }
        };
        torrentWorker.execute();

        dialog.dispose();
    }

    public void cancelTorrentFile(ActionEvent e) {
        dialog.dispose();
    }
}
