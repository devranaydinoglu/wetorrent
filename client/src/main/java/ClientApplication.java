
import base.Session;
import network.TrackerClient;
import ui.Window;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientApplication {

    private static final int DISK_THREADS = 2;

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
        ExecutorService diskExecutor = Executors.newFixedThreadPool(
            DISK_THREADS,
            Thread.ofPlatform().daemon().name("disk-io-", 1).factory()
        );
        TrackerClient trackerClient = new TrackerClient();

        Session session = new Session(trackerClient, executorService, diskExecutor);

        Window window = new Window(session);
        window.start();
    }

}
