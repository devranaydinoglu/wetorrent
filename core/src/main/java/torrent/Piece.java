package torrent;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class Piece {

    public static final int PIECE_LENGTH = 262144; // In bytes

    public static byte[] getByteString(File f) throws RuntimeException {
        try (FileInputStream fis = new FileInputStream(f)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            byte[] pieceBytes;
            while ((pieceBytes = fis.readNBytes(PIECE_LENGTH)).length > 0) {
                byte[] hashedPiece = DigestUtils.sha1(pieceBytes);
                bos.write(hashedPiece);
            }

            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] getByteString(List<Path> files) throws RuntimeException {
        ByteArrayOutputStream hashes = new ByteArrayOutputStream();
        byte[] buffer = new byte[PIECE_LENGTH];
        int filled = 0;

        for (Path file : files) {
            try (InputStream in = Files.newInputStream(file)) {
                int n;
                while ((n = in.read(buffer, filled, PIECE_LENGTH - filled)) > 0) {
                    filled += n;
                    if (filled == PIECE_LENGTH) {
                        hashes.writeBytes(DigestUtils.sha1(buffer));
                        filled = 0;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (filled > 0)
            hashes.writeBytes(DigestUtils.sha1(Arrays.copyOf(buffer, filled)));

        return hashes.toByteArray();
    }

}
