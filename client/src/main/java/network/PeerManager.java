package network;

import base.Session;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class PeerManager implements PeerConnected {

    private final Session session;
    private final PeerConnector peerConnector;
    private final byte[] infoHash;
    private final byte[] peerId;
    private volatile boolean[] bitfield = new boolean[0];

    private final Map<ByteBuffer, PeerConnection> connections = new ConcurrentHashMap<>();

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

    public void setBitfield(boolean[] bitfield) {
        this.bitfield = bitfield;
    }

    private boolean setUpConnection(PeerConnection connection) throws IOException {
        if (!connection.handshake()) {
            connection.close();
            return false;
        }

        connection.sendBitfield(bitfield);

        connections.put(ByteBuffer.wrap(connection.getRemotePeer().getId()), connection);

        return true;
    }

    public void listenIncoming() {
        peerConnector.listen();
    }

    public void close() {
        peerConnector.close();
    }

    public void connectToPeer(Peer peer) {
        Socket socket = peerConnector.initSocket(peer);
        if (socket == null)
            return;

        try {
            PeerConnection connection = new PeerConnection(socket, infoHash, peerId);
            if (setUpConnection(connection))
                connection.listen();
        } catch (IOException e) {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    @Override
    public void onIncomingPeerConnected(Socket socket) {
        try {
            PeerConnection connection = new PeerConnection(socket, infoHash, peerId);
            if (setUpConnection(connection))
                session.runReporting("Peer connection failed.", connection::listen);
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException ioe) {
                throw new RuntimeException("Failed to close peer socket.", ioe);
            }
        }
    }
}
