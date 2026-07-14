package org.mobilitydata.gbfs.validator.example;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mobilitydata.gbfs.validation.GbfsValidator;
import org.mobilitydata.gbfs.validation.GbfsValidatorFactory;
import org.mobilitydata.gbfs.validation.model.FileValidationError;
import org.mobilitydata.gbfs.validation.model.FileValidationResult;
import org.mobilitydata.gbfs.validation.model.ValidationResult;
import org.mobilitydata.gbfs.validator.loader.LoadedFile;
import org.mobilitydata.gbfs.validator.loader.Loader;

/**
 * Example showing how to validate a GBFS feed using gbfs-validator-java.
 *
 * <p>Two use cases are demonstrated:
 * <ol>
 *   <li>Single-file validation via {@link GbfsValidator#validateFile}</li>
 *   <li>Full-feed validation via {@link Loader} + {@link GbfsValidator#validate}:
 *       the Loader fetches gbfs.json and automatically discovers and loads all
 *       linked feed files, so no manual URL construction is needed.</li>
 * </ol>
 *
 * <p>Usage:
 * <pre>
 *   mvn compile exec:java
 *   # or
 *   gradle run
 * </pre>
 */
public class GbfsValidatorExample {

    // A real public GBFS feed (BIXI Montreal)
    private static final String GBFS_FEED_URL =
        "https://gbfs.velobixi.com/gbfs/gbfs.json";

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("=== GBFS Validator Java Example ===\n");

        GbfsValidator validator = GbfsValidatorFactory.getGbfsJsonValidator();

        // --- Example 1: Validate a single file ---
        // Useful when you already have the file content and just want schema validation.
        System.out.println("Example 1: Validate a single file");
        System.out.println("Fetching: " + GBFS_FEED_URL);
        String gbfsContent = fetchUrl(GBFS_FEED_URL);
        InputStream gbfsStream = new ByteArrayInputStream(gbfsContent.getBytes(StandardCharsets.UTF_8));
        FileValidationResult fileResult = validator.validateFile("gbfs", gbfsStream);
        printFileResult(fileResult);

        // --- Example 2: Validate a full feed ---
        // The Loader fetches gbfs.json, parses the feed URLs from its discovery data,
        // and loads all linked files — handling language prefixes and auth automatically.
        System.out.println("\nExample 2: Validate a full feed");
        Loader loader = new Loader();
        try {
            List<LoadedFile> loadedFiles = loader.load(GBFS_FEED_URL);

            Map<String, InputStream> fileMap = new HashMap<>();
            for (LoadedFile file : loadedFiles) {
                // Keep the discovery file (no language) and only "en" language files.
                // If a feed does not publish "en", swap "en" for the desired language code.
                String lang = file.language();
                if (lang != null && !lang.equals("en")) {
                    continue;
                }
                if (file.fileContents() != null) {
                    System.out.println("  Loaded: " + file.fileName() + " (" + file.url() + ")");
                    fileMap.put(file.fileName(), file.fileContents());
                } else {
                    file.loaderErrors().forEach(e ->
                        System.out.println("  Skipped: " + file.fileName()
                            + " (" + e.error() + ": " + e.message() + ")")
                    );
                }
            }

            ValidationResult feedResult = validator.validate(fileMap);
            printFeedResult(feedResult);
        } finally {
            loader.close();
        }
    }

    private static void printFileResult(FileValidationResult result) {
        System.out.println("  File    : " + result.file());
        System.out.println("  Version : " + result.version());
        System.out.println("  Exists  : " + result.exists());
        System.out.println("  Required: " + result.required());
        System.out.println("  Errors  : " + result.errorsCount());

        if (!result.errors().isEmpty()) {
            System.out.println("  Validation errors:");
            for (FileValidationError error : result.errors()) {
                System.out.println("    - [" + error.keyword() + "] " + error.message());
                System.out.println("      instance: " + error.violationPath());
                System.out.println("      schema  : " + error.schemaPath());
            }
        }
        if (!result.validatorErrors().isEmpty()) {
            System.out.println("  System errors:");
            result.validatorErrors().forEach(e ->
                System.out.println("    - " + e.error() + ": " + e.message())
            );
        }
    }

    private static void printFeedResult(ValidationResult result) {
        System.out.println("  Summary : " + result.summary());
        var presentFiles = result.files().entrySet().stream()
            .filter(e -> e.getValue().exists())
            .toList();
        System.out.println("  Files validated: " + presentFiles.size());
        presentFiles.forEach(e -> {
            System.out.printf("    %-35s errors=%d  version=%s%n",
                e.getKey(), e.getValue().errorsCount(), e.getValue().version());
        });

        long totalErrors = presentFiles.stream()
            .mapToLong(e -> e.getValue().errorsCount())
            .sum();
        System.out.println("\n  Total errors across all files: " + totalErrors);

        if (totalErrors == 0) {
            System.out.println("  ✓ Feed is valid!");
        } else {
            System.out.println("  ✗ Feed has validation errors.");
        }
    }

    private static String fetchUrl(String url) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " for " + url);
        }
        return response.body();
    }
}

