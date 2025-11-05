package org.entur.gbfs.validator.loader;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.apache.hc.core5.http.HttpHeaders;
import org.entur.gbfs.validator.loader.auth.BasicAuth;
import org.entur.gbfs.validator.loader.auth.BearerTokenAuth;
import org.entur.gbfs.validator.loader.auth.OAuthClientCredentialsGrantAuth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LoaderTest {

  private WireMockServer wireMockServer;
  private Loader loader;

  private String gbfsDiscoveryJson =
    "{\"version\": \"3.0\", \"data\": {\"feeds\": []}}";
  private String systemInformationJson =
    "{\"system_id\": \"test-system\", \"language\": \"en\", \"name\": \"Test System\"}";

  @BeforeEach
  void setUp() {
    wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
    wireMockServer.start();
    WireMock.configureFor("localhost", wireMockServer.port());
    loader = new Loader();
  }

  @AfterEach
  void tearDown() throws IOException {
    wireMockServer.stop();
    loader.close();
  }

  private String getBaseUrl() {
    return "http://localhost:" + wireMockServer.port();
  }

  private String convertStreamToString(InputStream is) throws IOException {
    if (is == null) {
      return null;
    }
    try (java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A")) {
      return s.hasNext() ? s.next() : "";
    }
  }

  @Test
  void testLoadFile_NoAuth_Success() throws IOException {
    stubFor(
      get(urlEqualTo("/gbfs.json"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(gbfsDiscoveryJson)
        )
    );

    List<LoadedFile> files = loader.load(getBaseUrl() + "/gbfs.json");

    assertNotNull(files);
    assertEquals(1, files.size());
    LoadedFile gbfsFile = files.get(0);
    assertNotNull(gbfsFile.fileContents());
    assertTrue(gbfsFile.loaderErrors().isEmpty(), "Expected no system errors");
    assertEquals(
      gbfsDiscoveryJson,
      convertStreamToString(gbfsFile.fileContents())
    );

    wireMockServer.verify(
      getRequestedFor(urlEqualTo("/gbfs.json"))
        .withoutHeader(HttpHeaders.AUTHORIZATION)
    );
  }

  @Test
  void testLoadFile_BasicAuth_Success() throws IOException {
    String username = "testuser";
    String password = "testpassword";
    String expectedAuthHeader =
      "Basic " +
      Base64
        .getEncoder()
        .encodeToString(
          (username + ":" + password).getBytes(StandardCharsets.UTF_8)
        );

    stubFor(
      get(urlEqualTo("/gbfs.json"))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo(expectedAuthHeader))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(gbfsDiscoveryJson)
        )
    );

    BasicAuth basicAuth = new BasicAuth(username, password);
    List<LoadedFile> files = loader.load(
      getBaseUrl() + "/gbfs.json",
      basicAuth
    );

    assertNotNull(files);
    assertEquals(1, files.size());
    LoadedFile gbfsFile = files.get(0);
    assertNotNull(gbfsFile.fileContents());
    assertTrue(gbfsFile.loaderErrors().isEmpty(), "Expected no system errors");
    assertEquals(
      gbfsDiscoveryJson,
      convertStreamToString(gbfsFile.fileContents())
    );

    wireMockServer.verify(
      getRequestedFor(urlEqualTo("/gbfs.json"))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo(expectedAuthHeader))
    );
  }

  @Test
  void testLoadFile_BasicAuth_Failure_WrongCredentials() throws IOException {
    stubFor(
      get(urlEqualTo("/gbfs.json")).willReturn(aResponse().withStatus(401))
    ); // Mock server returns 401 for any auth

    BasicAuth basicAuth = new BasicAuth("wronguser", "wrongpassword");
    List<LoadedFile> files = loader.load(
      getBaseUrl() + "/gbfs.json",
      basicAuth
    );

    assertNotNull(files);
    assertEquals(1, files.size());
    LoadedFile gbfsFile = files.get(0);
    assertNull(gbfsFile.fileContents());
    assertFalse(gbfsFile.loaderErrors().isEmpty(), "Expected system errors");
    assertEquals("CONNECTION_ERROR", gbfsFile.loaderErrors().get(0).error());
    assertTrue(gbfsFile.loaderErrors().get(0).message().contains("401"));
  }

  @Test
  void testLoadFile_BearerTokenAuth_Success() throws IOException {
    String token = "test_token";
    String expectedAuthHeader = "Bearer " + token;

    stubFor(
      get(urlEqualTo("/gbfs.json"))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo(expectedAuthHeader))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(gbfsDiscoveryJson)
        )
    );

    BearerTokenAuth bearerAuth = new BearerTokenAuth(token);
    List<LoadedFile> files = loader.load(
      getBaseUrl() + "/gbfs.json",
      bearerAuth
    );

    assertNotNull(files);
    assertEquals(1, files.size());
    LoadedFile gbfsFile = files.get(0);
    assertNotNull(gbfsFile.fileContents());
    assertTrue(gbfsFile.loaderErrors().isEmpty(), "Expected no system errors");
    assertEquals(
      gbfsDiscoveryJson,
      convertStreamToString(gbfsFile.fileContents())
    );

    wireMockServer.verify(
      getRequestedFor(urlEqualTo("/gbfs.json"))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo(expectedAuthHeader))
    );
  }

  @Test
  void testLoadFile_OAuthClientCredentials_Success() throws IOException {
    String clientId = "testClient";
    String clientSecret = "testSecret";
    String token = "oauth_test_token";
    String tokenUrl = "/oauth/token";
    String gbfsUrl = "/gbfs.json";

    // 1. Mock OAuth token endpoint
    stubFor(
      post(urlEqualTo(tokenUrl))
        .withHeader(
          "Content-Type",
          equalTo("application/x-www-form-urlencoded")
        )
        .withRequestBody(containing("grant_type=client_credentials"))
        .withRequestBody(containing("client_id=" + clientId))
        .withRequestBody(containing("client_secret=" + clientSecret))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              "{\"access_token\": \"" +
              token +
              "\", \"token_type\": \"Bearer\"}"
            )
        )
    );

    // 2. Mock GBFS file endpoint
    stubFor(
      get(urlEqualTo(gbfsUrl))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + token))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(gbfsDiscoveryJson)
        )
    );

    OAuthClientCredentialsGrantAuth oauthAuth =
      new OAuthClientCredentialsGrantAuth(
        clientId,
        clientSecret,
        getBaseUrl() + tokenUrl
      );
    List<LoadedFile> files = loader.load(getBaseUrl() + gbfsUrl, oauthAuth);

    assertNotNull(files);
    assertEquals(1, files.size());
    LoadedFile gbfsFile = files.get(0);
    assertNotNull(
      gbfsFile.fileContents(),
      "File contents should not be null on success"
    );
    assertTrue(
      gbfsFile.loaderErrors().isEmpty(),
      "Expected no system errors. Errors: " + gbfsFile.loaderErrors()
    );
    assertEquals(
      gbfsDiscoveryJson,
      convertStreamToString(gbfsFile.fileContents())
    );

    wireMockServer.verify(postRequestedFor(urlEqualTo(tokenUrl)));
    wireMockServer.verify(
      getRequestedFor(urlEqualTo(gbfsUrl))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + token))
    );
  }

  @Test
  void testLoadFile_OAuthClientCredentials_TokenFetchFailure()
    throws IOException {
    String clientId = "testClient";
    String clientSecret = "testSecret";
    String tokenUrl = "/oauth/token";
    String gbfsUrl = "/gbfs.json";

    // Mock OAuth token endpoint to return an error
    stubFor(
      post(urlEqualTo(tokenUrl))
        .willReturn(aResponse().withStatus(500).withBody("OAuth server error"))
    );

    OAuthClientCredentialsGrantAuth oauthAuth =
      new OAuthClientCredentialsGrantAuth(
        clientId,
        clientSecret,
        getBaseUrl() + tokenUrl
      );
    List<LoadedFile> files = loader.load(getBaseUrl() + gbfsUrl, oauthAuth);

    assertNotNull(files);
    assertEquals(1, files.size());
    LoadedFile gbfsFile = files.get(0);
    assertNull(gbfsFile.fileContents());
    assertFalse(
      gbfsFile.loaderErrors().isEmpty(),
      "Expected system errors due to token fetch failure"
    );
    LoaderError error = gbfsFile.loaderErrors().get(0);
    assertEquals("CONNECTION_ERROR", error.error()); // Loader wraps it in CONNECTION_ERROR
    assertTrue(
      error.message().contains("OAuth token fetch failed"),
      "Error message should indicate token fetch failure. Was: " +
      error.message()
    );

    wireMockServer.verify(postRequestedFor(urlEqualTo(tokenUrl)));
    wireMockServer.verify(0, getRequestedFor(urlEqualTo(gbfsUrl))); // GBFS endpoint should not be called
  }

  @Test
  void testLoad_WithDiscoveryFileAndFeed_V3_WithAuth() throws IOException {
    String token = "test_token_v3";
    String expectedAuthHeader = "Bearer " + token;
    String discoveryUrl = "/gbfs-v3.json";
    String systemInfoUrl = "/system_information-v3.json";

    String discoveryContentWithFeed = String.format(
      "{\"version\": \"3.0\", \"data\": {\"feeds\": [{\"name\": \"system_information\", \"url\": \"%s%s\"}]}}",
      getBaseUrl(),
      systemInfoUrl
    );

    // Mock discovery file
    stubFor(
      get(urlEqualTo(discoveryUrl))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo(expectedAuthHeader))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(discoveryContentWithFeed)
        )
    );

    // Mock system_information file
    stubFor(
      get(urlEqualTo(systemInfoUrl))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo(expectedAuthHeader))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(systemInformationJson)
        )
    );

    BearerTokenAuth bearerAuth = new BearerTokenAuth(token);
    List<LoadedFile> files = loader.load(
      getBaseUrl() + discoveryUrl,
      bearerAuth
    );

    assertNotNull(files);
    assertEquals(2, files.size(), "Expected discovery file and one feed file");

    LoadedFile discoveryFile = files
      .stream()
      .filter(f -> f.fileName().equals("gbfs-v3"))
      .findFirst()
      .orElse(null);
    LoadedFile systemInfoFile = files
      .stream()
      .filter(f -> f.fileName().equals("system_information"))
      .findFirst()
      .orElse(null);

    assertNotNull(discoveryFile, "Discovery file should be loaded");
    assertNotNull(discoveryFile.fileContents());
    assertTrue(
      discoveryFile.loaderErrors().isEmpty(),
      "Discovery file should have no errors"
    );
    assertEquals(
      discoveryContentWithFeed,
      convertStreamToString(discoveryFile.fileContents())
    );

    assertNotNull(systemInfoFile, "System Information file should be loaded");
    assertNotNull(systemInfoFile.fileContents());
    assertTrue(
      systemInfoFile.loaderErrors().isEmpty(),
      "System Information file should have no errors. Errors: " +
      (
        systemInfoFile.loaderErrors().isEmpty()
          ? "None"
          : systemInfoFile.loaderErrors().get(0).toString()
      )
    );
    assertEquals(
      systemInformationJson,
      convertStreamToString(systemInfoFile.fileContents())
    );

    wireMockServer.verify(
      getRequestedFor(urlEqualTo(discoveryUrl))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo(expectedAuthHeader))
    );
    wireMockServer.verify(
      getRequestedFor(urlEqualTo(systemInfoUrl))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo(expectedAuthHeader))
    );
  }
}
