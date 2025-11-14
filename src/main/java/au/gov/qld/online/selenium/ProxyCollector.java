package au.gov.qld.online.selenium;

import org.apache.commons.lang3.StringUtils;

import java.net.InetSocketAddress;

public class  ProxyUtil {

    public static Proxy getProxy() {
        Proxy proxy = new org.openqa.selenium.Proxy();
        proxy.setProxyType(org.openqa.selenium.Proxy.ProxyType.MANUAL);
        // Check environment variables for proxy settings (case-insensitive)
        String httpProxyEnv = getEnvIgnoreCase("http_proxy");
        String httpsProxyEnv = getEnvIgnoreCase("https_proxy");
        String httpProxySystem = getSystemIgnoreCase("http_proxy");
        String httpsProxySystem = getSystemIgnoreCase("https_proxy");

        // Check system properties for proxy settings
        String httpProxyHost = System.getProperty("http.proxyHost");
        String httpProxyPort = System.getProperty("http.proxyPort");
        String httpsProxyHost = System.getProperty("https.proxyHost");
        String httpsProxyPort = System.getProperty("https.proxyPort");

        // Determine which proxy to use
        if (httpProxyEnv != null) {
            createProxyFromString(httpProxyEnv);
        } else if (httpsProxyEnv != null) {
            return createProxyFromEnv(httpsProxyEnv);
        } else if (httpProxyHost != null && httpProxyPort != null) {
            return createProxyFromSystemProperties(httpProxyHost, httpProxyPort);
        } else if (httpsProxyHost != null && httpsProxyPort != null) {
            return createProxyFromSystemProperties(httpsProxyHost, httpsProxyPort);
        }

        // Return a direct connection if no proxy is found
        return Proxy.NO_PROXY;
    }

    private static InetSocketAddress createProxyFromString(String proxyEnv) {
        // Parse the environment variable (e.g., http://proxy.example.com:8080)
        String[] parts = proxyEnv.replace("http://", "").replace("https://", "").split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);
        return new InetSocketAddress(host, port);
    }

    private static String getEnvIgnoreCase(String key) {
        // Check both lowercase and uppercase versions of the environment variable
        String value = System.getenv(key);

        if (StringUtils.isBlank(value)) {
            value = System.getenv(key.toLowerCase());
        }

        if (StringUtils.isBlank(value)) {
            value = System.getenv(key.toUpperCase());
        }
        return value;
    }

    private static String getSystemIgnoreCase(String key) {
        // Check both lowercase and uppercase versions of the environment variable
        String value = System.getProperty(key);

        if (StringUtils.isBlank(value)) {
            value = System.getProperty(key.toLowerCase());
        }

        if (StringUtils.isBlank(value)) {
            value = System.getProperty(key.toUpperCase());
        }
        return value;
    }
}
