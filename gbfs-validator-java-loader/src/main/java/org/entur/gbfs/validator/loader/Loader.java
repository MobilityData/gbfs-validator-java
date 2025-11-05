/*
 *
 *  *
 *  *
 *  *  * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 *  *  * the European Commission - subsequent versions of the EUPL (the "Licence");
 *  *  * You may not use this work except in compliance with the Licence.
 *  *  * You may obtain a copy of the Licence at:
 *  *  *
 *  *  *   https://joinup.ec.europa.eu/software/page/eupl
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the Licence is distributed on an "AS IS" basis,
 *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  * See the Licence for the specific language governing permissions and
 *  *  * limitations under the Licence.
 *  *
 *
 */

package org.entur.gbfs.validator.loader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.entur.gbfs.validator.loader.auth.Authentication;
import org.entur.gbfs.validator.loader.auth.BasicAuth;
import org.entur.gbfs.validator.loader.auth.BearerTokenAuth;
import org.entur.gbfs.validator.loader.auth.OAuthClientCredentialsGrantAuth;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Loads GBFS (General Bikeshare Feed Specification) files from HTTP/HTTPS URLs or local file system.
 * Manages HTTP connection pooling and parallel file loading using a thread pool.
 * Thread-safe and designed to be used as a singleton bean.
 */
public class Loader {

  private final CloseableHttpClient httpClient;
  private final ExecutorService executorService;
  private final Map<String, String> customHeaders;

  private String getFileName(URI uri) {
    String path = uri.getPath();
    if (path == null || path.isEmpty()) {
      return "unknown_file";
    }
    return new File(path).getName();
  }

  /**
   * Creates a Loader with default configuration.
   * Uses 50 max total connections, 20 max per route, 5 second timeouts, 20 threads, and no custom headers.
   */
  public Loader() {
    this(50, 20, 5, 5, 20, Collections.emptyMap());
  }

  /**
   * Creates a Loader with custom configuration.
   *
   * @param maxTotalConnections maximum number of total HTTP connections in the pool
   * @param maxConnectionsPerRoute maximum number of connections per route
   * @param connectTimeoutSeconds connection timeout in seconds
   * @param responseTimeoutSeconds response timeout in seconds
   * @param threadPoolSize number of threads for parallel loading
   * @param customHeaders custom HTTP headers to include in all requests
   */
  public Loader(
    int maxTotalConnections,
    int maxConnectionsPerRoute,
    int connectTimeoutSeconds,
    int responseTimeoutSeconds,
    int threadPoolSize,
    Map<String, String> customHeaders
  ) {
    this.customHeaders =
      customHeaders != null ? customHeaders : new HashMap<>();

    PoolingHttpClientConnectionManager connectionManager =
      new PoolingHttpClientConnectionManager();
    connectionManager.setMaxTotal(maxTotalConnections);
    connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);

    RequestConfig requestConfig = RequestConfig
      .custom()
      .setConnectTimeout(Timeout.of(connectTimeoutSeconds, TimeUnit.SECONDS))
      .setResponseTimeout(Timeout.of(responseTimeoutSeconds, TimeUnit.SECONDS))
      .build();

    httpClient =
      HttpClients
        .custom()
        .setConnectionManager(connectionManager)
        .setDefaultRequestConfig(requestConfig)
        .build();

