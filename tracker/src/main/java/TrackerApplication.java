
import java.io.IOException;

public class TrackerApplication {

    public static void main(String[] args) throws IOException {
        Tracker tracker = new Tracker(8080);
        tracker.start();
    }

}
