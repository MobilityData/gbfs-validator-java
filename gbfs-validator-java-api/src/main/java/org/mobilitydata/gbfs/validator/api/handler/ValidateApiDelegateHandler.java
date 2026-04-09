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

package org.mobilitydata.gbfs.validator.api.handler;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mobilitydata.gbfs.validation.GbfsValidator;
import org.mobilitydata.gbfs.validation.GbfsValidatorFactory;
import org.mobilitydata.gbfs.validation.model.FileValidationError;
import org.mobilitydata.gbfs.validation.model.FileValidationResult;
import org.mobilitydata.gbfs.validation.model.ValidationResult;
import org.mobilitydata.gbfs.validation.model.ValidatorError;
import org.mobilitydata.gbfs.validator.api.gen.ValidateApiDelegate;
import org.mobilitydata.gbfs.validator.api.model.FileError;
import org.mobilitydata.gbfs.validator.api.model.GbfsFile;
import org.mobilitydata.gbfs.validator.api.model.SystemError;
import org.mobilitydata.gbfs.validator.api.model.ValidatePostRequest;
import org.mobilitydata.gbfs.validator.api.model.ValidatePostRequestAuth;
import org.mobilitydata.gbfs.validator.api.model.ValidationResultSummary;
import org.mobilitydata.gbfs.validator.loader.LoadedFile;
import org.mobilitydata.gbfs.validator.loader.Loader;
import org.mobilitydata.gbfs.validator.loader.LoaderError;
import org.mobilitydata.gbfs.validator.loader.auth.Authentication;
import org.openapitools.jackson.nullable.JsonNullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Service implementation for GBFS validation API operations.
 * Handles validation requests by loading GBFS files and running them through the validator.
 */
@Service
public class ValidateApiDelegateHandler implements ValidateApiDelegate {

  private static final Logger logger = LoggerFactory.getLogger(
    ValidateApiDelegateHandler.class
  );

  private final Loader loader;
  private final VersionProvider versionProvider;

  /**
   * Creates a new validation handler.
   *
   * @param loader the GBFS file loader to use
   * @param versionProvider provides access to application version information
   */
  public ValidateApiDelegateHandler(
    Loader loader,
    VersionProvider versionProvider
  ) {
    this.loader = loader;
    this.versionProvider = versionProvider;
  }

  /**
   * Cleans up resources when the service is destroyed.
   * Closes the loader's HTTP client and thread pool.
   */
  @PreDestroy
  public void destroy() {
    try {
      if (loader != null) {
        loader.close();
      }
    } catch (IOException e) {
      logger.error("Error closing Loader", e);
    }
  }

  /**
   * Validates a GBFS feed by loading all files and running validation.
   *
   * @param validatePostRequest the validation request containing feed URL and optional authentication
   * @return validation results with file-level errors and system errors
   */
  @Override
  public ResponseEntity<org.mobilitydata.gbfs.validator.api.model.ValidationResult> validatePost(
    ValidatePostRequest validatePostRequest
  ) {
    logger.debug(
      "Received request for url: {}",
      validatePostRequest.getFeedUrl()
    );
    try {
      Authentication loaderAuth = getAuthentication(validatePostRequest);

      List<LoadedFile> allLoadedFiles = loader.load(
        validatePostRequest.getFeedUrl(),
        loaderAuth
      );

      logger.debug("Loaded files: {}", allLoadedFiles.size());

      Multimap<String, LoadedFile> filesByLanguage = MultimapBuilder
        .hashKeys()
        .arrayListValues()
        .build();
      for (LoadedFile loadedFile : allLoadedFiles) {
        String langKey = loadedFile.language() != null
          ? loadedFile.language()
          : "default_lang";
        filesByLanguage.put(langKey, loadedFile);
      }

      GbfsValidator validator = GbfsValidatorFactory.getGbfsJsonValidator();
      List<org.mobilitydata.gbfs.validator.api.model.ValidationResult> resultsPerLanguage =
        new ArrayList<>();

      filesByLanguage
        .asMap()
        .forEach((languageKey, loadedFilesForLang) -> {
          logger.debug("Processing language group: {}", languageKey);
          Map<String, InputStream> validatorInputMap = new HashMap<>();
          List<LoadedFile> currentLanguageLoadedFiles = new ArrayList<>(
            loadedFilesForLang
          );

          for (LoadedFile file : currentLanguageLoadedFiles) {
            if (file.fileContents() != null) {
              validatorInputMap.put(file.fileName(), file.fileContents());
            }
          }

          ValidationResult internalValidationResult = validator.validate(
            validatorInputMap
          );

          resultsPerLanguage.add(
            mapValidationResult(
              internalValidationResult,
              currentLanguageLoadedFiles,
              "default_lang".equals(languageKey) ? null : languageKey
            )
          );
          logger.debug(
            "Processed {} files for language group: {}",
            currentLanguageLoadedFiles.size(),
            languageKey
          );
        });

      return ResponseEntity.ok(mergeValidationResults(resultsPerLanguage));
    } catch (IOException e) {
      logger.error("IOException during validation process", e);
      throw new RuntimeException(e);
    }
  }

