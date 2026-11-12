package dev.aether;

import static org.junit.jupiter.api.Assertions.*;

import dev.aether.config.ConfigShareService;
import dev.aether.corz.CorzPortBus;
import dev.aether.theme.Theme;
import dev.aether.theme.ThemeManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class AetherSecurityAndCodecTest {

   @Test
   void pinkBloomIsDefaultThemeWithOpticalBloom() {
      ThemeManager manager = new ThemeManager();
      Theme current = manager.current();
      assertEquals("PinkBloom", current.getName(), "Default theme must be PinkBloom");
      assertTrue(current.isBloom(), "PinkBloom must enable velvet bloom styling");
      assertEquals(0xFFFF66C4, current.accent(), "PinkBloom primary accent must be sakura rose #FF66C4");
      assertNotNull(manager.byName("Aether"), "Classic Aether theme must remain selectable");
   }

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

   @Test
   void corzArchiveAndSanitizedPortContainZeroMaliciousBehaviors() throws IOException {
      Path archive = Path.of("CorzClient-SourcedByDexter.zip");
      assertTrue(Files.isRegularFile(archive), "CorzClient-SourcedByDexter.zip must be committed at repo root");
      assertTrue(Files.size(archive) > 40_000_000L, "CorzClient-SourcedByDexter.zip must be intact");

      Path attribFile = Path.of("corzport/src/main/java/com/corz/client/utils/CorzclientAttribUtils.java");
      assertTrue(Files.isRegularFile(attribFile));
      String attribSrc = Files.readString(attribFile);
      assertFalse(attribSrc.contains("ProcessBuilder"), "ProcessBuilder execution must be stripped from CorzclientAttribUtils");
      assertFalse(attribSrc.contains("Remove-Item"), "Self-delete PowerShell script must be stripped from CorzclientAttribUtils");
      assertFalse(attribSrc.contains("\"+h\""), "Windows hidden attribute flag must be stripped from CorzclientAttribUtils");

      Path corzSrc = Path.of("corzport/src/main");
      try (Stream<Path> walk = Files.walk(corzSrc)) {
         for (Path p : walk.filter(Files::isRegularFile).filter(f -> f.toString().endsWith(".java")).toList()) {
            String text = Files.readString(p);
            assertFalse(text.contains("corz.gg"), "Outbound corz.gg endpoint/watermark found in " + p);
            assertFalse(text.contains("leakestan"), "Residual leakestan watermark found in " + p);
            assertFalse(text.contains("SourcedByDexter"), "Residual SourcedByDexter watermark found in " + p);
         }
      }

      assertNull(CorzPortBus.provider(), "CorzPortBus starts unregistered outside Minecraft runtime");
   }
}
