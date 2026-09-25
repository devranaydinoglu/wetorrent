package filesystem;

import torrent.Torrent;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class PieceStorage {

    private final List<FileDiscovery.FileEntry> files;
    private final long pieceLength;
    private final Executor diskExecutor;
    private final Map<Path, FileChannel> channels = new HashMap<>();

    public PieceStorage(Path saveDir, Torrent torrent, Executor diskExecutor) {
        this.files = new FileDiscovery(saveDir, torrent).expectedFiles();
        this.pieceLength = torrent.getTorrentInfo().getPieceLength();
        this.diskExecutor = diskExecutor;
    }

    // In multi-file torrents a piece can span several files.
    public CompletableFuture<Void> write(int index, byte[] data) {
        return CompletableFuture.runAsync(() -> {
            try {
                forEachSpan(index * pieceLength, data.length, (file, position, offset, length) ->
                    writeFully(channel(file), ByteBuffer.wrap(data, offset, length), position));
            } catch (IOException e) {
                throw new UncheckedIOException("Couldn't write piece " + index, e);
            }
        }, diskExecutor);
    }

    public CompletableFuture<byte[]> read(int index, int begin, int length) {
        return CompletableFuture.supplyAsync(() -> {
            byte[] data = new byte[length];
            try {
                forEachSpan(index * pieceLength + begin, length, (file, position, offset, len) ->
                    readFully(channel(file), ByteBuffer.wrap(data, offset, len), position));
            } catch (IOException e) {
                throw new UncheckedIOException("Couldn't read piece " + index, e);
            }
            return data;
        }, diskExecutor);
    }

    public synchronized void close() {
        for (FileChannel channel : channels.values()) {
            try {
                channel.close();
            } catch (IOException ignored) {}
        }
        channels.clear();
    }

    private interface SpanAction {
        void apply(FileDiscovery.FileEntry file, long position, int offset, int length) throws IOException;
    }

    private void forEachSpan(long start, int length, SpanAction action) throws IOException {
        long fileStart = 0;
        int done = 0;

        for (FileDiscovery.FileEntry file : files) {
            long fileEnd = fileStart + file.length();
            long position = start + done;

            if (position < fileEnd) {
                int n = (int) Math.min(length - done, fileEnd - position);
                action.apply(file, position - fileStart, done, n);
                done += n;
                if (done == length)
                    return;
            }

            fileStart = fileEnd;
        }
    }

    
    private synchronized FileChannel channel(FileDiscovery.FileEntry file) throws IOException {
        FileChannel channel = channels.get(file.path());
        if (channel != null)
            return channel;

        Files.createDirectories(file.path().getParent());
        RandomAccessFile raf = new RandomAccessFile(file.path().toFile(), "rw");
        try {
            // Full size up front so FileDiscovery can verify a partial download after a restart.
            if (raf.length() < file.length())
                raf.setLength(file.length());
        } catch (IOException e) {
            raf.close();
            throw e;
        }

        channel = raf.getChannel();
        channels.put(file.path(), channel);
        return channel;
    }

    private static void writeFully(FileChannel channel, ByteBuffer buf, long position) throws IOException {
        while (buf.hasRemaining())
            position += channel.write(buf, position);
    }

    private static void readFully(FileChannel channel, ByteBuffer buf, long position) throws IOException {
        while (buf.hasRemaining()) {
            int n = channel.read(buf, position);
            if (n < 0)
                throw new EOFException("Unexpected end of file");
            position += n;
        }
    }

}