  private static Authentication getAuthentication(
    ValidatePostRequest validatePostRequest
  ) {
    Authentication loaderAuth = null;
    ValidatePostRequestAuth apiAuth = validatePostRequest.getAuth();

    if (apiAuth != null) {
      String authType = apiAuth.getAuthType();
      if ("basicAuth".equals(authType)) {
        if (apiAuth.getUsername() != null && apiAuth.getPassword() != null) {
          loaderAuth =
            new org.mobilitydata.gbfs.validator.loader.auth.BasicAuth(
              apiAuth.getUsername(),
              apiAuth.getPassword()
            );
        }
      } else if ("bearerToken".equals(authType)) {
        if (apiAuth.getToken() != null) {
          loaderAuth =
            new org.mobilitydata.gbfs.validator.loader.auth.BearerTokenAuth(
              apiAuth.getToken()
            );
        }
      } else if ("oauthClientCredentialsGrant".equals(authType)) {
        if (
          apiAuth.getClientId() != null &&
          apiAuth.getClientSecret() != null &&
          apiAuth.getTokenUrl() != null
        ) {
          loaderAuth =
            new org.mobilitydata.gbfs.validator.loader.auth.OAuthClientCredentialsGrantAuth(
              apiAuth.getClientId(),
              apiAuth.getClientSecret(),
              apiAuth.getTokenUrl()
            );
        }
      }
    }
    return loaderAuth;
  }

  private org.mobilitydata.gbfs.validator.api.model.ValidationResult mergeValidationResults(
    List<org.mobilitydata.gbfs.validator.api.model.ValidationResult> results
  ) {
    if (results.isEmpty()) {
      org.mobilitydata.gbfs.validator.api.model.ValidationResult emptyApiResult =
        new org.mobilitydata.gbfs.validator.api.model.ValidationResult();
      ValidationResultSummary emptySummary = new ValidationResultSummary();
      emptySummary.setValidatorVersion(versionProvider.getVersion());
      emptySummary.setFiles(new ArrayList<>());
      emptyApiResult.setSummary(emptySummary);
      return emptyApiResult;
    }

    org.mobilitydata.gbfs.validator.api.model.ValidationResult mergedResult =
      new org.mobilitydata.gbfs.validator.api.model.ValidationResult();
    ValidationResultSummary summary = new ValidationResultSummary();
    summary.setValidatorVersion(
      results.get(0).getSummary().getValidatorVersion()
    );
    List<GbfsFile> allFiles = new ArrayList<>();
    results.forEach(result -> {
      if (
        result.getSummary() != null && result.getSummary().getFiles() != null
      ) {
        allFiles.addAll(result.getSummary().getFiles());
      }
    });

    summary.setFiles(allFiles);

    mergedResult.setSummary(summary);
    return mergedResult;
  }

