package ui.torrent;

import torrent.*;

import javax.swing.*;
import java.awt.*;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;

public class NewTorrentDialog {

    Window parent;
    private final JDialog dialog;
    JPanel contentPane;
    JPanel formPanel;
    GridBagConstraints gbc;
    private final JTextField trackerUrlField;
    SelectedFileInfoPanel selectedFileInfoPanel;

    private boolean confirmed = false;
    private File selectedFile;
    private File torrentDestination;
    Torrent torrent;

    public NewTorrentDialog(Window parent) {
        this.parent = parent;
        dialog = new JDialog(parent, "Create New Torrent", Dialog.ModalityType.APPLICATION_MODAL);

        contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout(10, 10));
        contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Form
        formPanel = new JPanel();
        formPanel.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.0;

        JLabel trackerLabel = new JLabel("Tracker URL:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        formPanel.add(trackerLabel, gbc);

        trackerUrlField = new JTextField();
        trackerUrlField.setColumns(15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        formPanel.add(trackerUrlField, gbc);

        JButton selectFileBtn = new JButton("Select File/Directory");
        selectFileBtn.addActionListener(this::selectFile);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        formPanel.add(selectFileBtn, gbc);
        gbc.anchor = GridBagConstraints.CENTER;

        JButton torrentDestinationBtn = new JButton("Torrent Destination");
        torrentDestinationBtn.addActionListener(this::selectTorrentSaveLocation);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        formPanel.add(torrentDestinationBtn, gbc);

        contentPane.add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this::cancelTorrentFile);
        buttonPanel.add(cancelButton);

        JButton createButton = new JButton("Create");
        createButton.addActionListener(this::createTorrent);
        buttonPanel.add(createButton);

        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPane);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
    }

    public void showDialog() {
        dialog.setVisible(true);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getTrackerUrl() {
        return trackerUrlField.getText().trim();
    }

    public void selectFile(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        int returnVal = fileChooser.showOpenDialog(contentPane);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();

            if (selectedFileInfoPanel != null)
                formPanel.remove(selectedFileInfoPanel.getContentPane());

            selectedFileInfoPanel = new SelectedFileInfoPanel(selectedFile);
            gbc.gridx = 1;
            gbc.gridy = 1;
            gbc.gridwidth = 2;
            gbc.weightx = 1.0;
            formPanel.add(selectedFileInfoPanel.getContentPane(), gbc);
            formPanel.revalidate();
            formPanel.repaint();

            dialog.pack();
            dialog.setLocationRelativeTo(parent);
        }
    }

    public void selectTorrentSaveLocation(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        int returnVal = fileChooser.showSaveDialog(contentPane);

        if (returnVal == JFileChooser.APPROVE_OPTION)
            torrentDestination = fileChooser.getSelectedFile();
    }

    public void createTorrent(ActionEvent e) {
        if (trackerUrlField.getText().isBlank()) {
            JOptionPane.showMessageDialog(
                dialog,
                "Please enter a tracker URL.",
                "Missing Tracker URL",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (selectedFile == null) {
            JOptionPane.showMessageDialog(
                dialog,
                "Please select a file to torrent.",
                "Missing File",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (torrentDestination == null) {
            JOptionPane.showMessageDialog(
                dialog,
                "Please select a destination for the torrent.",
                "Missing Destination",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (!selectedFile.exists()) {
            JOptionPane.showMessageDialog(
                dialog,
                "Selected file doesn't exist.",
                "File Doesn't Exist",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        TorrentCreator torrentCreator = new TorrentCreator();
        Torrent torrent = torrentCreator.create(selectedFile, trackerUrlField.getText());

        TorrentWriter torrentWriter = new TorrentWriter();
        torrentWriter.write(torrent, torrentDestination);

        confirmed = true;
        dialog.dispose();
    }

    public void cancelTorrentFile(ActionEvent e) {
        confirmed = false;
        dialog.dispose();
    }
}
