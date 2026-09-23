package network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class PeerConnector {

    private ServerSocket serverSocket = null;
    private PeerConnected peerConnected;

    private boolean running = false;

    public PeerConnector(int listenPort, PeerConnected peerConnected) {
        try {
            serverSocket = new ServerSocket(listenPort);
            this.peerConnected = peerConnected;
        } catch (IOException e) {
            System.out.println("Failed to create PeerConnector: " + e.getMessage());
        }
    }

    // Listen to incoming connection requests
    public void listen() {
        running = true;
        System.out.println("Listening to incoming peer connections");
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                peerConnected.onIncomingPeerConnected(socket);
            } catch (IOException e) {
                throw new RuntimeException("Failed to listen to peer.", e);
            }
        }
    }

    public void close() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
            System.out.println("Stopped listening to incoming peer connections");
        } catch (IOException ignored) {}
    }

    public Socket initSocket(Peer peer) {
        try {
            return new Socket(peer.getIp(), peer.getPort());
        } catch (IOException e) {
            return null;
        }
    }

}
