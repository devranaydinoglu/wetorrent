import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class Tracker {

    private static final String[] REQUIRED_QUERY_PARAMS = {
        "infoHash",
        "peerId",
        "port",
        "uploaded",
        "downloaded",
        "left"
    };

    private HttpServer server;
    private int port;

    public Tracker(int port) throws IOException {
        this.port = port;

        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handleRequest);
    }

    public void start() {
        server.start();
        System.out.println("Server listening on port " + this.port);
    }

    public void stop() {
        server.stop(0);
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.set("Content-Type", "text/plain");

        if (!exchange.getRequestMethod().equals("GET"))
            respond(400, "ERROR: Invalid request method", exchange);

        String queryParamsString = exchange.getRequestURI().getQuery();

        Map<String, String> queryParams = new HashMap<>();
        for (String param : queryParamsString.split("&")) {
            String[] entry = param.split("=");
            queryParams.put(entry[0], entry[1]);
        }

        if (!isQueryParamsValid(queryParams))
            respond(400, "ERROR: Missing required query params", exchange);



        respond(200, "SUCCESS", exchange);
    }

    private boolean isQueryParamsValid(Map<String, String> queryParams) {
        for (String name : REQUIRED_QUERY_PARAMS) {
            if (!queryParams.containsKey(name))
                return false;
        }

        return true;
    }

    private void respond(int statusCode, String response, HttpExchange exchange) throws IOException {
        byte[] responseInBytes = response.getBytes();
        exchange.sendResponseHeaders(statusCode, responseInBytes.length);

        OutputStream out = exchange.getResponseBody();
        out.write(responseInBytes);
        out.close();
    }
}
