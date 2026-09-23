
import base.Session;
import network.TrackerClient;
import ui.Window;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientApplication {

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
        TrackerClient trackerClient = new TrackerClient();

        Session session = new Session(trackerClient, executorService);

        Window window = new Window(session);
        window.start();
    }

}
