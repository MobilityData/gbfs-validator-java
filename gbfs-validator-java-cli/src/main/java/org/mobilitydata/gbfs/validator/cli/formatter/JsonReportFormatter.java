package org.mobilitydata.gbfs.validator.cli.formatter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.mobilitydata.gbfs.validation.model.ValidationResult;
import org.mobilitydata.gbfs.validator.loader.LoadedFile;

public class JsonReportFormatter implements ReportFormatter {

  private final ObjectMapper objectMapper;

  public JsonReportFormatter() {
    this.objectMapper = new ObjectMapper();
    this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
  }

  @Override
  public String format(
    ValidationResult result,
    List<LoadedFile> loadedFiles,
    boolean verbose
  ) {
    try {
      Map<String, Object> report = new HashMap<>();

      // Validation result
      report.put("summary", result.summary());
      report.put("files", result.files());

      // Loader errors
      List<Map<String, Object>> loaderErrors = loadedFiles
        .stream()
        .filter(file ->
          file.loaderErrors() != null && !file.loaderErrors().isEmpty()
        )
        .flatMap(file ->
          file
            .loaderErrors()
            .stream()
            .map(error -> {
              Map<String, Object> errorMap = new HashMap<>();
              errorMap.put("fileName", file.fileName());
              errorMap.put("error", error.error());
              errorMap.put("message", error.message());
              return errorMap;
            })
        )
        .collect(Collectors.toList());

      if (!loaderErrors.isEmpty()) {
        report.put("loaderErrors", loaderErrors);
      }

      return objectMapper.writeValueAsString(report);
    } catch (Exception e) {
      throw new RuntimeException(
        "Failed to serialize validation result to JSON",
        e
      );
    }
  }
}
