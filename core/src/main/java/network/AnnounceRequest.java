package network;

import java.net.URI;

public class AnnounceRequest {

    private final byte[] infoHash;
    private final byte[] peerId;
    private final int port;
    private final long uploaded;
    private final long downloaded;
    private final long left;
    private final String ip; // Optional
    private final String event; // Optional

    public AnnounceRequest(byte[] infoHash, byte[] peerId, int port, long uploaded, long downloaded, long left, String ip, String event) {
        this.infoHash = infoHash;
        this.peerId = peerId;
        this.port = port;
        this.uploaded = uploaded;
        this.downloaded = downloaded;
        this.left = left;
        this.ip = ip;
        this.event = event;
    }

    public byte[] getInfoHash() {
        return infoHash;
    }

    public byte[] getPeerId() {
        return peerId;
    }

    public int getPort() {
        return port;
    }

    public long getUploaded() {
        return uploaded;
    }

    public long getDownloaded() {
        return downloaded;
    }

    public long getLeft() {
        return left;
    }

    public String getIp() {
        return ip;
    }

    public String getEvent() {
        return event;
    }

    public URI constructUri(String announceUrl) {
        StringBuilder uriString = new StringBuilder();

        uriString.append(announceUrl);
        uriString.append("?info_hash=");
        uriString.append(percentEncode(infoHash));
        uriString.append("&peer_id=");
        uriString.append(percentEncode(peerId));
        uriString.append("&port=");
        uriString.append(port);
        uriString.append("&uploaded=");
        uriString.append(uploaded);
        uriString.append("&downloaded=");
        uriString.append(downloaded);
        uriString.append("&left=");
        uriString.append(left);

        if (ip != null && !ip.trim().isEmpty()) {
            uriString.append("&ip=");
            uriString.append(ip);
        }

        if (event != null && !event.trim().isEmpty()) {
            uriString.append("&event=");
            uriString.append(event);
        }

        return URI.create(uriString.toString());
    }

    private String percentEncode(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 3);

        for (byte b : bytes) {
            int value = b & 0xff;

            if ((value >= 'a' && value <= 'z') ||
                (value >= 'A' && value <= 'Z') ||
                (value >= '0' && value <= '9') ||
                value == '-' || value == '.' ||
                value == '_' || value == '~') {

                result.append((char) value);
            } else {
                result.append('%');
                result.append(Character.toUpperCase(
                    Character.forDigit((value >>> 4) & 0xf, 16)));
                result.append(Character.toUpperCase(
                    Character.forDigit(value & 0xf, 16)));
            }
        }

        return result.toString();
    }

}
