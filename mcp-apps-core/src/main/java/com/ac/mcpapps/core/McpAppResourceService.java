package com.ac.mcpapps.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** First release permits self-contained HTML only, with no external network or device access. */
public final class McpAppResourceService {
    public record Resource(String html, String resourceHash, Map<String, Object> meta) {}
    private final McpAppCatalog catalog;
    private final ObjectMapper mapper;
    private final Map<String, String> hashes = new ConcurrentHashMap<>();
    public McpAppResourceService(McpAppCatalog catalog, ObjectMapper mapper) { this.catalog = catalog; this.mapper = mapper; }
    public Resource read(String server, String uri) {
        try {
            var root = mapper.valueToTree(catalog.transport(server).readResource(uri));
            var contents = root.path("contents");
            if (!contents.isArray() || contents.size() != 1) throw invalid();
            var content = contents.get(0);
            if (!uri.equals(content.path("uri").asText())
                    || !"text/html;profile=mcp-app".equals(content.path("mimeType").asText())) throw invalid();
            String html = content.has("text") ? content.path("text").asText()
                    : new String(Base64.getDecoder().decode(content.path("blob").asText()), StandardCharsets.UTF_8);
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            if (bytes.length > 5 * 1024 * 1024 || !html.stripLeading().toLowerCase(Locale.ROOT).startsWith("<!doctype html")) throw invalid();
            // Fail closed when the server asks for features this release cannot enforce.
            var ui = content.path("_meta").path("ui");
            if (!ui.isMissingNode()) {
                for (String key : List.of("connectDomains", "resourceDomains", "frameDomains", "baseUriDomains")) {
                    var domains = ui.path("csp").path(key);
                    if (!domains.isMissingNode() && (!domains.isArray() || !domains.isEmpty())) throw invalid();
                }
                if (ui.has("permissions") && !ui.path("permissions").isEmpty()) throw invalid();
                if (ui.has("domain")) throw invalid();
            }
            String hash = "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            String previous;
            synchronized (hashes) {
                String key = server + "\n" + uri;
                if (!hashes.containsKey(key) && hashes.size() >= 1000) throw new McpAppException("APP_QUOTA_EXCEEDED");
                previous = hashes.putIfAbsent(key, hash);
            }
            if (previous != null && !previous.equals(hash)) throw new McpAppException("APP_VERSION_MISMATCH");
            return new Resource(html, hash, Map.of("ui", Map.of("csp", Map.of("connectDomains", List.of(),
                    "resourceDomains", List.of()), "prefersBorder", true)));
        } catch (McpAppException e) { throw e; }
        catch (Exception e) { throw invalid(); }
    }
    private McpAppException invalid() { return new McpAppException("APP_RESOURCE_INVALID"); }
}
