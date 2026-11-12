package dev.aether.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 100% offline, zero-network config sharing codec for Aether Client.
 * Encodes config JSON into compact GZIP + Base64URL tokens ("AETHER1:...")
 * and accepts either "AETHER1:..." tokens or raw config JSON on import.
 */
public final class ConfigShareService {
   public static final String PREFIX = "AETHER1:";
   public static final String NETWORK_DISCLOSURE = "Aether Client config sharing is 100% local/offline via clipboard tokens (zero network calls).";
   private static final int MAX_DECOMPRESSED_BYTES = 2 * 1024 * 1024;

   private ConfigShareService() {
   }

   public static CompletableFuture<Result> export(String configJson) {
      if (configJson == null || configJson.isBlank()) {
         return CompletableFuture.completedFuture(new Result(false, "Save a config before exporting.", null));
      }
      try {
         JsonObject parsed = JsonParser.parseString(configJson).getAsJsonObject();
         String compact = parsed.toString();
         ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
         try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(compact.getBytes(StandardCharsets.UTF_8));
         }
         String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(baos.toByteArray());
         return CompletableFuture.completedFuture(new Result(true, PREFIX + encoded, compact));
      } catch (Exception e) {
         return CompletableFuture.completedFuture(new Result(false, "Failed to encode config token.", null));
      }
   }

   public static CompletableFuture<Result> importCode(String input) {
      if (input == null || input.isBlank()) {
         return CompletableFuture.completedFuture(new Result(false, "Clipboard is empty.", null));
      }
      String trimmed = input.trim();
      if (trimmed.startsWith("{")) {
         try {
            JsonObject obj = JsonParser.parseString(trimmed).getAsJsonObject();
            return CompletableFuture.completedFuture(new Result(true, "Config imported.", obj.toString()));
         } catch (Exception e) {
            return CompletableFuture.completedFuture(new Result(false, "Clipboard JSON is invalid.", null));
         }
      }
      if (!trimmed.startsWith(PREFIX)) {
         return CompletableFuture.completedFuture(new Result(false, "Copy an AETHER1:... config token or JSON first.", null));
      }
      String payload = trimmed.substring(PREFIX.length()).trim();
      if (payload.isEmpty()) {
         return CompletableFuture.completedFuture(new Result(false, "Config token is incomplete.", null));
      }
      try {
         byte[] compressed = Base64.getUrlDecoder().decode(payload);
         ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
         try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            byte[] buf = new byte[4096];
            int total = 0;
            int read;
            while ((read = gis.read(buf)) != -1) {
               total += read;
               if (total > MAX_DECOMPRESSED_BYTES) {
                  return CompletableFuture.completedFuture(new Result(false, "Config token exceeds maximum size.", null));
               }
               out.write(buf, 0, read);
            }
         }
         String json = out.toString(StandardCharsets.UTF_8);
         JsonParser.parseString(json).getAsJsonObject();
         return CompletableFuture.completedFuture(new Result(true, "Config imported.", json));
      } catch (Exception e) {
         return CompletableFuture.completedFuture(new Result(false, "Invalid or corrupted config token.", null));
      }
   }

   public record Result(boolean ok, String message, String config) {
   }
}
