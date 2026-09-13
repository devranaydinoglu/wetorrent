package torrent;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MultiFileInfo {

    private final long length;
    private final List<String> path;

    public MultiFileInfo(long length, List<String> path) {
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
