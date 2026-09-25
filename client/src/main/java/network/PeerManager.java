package network;

import base.Session;
import torrent.PieceManager;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PeerManager implements PeerConnected {

    private static final int UNCHOKE_SLOTS = 3;
    private static final int TICK_SECONDS = 10;
    private static final int OPTIMISTIC_UNCHOKE_TICKS = 3;

    private final Session session;
    private final PeerConnector peerConnector;
    private final byte[] infoHash;
    private final byte[] peerId;
    private volatile PieceManager pieceManager;

    private final Map<ByteBuffer, PeerConnection> connections = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
    private final Random random = new Random();
    private PeerConnection optimisticUnchoke;
    private int tick;

    public PeerManager(
        Session session,
        int listenPort,
        byte[] infoHash,
        byte[] peerId
    ) {
        this.session = session;
        peerConnector = new PeerConnector(listenPort, this::onIncomingPeerConnected);
        this.infoHash = infoHash;
        this.peerId = peerId;
    }

    public void setPieceManager(PieceManager pieceManager) {
        this.pieceManager = pieceManager;
    }

    private boolean setUpConnection(PeerConnection connection) {
        if (!connection.handshake()) {
            connection.close();
            return false;
        }

        connection.sendBitfield(pieceManager.getCompleted());

        connections.put(key(connection), connection);

        return true;
    }

    private void runConnection(PeerConnection connection) {
        try {
            connection.listen();
        } finally {
            connections.remove(key(connection), connection);
        }
    }

    public void broadcastHave(int index) {
        for (PeerConnection connection : connections.values()) {
            connection.sendHave(index);
            connection.updateInterest();
        }
    }

    public void startPeriodicTasks() {
        scheduler.scheduleAtFixedRate(this::runPeriodicTasks, TICK_SECONDS, TICK_SECONDS, TimeUnit.SECONDS);
    }

    // Fills a free upload slot right away instead of waiting for the next rechoke.
    synchronized void onPeerInterested(PeerConnection connection) {
        long unchoked = connections.values().stream()
            .filter(c -> c.getAmChoking() == ChokeStatus.UNCHOKED)
            .count();

        if (unchoked < UNCHOKE_SLOTS + 1)
            connection.setAmChoking(ChokeStatus.UNCHOKED);
    }

    private void runPeriodicTasks() {
        try {
            rechoke();
            for (PeerConnection connection : connections.values()) {
                connection.cancelTimedOutRequests();
                connection.sendKeepAliveIfIdle();
            }
        } catch (RuntimeException e) {
            System.out.println("Periodic peer task failed: " + e.getMessage());
        }
    }

    private synchronized void rechoke() {
        boolean seeding = pieceManager.getCompleted().isComplete();

        // While downloading, reward peers that upload. While seeding, favor the fastest downloaders.
        Map<PeerConnection, Long> rates = new HashMap<>();
        for (PeerConnection c : connections.values()) {
            long down = c.takeDownloaded();
            long up = c.takeUploaded();
            rates.put(c, seeding ? up : down);
        }

        List<PeerConnection> interested = rates.keySet().stream()
            .filter(c -> c.getPeerInterested() == InterestStatus.INTERESTED)
            .sorted(Comparator.comparingLong((PeerConnection c) -> rates.get(c)).reversed())
            .toList();

        Set<PeerConnection> unchoke = new HashSet<>(interested.subList(0, Math.min(UNCHOKE_SLOTS, interested.size())));

        if (tick++ % OPTIMISTIC_UNCHOKE_TICKS == 0 || !rates.containsKey(optimisticUnchoke)) {
            List<PeerConnection> candidates = interested.stream()
                .filter(c -> !unchoke.contains(c))
                .toList();
            optimisticUnchoke = candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
        }
        if (optimisticUnchoke != null)
            unchoke.add(optimisticUnchoke);

        for (PeerConnection c : rates.keySet())
            c.setAmChoking(unchoke.contains(c) ? ChokeStatus.UNCHOKED : ChokeStatus.CHOKED);
    }

    private static ByteBuffer key(PeerConnection connection) {
        return ByteBuffer.wrap(connection.getRemotePeer().getId());
    }

    public void listenIncoming() {
        peerConnector.listen();
    }

    public void close() {
        scheduler.shutdownNow();
        peerConnector.close();
        for (PeerConnection connection : connections.values())
            connection.close();
    }

    public void connectToPeer(Peer peer) {
        Socket socket = peerConnector.initSocket(peer);
        if (socket == null)
            return;

        try {
            PeerConnection connection = new PeerConnection(socket, infoHash, peerId, pieceManager, this);
            if (setUpConnection(connection))
                runConnection(connection);
        } catch (IOException e) {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    @Override
    public void onIncomingPeerConnected(Socket socket) {
        try {
            PeerConnection connection = new PeerConnection(socket, infoHash, peerId, pieceManager, this);
            if (setUpConnection(connection))
                session.runReporting("Peer connection failed.", () -> runConnection(connection));
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException ioe) {
                throw new RuntimeException("Failed to close peer socket.", ioe);
            }
        }
    }
}
