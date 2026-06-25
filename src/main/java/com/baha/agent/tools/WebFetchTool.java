package com.baha.agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.util.Set;

/**
 * Web-fetch tool exposed to the agent. Fetches readable text from a PUBLIC
 * http/https page. Sink with ONE declared network boundary.
 *
 * <p>SSRF defenses: scheme allowlist; rejection of loopback / any-local /
 * link-local / site-local / IPv6 unique-local (fc00::/7) / multicast targets;
 * and redirects disabled so a public page cannot 3xx-bounce to an internal
 * address.
 *
 * <p>Residual (documented) limitation: the guard validates the host's resolved
 * address, then the HTTP client resolves again at connect time — a classic
 * TOCTOU. A determined attacker controlling a domain with very low-TTL DNS
 * could rebind between the two lookups. Fully closing this needs address
 * pinning at the socket layer; out of scope for this MVP and noted in
 * validation-report.md.
 */
@Component
public class WebFetchTool {

    /** Seam so DNS resolution can be faked in tests without real network. */
    @FunctionalInterface
    interface HostResolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    private static final int MAX_CHARS = 4000;

    private final RestClient http;
    private final HostResolver resolver;

    @Autowired
    public WebFetchTool(RestClient.Builder builder) {
        // Pin redirects to NEVER so a public page can't bounce to an internal host.
        this(builder.requestFactory(new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build())).build(),
                InetAddress::getAllByName);
    }

    WebFetchTool(RestClient http, HostResolver resolver) {
        this.http = http;
        this.resolver = resolver;
    }

    @Tool(description = "Fetch readable text from a public http or https web page. Returns plain text.")
    public String fetchUrl(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (Exception e) {
            return "Blocked: not a valid URL.";
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (!ALLOWED_SCHEMES.contains(scheme)) {
            return "Blocked: only http and https URLs are allowed.";
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return "Blocked: URL has no host.";
        }
        String bareHost = (host.startsWith("[") && host.endsWith("]"))
                ? host.substring(1, host.length() - 1)
                : host;

        try {
            for (InetAddress addr : resolver.resolve(bareHost)) {
                if (isPrivate(addr)) {
                    return "Blocked: target host resolves to a private or local address.";
                }
            }
        } catch (UnknownHostException e) {
            return "Blocked: cannot resolve host.";
        }

        try {
            String body = http.get().uri(uri).retrieve().body(String.class);
            return clean(body);
        } catch (Exception e) {
            return "Could not fetch the page: " + e.getMessage();
        }
    }

    private boolean isPrivate(InetAddress addr) {
        return addr.isLoopbackAddress()
                || addr.isAnyLocalAddress()
                || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress()
                || addr.isMulticastAddress()
                || isUniqueLocalIpv6(addr);
    }

    /** IPv6 unique-local addresses (fc00::/7) — internal, but missed by isSiteLocalAddress(). */
    private boolean isUniqueLocalIpv6(InetAddress addr) {
        return addr instanceof Inet6Address && (addr.getAddress()[0] & 0xfe) == 0xfc;
    }

    private String clean(String body) {
        if (body == null || body.isBlank()) {
            return "(empty page)";
        }
        String text = body
                .replaceAll("(?s)<script.*?</script>", " ")
                .replaceAll("(?s)<style.*?</style>", " ")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (text.length() > MAX_CHARS) {
            return text.substring(0, MAX_CHARS) + " …[truncated]";
        }
        return text;
    }
}
