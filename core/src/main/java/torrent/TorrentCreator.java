package torrent;

import bencode.Bencode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class TorrentCreator {

    public Torrent createFromFile(File f, String trackerUrl) {
        TorrentInfo torrentInfo;

        if (f.isDirectory()) {
            List<FileInfo> fileInfoList = new ArrayList<>();
            List<Path> filePaths = new ArrayList<>();
            Path p = f.toPath();
            try {
                getSubFilesRecursively(fileInfoList, filePaths, p, p);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            torrentInfo = new MultiFileTorrentInfo(
                p.getFileName().toString(),
                Piece.PIECE_LENGTH,
                Piece.getByteString(filePaths),
                fileInfoList
            );
        } else {
            torrentInfo = new SingleFileTorrentInfo(
                f.getName(),
                Piece.PIECE_LENGTH,
                Piece.getByteString(f),
                f.length()
            );
        }

        return new Torrent(trackerUrl, torrentInfo, Instant.now());
    }

    public Torrent createFromBytes(byte[] b) {
        try {
            Map<String, Object> root = (Map<String, Object>) Bencode.decode(new ByteArrayInputStream(b));

            ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

            String announce = new String((byte[]) root.get("announce"), StandardCharsets.UTF_8);
            long createdEpoch = ((Number) root.get("creation date")).longValue();

            Map<String, Object> info = (Map<String, Object>) root.get("info");
            info.put("name", new String((byte[]) info.get("name"), StandardCharsets.UTF_8));

            if (info.containsKey("files")) {
                List<Map<String, Object>> files = (List<Map<String, Object>>) info.get("files");
                for (Map<String, Object> file : files) {
                    List<byte[]> rawPath = (List<byte[]>) file.get("path");
                    List<String> path = rawPath.stream()
                        .map(p -> new String(p, StandardCharsets.UTF_8))
                        .toList();
                    file.put("path", path);
                }
            }

            TorrentInfo torrentInfo = info.containsKey("length")
                ? mapper.convertValue(info, SingleFileTorrentInfo.class)
                : mapper.convertValue(info, MultiFileTorrentInfo.class);

            return new Torrent(announce, torrentInfo, Instant.ofEpochSecond(createdEpoch));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void getSubFilesRecursively(List<FileInfo> files, List<Path> paths, Path current, Path root)
        throws IOException
    {
        if (!Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
            if (Files.isHidden(current))
                return;

            Path relativePath = root.relativize(current);
            List<String> splitPath = new ArrayList<>();
            for (Path p : relativePath) {
                splitPath.add(p.toString());
            }

            FileInfo multiFile = new FileInfo(
                current.toFile().length(),
                splitPath
            );
            files.add(multiFile);
            paths.add(current);

            return;
        }

        try (Stream<Path> stream = Files.list(current)) {
            List<Path> childPaths = stream.sorted().toList();

            if (!childPaths.isEmpty()) {
                for (Path p : childPaths) {
                    getSubFilesRecursively(files, paths, p, root);
                }
            }
        }
    }

}
