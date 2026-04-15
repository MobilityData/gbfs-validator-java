package org.mobilitydata.gbfs.validator.cli.formatter;

import java.util.List;
import org.mobilitydata.gbfs.validation.model.FileValidationError;
import org.mobilitydata.gbfs.validation.model.FileValidationResult;
import org.mobilitydata.gbfs.validation.model.ValidationResult;
import org.mobilitydata.gbfs.validation.model.ValidatorError;
import org.mobilitydata.gbfs.validator.loader.LoadedFile;
import org.mobilitydata.gbfs.validator.loader.LoaderError;

public class ConsoleReportFormatter implements ReportFormatter {

  private static final String HEADER = "=" + "=".repeat(79);
  private static final String SUBHEADER = "-" + "-".repeat(79);

  @Override
  public String format(
    ValidationResult result,
    List<LoadedFile> loadedFiles,
    boolean verbose
  ) {
    StringBuilder sb = new StringBuilder();

    // Header
    sb.append(HEADER).append("\n");
    sb.append("GBFS Validation Report\n");
    sb.append(HEADER).append("\n\n");

    // Summary
    sb.append("Version: ").append(result.summary().version()).append("\n");
    sb.append("Timestamp: ").append(result.summary().timestamp()).append("\n");
    sb
      .append("Total Errors: ")
      .append(result.summary().errorsCount())
      .append("\n");
    sb.append("Files Validated: ").append(result.files().size()).append("\n\n");

    // Loader Errors (if any)
    boolean hasLoaderErrors = loadedFiles
      .stream()
      .anyMatch(file ->
        file.loaderErrors() != null && !file.loaderErrors().isEmpty()
      );

    if (hasLoaderErrors) {
      sb.append(SUBHEADER).append("\n");
      sb.append("LOADER ERRORS\n");
      sb.append(SUBHEADER).append("\n\n");

      for (LoadedFile file : loadedFiles) {
        if (file.loaderErrors() != null && !file.loaderErrors().isEmpty()) {
          sb.append("File: ").append(file.fileName()).append("\n");
          for (LoaderError error : file.loaderErrors()) {
            sb
              .append("  ✗ ")
              .append(error.error())
              .append(": ")
              .append(error.message())
              .append("\n");
          }
          sb.append("\n");
        }
      }
    }

    // File Results
    sb.append(SUBHEADER).append("\n");
    sb.append("VALIDATION RESULTS\n");
    sb.append(SUBHEADER).append("\n\n");

    for (var entry : result.files().entrySet()) {
      String fileName = entry.getKey();
      FileValidationResult fileResult = entry.getValue();

      if (fileResult == null) {
        continue;
      }

      // File status
      String status = getFileStatus(fileResult);
      sb.append(status).append(" ").append(fileName);

      if (fileResult.required()) {
        sb.append(" [REQUIRED]");
      }

      if (!fileResult.exists()) {
        sb.append(" - NOT FOUND");
      } else if (fileResult.errorsCount() > 0) {
        sb.append(" - ").append(fileResult.errorsCount()).append(" error(s)");
      }

      sb.append("\n");

      // Validator errors (system errors)
      if (
        fileResult.validatorErrors() != null &&
        !fileResult.validatorErrors().isEmpty()
      ) {
        for (ValidatorError error : fileResult.validatorErrors()) {
          sb.append("  ✗ SYSTEM ERROR: ").append(error.message()).append("\n");
        }
      }

      // Validation errors (schema violations)
      if (
        verbose && fileResult.errors() != null && !fileResult.errors().isEmpty()
      ) {
        for (FileValidationError error : fileResult.errors()) {
          sb.append("  ✗ ").append(error.message()).append("\n");
          sb.append("      Path: ").append(error.violationPath()).append("\n");
          sb.append("      Schema: ").append(error.schemaPath()).append("\n");
        }
      } else if (!verbose && fileResult.errorsCount() > 0) {
        sb.append("  (Use --verbose to see detailed errors)\n");
      }

      sb.append("\n");
    }

    // Footer
    sb.append(HEADER).append("\n");
    String resultText = result.summary().errorsCount() == 0
      ? "VALID"
      : "INVALID";
    sb.append("Result: ").append(resultText).append("\n");
    sb.append(HEADER).append("\n");

    return sb.toString();
  }

  private String getFileStatus(FileValidationResult fileResult) {
    if (!fileResult.exists()) {
      return "⚠";
    } else if (
      fileResult.errorsCount() == 0 &&
      (
        fileResult.validatorErrors() == null ||
        fileResult.validatorErrors().isEmpty()
      )
    ) {
      return "✓";
    } else {
      return "✗";
    }
  }
}
