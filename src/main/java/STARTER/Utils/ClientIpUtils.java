package STARTER.Utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ClientIpUtils {

    public static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");

        if (forwarded != null && !forwarded.isBlank()) {
            return normalize(forwarded.split(",")[0]);
        }

        String realIp = request.getHeader("X-Real-IP");

        if (realIp != null && !realIp.isBlank()) {
            return normalize(realIp);
        }

        return normalize(request.getRemoteAddr());
    }

    public static String normalize(String ip) {

        if (ip == null || ip.isBlank()) {
            return "-";
        }

        String value = ip.trim();

        if ("0:0:0:0:0:0:0:1".equals(value) || "::1".equals(value)) {
            return "127.0.0.1"; // localhost real path
        }

        if (value.regionMatches(true, 0, "::ffff:", 0, 7)) {
            return value.substring(7);
        }

        int lastColon = value.lastIndexOf(':');

        if (lastColon >= 0 && value.indexOf('.') > lastColon) {
            String ipv4Tail = value.substring(lastColon + 1);
            
            if (ipv4Tail.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
                return ipv4Tail;
            }
        }

        return value;
    }
}