  private org.mobilitydata.gbfs.validator.api.model.ValidationResult mapValidationResult(
    ValidationResult internalValidationResult,
    List<LoadedFile> loadedFilesForLanguage,
    String language
  ) {
    ValidationResultSummary validationResultSummary =
      new ValidationResultSummary();
    validationResultSummary.setValidatorVersion(versionProvider.getVersion());

    validationResultSummary.setFiles(
      mapFiles(
        loadedFilesForLanguage,
        internalValidationResult.files(),
        language
      )
    );

    org.mobilitydata.gbfs.validator.api.model.ValidationResult apiResult =
      new org.mobilitydata.gbfs.validator.api.model.ValidationResult();
    apiResult.setSummary(validationResultSummary);
    return apiResult;
  }

  private List<GbfsFile> mapFiles(
    List<LoadedFile> loadedFilesForLanguage,
    Map<String, FileValidationResult> validatedFileResultsMap,
    String language
  ) {
    List<GbfsFile> apiGbfsFiles = new ArrayList<>();

    for (LoadedFile loadedFile : loadedFilesForLanguage) {
      GbfsFile apiFile = new GbfsFile();
      apiFile.setName(loadedFile.fileName());
      apiFile.setUrl(loadedFile.url());

      List<SystemError> combinedApiSystemErrors = new ArrayList<>();

      List<LoaderError> loaderSystemErrors = loadedFile.loaderErrors();
      if (loaderSystemErrors != null && !loaderSystemErrors.isEmpty()) {
        combinedApiSystemErrors.addAll(
          mapLoaderSystemErrorsToApi(loaderSystemErrors)
        );
      }

      FileValidationResult validationResult = validatedFileResultsMap.get(
        loadedFile.fileName()
      );

      if (validationResult != null) {
        apiFile.setSchema(validationResult.schema());
        apiFile.setVersion(validationResult.version());
        apiFile.setErrors(mapFileValidationErrors(validationResult.errors()));

        List<ValidatorError> validatorSystemErrors =
          validationResult.validatorErrors();
        if (validatorSystemErrors != null && !validatorSystemErrors.isEmpty()) {
          combinedApiSystemErrors.addAll(
            mapValidatorSystemErrorsToApi(validatorSystemErrors)
          );
        }
      } else {
        apiFile.setErrors(new ArrayList<>());
      }

      apiFile.setSystemErrors(combinedApiSystemErrors);

      if (
        loadedFile.fileName().equals("gbfs.json") ||
        loadedFile.fileName().equals("gbfs")
      ) {
        apiFile.setLanguage(null);
      } else {
        apiFile.setLanguage(JsonNullable.of(language));
      }
      apiGbfsFiles.add(apiFile);
    }
    return apiGbfsFiles;
  }

  private List<SystemError> mapLoaderSystemErrorsToApi(
    List<LoaderError> loaderSystemErrors
  ) {
    if (loaderSystemErrors == null) {
      return new ArrayList<>();
    }
    return loaderSystemErrors
      .stream()
      .map(loaderError -> {
        SystemError apiError = new SystemError();
        apiError.setError(loaderError.error());
        apiError.setMessage(loaderError.message());
        return apiError;
      })
      .toList();
  }

  private List<SystemError> mapValidatorSystemErrorsToApi(
    List<ValidatorError> validatorSystemErrors
  ) {
    if (validatorSystemErrors == null) {
      return new ArrayList<>();
    }
    return validatorSystemErrors
      .stream()
      .map(validatorError -> {
        SystemError apiError = new SystemError();
        apiError.setError(validatorError.error());
        apiError.setMessage(validatorError.message());
        return apiError;
      })
      .toList();
  }

  private List<FileError> mapFileValidationErrors(
    List<FileValidationError> errors
  ) {
    if (errors == null) {
      return new ArrayList<>();
    }
    return errors
      .stream()
      .map(error -> {
        var mapped = new FileError();
        mapped.setMessage(error.message());
        mapped.setInstancePath(error.violationPath());
        mapped.setSchemaPath(error.schemaPath());
        mapped.setKeyword(error.keyword());
        return mapped;
      })
      .toList();
  }
}
