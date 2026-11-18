package example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

public final class ShippingQuoteHttpTest {
    public static void main(String[] args) throws Exception {
        var server = HealthServer.start(0);
        var base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());

        try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            var page = client.send(HttpRequest.newBuilder(base.resolve("/")).build(),
                    HttpResponse.BodyHandlers.ofString());
            check(page.statusCode(), 200, "page status");
            check(page.body().contains("id=\"quote-form\""), true, "page packaged in classpath");
            check(page.headers().firstValue("Cache-Control").orElse(""), "no-store", "page cache");
            assertResponse(client, base, "GET", "/version", 200,
                    "{\"version\":\"shipping-ui-v2\",\"freeShippingThreshold\":40000,\"standardShippingFee\":3000,\"expressSurcharge\":2000}", "application/json");
            assertResponse(client, base, "GET", "/missing", 404,
                    "{\"error\":\"not_found\"}", "application/json");
            assertResponse(client, base, "POST", "/", 405,
                    "{\"error\":\"method_not_allowed\"}", "application/json");
            assertResponse(client, base, "GET", "/health", 200, "ok", "text/plain");
            assertResponse(client, base, "GET", "/shipping-quote?subtotal=29999", 200,
                    "{\"subtotal\":29999,\"shippingFee\":3000,\"total\":32999}", "application/json");
            assertResponse(client, base, "GET", "/shipping-quote?subtotal=30000", 200,
                    "{\"subtotal\":30000,\"shippingFee\":3000,\"total\":33000}", "application/json");

            // 이전 API 호출도 유지하면서 새 정책의 경계값과 배송 옵션을 확인한다.
            assertResponse(client, base, "GET", "/shipping-quote?subtotal=35000", 200,
                    "{\"subtotal\":35000,\"shippingFee\":3000,\"total\":38000}", "application/json");
            assertResponse(client, base, "GET", "/shipping-quote?subtotal=40000", 200,
                    "{\"subtotal\":40000,\"shippingFee\":0,\"total\":40000}", "application/json");
            assertResponse(client, base, "GET", "/shipping-quote?subtotal=39999&delivery=standard", 200,
                    "{\"subtotal\":39999,\"shippingFee\":3000,\"total\":42999}", "application/json");
            assertResponse(client, base, "GET", "/shipping-quote?subtotal=35000&delivery=express", 200,
                    "{\"subtotal\":35000,\"shippingFee\":5000,\"total\":40000}", "application/json");
            assertResponse(client, base, "GET", "/shipping-quote?subtotal=40000&delivery=express", 200,
                    "{\"subtotal\":40000,\"shippingFee\":2000,\"total\":42000}", "application/json");
            for (String query : new String[] {"", "?subtotal=-1", "?subtotal=abc",
                    "?subtotal=1000000000", "?subtotal=10&subtotal=20", "?subtotal=35000&delivery=unknown",
                    "?subtotal=35000&delivery=express&delivery=standard"}) {
                assertResponse(client, base, "GET", "/shipping-quote" + query, 400,
                        "{\"error\":\"invalid_subtotal\"}", "application/json");
            }

            var rejected = assertResponse(client, base, "POST", "/shipping-quote?subtotal=30000", 405,
                    "{\"error\":\"method_not_allowed\"}", "application/json");
            check(rejected.headers().firstValue("Allow").orElse(""), "GET", "Allow header");
            assertResponse(client, base, "GET", "/shipping-quote/other?subtotal=30000", 404,
                    "{\"error\":\"not_found\"}", "application/json");
        } finally {
            server.stop(0);
        }
        System.out.println("HTTP checks passed: page, version, routing, /health, shipping policy boundaries and delivery options");
    }

    private static HttpResponse<String> assertResponse(HttpClient client, URI base, String method,
            String path, int status, String body, String contentType) throws Exception {
        var request = HttpRequest.newBuilder(base.resolve(path))
                .timeout(Duration.ofSeconds(5))
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        check(response.statusCode(), status, path + " status");
        check(response.body().strip(), body, path + " body");
        check(response.headers().firstValue("Content-Type").orElse(""),
                contentType + "; charset=utf-8", path + " content type");
        return response;
    }

    private static void check(Object actual, Object expected, String description) {
        if (!Objects.equals(actual, expected)) {
            throw new AssertionError(description + ": expected " + expected + ", got " + actual);
        }
    }
}
