package torrent;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MultiFileTorrentInfo extends TorrentInfo {

    private final List<FileInfo> files;

    public MultiFileTorrentInfo(@JsonProperty("name") String name,
                                @JsonProperty("piece length") long pieceLength,
                                @JsonProperty("pieces") byte[] pieces,
                                @JsonProperty("files") List<FileInfo> files
    ) {
        super(name, pieceLength, pieces);
        this.files = files;
    }

    public List<FileInfo> getFiles() {
        return files;
    }

    @Override
    public long getLength() {
        long totalLength = 0;
        for (FileInfo fi : files) {
            totalLength += fi.getLength();
        }
        return totalLength;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> info = new TreeMap<>();

        info.put("name", getName());
        info.put("piece length", getPieceLength());
        info.put("pieces", getPieces());
        info.put(
            "files",
            files.stream()
                .map(FileInfo::toMap)
                .toList()
        );

        return info;
    }

    @Override
    public String toString() {
        return "TorrentInfo{" +
            "name='" + getName() + '\'' +
            ", pieceLength=" + getPieceLength() +
            ", pieces=" + Arrays.toString(getPieces()) +
            "}, " +
            "MultiFileTorrentInfo{" +
            "files=" + files +
            '}';
    }
}
