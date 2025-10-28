/*
 *
 *
 *  * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 *  * the European Commission - subsequent versions of the EUPL (the "Licence");
 *  * You may not use this work except in compliance with the Licence.
 *  * You may obtain a copy of the Licence at:
 *  *
 *  *   https://joinup.ec.europa.eu/software/page/eupl
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the Licence is distributed on an "AS IS" basis,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the Licence for the specific language governing permissions and
 *  * limitations under the Licence.
 *
 */

package org.entur.gbfs.validation.validator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.entur.gbfs.validation.GbfsValidator;
import org.entur.gbfs.validation.model.FileValidationResult;
import org.entur.gbfs.validation.model.ValidationResult;
import org.entur.gbfs.validation.model.ValidationSummary;
import org.entur.gbfs.validation.model.ValidatorError; // Changed to use model.SystemError
import org.entur.gbfs.validation.validator.versions.Version;
import org.entur.gbfs.validation.validator.versions.VersionFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GbfsJsonValidator implements GbfsValidator {

  private static final Logger LOG = LoggerFactory.getLogger(
    GbfsJsonValidator.class
  );

  private static final String DEFAULT_VERSION = "2.3";

  private record ParsedFeedContainer(
    String feedName,
    JSONObject jsonObject,
    List<ValidatorError> parsingErrors,
    String originalContent
  ) {
    ParsedFeedContainer(
      String feedName,
      JSONObject jsonObject,
      String originalContent
    ) {
      this(feedName, jsonObject, new ArrayList<>(), originalContent);
    }
  }

  private static final List<String> FEEDS = Arrays.asList(
    "gbfs",
    "gbfs_versions",
    "system_information",
    "vehicle_types",
    "station_information",
    "station_status",
    "free_bike_status",
    "vehicle_status",
    "system_hours",
    "system_alerts",
    "system_alerts",
    "system_calendar",
    "system_regions",
    "system_pricing_plans",
    "geofencing_zones"
  );

  @Override
  public ValidationResult validate(Map<String, InputStream> rawFeeds) {
    Map<String, ParsedFeedContainer> parsedFeedsMap = parseFeeds(rawFeeds);
    Map<String, JSONObject> feedMap = parsedFeedsMap
      .entrySet()
      .stream()
      .collect(
        Collectors.toMap(
          Map.Entry::getKey,
          entry -> entry.getValue().jsonObject()
        )
      );
    Map<String, FileValidationResult> fileValidations = new HashMap<>();

    Version version = detectVersionFromParsedFeeds(parsedFeedsMap);

    for (String feedName : FEEDS) {
      ParsedFeedContainer parsedContainer = parsedFeedsMap.get(feedName);

      if (parsedContainer == null) {
        continue;
      }

      if (parsedContainer.jsonObject() == null) {
        // Parsing failed or stream read error
        FileValidationResult result = new FileValidationResult(
          feedName,
          version.isFileRequired(feedName),
          true,
          0,
          version.getSchema(feedName).toString(),
          parsedContainer.originalContent(),
          null,
          Collections.emptyList(),
          parsedContainer.parsingErrors()
        );
        fileValidations.put(feedName, result);
      } else {
        FileValidationResult validationResult = validateFile(feedName, feedMap);
        if (validationResult != null) {
          fileValidations.put(feedName, validationResult);
        }
      }
    }

    // Re-evaluate version based on all successfully validated files, if necessary, or stick to initial.
    // For now, the initial version detection is used for missing file checks.
    version = findVersion(fileValidations); // This uses validated files' versions

    List<String> missingFiles = findMissingFiles(version, fileValidations);
    handleMissingFiles(fileValidations, missingFiles, version); // This creates FVRs for missing files

    ValidationSummary summary = new ValidationSummary(
      version.getVersionString(),
      System.currentTimeMillis(),
      fileValidations
        .values()
        .stream()
        .filter(Objects::nonNull)
        .map(FileValidationResult::errorsCount) // This counts only validation errors
        .reduce(Integer::sum)
        .orElse(0)
    );

    return new ValidationResult(summary, fileValidations);
  }

  private Version detectVersionFromParsedFeeds(
    Map<String, ParsedFeedContainer> parsedFeeds
  ) {
    ParsedFeedContainer gbfsContainer = parsedFeeds.get("gbfs");
    if (gbfsContainer != null && gbfsContainer.jsonObject() != null) {
      // Use optString to handle v1.0 feeds that don't have a version field
      String versionStr = gbfsContainer.jsonObject().optString("version", null);
      if (versionStr != null && !versionStr.isEmpty()) {
        return VersionFactory.createVersion(versionStr);
      }
    }
    return VersionFactory.createVersion(GbfsJsonValidator.DEFAULT_VERSION);
  }

  private List<String> findMissingFiles(
    Version version,
    Map<String, FileValidationResult> fileValidations
  ) {
    return version
      .getFileNames()
      .stream()
      .filter(Predicate.not(fileValidations::containsKey))
      .toList();
  }

  @Override
  public FileValidationResult validateFile(String fileName, InputStream file) {
    ParsedFeedContainer parsedContainer = parseFeed(fileName, file);

    if (parsedContainer.jsonObject() == null) {
      // Determine version for schema and requirement - this is tricky for a single file
      // For now, using default version. A more robust approach might require context.
      Version tempVersion = VersionFactory.createVersion(DEFAULT_VERSION);
      return new FileValidationResult(
        fileName,
        tempVersion.isFileRequired(fileName),
        true, // File was provided
        0,
        tempVersion.getSchema(fileName).toString(),
        parsedContainer.originalContent(),
        null, // File specific version unknown
        Collections.emptyList(),
        parsedContainer.parsingErrors()
      );
    } else {
      return validateFile(
        fileName,
        Map.of(fileName, parsedContainer.jsonObject())
      );
    }
  }

  private void handleMissingFiles(
    Map<String, FileValidationResult> fileValidations,
    List<String> missingFiles,
    Version version
  ) {
    FileValidator fileValidator = FileValidator.getFileValidator(
      version.getVersionString()
    );
    missingFiles.forEach(file -> {
      FileValidationResult missingResult = fileValidator.validateMissingFile(
        file
      );
      fileValidations.put(file, missingResult);
    });
  }

  private Version findVersion(
    Map<String, FileValidationResult> fileValidations
  ) {
    Set<String> versions = fileValidations
      .values()
      .stream()
      .filter(Objects::nonNull)
      .map(FileValidationResult::version) // Version from the file's content itself
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());

    if (versions.isEmpty()) {
      // If no file provided a version (e.g. all failed parsing or were missing), check gbfs.json specifically
      FileValidationResult gbfsFileResult = fileValidations.get("gbfs");
      if (
        gbfsFileResult != null &&
        gbfsFileResult.fileContents() != null &&
        gbfsFileResult.validatorErrors().isEmpty() &&
        gbfsFileResult.errors().isEmpty()
      ) {
        // Attempt to parse gbfs.json again if it was valid but its version wasn't captured by FileValidationResult.version
        // This is a bit convoluted; ideally, FileValidationResult.version would be reliably populated.
        try {
          JSONObject gbfsJson = new JSONObject(gbfsFileResult.fileContents());
          if (gbfsJson.has("version")) {
            return VersionFactory.createVersion(gbfsJson.getString("version"));
          }
        } catch (JSONException e) {
          LOG.warn(
            "Could not re-parse gbfs.json for version during findVersion, using default.",
            e
          );
        }
      }
    }

    if (versions.size() > 1) {
      LOG.warn(
        "Found multiple versions in files during validation: {}",
        versions
      );
      // Prioritize gbfs.json version if present among multiple versions
      FileValidationResult gbfsFile = fileValidations.get("gbfs");
      if (
        gbfsFile != null &&
        gbfsFile.version() != null &&
        versions.contains(gbfsFile.version())
      ) {
        return VersionFactory.createVersion(gbfsFile.version());
      }
    }

    return VersionFactory.createVersion(
      versions.stream().findFirst().orElse(DEFAULT_VERSION)
    );
  }

  private FileValidationResult validateFile(
    String feedName,
    Map<String, JSONObject> feedMap
  ) {
    JSONObject feed = feedMap.get(feedName);
    if (feed == null) {
      return null;
    }

    String detectedVersion = feed.has("version")
      ? feed.getString("version")
      : "1.0";
    FileValidator fileValidator = FileValidator.getFileValidator(
      detectedVersion
    );
    return fileValidator.validate(feedName, feedMap);
  }

  private Map<String, ParsedFeedContainer> parseFeeds(
    Map<String, InputStream> rawFeeds
  ) {
    Map<String, ParsedFeedContainer> feedMap = new HashMap<>();
    rawFeeds.forEach((name, value) -> feedMap.put(name, parseFeed(name, value))
    );
    return feedMap;
  }

  private ParsedFeedContainer parseFeed(String name, InputStream raw) {
    String asString;
    try (
      BufferedReader reader = new BufferedReader(new InputStreamReader(raw))
    ) {
      asString =
        reader.lines().collect(Collectors.joining(System.lineSeparator()));
    } catch (IOException | UncheckedIOException e) {
      LOG.warn(
        "IOException while reading feed name={}: {}",
        name,
        e.getMessage(),
        e
      );
      return new ParsedFeedContainer(
        name,
        null,
        List.of(
          new ValidatorError(
            "READ_ERROR",
            "IOException reading stream for " + name + ": " + e.getMessage()
          )
        ),
        null
      );
    }

    try {
      return new ParsedFeedContainer(name, new JSONObject(asString), asString);
    } catch (JSONException e) {
      LOG.warn(
        "Failed to parse json for feed name={} content={}: {}",
        name,
        asString,
        e.getMessage(),
        e
      );
      return new ParsedFeedContainer(
        name,
        null,
        List.of(new ValidatorError("PARSE_ERROR", e.getMessage())),
        asString
      );
    }
  }
}
