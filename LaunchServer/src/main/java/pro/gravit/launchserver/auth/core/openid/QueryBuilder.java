package pro.gravit.launchserver.auth.core.openid;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author Xakep_SDK
 */
public class QueryBuilder {
    private final String uri;
    private final StringBuilder query = new StringBuilder();

    public QueryBuilder(String uri) {
        this.uri = uri;
    }

    public static QueryBuilder get(String uri) {
        Objects.requireNonNull(uri, "uri");
        if (uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        return new QueryBuilder(uri);
    }

    public static QueryBuilder post() {
        return new QueryBuilder(null);
    }

    public QueryBuilder addQuery(String key, String value) {
        if (!query.isEmpty()) {
            query.append('&');
        }
        query.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                .append('=')
                .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        return this;
    }

    public String toUriString() {
        if (uri != null) {
            if (query. isEmpty()) {
                return uri;
            }
            return uri + '?' + query;
        }
        return toQueryString();
    }

    public String toQueryString() {
        return query.toString();
    }

    @Override
    public String toString() {
        return toUriString();
    }
}

