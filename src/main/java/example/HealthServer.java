package example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public final class HealthServer {
    private static final String SHIPPING_PATH = "/shipping-quote";
    private static final String JSON = "application/json; charset=utf-8";
    private static final Pattern SUBTOTAL_QUERY = Pattern.compile("subtotal=([0-9]{1,9})");

    private HealthServer() {}

    public static void main(String[] args) throws IOException {
        int port = Integer.getInteger("service.port", 8080);
        HttpServer server = start(port);
        System.out.println("Listening on " + server.getAddress().getPort());
    }

    static HttpServer start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", exchange ->
                respond(exchange, 200, "text/plain; charset=utf-8", "ok\n"));
        server.createContext(SHIPPING_PATH, HealthServer::shippingQuote);
        server.start();
        return server;
    }

    private static void shippingQuote(HttpExchange exchange) throws IOException {
        if (!SHIPPING_PATH.equals(exchange.getRequestURI().getPath())) {
            respond(exchange, 404, JSON, "{\"error\":\"not_found\"}\n");
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Allow", "GET");
            respond(exchange, 405, JSON, "{\"error\":\"method_not_allowed\"}\n");
            return;
        }

        String query = exchange.getRequestURI().getRawQuery();
        var subtotal = SUBTOTAL_QUERY.matcher(query == null ? "" : query);
        if (!subtotal.matches()) {
            respond(exchange, 400, JSON, "{\"error\":\"invalid_subtotal\"}\n");
            return;
        }

        ShippingQuote quote = ShippingQuote.forSubtotal(Integer.parseInt(subtotal.group(1)));
        String response = """
                {"subtotal":%d,"shippingFee":%d,"total":%d}
                """.formatted(quote.subtotal(), quote.shippingFee(), quote.total());
        respond(exchange, 200, JSON, response);
    }

    private static void respond(HttpExchange exchange, int status, String contentType, String body)
            throws IOException {
        try (exchange) {
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            if ("HEAD".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(status, -1);
                return;
            }
            exchange.sendResponseHeaders(status, response.length);
            exchange.getResponseBody().write(response);
        }
    }
}
