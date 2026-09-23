package torrent;

import base.Session;
import bencode.Bencode;
import filesystem.FileDiscovery;
import network.*;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.net.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class TorrentHandle {

    private static final int MIN_PORT = 6881;
    private static final int MAX_PORT = 6889;

    private final Session session;
    private final PeerManager peerManager;
    private final FileDiscovery fileDiscovery;
    private final TrackerClient trackerClient;
    private final Torrent torrent;
    private final Path saveDir;
    private final byte[] localPeerId;
    private byte[] infoHash;
    private boolean[] bitfield;
    private final ExecutorService executorService;
    private int listenPort;

    public TorrentHandle(
        Session session,
        TrackerClient trackerClient,
        Torrent torrent,
        Path saveDir,
        byte[] localPeerId,
        ExecutorService executorService)
    {
        this.session = session;
        this.trackerClient = trackerClient;
        this.torrent = torrent;
        this.saveDir = saveDir;
        this.localPeerId = localPeerId;
        this.executorService = executorService;
        System.out.println(Arrays.toString(localPeerId));
        byte[] bencodedInfoDict = Bencode.encode(torrent.getTorrentInfo().toMap());
        infoHash = DigestUtils.sha1(bencodedInfoDict);
        this.listenPort = getAvailablePort();

        peerManager = new PeerManager(session, listenPort, infoHash, localPeerId);

        fileDiscovery = new FileDiscovery(saveDir, torrent);
    }

    public String getInfoHashHex() {
        return HexFormat.of().formatHex(infoHash);
    }

    public void start() {
        String localIp = "";
        try {
            localIp = getLanIp();
        } catch (SocketException e) {
            throw new RuntimeException("Failed to get IP.", e);
        }

        AnnounceRequest announceReq = new AnnounceRequest(
            infoHash,
            localPeerId,
            listenPort,
            0,
            0,
            torrent.getTorrentInfo().getLength(),
            localIp,
            null
        );

        bitfield = createBitfield();
        peerManager.setBitfield(bitfield);

        try {
            session.runReporting("Peer listener failed.", peerManager::listenIncoming);

            AnnounceResponse announceRes = trackerClient.sendAnnounceRequest(torrent.getAnnounce(), announceReq);

            for (Peer peer : announceRes.getPeers()) {
                if (!Arrays.equals(peer.getId(), localPeerId))
                    session.runReporting("Peer connection failed.", () -> peerManager.connectToPeer(peer));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            close();
            throw new RuntimeException("Couldn't reach tracker.", e);
        }
    }

    public void close() {
        peerManager.close();
    }

    private int getAvailablePort() throws RuntimeException {
        for (int port = MIN_PORT; port < MAX_PORT; port++) {
            try (ServerSocket ss = new ServerSocket(port)) {
                ss.setReuseAddress(true);
                ss.close();
                return port;
            } catch (IOException e) {}
        }

        throw new RuntimeException("No port available.");
    }

    private String getLanIp() throws SocketException {
        Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
        while (netInterfaces.hasMoreElements()) {
            NetworkInterface netInterface = netInterfaces.nextElement();
            if (netInterface.isLoopback() || !netInterface.isUp())
                continue;

            Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
            while(addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();

                if (addr instanceof Inet4Address &&
                    !addr.isLoopbackAddress() &&
                    !addr.isLinkLocalAddress() &&
                    addr.isSiteLocalAddress()
                ) {
                    return addr.getHostAddress();
                }
            }
        }

        throw new RuntimeException("Failed to get IP.");
    }

    private boolean[] createBitfield() {
        return fileDiscovery.computeBitfield();
    }
}
