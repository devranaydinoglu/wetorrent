package ui;

import ui.torrent.NewTorrentDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class TopPanel {

    private final JPanel contentPane;
    private File torrentFile;

    public TopPanel() {
        contentPane = new JPanel();

        FlowLayout flowLayout = new FlowLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        contentPane.setLayout(flowLayout);

        JButton newTorrentBtn = new JButton("New");
        newTorrentBtn.addActionListener(this::showNewTorrent);
        contentPane.add(newTorrentBtn);

        JButton downloadTorrentBtn = new JButton("Download");
        downloadTorrentBtn.addActionListener(this::downloadTorrent);
        contentPane.add(downloadTorrentBtn);
    }

    public JPanel getContentPane() {
        return contentPane;
    }

    public void showNewTorrent(ActionEvent e) {
        NewTorrentDialog newTorrentDialog = new NewTorrentDialog(SwingUtilities.getWindowAncestor(contentPane));
        newTorrentDialog.showDialog();
    }

    public void downloadTorrent(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        int returnVal = fileChooser.showOpenDialog(contentPane);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            torrentFile = fileChooser.getSelectedFile();
            System.out.println("File selected: " + torrentFile.getName());
        }
    }
}
