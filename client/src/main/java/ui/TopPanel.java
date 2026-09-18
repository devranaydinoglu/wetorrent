package ui;

import bencode.Bencode;
import network.AnnounceRequest;
import network.TrackerClient;
import org.apache.commons.codec.digest.DigestUtils;
import torrent.Torrent;
import torrent.TorrentCreator;
import torrent.TorrentReader;
import ui.torrent.NewTorrentDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

public class TopPanel {

    private final Window window;
    private final JPanel contentPane;
    private File torrentFile;

    public TopPanel(Window window) {
        this.window = window;
        contentPane = new JPanel();

        FlowLayout flowLayout = new FlowLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        contentPane.setLayout(flowLayout);

        JButton newTorrentBtn = new JButton("New");
        newTorrentBtn.addActionListener(this::showNewTorrent);
        contentPane.add(newTorrentBtn);

        JButton downloadTorrentBtn = new JButton("Download");
        downloadTorrentBtn.addActionListener(this::selectTorrent);
        contentPane.add(downloadTorrentBtn);
    }

    public JPanel getContentPane() {
        return contentPane;
    }

    public void showNewTorrent(ActionEvent e) {
        NewTorrentDialog newTorrentDialog = new NewTorrentDialog(SwingUtilities.getWindowAncestor(contentPane));
        newTorrentDialog.showDialog();
    }

    public void selectTorrent(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        int returnVal = fileChooser.showOpenDialog(contentPane);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            torrentFile = fileChooser.getSelectedFile();
            String fileName = torrentFile.getName();
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

            TorrentReader reader = new TorrentReader();
            byte[] torrentFileBytes = reader.read(torrentFile);

            TorrentCreator creator = new TorrentCreator();
            Torrent torrent = creator.createFromBytes(torrentFileBytes);

            try {
                byte[] bencodedInfoDict = Bencode.encode(torrent.getTorrentInfo().toMap());

                AnnounceRequest announceReq = new AnnounceRequest(
                    DigestUtils.sha1(bencodedInfoDict),
                    window.getPeerId(),
                    6881,
                    0,
                    0,
                    torrent.getTorrentInfo().getLength(),
                    null,
                    null
                );

                TrackerClient client = new TrackerClient();

                try {
                    client.sendAnnounceRequest(torrent.getAnnounce(), announceReq);
                } catch (IOException | InterruptedException ex) {
                    JOptionPane.showMessageDialog(
                        fileChooser,
                        "Couldn't connect with server.",
                        "Server Connection Failed",
                        JOptionPane.WARNING_MESSAGE
                    );
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(
                    fileChooser,
                    "Couldn't process file.",
                    "File Processing Failed",
                    JOptionPane.WARNING_MESSAGE
                );
            }
        }
    }
}
