package torrent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class TorrentReader {

    public byte[] read(File f) {
        try (FileInputStream fis = new FileInputStream(f)) {
            return fis.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
