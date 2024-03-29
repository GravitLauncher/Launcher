package pro.gravit.utils.helper;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class QueryHelper {
    public static Map<String, List<String>> splitUriQuery(URI uri) {
        var query = uri.getRawQuery();
        if (query == null) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> params = new HashMap<>();
        String[] split = query.split("&");
        for (String qParams : split) {
            String[] splitParams = qParams.split("=");
            List<String> strings = params.computeIfAbsent(URLDecoder.decode(splitParams[0], StandardCharsets.UTF_8),
                    k -> new ArrayList<>(1));
            strings.add(URLDecoder.decode(splitParams[1], StandardCharsets.UTF_8));
        }
        return params;
    }

    public static String encodeFormPair(String key, String value) {
        return URLEncoder.encode(key, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
