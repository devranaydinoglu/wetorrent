package torrent;

import filesystem.PieceStorage;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.IntConsumer;

public class PieceManager {

    public static final int BLOCK_SIZE = 16 * 1024;

    public enum BlockResult { REJECTED, ACCEPTED, PIECE_VERIFIED, PIECE_FAILED }

    public record BlockRequest(int index, int begin, int length) {}

    private final byte[] hashes;
    private final long pieceLength;
    private final long totalLength;
    private final Bitfield completed;
    private final PieceStorage storage;
    private final IntConsumer onPieceCompleted;
    private final int[] availability;
    private final Map<Integer, PieceInProgress> inProgress = new HashMap<>();
    private final Random random = new Random();

    public PieceManager(TorrentInfo info, Bitfield completed, PieceStorage storage, IntConsumer onPieceCompleted) {
        this.hashes = info.getPieces();
        this.pieceLength = info.getPieceLength();
        this.totalLength = info.getLength();
        this.completed = completed;
        this.storage = storage;
        this.onPieceCompleted = onPieceCompleted;
        this.availability = new int[completed.size()];
    }

    public int getPieceCount() {
        return completed.size();
    }

    public Bitfield getCompleted() {
        return completed;
    }

    public int pieceLength(int index) {
        return (int) Math.min(pieceLength, totalLength - index * pieceLength);
    }

    public byte[] readBlock(int index, int begin, int length) {
        return storage.read(index, begin, length);
    }

    public synchronized void addAvailability(int index) {
        availability[index]++;
    }

    public synchronized void addAvailability(Bitfield remote) {
        boolean[] has = remote.getBitfield();
        for (int i = 0; i < has.length; i++)
            if (has[i]) availability[i]++;
    }

    public synchronized void removeAvailability(Bitfield remote) {
        boolean[] has = remote.getBitfield();
        for (int i = 0; i < has.length; i++)
            if (has[i]) availability[i]--;
    }

    public boolean isInteresting(Bitfield remote) {
        boolean[] has = remote.getBitfield();
        boolean[] done = completed.getBitfield();
        for (int i = 0; i < has.length; i++)
            if (has[i] && !done[i]) return true;
        return false;
    }

    // Reserves blocks for the owner so no other peer is asked for the same data.
    public synchronized List<BlockRequest> nextRequests(Object owner, Bitfield remote, int max) {
        List<BlockRequest> requests = new ArrayList<>();
        if (max <= 0)
            return requests;

        boolean[] remoteHas = remote.getBitfield();

        for (PieceInProgress p : inProgress.values())
            if (p.owner == owner)
                p.addUnrequested(requests, max);

        // Finish pieces abandoned by choked or disconnected peers before starting new ones.
        for (PieceInProgress p : inProgress.values()) {
            if (requests.size() >= max)
                break;
            if (p.owner == null && remoteHas[p.index]) {
                p.owner = owner;
                p.addUnrequested(requests, max);
            }
        }

        while (requests.size() < max) {
            int index = pickRarest(remoteHas);
            if (index < 0)
                break;

            PieceInProgress p = new PieceInProgress(index, pieceLength(index), owner);
            inProgress.put(index, p);
            p.addUnrequested(requests, max);
        }

        return requests;
    }

    public BlockResult onBlock(Object owner, int index, int begin, byte[] block) {
        PieceInProgress piece;
        synchronized (this) {
            piece = inProgress.get(index);
            if (piece == null || piece.owner != owner || !piece.accept(begin, block))
                return BlockResult.REJECTED;
            if (!piece.isFullyReceived())
                return BlockResult.ACCEPTED;
            piece.verifying = true;
        }

        // Hash and write outside the lock so other connections aren't blocked on disk I/O.
        boolean stored = false;
        try {
            if (hashMatches(index, piece.data)) {
                storage.write(index, piece.data);
                stored = true;
            }
        } finally {
            synchronized (this) {
                inProgress.remove(index);
                if (stored)
                    completed.setPiece(index);
            }
        }

        if (!stored)
            return BlockResult.PIECE_FAILED;

        onPieceCompleted.accept(index);
        return BlockResult.PIECE_VERIFIED;
    }

    // Keeps received blocks so another peer can finish the piece.
    public synchronized void release(Object owner) {
        for (PieceInProgress p : inProgress.values()) {
            if (p.owner == owner && !p.verifying) {
                p.owner = null;
                p.resetUnreceived();
            }
        }
    }

    private int pickRarest(boolean[] remoteHas) {
        boolean[] done = completed.getBitfield();
        int best = -1;
        int bestAvailability = Integer.MAX_VALUE;
        int ties = 0;

        for (int i = 0; i < remoteHas.length; i++) {
            if (!remoteHas[i] || done[i] || inProgress.containsKey(i))
                continue;

            if (availability[i] < bestAvailability) {
                best = i;
                bestAvailability = availability[i];
                ties = 1;
            } else if (availability[i] == bestAvailability && random.nextInt(++ties) == 0) {
                best = i; // Uniform random choice among equally rare pieces
            }
        }

        return best;
    }

    private boolean hashMatches(int index, byte[] data) {
        byte[] hash = DigestUtils.sha1(data);
        return Arrays.equals(hash, 0, 20, hashes, index * 20, index * 20 + 20);
    }

    private static final class PieceInProgress {

        final int index;
        final byte[] data;
        final boolean[] requested;
        final boolean[] received;
        int receivedCount;
        Object owner;
        boolean verifying;

        PieceInProgress(int index, int length, Object owner) {
            this.index = index;
            this.data = new byte[length];
            int blocks = (length + BLOCK_SIZE - 1) / BLOCK_SIZE;
            this.requested = new boolean[blocks];
            this.received = new boolean[blocks];
            this.owner = owner;
        }

        int blockLength(int block) {
            return Math.min(BLOCK_SIZE, data.length - block * BLOCK_SIZE);
        }

        void addUnrequested(List<BlockRequest> out, int max) {
            for (int b = 0; b < requested.length && out.size() < max; b++) {
                if (!requested[b]) {
                    requested[b] = true;
                    out.add(new BlockRequest(index, b * BLOCK_SIZE, blockLength(b)));
                }
            }
        }

        boolean accept(int begin, byte[] block) {
            if (begin < 0 || begin % BLOCK_SIZE != 0)
                return false;

            int b = begin / BLOCK_SIZE;
            if (b >= received.length || received[b] || block.length != blockLength(b))
                return false;

            System.arraycopy(block, 0, data, begin, block.length);
            received[b] = true;
            requested[b] = true;
            receivedCount++;
            return true;
        }

        boolean isFullyReceived() {
            return receivedCount == received.length;
        }

        void resetUnreceived() {
            System.arraycopy(received, 0, requested, 0, received.length);
        }
    }

}
