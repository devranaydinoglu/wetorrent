package network;

import torrent.Bitfield;
import torrent.PieceManager;
import torrent.PieceManager.BlockRequest;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class PeerConnection {

    private static final int MAX_PENDING_REQUESTS = 10;
    private static final int MAX_QUEUED_MESSAGES = 256;
    private static final long REQUEST_TIMEOUT_MS = 60_000;
    private static final long KEEP_ALIVE_INTERVAL_MS = 90_000;

    private sealed interface Outgoing {
        record Frame(MessageType type, byte[] payload) implements Outgoing {}
        record Block(BlockRequest request, byte[] data) implements Outgoing {}
        record KeepAlive() implements Outgoing {}
    }

    private Peer remotePeer = null;
    private volatile Bitfield remoteBitfield;
    private final Socket socket;
    private final OutputStream out;
    private final InputStream in;
    private final byte[] infoHash;
    private final byte[] localPeerId;
    private final PieceManager pieceManager;
    private final PeerManager peerManager;

    private volatile ChokeStatus amChoking = ChokeStatus.CHOKED;
    private volatile InterestStatus amInterested = InterestStatus.NOT_INTERESTED;
    private volatile ChokeStatus peerChoking = ChokeStatus.CHOKED;
    private volatile InterestStatus peerInterested = InterestStatus.NOT_INTERESTED;
    private final Object chokeLock = new Object();
    private final Object interestLock = new Object();

    // Only the writer thread touches the socket's output stream after the handshake.
    private final BlockingQueue<Outgoing> outgoing = new LinkedBlockingQueue<>();
    private volatile Thread writer;
    private volatile long lastSentAt = System.currentTimeMillis();
    // Upload requests whose block is still being read from disk.
    private final Set<BlockRequest> pendingUploads = ConcurrentHashMap.newKeySet();

    private final Set<BlockRequest> pendingRequests = new HashSet<>();
    private long lastBlockAt;
    private final AtomicLong downloaded = new AtomicLong();
    private final AtomicLong uploaded = new AtomicLong();

    private boolean receivedMessage = false;
    private volatile boolean running = false;

    public PeerConnection(
        Socket socket,
        byte[] infoHash,
        byte[] localPeerId,
        PieceManager pieceManager,
        PeerManager peerManager
    ) throws IOException {
        this.socket = socket;
        out = socket.getOutputStream();
        in = socket.getInputStream();
        this.infoHash = infoHash;
        this.localPeerId = localPeerId;
        this.pieceManager = pieceManager;
        this.peerManager = peerManager;
        remoteBitfield = new Bitfield(pieceManager.getPieceCount());
    }

    public Peer getRemotePeer() {
        return remotePeer;
    }

    public ChokeStatus getAmChoking() {
        return amChoking;
    }

    public InterestStatus getPeerInterested() {
        return peerInterested;
    }

    public long takeDownloaded() {
        return downloaded.getAndSet(0);
    }

    public long takeUploaded() {
        return uploaded.getAndSet(0);
    }

    private void send(MessageType type, byte[] payload) {
        outgoing.add(new Outgoing.Frame(type, payload));
    }

    public void sendBitfield(Bitfield bitfield) {
        if (!bitfield.isEmpty())
            send(MessageType.BITFIELD, bitfield.toWire());
    }

    public void sendHave(int index) {
        send(MessageType.HAVE, ByteBuffer.allocate(4).putInt(index).array());
    }

    public void sendKeepAliveIfIdle() {
        if (System.currentTimeMillis() - lastSentAt > KEEP_ALIVE_INTERVAL_MS)
            outgoing.add(new Outgoing.KeepAlive());
    }

    public void updateInterest() {
        synchronized (interestLock) {
            boolean interesting = pieceManager.isInteresting(remoteBitfield);

            if (interesting && amInterested == InterestStatus.NOT_INTERESTED) {
                amInterested = InterestStatus.INTERESTED;
                send(MessageType.INTERESTED, new byte[0]);
            } else if (!interesting && amInterested == InterestStatus.INTERESTED) {
                amInterested = InterestStatus.NOT_INTERESTED;
                send(MessageType.NOT_INTERESTED, new byte[0]);
            }
        }
    }

    public void setAmChoking(ChokeStatus status) {
        synchronized (chokeLock) {
            if (amChoking == status)
                return;

            amChoking = status;
            if (status == ChokeStatus.CHOKED) {
                // Choking discards all of the peer's queued requests.
                pendingUploads.clear();
                outgoing.removeIf(o -> o instanceof Outgoing.Block);
                send(MessageType.CHOKE, new byte[0]);
            } else {
                send(MessageType.UNCHOKE, new byte[0]);
            }
        }
    }

    public synchronized void cancelTimedOutRequests() {
        if (pendingRequests.isEmpty() || System.currentTimeMillis() - lastBlockAt < REQUEST_TIMEOUT_MS)
            return;

        for (BlockRequest r : pendingRequests)
            send(MessageType.CANCEL, encodeRequest(r));
        pendingRequests.clear();
        pieceManager.release(this);
    }

    public boolean handshake() {
        ByteArrayOutputStream header = new ByteArrayOutputStream();
        byte[] pstrLength = {19};
        header.writeBytes(pstrLength);
        byte[] protocol = "BitTorrent protocol".getBytes(StandardCharsets.UTF_8);
        header.writeBytes(protocol);
        byte[] ext = new byte[8];
        header.writeBytes(ext);
        header.writeBytes(infoHash);
        header.writeBytes(localPeerId);

        try {
            out.write(header.toByteArray());
            out.flush();

            byte[] remoteHandshake = in.readNBytes(68);

            if (remoteHandshake.length != 68)
                return false;

            if (remoteHandshake[0] != 19)
                return false;

            String protocolStr = new String(Arrays.copyOfRange(remoteHandshake, 1, 20), StandardCharsets.UTF_8);
            if (!protocolStr.equals("BitTorrent protocol"))
                return false;

            byte[] remoteInfoHash = Arrays.copyOfRange(remoteHandshake, 28, 48);
            if (!Arrays.equals(remoteInfoHash, infoHash))
                return false;

            byte[] remotePeerId = Arrays.copyOfRange(remoteHandshake, 48, 68);


            if (remotePeer == null) {
                InetAddress socketAddress = socket.getInetAddress();
                remotePeer = new Peer(remotePeerId, socketAddress.getHostAddress(), socket.getPort());
            }

        } catch (IOException e) {
            return false;
        }

        return true;
    }

    // Listen to incoming messages once connection has been established
    public void listen() {
        running = true;
        writer = Thread.ofVirtual().start(this::writeLoop);
        // Caps allocation from the untrusted length prefix.
        int maxLength = Math.max(PieceManager.BLOCK_SIZE + 9, (pieceManager.getPieceCount() + 7) / 8 + 1);

        try {
            while (running) {
                byte[] lengthBytes = in.readNBytes(4);
                if (lengthBytes.length < 4)
                    break;

                int length = getIntFromBytes(lengthBytes);
                if (length == 0)
                    continue;
                if (length < 0 || length > maxLength)
                    throw new IOException("Invalid message length: " + length);

                MessageType type = MessageType.getTypeFromInt(in.read());
                byte[] payloadBytes = in.readNBytes(length - 1);
                if (payloadBytes.length != length - 1)
                    break;

                handleMessage(new Message(length, type, payloadBytes));
            }
        } catch (IOException e) {
            System.out.println("Connection lost: " + e.getMessage());
        } finally {
            stopListening();
            pieceManager.removeAvailability(remoteBitfield);
            pieceManager.release(this);
        }
    }

    public void stopListening() {
        running = false;
        close();
    }

    public void close() {
        running = false;
        Thread w = writer;
        if (w != null)
            w.interrupt();
        try {
            socket.close();
        } catch (IOException ignored) {}
    }

    private void writeLoop() {
        try {
            while (running) {
                switch (outgoing.take()) {
                    case Outgoing.Frame f -> writeFrame(f.type(), f.payload());
                    case Outgoing.Block b -> writeBlock(b.request(), b.data());
                    case Outgoing.KeepAlive k -> writeKeepAlive();
                }
            }
        } catch (InterruptedException ignored) {
        } catch (IOException e) {
            System.out.println("Send failed: " + e.getMessage());
        } finally {
            close();
        }
    }

    private void writeFrame(MessageType type, byte[] payload) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(4 + 1 + payload.length);
        buf.putInt(1 + payload.length);
        buf.put((byte) type.getId());
        buf.put(payload);
        out.write(buf.array());
        out.flush();
        lastSentAt = System.currentTimeMillis();
    }

    private void writeKeepAlive() throws IOException {
        out.write(new byte[4]);
        out.flush();
        lastSentAt = System.currentTimeMillis();
    }

    private void writeBlock(BlockRequest r, byte[] block) throws IOException {
        if (amChoking == ChokeStatus.CHOKED)
            return;

        byte[] payload = ByteBuffer.allocate(8 + block.length)
            .putInt(r.index())
            .putInt(r.begin())
            .put(block)
            .array();
        writeFrame(MessageType.PIECE, payload);
        uploaded.addAndGet(block.length);
    }

    private int getIntFromBytes(byte[] lengthBytes) {
        int value = 0;
        for (byte b : lengthBytes) {
            value = (value << 8) + (b & 0xFF);
        }

        return value;
    }

    private void handleMessage(Message msg) throws IOException {
        byte[] payload = msg.getPayload();

        switch (msg.getType()) {
            case CHOKE:
                handleChoke();
                break;
            case UNCHOKE:
                peerChoking = ChokeStatus.UNCHOKED;
                requestBlocks();
                break;
            case INTERESTED:
                peerInterested = InterestStatus.INTERESTED;
                peerManager.onPeerInterested(this);
                break;
            case NOT_INTERESTED:
                peerInterested = InterestStatus.NOT_INTERESTED;
                break;
            case HAVE:
                handleHave(payload);
                break;
            case BITFIELD:
                handleBitfield(payload);
                break;
            case REQUEST:
                handleRequest(payload);
                break;
            case PIECE:
                handlePiece(payload);
                break;
            case CANCEL:
                handleCancel(parseRequest(payload));
                break;
            default:
                break;
        }

        receivedMessage = true;
    }

    private synchronized void handleChoke() {
        peerChoking = ChokeStatus.CHOKED;
        pendingRequests.clear();
        pieceManager.release(this);
    }

    private void handleHave(byte[] payload) throws IOException {
        if (payload.length != 4)
            throw new IOException("Malformed HAVE message");

        int index = ByteBuffer.wrap(payload).getInt();
        if (index < 0 || index >= remoteBitfield.size())
            throw new IOException("HAVE index out of range: " + index);

        if (remoteBitfield.setPiece(index))
            pieceManager.addAvailability(index);

        updateInterest();
        requestBlocks();
    }

    private void handleBitfield(byte[] payload) throws IOException {
        if (receivedMessage)
            throw new IOException("BITFIELD is only allowed as the first message");

        try {
            remoteBitfield = Bitfield.fromWire(payload, pieceManager.getPieceCount());
        } catch (IllegalArgumentException e) {
            throw new IOException("Malformed BITFIELD message: " + e.getMessage());
        }

        pieceManager.addAvailability(remoteBitfield);
        updateInterest();
    }

    private void handleRequest(byte[] payload) throws IOException {
        BlockRequest r = parseRequest(payload);
        if (r.length() <= 0 || r.length() > PieceManager.BLOCK_SIZE || r.begin() < 0
            || (long) r.begin() + r.length() > pieceManager.pieceLength(r.index()))
            throw new IOException("Invalid REQUEST: " + r);

        if (amChoking == ChokeStatus.CHOKED || !pieceManager.getCompleted().hasPiece(r.index()))
            return;
        if (pendingUploads.size() + outgoing.size() >= MAX_QUEUED_MESSAGES || !pendingUploads.add(r))
            return;

        pieceManager.readBlock(r.index(), r.begin(), r.length()).whenComplete((block, error) -> {
            if (error != null) {
                System.out.println("Couldn't read block for upload: " + error.getMessage());
                close();
            } else if (pendingUploads.remove(r)) {
                outgoing.add(new Outgoing.Block(r, block));
            }
        });
    }

    private void handleCancel(BlockRequest r) {
        pendingUploads.remove(r);
        outgoing.removeIf(o -> o instanceof Outgoing.Block b && b.request().equals(r));
    }

    private synchronized void handlePiece(byte[] payload) throws IOException {
        if (payload.length < 8)
            throw new IOException("Malformed PIECE message");

        ByteBuffer buf = ByteBuffer.wrap(payload);
        int index = buf.getInt();
        int begin = buf.getInt();
        byte[] block = Arrays.copyOfRange(payload, 8, payload.length);

        // Blocks we didn't ask for, or already cancelled, are dropped.
        if (!pendingRequests.remove(new BlockRequest(index, begin, block.length)))
            return;

        lastBlockAt = System.currentTimeMillis();
        downloaded.addAndGet(block.length);

        if (pieceManager.onBlock(this, index, begin, block) == PieceManager.BlockResult.PIECE_FAILED)
            System.out.println("Piece " + index + " from " + remotePeer.getIp() + " failed hash check");

        requestBlocks();
    }

    private synchronized void requestBlocks() {
        if (peerChoking == ChokeStatus.CHOKED || amInterested == InterestStatus.NOT_INTERESTED)
            return;

        if (pendingRequests.isEmpty())
            lastBlockAt = System.currentTimeMillis();

        int slots = MAX_PENDING_REQUESTS - pendingRequests.size();
        for (BlockRequest r : pieceManager.nextRequests(this, remoteBitfield, slots)) {
            send(MessageType.REQUEST, encodeRequest(r));
            pendingRequests.add(r);
        }
    }

    private BlockRequest parseRequest(byte[] payload) throws IOException {
        if (payload.length != 12)
            throw new IOException("Malformed REQUEST/CANCEL message");

        ByteBuffer buf = ByteBuffer.wrap(payload);
        BlockRequest r = new BlockRequest(buf.getInt(), buf.getInt(), buf.getInt());
        if (r.index() < 0 || r.index() >= pieceManager.getPieceCount())
            throw new IOException("Piece index out of range: " + r.index());
        return r;
    }

    private static byte[] encodeRequest(BlockRequest r) {
        return ByteBuffer.allocate(12)
            .putInt(r.index())
            .putInt(r.begin())
            .putInt(r.length())
            .array();
    }

}
