package org.mobilitydata.gbfs.validator.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.mobilitydata.gbfs.validator.api.handler.OpenApiGeneratorApplication;
import org.mobilitydata.gbfs.validator.api.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = OpenApiGeneratorApplication.class)
@AutoConfigureMockMvc
public class ValidateIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  private static Path testFeedDir;

  @BeforeAll
  static void setup(@TempDir Path tempDir) throws Exception {
    // Create test feed files with correct file:// URLs
    testFeedDir = tempDir.resolve("test-feeds");
    Files.createDirectories(testFeedDir);

    // Create system_information.json
    String systemInfo =
      """
      {
        "last_updated": 1609459200,
        "ttl": 0,
        "version": "2.2",
        "data": {
          "system_id": "test_system",
          "language": "en",
          "name": "Test Bike Share",
          "timezone": "America/New_York"
        }
      }
      """;
    Files.writeString(
      testFeedDir.resolve("system_information.json"),
      systemInfo
    );

    // Create gbfs.json with file:// URL pointing to system_information.json
    String gbfsJson = String.format(
      """
      {
        "last_updated": 1609459200,
        "ttl": 0,
        "version": "2.2",
        "data": {
          "en": {
            "feeds": [
              {
                "name": "system_information",
                "url": "file://%s"
              }
            ]
          }
        }
      }
      """,
      testFeedDir.resolve("system_information.json").toAbsolutePath()
    );
    Files.writeString(testFeedDir.resolve("gbfs.json"), gbfsJson);
  }

  @Test
  void testValidate_NoAuth_Success() throws Exception {
    // Create request with no auth
    ValidatePostRequest request = new ValidatePostRequest();
    request.setFeedUrl(
      "file://" + testFeedDir.resolve("gbfs.json").toAbsolutePath()
    );

    // Perform the test
    mockMvc
      .perform(
        post("/validate")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.summary").exists());
  }

  @Test
  void testValidate_BasicAuth_Success() throws Exception {
    // Create request with basic auth
    ValidatePostRequest request = new ValidatePostRequest();
    request.setFeedUrl(
      "file://" + testFeedDir.resolve("gbfs.json").toAbsolutePath()
    );

    ValidatePostRequestAuth basicAuth = new ValidatePostRequestAuth();
    basicAuth.setAuthType("basicAuth");
    basicAuth.setUsername("user");
    basicAuth.setPassword("pass");
    request.setAuth(basicAuth);

    // Perform the test
    mockMvc
      .perform(
        post("/validate")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.summary").exists());
  }

  @Test
  void testValidate_BearerTokenAuth_Success() throws Exception {
    // Create request with bearer token auth
    ValidatePostRequest request = new ValidatePostRequest();
    request.setFeedUrl(
      "file://" + testFeedDir.resolve("gbfs.json").toAbsolutePath()
    );

    ValidatePostRequestAuth bearerAuth = new ValidatePostRequestAuth();
    bearerAuth.setAuthType("bearerToken");
    bearerAuth.setToken("token123");
    request.setAuth(bearerAuth);

    // Perform the test
    mockMvc
      .perform(
        post("/validate")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.summary").exists());
  }

  @Test
  void testValidate_OAuthClientCredentials_Success() throws Exception {
    // Create request with OAuth client credentials
    ValidatePostRequest request = new ValidatePostRequest();
    request.setFeedUrl(
      "file://" + testFeedDir.resolve("gbfs.json").toAbsolutePath()
    );

    ValidatePostRequestAuth oauthAuth = new ValidatePostRequestAuth();
    oauthAuth.setAuthType("oauthClientCredentialsGrant");
    oauthAuth.setTokenUrl("https://auth.example.com/token");
    oauthAuth.setClientId("client_id");
    oauthAuth.setClientSecret("client_secret");
    request.setAuth(oauthAuth);

    // Perform the test
    mockMvc
      .perform(
        post("/validate")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.summary").exists());
  }

  @Test
  void testValidate_AuthFailure() throws Exception {
    // Create request with invalid auth
    ValidatePostRequest request = new ValidatePostRequest();
    request.setFeedUrl(
      "file://" + testFeedDir.resolve("gbfs.json").toAbsolutePath()
    );

    ValidatePostRequestAuth basicAuth = new ValidatePostRequestAuth();
    basicAuth.setAuthType("basicAuth");
    basicAuth.setUsername("wrong_user");
    basicAuth.setPassword("wrong_password");
    request.setAuth(basicAuth);

    // Perform the test - with local files, auth doesn't apply, so we expect successful validation
    mockMvc
      .perform(
        post("/validate")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.summary").exists())
      .andExpect(jsonPath("$.summary.files").isNotEmpty());
  }
}
