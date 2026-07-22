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
import java.util.Map;
import org.mobilitydata.gbfs.validation.GbfsValidator;
import org.mobilitydata.gbfs.validation.GbfsValidatorFactory;
import org.mobilitydata.gbfs.validation.model.FileValidationError;
import org.mobilitydata.gbfs.validation.model.FileValidationResult;
import org.mobilitydata.gbfs.validation.model.ValidationResult;

/**
 * Example showing how to validate a GBFS feed using gbfs-validator-java.
 *
 * <p>This example:
 * <ol>
 *   <li>Fetches gbfs.json from a public GBFS feed</li>
 *   <li>Validates the file using GbfsValidatorFactory</li>
 *   <li>Prints the validation results to stdout</li>
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

        // --- Example 1: Validate a single file ---
        System.out.println("Example 1: Validate a single file");
        System.out.println("Fetching: " + GBFS_FEED_URL);
        String fileContents = fetchUrl(GBFS_FEED_URL);

        GbfsValidator validator = GbfsValidatorFactory.getGbfsJsonValidator();
        InputStream fileStream = new ByteArrayInputStream(
            fileContents.getBytes(StandardCharsets.UTF_8)
        );

        // The API expects file names WITHOUT the .json extension (e.g. "gbfs", not "gbfs.json")
        FileValidationResult fileResult = validator.validateFile("gbfs", fileStream);
        printFileResult(fileResult);

        // --- Example 2: Validate a full feed (multiple files) ---
        System.out.println("\nExample 2: Validate a full feed");
        Map<String, InputStream> feedFiles = new HashMap<>();

        // Keys must be the GBFS file type name (no .json extension)
        feedFiles.put("gbfs", new ByteArrayInputStream(
            fileContents.getBytes(StandardCharsets.UTF_8)
        ));

        // Fetch additional files — URL uses .json, but map key does not
        String[] additionalFileNames = {
            "system_information",
            "station_information",
            "station_status",
            "free_bike_status",
        };
        String baseUrl = GBFS_FEED_URL.substring(0, GBFS_FEED_URL.lastIndexOf('/') + 1);
        for (String fileType : additionalFileNames) {
            try {
                String content = fetchUrl(baseUrl + fileType + ".json");
                feedFiles.put(fileType, new ByteArrayInputStream(
                    content.getBytes(StandardCharsets.UTF_8)
                ));
                System.out.println("  Loaded: " + fileType);
            } catch (Exception e) {
                System.out.println("  Skipped: " + fileType + " (" + e.getMessage() + ")");
            }
        }

        ValidationResult feedResult = validator.validate(feedFiles);
        printFeedResult(feedResult);
    }

    private static void printFileResult(FileValidationResult result) {
        System.out.println("  File    : " + result.file());
        System.out.println("  Version : " + result.version());
        System.out.println("  Schema  : " + result.schema());
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
        System.out.println("  Files validated: " + result.files().size());
        result.files().forEach((name, fileResult) -> {
            System.out.printf("    %-35s errors=%d  version=%s%n",
                name, fileResult.errorsCount(), fileResult.version());
        });

        long totalErrors = result.files().values().stream()
            .mapToLong(FileValidationResult::errorsCount)
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
        HttpResponse<String> response = client.send(
            request, HttpResponse.BodyHandlers.ofString()
        );
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " for " + url);
        }
        return response.body();
    }
}