    executorService = Executors.newFixedThreadPool(threadPoolSize);
  }

  /**
   * Loads GBFS files from the given discovery file URL without authentication.
   *
   * @param discoveryURIString URL or file path to the GBFS discovery file
   * @return list of loaded files with their content and metadata
   * @throws IOException if an error occurs during loading
   */
  public List<LoadedFile> load(String discoveryURIString) throws IOException {
    return load(discoveryURIString, null);
  }

  /**
   * Loads GBFS files from the given discovery file URL with authentication.
   *
   * @param discoveryURIString URL or file path to the GBFS discovery file
   * @param auth authentication credentials for protected feeds, or null for public feeds
   * @return list of loaded files with their content and metadata
   * @throws IOException if an error occurs during loading
   */
  public List<LoadedFile> load(String discoveryURIString, Authentication auth)
    throws IOException {
    URI discoveryURI = URI.create(discoveryURIString);
    LoadedFile discoveryLoadedFile = loadFile(discoveryURI, auth);

    if (discoveryLoadedFile.fileContents() == null) {
      List<LoadedFile> loadedFiles = new ArrayList<>();
      loadedFiles.add(discoveryLoadedFile);
      return loadedFiles;
    }

    ByteArrayOutputStream discoveryFileCopy = new ByteArrayOutputStream();
    org.apache.commons.io.IOUtils.copy(
      discoveryLoadedFile.fileContents(),
      discoveryFileCopy
    );
    byte[] discoveryFileBytes = discoveryFileCopy.toByteArray();
    discoveryLoadedFile.fileContents().close();

    JSONObject discoveryFileJson = new JSONObject(
      new JSONTokener(new ByteArrayInputStream(discoveryFileBytes))
    );
    // Default to version 1.0 if no version field is present (as in GBFS v1.0)
    String version = discoveryFileJson.optString("version", "1.0");

    List<LoadedFile> loadedFiles = new ArrayList<>();
    // Normalize discovery file name to "gbfs" (without extension) for validator compatibility
    String discoveryFileName = discoveryLoadedFile
      .fileName()
      .replaceFirst("\\.json$", "");
    loadedFiles.add(
      new LoadedFile(
        discoveryFileName,
        discoveryLoadedFile.url(),
        new ByteArrayInputStream(discoveryFileBytes),
        discoveryLoadedFile.language(),
        discoveryLoadedFile.loaderErrors()
      )
    );

    if (version.matches("^3\\.\\d")) {
      loadedFiles.addAll(
        getV3Files(discoveryFileJson, loadedFiles.get(0), auth)
      );
    } else {
      loadedFiles.addAll(
        getPreV3Files(discoveryFileJson, loadedFiles.get(0), auth)
      );
    }

    return loadedFiles;
  }

  private List<LoadedFile> getV3Files(
    JSONObject discoveryFileJson,
    LoadedFile gbfsLoadedFile,
    Authentication auth
  ) {
    List<LoadedFile> loadedFeedFiles = new ArrayList<>();

    try {
      List<CompletableFuture<LoadedFile>> futures = discoveryFileJson
        .getJSONObject("data")
        .getJSONArray("feeds")
        .toList()
        .stream()
        .map(feed -> {
          @SuppressWarnings("unchecked")
          Map<String, Object> feedMap = (Map<String, Object>) feed;
          String url = (String) feedMap.get("url");
          String name = (String) feedMap.get("name");

          return CompletableFuture.supplyAsync(
            () -> {
              LoadedFile loadedFile = loadFile(URI.create(url), auth);
              return new LoadedFile(
                name,
                url,
                loadedFile.fileContents(),
                loadedFile.language(),
                loadedFile.loaderErrors()
              );
            },
            executorService
          );
        })
        .toList();
      loadedFeedFiles.addAll(
        futures.stream().map(CompletableFuture::join).toList()
      );
    } catch (Exception e) {
      // If we can't parse the discovery file structure, return empty list
      // so the discovery file itself can be validated and report proper errors
    }

    return loadedFeedFiles;
  }

  private List<LoadedFile> getPreV3Files(
    JSONObject discoveryFileJson,
    LoadedFile gbfsLoadedFile,
    Authentication auth
  ) {
    List<LoadedFile> loadedFeedFiles = new ArrayList<>();
    List<CompletableFuture<LoadedFile>> futures = new ArrayList<>();

    try {
      discoveryFileJson
        .getJSONObject("data")
        .keys()
        .forEachRemaining(languageKey -> {
          discoveryFileJson
            .getJSONObject("data")
            .getJSONObject(languageKey)
            .getJSONArray("feeds")
            .toList()
            .forEach(feed -> {
              @SuppressWarnings("unchecked")
              Map<String, Object> feedMap = (Map<String, Object>) feed;
              String url = (String) feedMap.get("url");
              String name = (String) feedMap.get("name");

              futures.add(
                CompletableFuture.supplyAsync(
                  () -> {
                    LoadedFile loadedFile = loadFile(URI.create(url), auth);
                    return new LoadedFile(
                      name,
                      url,
                      loadedFile.fileContents(),
                      languageKey,
                      loadedFile.loaderErrors()
                    );
                  },
                  executorService
                )
              );
            });
        });

      loadedFeedFiles.addAll(
        futures.stream().map(CompletableFuture::join).toList()
      );
    } catch (Exception e) {
      // If we can't parse the discovery file structure, return empty list
      // so the discovery file itself can be validated and report proper errors
    }

    return loadedFeedFiles;
  }

  private LoadedFile loadFile(URI fileURI, Authentication auth) {
    String fileName = getFileName(fileURI);
    String url = fileURI.toString();

    if ("file".equals(fileURI.getScheme())) {
      try {
        InputStream stream = getFileInputStream(fileURI);
        return new LoadedFile(fileName, url, stream, null, new ArrayList<>());
      } catch (FileNotFoundException e) {
        List<LoaderError> errors = new ArrayList<>();
        errors.add(new LoaderError("FILE_NOT_FOUND", e.getMessage()));
        return new LoadedFile(fileName, url, null, null, errors);
      }
    } else if (
      "https".equals(fileURI.getScheme()) || "http".equals(fileURI.getScheme())
    ) {
      try {
        InputStream stream = getHTTPInputStream(fileURI, auth);
        return new LoadedFile(fileName, url, stream, null, new ArrayList<>());
      } catch (IOException e) {
        List<LoaderError> errors = new ArrayList<>();
        errors.add(new LoaderError("CONNECTION_ERROR", e.getMessage()));
        return new LoadedFile(fileName, url, null, null, errors);
      } catch (ParseException e) { // Catch ParseException from getHTTPInputStream
        List<LoaderError> errors = new ArrayList<>();
        errors.add(new LoaderError("PARSE_ERROR", e.getMessage()));
        return new LoadedFile(fileName, url, null, null, errors);
      }
    }

    List<LoaderError> errors = new ArrayList<>();
    errors.add(
      new LoaderError(
        "UNSUPPORTED_SCHEME",
        "Scheme not supported: " + fileURI.getScheme()
      )
    );
    return new LoadedFile(fileName, url, null, null, errors);
  }

  private static FileInputStream getFileInputStream(URI fileURI)
    throws FileNotFoundException {
    return new FileInputStream(new File(fileURI));
  }

  private InputStream getHTTPInputStream(URI fileURI, Authentication auth)
    throws IOException, ParseException {
    HttpGet httpGet = new HttpGet(fileURI);

    customHeaders.forEach(httpGet::setHeader);

    if (auth != null) {
      if (auth instanceof BasicAuth basicAuth) {
        String authHeader =
          "Basic " +
          Base64
            .getEncoder()
            .encodeToString(
              (
                basicAuth.getUsername() + ":" + basicAuth.getPassword()
              ).getBytes()
            );
        httpGet.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
      } else if (auth instanceof BearerTokenAuth bearerAuth) {
        httpGet.setHeader(
          HttpHeaders.AUTHORIZATION,
          "Bearer " + bearerAuth.getToken()
        );
      } else if (auth instanceof OAuthClientCredentialsGrantAuth oauth) {
        try {
          String token = fetchOAuthToken(
            oauth.getTokenUrl(),
            oauth.getClientId(),
            oauth.getClientSecret()
          );
          httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        } catch (Exception e) {
          throw new IOException(
            "OAuth token fetch failed: " + e.getMessage(),
            e
          );
        }
      }
    }

    try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
      if (response.getCode() >= 300) {
        EntityUtils.consumeQuietly(response.getEntity());
        throw new IOException(
          "HTTP error fetching file: " +
          response.getCode() +
          " " +
          response.getReasonPhrase()
        );
      }
      String content = EntityUtils.toString(response.getEntity());
      return new ByteArrayInputStream(content.getBytes());
    }
  }

  private String fetchOAuthToken(
    String tokenUrl,
    String clientId,
    String clientSecret
  ) throws IOException, ParseException {
    HttpPost tokenRequest = new HttpPost(tokenUrl);
    tokenRequest.setHeader("Content-Type", "application/x-www-form-urlencoded");
    String body =
      "grant_type=client_credentials&client_id=" +
      clientId +
      "&client_secret=" +
      clientSecret;
    tokenRequest.setEntity(new StringEntity(body));

    try (CloseableHttpResponse response = httpClient.execute(tokenRequest)) {
      if (response.getCode() >= 300) {
        EntityUtils.consumeQuietly(response.getEntity());
        throw new IOException(
          "OAuth token request failed: " +
          response.getCode() +
          " " +
          response.getReasonPhrase()
        );
      }
      String responseString = EntityUtils.toString(response.getEntity());
      JSONObject jsonResponse = new JSONObject(responseString);
      if (!jsonResponse.has("access_token")) {
        throw new IOException(
          "OAuth token response did not contain access_token"
        );
      }
      return jsonResponse.getString("access_token");
    }
  }

  /**
   * Closes the HTTP client and shuts down the thread pool.
   * Attempts graceful shutdown with a 5-second timeout before forcing termination.
   *
   * @throws IOException if an error occurs closing the HTTP client
   */
  public void close() throws IOException {
    if (httpClient != null) {
      httpClient.close();
    }

    if (executorService != null && !executorService.isShutdown()) {
      executorService.shutdown();
      try {
        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
          executorService.shutdownNow();
        }
      } catch (InterruptedException e) {
        executorService.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }
}
