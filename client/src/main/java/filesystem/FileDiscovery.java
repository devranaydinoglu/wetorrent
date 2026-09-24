package filesystem;

import org.apache.commons.codec.digest.DigestUtils;
import torrent.MultiFileTorrentInfo;
import torrent.Torrent;
import torrent.TorrentInfo;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

public class FileDiscovery {

    record FileEntry(Path path, long length) {}

    private final Path saveDir;
    private final Torrent torrent;

    public FileDiscovery(Path saveDir, Torrent torrent) {
        this.saveDir = saveDir;
        this.torrent = torrent;
    }

    public boolean[] computeBitfield() {
        TorrentInfo info = torrent.getTorrentInfo();
        byte[] hashes = info.getPieces();
        int pieceLength = (int) info.getPieceLength();
        boolean[] have = new boolean[hashes.length / 20];

        byte[] buffer = new byte[pieceLength];
        int filled = 0;
        int pieceIndex = 0;
        boolean pieceIntact = true;

        for (FileEntry file : expectedFiles()) {
            long remaining = file.length();
            try (InputStream in = isUsable(file) ? Files.newInputStream(file.path()) : null) {
                while (remaining > 0) {
                    int toRead = (int) Math.min(pieceLength - filled, remaining);
                    if (in == null || in.readNBytes(buffer, filled, toRead) != toRead)
                        pieceIntact = false;

                    filled += toRead;
                    remaining -= toRead;

                    if (filled == pieceLength) {
                        have[pieceIndex] = pieceIntact && hashMatches(buffer, filled, hashes, pieceIndex);
                        pieceIndex++;
                        filled = 0;
                        pieceIntact = true;
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Couldn't read " + file.path(), e);
            }
        }

        if (filled > 0)
            have[pieceIndex] = pieceIntact && hashMatches(buffer, filled, hashes, pieceIndex);

        return have;
    }

    List<FileEntry> expectedFiles() {
        TorrentInfo info = torrent.getTorrentInfo();
        if (info instanceof MultiFileTorrentInfo multi) {
            Path root = saveDir.resolve(info.getName()).normalize();
            return multi.getFiles().stream()
                .map(fi -> new FileEntry(safeResolve(root, fi.getPath()), fi.getLength()))
                .toList();
        }
        return List.of(new FileEntry(safeResolve(saveDir, List.of(info.getName())), info.getLength()));
    }

    private static Path safeResolve(Path root, List<String> parts) {
        Path p = root;
        for (String part : parts)
            p = p.resolve(part);
        p = p.normalize();
        if (!p.startsWith(root.normalize()))
            throw new IllegalArgumentException("Illegal path in torrent: " + parts);
        return p;
    }

    private static boolean isUsable(FileEntry file) throws IOException {
        return Files.isRegularFile(file.path()) && Files.size(file.path()) == file.length();
    }

    private static boolean hashMatches(byte[] data, int len, byte[] hashes, int index) {
        MessageDigest md = DigestUtils.getSha1Digest();
        md.update(data, 0, len);
        return Arrays.equals(md.digest(), 0, 20, hashes, index * 20, index * 20 + 20);
    }
}
