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

import org.entur.gbfs.validation.model.FileValidationError;
import org.entur.gbfs.validation.model.FileValidationResult;
import org.entur.gbfs.validation.validator.versions.Version;
import org.entur.gbfs.validation.validator.versions.VersionFactory;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class FileValidator {
    private static final Logger logger = LoggerFactory.getLogger(FileValidator.class);
    private final Version version;
    private static final Map<String, FileValidator> FILE_VALIDATORS = new ConcurrentHashMap<>();


    public static FileValidator getFileValidator(
            String detectedVersion
    ) {
        if (FILE_VALIDATORS.containsKey(detectedVersion)) {
            return FILE_VALIDATORS.get(detectedVersion);
        } else {
            Version version = VersionFactory.createVersion(detectedVersion);

            FileValidator fileValidator = new FileValidator(version);
            FILE_VALIDATORS.put(detectedVersion, fileValidator);
            return fileValidator;
        }
    }

    protected FileValidator(
            Version version
    ) {
        this.version = version;
    }

    public FileValidationResult validate(String feedName, Map<String, JSONObject> feedMap) {
        if (version.getFileNames().contains(feedName)) {
            JSONObject feed = feedMap.get(feedName);
            int errorsCount = 0;
            List<FileValidationError> validationErrors = List.of();

            try {
                version.validate(feedName, feedMap);
            } catch (ValidationException validationException) {
                errorsCount = validationException.getViolationCount();
                validationErrors = mapToValidationErrors(validationException);
            }

            return new FileValidationResult(
                    feedName,
                    isRequired(feedName),
                    feed != null,
                    errorsCount,
                    version.getSchema(feedName, feedMap).toString(),
                    Optional.ofNullable(feed).map(JSONObject::toString).orElse(null),
                    version.getVersionString(),
                    validationErrors
            );
        }

        logger.warn("Schema not found for gbfs feed={} version={}", feedName, version.getVersionString());
        return null;
    }

    List<FileValidationError> mapToValidationErrors(ValidationException validationException) {
        if (validationException.getCausingExceptions().isEmpty()) {
            return List.of(
                    new FileValidationError(
                        validationException.getSchemaLocation(),
                        validationException.getPointerToViolation(),
                        validationException.getMessage()
                )
            );
        } else {
            return validationException.getCausingExceptions().stream()
                    .map(this::mapToValidationErrors)
                    .flatMap(List::stream)
                    .toList();
        }
    }

    private boolean isRequired(String feedName) {
        return version.isFileRequired(feedName);
    }


    public FileValidationResult validateMissingFile(String file) {
        var isRequired = version.isFileRequired(file);
        return new FileValidationResult(
                file,
                isRequired,
                false,
                isRequired ? 1 : 0,
                version.getSchema(file).toString(),
                null,
                version.getVersionString(),
                List.of()
        );
    }
}
