package org.mobilitydata.gbfs.validator.cli.formatter;

import java.util.List;
import org.mobilitydata.gbfs.validation.model.ValidationResult;
import org.mobilitydata.gbfs.validator.loader.LoadedFile;

public interface ReportFormatter {
  /**
   * Format validation results into a report string
   * @param result Validation result from GbfsValidator
   * @param loadedFiles List of loaded files (to report loader errors)
   * @param verbose Whether to include detailed error information
   * @return Formatted report string
   */
  String format(
    ValidationResult result,
    List<LoadedFile> loadedFiles,
    boolean verbose
  );
}
