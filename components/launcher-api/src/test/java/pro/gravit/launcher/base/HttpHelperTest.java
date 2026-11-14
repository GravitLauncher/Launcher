package pro.gravit.launcher.base;

import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLSession;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class HttpHelperTest {
    @Test
    void closesUnexpectedContentTypeBody() {
        var body = new TrackingStream();
        var response = new Response(body);
        var handler = new HttpHelper.BasicJsonHttpErrorHandler<>(String.class);
        assertThrows(RuntimeException.class, () -> handler.apply(response));
        assertTrue(body.closed);
    }

    private static final class TrackingStream extends ByteArrayInputStream {
        private boolean closed;
        TrackingStream() { super(new byte[0]); }
        @Override public void close() { closed = true; }
    }

    private record Response(InputStream body) implements HttpResponse<InputStream> {
        public int statusCode() { return 200; }
        public HttpRequest request() { return HttpRequest.newBuilder(uri()).build(); }
        public Optional<HttpResponse<InputStream>> previousResponse() { return Optional.empty(); }
        public HttpHeaders headers() { return HttpHeaders.of(Map.of("Content-Type", List.of("text/html")), (a, b) -> true); }
        public Optional<SSLSession> sslSession() { return Optional.empty(); }
        public URI uri() { return URI.create("http://localhost/test"); }
        public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
    }
}
