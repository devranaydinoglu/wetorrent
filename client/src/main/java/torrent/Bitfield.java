package torrent;

public class Bitfield {

    private final boolean[] bitfield;
    private int count;

    public Bitfield(int size) {
        bitfield = new boolean[size];
    }

    public Bitfield(boolean[] bitfield) {
        this.bitfield = bitfield.clone();
        for (boolean b : this.bitfield)
            if (b) count++;
    }

    public static Bitfield fromWire(byte[] wire, int size) {
        if (wire.length != (size + 7) / 8)
            throw new IllegalArgumentException("Bitfield has wrong length");

        Bitfield bf = new Bitfield(size);
        for (int i = 0; i < wire.length * 8; i++) {
            if ((wire[i / 8] & (0x80 >>> (i % 8))) == 0)
                continue;
            if (i >= size)
                throw new IllegalArgumentException("Bitfield has spare bits set");
            bf.setPiece(i);
        }
        return bf;
    }

    public int size() {
        return bitfield.length;
    }

    public synchronized boolean hasPiece(int i) {
        return bitfield[i];
    }

    // Returns false if the piece was already set.
    public synchronized boolean setPiece(int i) {
        if (bitfield[i])
            return false;
        bitfield[i] = true;
        count++;
        return true;
    }

    public synchronized boolean isEmpty() {
        return count == 0;
    }

    public synchronized boolean isComplete() {
        return count == bitfield.length;
    }

    public synchronized boolean[] getBitfield() {
        return bitfield.clone();
    }

    public synchronized byte[] toWire() {
        byte[] out = new byte[(bitfield.length + 7) / 8];
        for (int i = 0; i < bitfield.length; i++)
            if (bitfield[i])
                out[i / 8] |= (byte) (0x80 >>> (i % 8));
        return out;
    }

}
