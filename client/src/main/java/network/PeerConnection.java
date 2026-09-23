package network;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class PeerConnection {

    private Peer remotePeer = null;
    private final Socket socket;
    private final OutputStream out;
    private final InputStream in;
    private final byte[] infoHash;
    private final byte[] localPeerId;

    private ChokeStatus chokeStatus = ChokeStatus.CHOKED;
    private InterestStatus interestStatus = InterestStatus.NOT_INTERESTED;

    private boolean running = false;

    public PeerConnection(Socket socket, byte[] infoHash, byte[] localPeerId) throws IOException {
        this.socket = socket;
        out = socket.getOutputStream();
        in = socket.getInputStream();
        this.infoHash = infoHash;
        this.localPeerId = localPeerId;
    }

    public Peer getRemotePeer() {
        return remotePeer;
    }

    public synchronized void send(MessageType type, byte[] payload) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(4 + 1 + payload.length);
        buf.putInt(1 + payload.length);
        buf.put((byte) type.getId());
        buf.put(payload);
        out.write(buf.array());
        out.flush();
    }

    public void sendBitfield(boolean[] have) throws IOException {
        for (boolean b : have) {
            if (b) {
                send(MessageType.BITFIELD, bitfieldWire(have));
                return;
            }
        }
    }

    public boolean handshake() {
        ByteArrayOutputStream header = new ByteArrayOutputStream();
        byte[] pstrLength = {19};
        header.writeBytes(pstrLength);
        byte[] protocol = "BitTorrent protocol".getBytes(StandardCharsets.UTF_8);
        header.writeBytes(protocol);
        byte[] ext = new byte[8];
        header.writeBytes(ext);
        header.writeBytes(infoHash);
        header.writeBytes(localPeerId);

        try {
            out.write(header.toByteArray());
            out.flush();

            byte[] remoteHandshake = in.readNBytes(68);

            if (remoteHandshake.length != 68)
                return false;

            if (remoteHandshake[0] != 19)
                return false;

            String protocolStr = new String(Arrays.copyOfRange(remoteHandshake, 1, 20), StandardCharsets.UTF_8);
            if (!protocolStr.equals("BitTorrent protocol"))
                return false;

            byte[] remoteInfoHash = Arrays.copyOfRange(remoteHandshake, 28, 48);
            if (!Arrays.equals(remoteInfoHash, infoHash))
                return false;

            byte[] remotePeerId = Arrays.copyOfRange(remoteHandshake, 48, 68);


            if (remotePeer == null) {
                InetAddress socketAddress = socket.getInetAddress();
                remotePeer = new Peer(remotePeerId, socketAddress.getHostAddress(), socket.getPort());
            }

        } catch (IOException e) {
            return false;
        }

        return true;
    }

    // Listen to incoming messages once connection has been established
    public void listen() {
        running = true;

        while (running) {
            try {
                byte[] lengthBytes = in.readNBytes(4);
                if (lengthBytes.length < 4) {
                    stopListening(); return;
                }
                int length = getIntFromBytes(lengthBytes);
                if (length == 0)
                    continue;

                MessageType type = MessageType.getTypeFromInt(in.read());
                byte[] payloadBytes = in.readNBytes(length - 1);

                Message msg = new Message(length, type, payloadBytes);
                handleMessage(msg);
            } catch (IOException e) {
                System.out.println("Connection lost: " + e.getMessage());
                stopListening();
            }
        }
    }

    public void stopListening() {
        running = false;
        close();
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {}
    }

    private int getIntFromBytes(byte[] lengthBytes) {
        int value = 0;
        for (byte b : lengthBytes) {
            value = (value << 8) + (b & 0xFF);
        }

        return value;
    }

    private byte[] bitfieldWire(boolean[] have) {
        byte[] out = new byte[(have.length + 7) / 8];
        for (int i = 0; i < have.length; i++)
            if (have[i])
                out[i / 8] |= (byte) (0x80 >>> (i % 8));
        return out;
    }

    private void handleMessage(Message msg) {
//        System.out.println("MESSAGE RECEIVED: Length-" + msg.getLength() + " Type-" + msg.getType());

        switch (msg.getType()) {
            case CHOKE:
                break;
            case UNCHOKE:
                break;
            case INTERESTED:
                break;
            case NOT_INTERESTED:
                break;
            case HAVE:
                break;
            case BITFIELD:
                break;
            case REQUEST:
                break;
            case PIECE:
                break;
            case CANCEL:
                break;
            default:
                break;
        }
    }

}
