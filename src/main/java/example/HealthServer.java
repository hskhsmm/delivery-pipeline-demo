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
    private static final Pattern SUBTOTAL_QUERY = Pattern.compile("subtotal=([0-9]{1,9})(?:&delivery=(standard|express))?");

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
        server.createContext("/", HealthServer::page);
        server.start();
        return server;
    }

    /** 화면과 API를 같은 서버에서 제공해 배포된 백엔드에 직접 요청한다. */
    private static void page(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (!path.equals("/") && !path.equals("/version")) {
            respond(exchange, 404, JSON, "{\"error\":\"not_found\"}\n");
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Allow", "GET");
            respond(exchange, 405, JSON, "{\"error\":\"method_not_allowed\"}\n");
            return;
        }
        // 새 버전 시연 시 이 버전과 ShippingQuote의 기준 금액을 함께 변경한다.
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        if (path.equals("/version")) {
            respond(exchange, 200, JSON,
                    "{\"version\":\"shipping-ui-v2\",\"freeShippingThreshold\":%d,\"standardShippingFee\":%d,\"expressSurcharge\":%d}\n"
                            .formatted(ShippingQuote.FREE_SHIPPING_THRESHOLD, ShippingQuote.STANDARD_SHIPPING_FEE, ShippingQuote.EXPRESS_SURCHARGE));
            return;
        }
        try (var resource = HealthServer.class.getResourceAsStream("/index.html")) {
            if (resource == null) {
                respond(exchange, 500, JSON, "{\"error\":\"page_unavailable\"}\n");
                return;
            }
            respond(exchange, 200, "text/html; charset=utf-8",
                    new String(resource.readAllBytes(), StandardCharsets.UTF_8));
        }
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

        ShippingQuote quote = ShippingQuote.forSubtotal(Integer.parseInt(subtotal.group(1)),
                "express".equals(subtotal.group(2)));
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
