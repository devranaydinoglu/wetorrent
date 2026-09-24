package filesystem;

import torrent.Torrent;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class PieceStorage {

    private final List<FileDiscovery.FileEntry> files;
    private final long pieceLength;

    public PieceStorage(Path saveDir, Torrent torrent) {
        this.files = new FileDiscovery(saveDir, torrent).expectedFiles();
        this.pieceLength = torrent.getTorrentInfo().getPieceLength();
    }

    // In multi-file torrents a piece can span several files.
    public synchronized void write(int index, byte[] data) {
        try {
            forEachSpan(index * pieceLength, data.length,
                (file, position, offset, length) -> writeToFile(file, position, data, offset, length));
        } catch (IOException e) {
            throw new UncheckedIOException("Couldn't write piece " + index, e);
        }
    }

    public synchronized byte[] read(int index, int begin, int length) {
        byte[] data = new byte[length];
        try {
            forEachSpan(index * pieceLength + begin, length,
                (file, position, offset, len) -> readFromFile(file, position, data, offset, len));
        } catch (IOException e) {
            throw new UncheckedIOException("Couldn't read piece " + index, e);
        }
        return data;
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

    private static void readFromFile(FileDiscovery.FileEntry file, long position, byte[] data, int offset, int length)
        throws IOException
    {
        try (RandomAccessFile raf = new RandomAccessFile(file.path().toFile(), "r")) {
            raf.seek(position);
            raf.readFully(data, offset, length);
        }
    }

    private static void writeToFile(FileDiscovery.FileEntry file, long position, byte[] data, int offset, int length)
        throws IOException
    {
        Files.createDirectories(file.path().getParent());
        try (RandomAccessFile raf = new RandomAccessFile(file.path().toFile(), "rw")) {
            // Full size up front so FileDiscovery can verify a partial download after a restart.
            if (raf.length() < file.length())
                raf.setLength(file.length());
            raf.seek(position);
            raf.write(data, offset, length);
        }
    }

}
