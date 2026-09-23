package torrent;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;

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

}
