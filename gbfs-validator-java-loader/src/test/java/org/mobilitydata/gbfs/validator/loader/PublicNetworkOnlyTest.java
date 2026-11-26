package org.mobilitydata.gbfs.validator.loader;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.apache.hc.client5.http.DnsResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Loader with publicNetworkOnly, as the HTTP API uses it: no local files, no private networks. */
public class PublicNetworkOnlyTest {

  private WireMockServer wireMockServer;
  private Loader loader;

  @BeforeEach
  void setUp() {
    wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
    wireMockServer.start();
    WireMock.configureFor("localhost", wireMockServer.port());
    stubFor(
      get(anyUrl())
        .willReturn(
          aResponse()
            .withBody("{\"version\": \"3.0\", \"data\": {\"feeds\": []}}")
        )
    );
    loader = new Loader(50, 20, 5, 5, 4, Collections.emptyMap(), true);
  }

  @AfterEach
  void tearDown() throws IOException {
    wireMockServer.stop();
    loader.close();
  }

  private static LoaderError onlyError(List<LoadedFile> files) {
    assertEquals(1, files.size());
    assertNull(files.get(0).fileContents());
    assertEquals(1, files.get(0).loaderErrors().size());
    return files.get(0).loaderErrors().get(0);
  }

  @Test
  void loopbackHostsAreRejectedWithoutAnyRequest() throws IOException {
    for (String host : List.of("localhost", "127.0.0.1")) {
      LoaderError error = onlyError(
        loader.load(
          "http://" + host + ":" + wireMockServer.port() + "/gbfs.json"
        )
      );
      assertEquals("CONNECTION_ERROR", error.error(), host);
      assertTrue(
        error.message().contains("non-public address"),
        error.message()
      );
    }
    verify(0, getRequestedFor(anyUrl()));
  }

  @Test
  void localFilesAreRejected(@TempDir Path dir) throws IOException {
    Path secret = Files.writeString(dir.resolve("gbfs.json"), "{}");
    LoaderError error = onlyError(loader.load(secret.toUri().toString()));
    assertEquals("UNSUPPORTED_SCHEME", error.error());
  }

  @Test
  void defaultLoaderStillLoadsLoopbackUrls() throws IOException {
    Loader defaultLoader = new Loader();
    try {
      List<LoadedFile> files = defaultLoader.load(
        "http://localhost:" + wireMockServer.port() + "/gbfs.json"
      );
      assertTrue(files.get(0).loaderErrors().isEmpty());
    } finally {
      defaultLoader.close();
    }
  }

  @Test
  void resolverRejectsHostsResolvingToNonPublicAddresses() throws Exception {
    DnsResolver fake = new DnsResolver() {
      @Override
      public InetAddress[] resolve(String host) throws UnknownHostException {
        return switch (host) {
          case "rebind.example" -> new InetAddress[] {
            InetAddress.getByName("93.184.215.14"),
            InetAddress.getByName("169.254.169.254"),
          };
          default -> new InetAddress[] {
            InetAddress.getByName("93.184.215.14"),
          };
        };
      }

      @Override
      public String resolveCanonicalHostname(String host) {
        return host;
      }
    };
    PublicAddressDnsResolver resolver = new PublicAddressDnsResolver(fake);
    assertThrows(
      UnknownHostException.class,
      () -> resolver.resolve("rebind.example")
    );
    assertEquals(1, resolver.resolve("feeds.example").length);
  }

  @Test
  void publicAndNonPublicAddresses() throws UnknownHostException {
    for (String address : List.of(
      "127.0.0.1",
      "10.1.2.3",
      "172.16.0.1",
      "192.168.1.1",
      "169.254.169.254",
      "100.64.0.1",
      "0.0.0.0",
      "224.0.0.1",
      "255.255.255.255",
      "198.18.0.1",
      "192.0.0.1",
      "::1",
      "::",
      "fe80::1",
      "fd00::1",
      "fc00::1",
      "::ffff:127.0.0.1",
      "::ffff:10.0.0.1"
    )) {
      assertFalse(
        PublicAddressDnsResolver.isPublic(InetAddress.getByName(address)),
        address
      );
    }
    for (String address : List.of(
      "8.8.8.8",
      "93.184.215.14",
      "172.32.0.1",
      "100.128.0.1",
      "2606:4700::1111"
    )) {
      assertTrue(
        PublicAddressDnsResolver.isPublic(InetAddress.getByName(address)),
        address
      );
    }
  }
}
