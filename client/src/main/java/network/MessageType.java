package network;

public enum MessageType {

    NONE (-1),
    CHOKE (0),
    UNCHOKE (1),
    INTERESTED (2),
    NOT_INTERESTED (3),
    HAVE (4),
    BITFIELD (5),
    REQUEST (6),
    PIECE (7),
    CANCEL (8);

    private final int id;

    MessageType(int id) { this.id = id; }

    public int getId() { return id; }

    public static MessageType getTypeFromInt(int i) {
        for (MessageType t : values())
            if (t.id == i) return t;
        return NONE;
    }

}
