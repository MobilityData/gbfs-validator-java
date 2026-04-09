package org.mobilitydata.gbfs.validator.cli;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.mobilitydata.gbfs.validation.GbfsValidator;
import org.mobilitydata.gbfs.validation.GbfsValidatorFactory;
import org.mobilitydata.gbfs.validation.model.ValidationResult;
import org.mobilitydata.gbfs.validator.cli.formatter.ConsoleReportFormatter;
import org.mobilitydata.gbfs.validator.cli.formatter.JsonReportFormatter;
import org.mobilitydata.gbfs.validator.cli.formatter.ReportFormatter;
import org.mobilitydata.gbfs.validator.loader.LoadedFile;
import org.mobilitydata.gbfs.validator.loader.Loader;
import org.mobilitydata.gbfs.validator.loader.auth.Authentication;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
  name = "gbfs-validator",
  mixinStandardHelpOptions = true,
  versionProvider = VersionProvider.class,
  description = "Validate GBFS feeds against JSON schemas",
  headerHeading = "Usage:%n%n",
  synopsisHeading = "%n",
  descriptionHeading = "%nDescription:%n%n",
  parameterListHeading = "%nParameters:%n",
  optionListHeading = "%nOptions:%n"
)
public class GbfsValidatorCli implements Callable<Integer> {

  @Option(
    names = { "-u", "--url" },
    description = "URL of the GBFS feed (discovery endpoint)",
    required = true
  )
  private String feedUrl;

  @Option(
    names = { "-s", "--save-report" },
    description = "Local path to output report file"
  )
  private File reportFile;

  @Option(
    names = { "-pr", "--print-report" },
    description = "Print report to standard output (yes/no, default: yes)",
    defaultValue = "yes"
  )
  private String printReport;

  @Option(
    names = { "-vb", "--verbose" },
    description = "Verbose mode - show detailed error information"
  )
  private boolean verbose;

  @Option(
    names = { "--format" },
    description = "Output format: text or json (default: text)",
    defaultValue = "text"
  )
  private String format;

  @ArgGroup(exclusive = false, heading = "%nAuthentication Options:%n")
  private AuthOptions authOptions;

  @Override
  public Integer call() throws Exception {
    Loader loader = null;

    try {
      Authentication auth = AuthenticationHandler.buildAuthentication(
        authOptions
      );

      loader = new Loader();
      List<LoadedFile> loadedFiles = loader.load(feedUrl, auth);

      boolean hasFatalLoaderErrors = hasFatalLoaderErrors(loadedFiles);
      if (hasFatalLoaderErrors && hasNoValidContent(loadedFiles)) {
        System.err.println("ERROR: Failed to load any feeds from " + feedUrl);
        return 2;
      }

      Map<String, InputStream> fileMap = buildFileMap(loadedFiles);

      GbfsValidator validator = GbfsValidatorFactory.getGbfsJsonValidator();
      ValidationResult result = validator.validate(fileMap);

      String report = formatReport(result, loadedFiles);
      outputReport(report);

      return determineExitCode(hasFatalLoaderErrors, result);
    } catch (Exception e) {
      System.err.println("ERROR: " + e.getMessage());
      if (verbose) {
        e.printStackTrace(System.err);
      }
      return 2;
    } finally {
      if (loader != null) {
        loader.close();
      }
    }
  }

  private boolean hasFatalLoaderErrors(List<LoadedFile> loadedFiles) {
    return loadedFiles
      .stream()
      .anyMatch(file ->
        file.loaderErrors() != null && !file.loaderErrors().isEmpty()
      );
  }

  private boolean hasNoValidContent(List<LoadedFile> loadedFiles) {
    return loadedFiles.stream().noneMatch(file -> file.fileContents() != null);
  }

  private Map<String, InputStream> buildFileMap(List<LoadedFile> loadedFiles) {
    Map<String, InputStream> fileMap = new HashMap<>();
    for (LoadedFile file : loadedFiles) {
      if (file.fileContents() != null) {
        fileMap.put(file.fileName(), file.fileContents());
      }
    }
    return fileMap;
  }

  private String formatReport(
    ValidationResult result,
    List<LoadedFile> loadedFiles
  ) {
    ReportFormatter formatter = createFormatter(format);
    return formatter.format(result, loadedFiles, verbose);
  }

  private void outputReport(String report) throws IOException {
    if ("yes".equalsIgnoreCase(printReport)) {
      System.out.println(report);
    }

    if (reportFile != null) {
      ReportWriter.writeReport(reportFile, report);
      if (!"yes".equalsIgnoreCase(printReport)) {
        System.out.println("Report saved to: " + reportFile.getAbsolutePath());
      }
    }
  }

  private int determineExitCode(
    boolean hasFatalLoaderErrors,
    ValidationResult result
  ) {
    if (hasFatalLoaderErrors) {
      return 2;
    } else if (result.summary().errorsCount() > 0) {
      return 1;
    } else {
      return 0;
    }
  }

  private ReportFormatter createFormatter(String format) {
    return switch (format.toLowerCase()) {
      case "json" -> new JsonReportFormatter();
      default -> new ConsoleReportFormatter();
    };
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new GbfsValidatorCli()).execute(args);
    System.exit(exitCode);
  }
}
