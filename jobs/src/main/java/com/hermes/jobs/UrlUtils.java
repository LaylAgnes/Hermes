package com.hermes.jobs;

import java.net.URI;

public class UrlUtils {

    public static String extractDomain(String url) {
        try {
            String host = URI.create(url).getHost();
            if (host == null) return "unknown";

            if (host.startsWith("www."))
                host = host.substring(4);

            return host;
        } catch (Exception e) {
            return "unknown";
        }
    }

    public static String extractCompany(String url) {
        try {
            var uri = URI.create(url);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return "unknown";
            }

            // greenhouse
            if (host.contains("greenhouse")) {
                String[] parts = uri.getPath().split("/");
                if (parts.length > 1) return parts[1];
            }

            // gupy
            if (host.contains("gupy")) {
                return host.split("\\.")[0];
            }

            // fallback
            return host.split("\\.")[0];

        } catch (Exception e) {
            return "unknown";
        }
    }

    public static String extractSource(String url) {
        String host = extractDomain(url);

        if (host.contains("gupy")) return "gupy";
        if (host.contains("greenhouse")) return "greenhouse";
        if (host.contains("lever")) return "lever";
        if (host.contains("workday")) return "workday";

        return "site";
    }
}
