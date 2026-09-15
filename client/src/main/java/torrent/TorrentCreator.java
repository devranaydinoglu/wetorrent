package torrent;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TorrentCreator {

    public Torrent create(File f, String trackerUrl) {
        TorrentInfo torrentInfo;

        if (f.isDirectory()) {
            List<MultiFileInfo> multiFileInfoList = new ArrayList<>();
            getSubFilesRecursively(multiFileInfoList, f, f.getName());

            torrentInfo = new TorrentInfo(
                f.getName(),
                Piece.PIECE_LENGTH,
                "".getBytes(StandardCharsets.UTF_8), // TODO: calculate byte string
                multiFileInfoList
            );
        } else {
            torrentInfo = new TorrentInfo(
                f.getName(),
                Piece.PIECE_LENGTH,
                Piece.getByteString(f),
                f.length()
            );
        }

        return new Torrent(trackerUrl, torrentInfo, Instant.now());
    }

    private void getSubFilesRecursively(List<MultiFileInfo> files, File currentFile, String rootDir) {
        if (!currentFile.isDirectory()) {
            if (currentFile.isHidden())
                return;

            String relPath = currentFile.getPath();
            int relPathStart = relPath.lastIndexOf(rootDir);
            String[] splitPath = relPath.substring(relPathStart).split("/");

            MultiFileInfo multiFile = new MultiFileInfo(
                currentFile.length(),
                Arrays.stream(splitPath).toList()
            );
            files.add(multiFile);

            return;
        }

        for (File f : currentFile.listFiles()) {
            getSubFilesRecursively(files, f, rootDir);
        }
    }

}
