package base;

import network.TrackerClient;
import torrent.*;

import java.io.File;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

public class Session {

    private final TrackerClient trackerClient;
    private final ExecutorService executorService;
    private volatile ErrorListener errorListener = (msg, t) -> {};

    private final Map<String, TorrentHandle> torrentHandles = new ConcurrentHashMap<>();
    private final byte[] peerId;

    public Session(TrackerClient trackerClient, ExecutorService executorService) {
        this.trackerClient = trackerClient;
        this.executorService = executorService;

        peerId = new byte[20];
        new SecureRandom().nextBytes(peerId);
    }

    public TrackerClient getTrackerClient() {
        return trackerClient;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void setErrorListener(ErrorListener errorListener) {
        this.errorListener = errorListener;
    }

    public byte[] getPeerId() {
        return peerId;
    }

    public void runReporting(String errorMsg, Runnable task) {
        executorService.submit(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                errorListener.onError(errorMsg, t);
            }
        });
    }

    public void createTorrent(File file, String trackerUrl, File torrentDestination) {
        TorrentCreator torrentCreator = new TorrentCreator();
        Torrent torrent = torrentCreator.createFromFile(file, trackerUrl);

        TorrentWriter torrentWriter = new TorrentWriter();
        torrentWriter.write(torrent, torrentDestination);
    }

    public void addTorrent(File torrentFile, Path saveDir) {
        TorrentReader reader = new TorrentReader();
        byte[] torrentFileBytes = reader.read(torrentFile);

        TorrentCreator creator = new TorrentCreator();
        Torrent torrent = creator.createFromBytes(torrentFileBytes);

        TorrentHandle handle = new TorrentHandle(this,
            trackerClient,
            torrent,
            saveDir,
            peerId,
            executorService
        );

        String handleKey = handle.getInfoHashHex();
        torrentHandles.put(handleKey, handle);

        runReporting("Couldn't start torrent.", () -> {
            try {
                handle.start();
            } catch (Throwable t) {
                torrentHandles.remove(handleKey, handle);
                handle.close();
                throw t;
            }
        });
    }
}
