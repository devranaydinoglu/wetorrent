package torrent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class TorrentWriter {

    public void write(Torrent t, File destination) {
        byte[] bencodedTorrent = t.encode();
        File bencodedTorrentFile = new File(
            destination.getParent(),
            destination.getName() + ".torrent"
        );

        try (FileOutputStream fos = new FileOutputStream(bencodedTorrentFile)) {
            fos.write(bencodedTorrent);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
