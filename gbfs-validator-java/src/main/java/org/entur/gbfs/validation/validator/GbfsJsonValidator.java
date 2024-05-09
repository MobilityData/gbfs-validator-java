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

import org.entur.gbfs.validation.GbfsValidator;
import org.entur.gbfs.validation.model.FileValidationResult;
import org.entur.gbfs.validation.model.ValidationResult;
import org.entur.gbfs.validation.model.ValidationSummary;
import org.entur.gbfs.validation.validator.versions.Version;
import org.entur.gbfs.validation.validator.versions.VersionFactory;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class GbfsJsonValidator implements GbfsValidator {
    private static final Logger LOG = LoggerFactory.getLogger(GbfsJsonValidator.class);

    private static final String DEFAULT_VERSION = "2.3";

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
        Map<String, JSONObject> feedMap = parseFeeds(rawFeeds);
        Map<String, FileValidationResult> fileValidations = new HashMap<>();

        FEEDS.stream()
                .map(feed -> validateFile(feed, feedMap))
                .filter(Objects::nonNull)
                .forEach(fileValidationResult -> fileValidations.put(fileValidationResult.file(), fileValidationResult));


        Version version = findVersion(fileValidations);

        List<String> missingFiles = findMissingFiles(version, fileValidations);

        handleMissingFiles(fileValidations, missingFiles, version);

        ValidationSummary summary = new ValidationSummary(
                version.getVersionString(),
                System.currentTimeMillis(),
                fileValidations.values().stream()
                        .filter(Objects::nonNull)
                        .map(FileValidationResult::errorsCount)
                        .reduce(Integer::sum).orElse(0)
        );

        return new ValidationResult(
                summary,
                fileValidations
        );
    }

    private List<String> findMissingFiles(Version version, Map<String, FileValidationResult> fileValidations) {
        return version.getFileNames().stream().filter(Predicate.not(fileValidations::containsKey)).toList();
    }

    @Override
    public FileValidationResult validateFile(String fileName, InputStream file) {
        return validateFile(fileName, Map.of(fileName, new JSONObject(new JSONTokener(file))));
    }

    private void handleMissingFiles(Map<String, FileValidationResult> fileValidations, List<String> missingFiles, Version version) {
        FileValidator fileValidator = FileValidator.getFileValidator(version.getVersionString());
        missingFiles
                        .forEach(file ->
                                fileValidations.put(file, fileValidator.validateMissingFile(file))
                                );
    }

    private Version findVersion(Map<String, FileValidationResult> fileValidations) {
        Set<String> versions = fileValidations.values().stream()
            .filter(Objects::nonNull)
            .map(FileValidationResult::version)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (versions.size() > 1) {
            LOG.warn("Found multiple versions in files while during validation: {}", versions);
        }

        return  VersionFactory.createVersion(
                versions.stream().findFirst().orElse(DEFAULT_VERSION)
        );
    }

    private FileValidationResult validateFile(String feedName, Map<String, JSONObject> feedMap) {
        JSONObject feed = feedMap.get(feedName);
        if (feed == null) {
            return null;
        }

        // Assume no version means version 1.0
        String detectedVersion = "1.0";

        if (feed.has("version")) {
            detectedVersion = feed.getString("version");
        }

        // find correct file validator
        FileValidator fileValidator = FileValidator.getFileValidator(detectedVersion);
        return fileValidator.validate(feedName, feedMap);
    }

    private Map<String, JSONObject> parseFeeds(Map<String, InputStream> rawFeeds) {
        Map<String, JSONObject> feedMap = new HashMap<>();
        rawFeeds.forEach((name, value) -> feedMap.put(name, parseFeed(value)));
        return feedMap;
    }

    private JSONObject parseFeed(InputStream raw) {
        String asString = getFeedAsString(raw);
        return new JSONObject(asString);
    }

    private String getFeedAsString(InputStream rawFeed) {
        StringBuilder stringBuilder = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(rawFeed))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            return null;
        }
        return stringBuilder.toString();
    }
}
