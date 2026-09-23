package network;

public class Message {

    private final int length;
    private final MessageType type;
    private final byte[] payload;

    public Message(int length, MessageType type, byte[] payload) {
        this.length = length;
        this.type = type;
        this.payload = payload;
    }

    public int getLength() {
        return length;
    }

    public MessageType getType() {
        return type;
    }

    public byte[] getPayload() {
        return payload;
    }
}
