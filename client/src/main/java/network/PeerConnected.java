package network;

import java.net.Socket;

public interface PeerConnected {

    void onIncomingPeerConnected(Socket socket);

}
