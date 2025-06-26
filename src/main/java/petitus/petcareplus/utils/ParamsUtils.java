package petitus.petcareplus.utils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ParamsUtils {

    public static Map<String, String> extractRawParams(String queryString) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0 && idx < pair.length() - 1) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = pair.substring(idx + 1); // ❗ Giữ nguyên, KHÔNG decode
                params.put(key, value);
            }
        }
        return params;
    }
}
