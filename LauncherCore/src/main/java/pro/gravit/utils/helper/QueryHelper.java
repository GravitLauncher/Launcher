package pro.gravit.utils.helper;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class QueryHelper {
    public static Map<String, List<String>> splitUriQuery(URI uri) {
        String query = uri.getRawQuery();
        if (query == null) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> params = new HashMap<>();
        String[] split = query.split("&");
        for (String qParams : split) {
            String[] splitParams = qParams.split("=");
            List<String> strings = params.computeIfAbsent(decode(splitParams[0], StandardCharsets.UTF_8),
                    k -> new ArrayList<>(1));
            strings.add(decode(splitParams[1], StandardCharsets.UTF_8));
        }
        return params;
    }

    public static String encodeFormPair(String key, String value) {
        return encode(key, StandardCharsets.UTF_8) + "=" + encode(value, StandardCharsets.UTF_8);
    }

    private static String encode(String value, Charset charset) {
        try {
            return URLEncoder.encode(value, charset.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String decode(String value, Charset charset) {
        try {
            return URLDecoder.decode(value, charset.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
