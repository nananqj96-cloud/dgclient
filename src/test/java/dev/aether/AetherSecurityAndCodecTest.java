package dev.aether;

import static org.junit.jupiter.api.Assertions.*;

import dev.aether.config.ConfigShareService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class AetherSecurityAndCodecTest {

   @Test
   void offlineConfigShareRoundTripWorksWithoutNetwork() {
      String sampleJson = "{\"format\":\"aetherclient-config\",\"version\":1,\"name\":\"Main\",\"state\":{\"modules\":{}}}";
      ConfigShareService.Result exported = ConfigShareService.export(sampleJson).join();
      assertTrue(exported.ok());
      assertTrue(exported.message().startsWith("AETHER1:"));

      ConfigShareService.Result imported = ConfigShareService.importCode(exported.message()).join();
      assertTrue(imported.ok());
      assertNotNull(imported.config());
      assertTrue(imported.config().contains("aetherclient-config"));

      ConfigShareService.Result rawImported = ConfigShareService.importCode(sampleJson).join();
      assertTrue(rawImported.ok());

      ConfigShareService.Result invalid = ConfigShareService.importCode("AETHER1:not-valid-gzip").join();
      assertFalse(invalid.ok());
   }

   @Test
   void sourceTreeContainsZeroResidualBrandingOrHttpClients() throws IOException {
      Path mainSrc = Path.of("src/main");
      assertTrue(Files.isDirectory(mainSrc));
      try (Stream<Path> walk = Files.walk(mainSrc)) {
         List<Path> files = walk
            .filter(Files::isRegularFile)
            .filter(p -> {
               String fn = p.getFileName().toString();
               return fn.endsWith(".java") || fn.endsWith(".json");
            })
            .toList();
         assertFalse(files.isEmpty());
         for (Path f : files) {
            String content = Files.readString(f);
            String lower = content.toLowerCase(Locale.ROOT);
            assertFalse(lower.contains("vulxts"), "Residual 'vulxts' found in " + f);
            assertFalse(lower.contains("sub5larp"), "Residual 'sub5larp' found in " + f);
            assertFalse(lower.contains("larpaddon"), "Residual 'larpaddon' found in " + f);
            assertFalse(content.contains("java.net.http.HttpClient"), "Outbound HttpClient found in " + f);
         }
      }
   }
}
