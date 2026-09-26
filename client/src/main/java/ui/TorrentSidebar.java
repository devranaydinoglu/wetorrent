package ui;

import base.Session;
import base.TorrentListener;
import torrent.TorrentHandle;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class TorrentSidebar {

    private final JPanel contentPane;
    private final DefaultListModel<TorrentHandle> listModel = new DefaultListModel<>();
    private final JList<TorrentHandle> torrentList = new JList<>(listModel);

    public TorrentSidebar(Session session) {
        contentPane = new JPanel(new BorderLayout());

        torrentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        torrentList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus
            ) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(((TorrentHandle) value).getName());
                return this;
            }
        });

        contentPane.add(new JScrollPane(torrentList), BorderLayout.CENTER);

        // Listener callbacks come from background threads.
        session.setTorrentListener(new TorrentListener() {
            @Override
            public void onTorrentAdded(TorrentHandle handle) {
                SwingUtilities.invokeLater(() -> listModel.addElement(handle));
            }

            @Override
            public void onTorrentRemoved(TorrentHandle handle) {
                SwingUtilities.invokeLater(() -> listModel.removeElement(handle));
            }
        });
    }

    public JPanel getContentPane() {
        return contentPane;
    }

    public void addSelectionListener(Consumer<TorrentHandle> listener) {
        torrentList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                listener.accept(torrentList.getSelectedValue());
        });
    }

}
