package ui;

import torrent.PieceManager.PieceState;
import torrent.TorrentHandle;

import javax.swing.*;
import java.awt.*;

public class PiecesPanel {

    private static final int REFRESH_MS = 1000;

    private final JPanel contentPane;
    private final JProgressBar overallProgress = new JProgressBar(0, 100);
    private final PieceMap pieceMap = new PieceMap();
    private TorrentHandle torrent;

    public PiecesPanel() {
        contentPane = new JPanel(new BorderLayout(0, 8));
        contentPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        overallProgress.setStringPainted(true);
        contentPane.add(overallProgress, BorderLayout.NORTH);
        contentPane.add(pieceMap, BorderLayout.CENTER);

        refresh();
        new Timer(REFRESH_MS, e -> refresh()).start();
    }

    public JPanel getContentPane() {
        return contentPane;
    }

    public void setTorrent(TorrentHandle torrent) {
        this.torrent = torrent;
        refresh();
    }

    private void refresh() {
        PieceState[] states = torrent == null ? new PieceState[0] : torrent.getPieceStates();
        pieceMap.setStates(states);

        if (states.length == 0) {
            overallProgress.setValue(0);
            overallProgress.setString(torrent == null ? "No torrent selected" : "Starting...");
            return;
        }

        int done = 0;
        for (PieceState state : states)
            if (state == PieceState.DONE) done++;

        int percent = (int) (done * 100L / states.length);
        overallProgress.setValue(percent);
        overallProgress.setString(done + " / " + states.length + " pieces (" + percent + "%)");
    }

    private static final class PieceMap extends JComponent {

        private static final int MAX_CELL = 16;
        private static final Color MISSING_COLOR = new Color(0xDDDDDD);
        private static final Color DOWNLOADING_COLOR = new Color(0x3B82F6);
        private static final Color DONE_COLOR = new Color(0x22C55E);

        private PieceState[] states = new PieceState[0];

        void setStates(PieceState[] states) {
            this.states = states;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int n = states.length;
            int w = getWidth();
            int h = getHeight();
            if (n == 0 || w == 0 || h == 0)
                return;

            // Largest square cell that still fits every piece in the visible area.
            int cell = Math.max(1, Math.min(MAX_CELL, (int) Math.sqrt((double) w * h / n)));
            while (cell > 1 && (long) (w / cell) * (h / cell) < n)
                cell--;

            int cols = Math.max(1, w / cell);
            int gap = cell >= 4 ? 1 : 0;

            for (int i = 0; i < n; i++) {
                g.setColor(switch (states[i]) {
                    case DONE -> DONE_COLOR;
                    case DOWNLOADING -> DOWNLOADING_COLOR;
                    case MISSING -> MISSING_COLOR;
                });
                g.fillRect((i % cols) * cell, (i / cols) * cell, cell - gap, cell - gap);
            }
        }
    }

}
