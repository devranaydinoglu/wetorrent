import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class TrackerApplication {

    public static void main(String[] args) throws IOException {
        Tracker tracker = new Tracker(8080);

        tracker.start();
    }

}
