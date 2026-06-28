/*
 * Decompiled with CFR 0.152.
 */
package mahikariui.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import mahikariui.utils.OSUtils;
import mahikariui.utils.StringUtils;

public class UrlUtils {
    private static final String USER_AGENT = "mahikariui/v1.0.3";
    private static final int MAX_HTTP_REDIRECTS = Integer.getInteger("http.maxRedirects", 20);
    private static final int HTTP_TIMEOUT_SECS = Integer.getInteger("http.timeoutSecs", 15);

    /*
     * Enabled aggressive block sorting
     */
    public static InputStream getURLStream(URI url) throws Exception {
        String encoding;
        boolean isGzipEncoded;
        URLConnection connection;
        if (OSUtils.JAVA_SPEC < 1.8f) {
            System.setProperty("https.protocols", "TLSv1.2");
        }
        URI currentUrl = url;
        int redirects = 0;
        while (true) {
            if (redirects >= MAX_HTTP_REDIRECTS) {
                throw new IOException("Too many redirects while trying to fetch " + String.valueOf(url));
            }
            connection = url.toURL().openConnection();
            connection.addRequestProperty("Accept-Encoding", "gzip");
            connection.addRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(HTTP_TIMEOUT_SECS * 1000);
            if (!(connection instanceof HttpURLConnection)) break;
            HttpURLConnection huc = (HttpURLConnection)connection;
            huc.setInstanceFollowRedirects(false);
            int responseCode = huc.getResponseCode();
            if (responseCode < 300 || responseCode > 399) break;
            String loc = huc.getHeaderField("Location");
            if (StringUtils.isNullOrEmpty(loc)) {
                throw new IOException("Got a 3xx response code but Location header was null while trying to fetch " + String.valueOf(url));
            }
            currentUrl = new URI(currentUrl.toString());
            currentUrl.resolve(loc);
            ++redirects;
        }
        boolean bl = isGzipEncoded = !StringUtils.isNullOrEmpty(encoding = connection.getContentEncoding()) && encoding.equals("gzip");
        if (isGzipEncoded) {
            return new GZIPInputStream(connection.getInputStream());
        }
        return connection.getInputStream();
    }

    public static String readerToString(BufferedReader reader) throws Exception {
        String response = reader.lines().collect(Collectors.joining("\n"));
        reader.close();
        return response;
    }

    public static String getURLText(URI url, String encoding) throws Exception {
        return UrlUtils.readerToString(UrlUtils.getURLReader(url, encoding));
    }

    public static String getURLText(String url, String encoding) throws Exception {
        return UrlUtils.readerToString(UrlUtils.getURLReader(url, encoding));
    }

    public static BufferedReader getURLReader(String url, String encoding) throws Exception {
        return UrlUtils.getURLReader(new URI(url), encoding);
    }

    public static BufferedReader getURLReader(URI url, String encoding) throws Exception {
        return new BufferedReader(UrlUtils.getURLStreamReader(url, encoding));
    }

    public static InputStreamReader getURLStreamReader(URI url, String encoding) throws Exception {
        return new InputStreamReader(UrlUtils.getURLStream(url), Charset.forName(encoding));
    }
}

