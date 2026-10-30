package com.insurance.gw.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Central place for framework configuration.
 * Precedence: JVM system property (-Dkey=value, e.g. from Maven/CI)  >  config.properties.
 * That precedence is what lets the same code run locally, in Docker and in a CI pipeline unchanged.
 */
public final class ConfigManager {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = ConfigManager.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in == null) {
                throw new IllegalStateException("config.properties not found on the classpath");
            }
            PROPS.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load config.properties", e);
        }
    }

    private ConfigManager() {
    }

    public static String get(String key) {
        String fromJvm = System.getProperty(key);
        if (fromJvm != null && !fromJvm.isBlank()) {
            return fromJvm.trim();
        }
        String value = PROPS.getProperty(key);
        return value == null ? "" : value.trim();
    }

    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public static boolean getBool(String key) {
        return Boolean.parseBoolean(get(key));
    }

    public static boolean isMockApp() {
        String url = get("base.url");
        return url.isEmpty() || url.equalsIgnoreCase("mock");
    }

    /** URL of the application under test (the bundled mock app unless base.url points at a real environment). */
    public static String baseUrl() {
        if (!isMockApp()) {
            return get("base.url");
        }
        URL page = ConfigManager.class.getClassLoader().getResource("mockapp/index.html");
        if (page == null) {
            throw new IllegalStateException("mockapp/index.html not found on the classpath");
        }
        try {
            return Paths.get(page.toURI()).toUri().toString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
