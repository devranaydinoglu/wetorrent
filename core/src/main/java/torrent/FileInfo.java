package torrent;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class FileInfo {

    private final long length;
    private final List<String> path;

    public FileInfo(@JsonProperty("length") long length, @JsonProperty("path") List<String> path) {
        this.length = length;
        this.path = path;
    }

    public long getLength() {
        return length;
    }

    public List<String> getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "MultiFileDictionary{" +
            "\nlength=" + length +
            "\npath=" + path +
            "\n}";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> info = new TreeMap<>();
        info.put("length", length);
        info.put("path", path);
        return info;
    }
}
