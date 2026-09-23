package ui;

import base.Session;
import ui.torrent.AddTorrentDialog;
import ui.torrent.NewTorrentDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class TopPanel {

    private final Session session;
    private final JPanel contentPane;

    public TopPanel(Session session) {
        this.session = session;
        contentPane = new JPanel();

        FlowLayout flowLayout = new FlowLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        contentPane.setLayout(flowLayout);

        JButton newTorrentBtn = new JButton("New");
        newTorrentBtn.addActionListener(this::showNewTorrent);
        contentPane.add(newTorrentBtn);

        JButton downloadTorrentBtn = new JButton("Download");
        downloadTorrentBtn.addActionListener(this::showAddTorrent);
        contentPane.add(downloadTorrentBtn);
    }

    public JPanel getContentPane() {
        return contentPane;
    }

    public void showNewTorrent(ActionEvent e) {
        NewTorrentDialog newTorrentDialog = new NewTorrentDialog(
            SwingUtilities.getWindowAncestor(contentPane),
            session
        );

        newTorrentDialog.showDialog();
    }

    public void showAddTorrent(ActionEvent e) {
        AddTorrentDialog addTorrentDialog = new AddTorrentDialog(
            SwingUtilities.getWindowAncestor(contentPane),
            session
        );

        addTorrentDialog.showDialog();
    }

}
