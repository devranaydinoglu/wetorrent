package ui.torrent;

import torrent.MultiFileInfo;
import torrent.Piece;
import torrent.Torrent;
import torrent.TorrentInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private File selectedTorrentSaveLocation;
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

        JButton torrentSaveLocationBtn = new JButton("Torrent Save Location");
        torrentSaveLocationBtn.addActionListener(this::selectTorrentSaveLocation);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        formPanel.add(torrentSaveLocationBtn, gbc);

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
            selectedTorrentSaveLocation = fileChooser.getSelectedFile();
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

        if (!selectedFile.exists()) {
            JOptionPane.showMessageDialog(
                dialog,
                "Please select a file to torrent.",
                "Missing File",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        confirmed = true;

        TorrentInfo torrentInfo;
        if (selectedFile.isDirectory()) {
            List<MultiFileInfo> multiFileInfoList = new ArrayList<>();
            getSubFilesRecursively(multiFileInfoList, selectedFile, selectedFile.getName());

            torrentInfo = new TorrentInfo(
                selectedFile.getName(),
                Piece.PIECE_LENGTH,
                "".getBytes(StandardCharsets.UTF_8), // TODO: calculate byte string
                multiFileInfoList
            );
        } else {
            torrentInfo = new TorrentInfo(
                selectedFile.getName(),
                Piece.PIECE_LENGTH,
                Piece.getByteString(selectedFile),
                selectedFile.length()
            );
        }

        torrent = new Torrent(trackerUrlField.getText(), torrentInfo, Instant.now());

        byte[] bencodedTorrent = torrent.encode();
        File bencodedTorrentFile = new File(
            selectedTorrentSaveLocation.getParent(),
            selectedTorrentSaveLocation.getName() + ".torrent"
        );

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(bencodedTorrentFile);
            fos.write(bencodedTorrent);
            fos.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            if(fos != null) {
                try {
                    fos.close();
                } catch(IOException ignore) { }
            }
        }

        dialog.dispose();
    }

    public void cancelTorrentFile(ActionEvent e) {
        confirmed = false;
        dialog.dispose();
    }

    private void getSubFilesRecursively(List<MultiFileInfo> seenMultiFiles, File currentFile, String rootDir) {
        if (!currentFile.isDirectory()) {
            if (currentFile.isHidden())
                return;

            String relPath = currentFile.getPath();
            int relPathStart = relPath.lastIndexOf(rootDir);
            String[] splitPath = relPath.substring(relPathStart).split("/");

            MultiFileInfo multiFile = new MultiFileInfo(
                currentFile.length(),
                Arrays.stream(splitPath).toList()
            );
            seenMultiFiles.add(multiFile);

            return;
        }

        for (File f : currentFile.listFiles()) {
            getSubFilesRecursively(seenMultiFiles, f, rootDir);
        }
    }
}
