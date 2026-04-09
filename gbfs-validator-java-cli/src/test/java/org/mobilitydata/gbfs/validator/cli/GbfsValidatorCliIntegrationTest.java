package org.mobilitydata.gbfs.validator.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

public class GbfsValidatorCliIntegrationTest {

  private static Path testFeedDir;

  /**
   * Helper class to capture System.out/err and execute CLI commands
   */
  static class CliResult {

    final int exitCode;
    final String output;

    CliResult(int exitCode, String output) {
      this.exitCode = exitCode;
      this.output = output;
    }
  }

  /**
   * Execute CLI command and capture output
   */
  static CliResult execute(String... args) {
    ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    PrintStream originalOut = System.out;
    PrintStream originalErr = System.err;

    try {
      System.setOut(new PrintStream(outContent));
      System.setErr(new PrintStream(errContent));

      CommandLine cmd = new CommandLine(new GbfsValidatorCli());
      int exitCode = cmd.execute(args);
      String output = outContent.toString() + errContent.toString();

      return new CliResult(exitCode, output);
    } finally {
      System.setOut(originalOut);
      System.setErr(originalErr);
    }
  }

  @BeforeAll
  static void setup(@TempDir Path tempDir) throws Exception {
    // Create test feed files with correct file:// URLs
    testFeedDir = tempDir.resolve("test-feeds");
    Files.createDirectories(testFeedDir);

    // Create system_information.json
    String systemInfo =
      """
      {
        "last_updated": 1609459200,
        "ttl": 0,
        "version": "2.2",
        "data": {
          "system_id": "test_system",
          "language": "en",
          "name": "Test Bike Share",
          "timezone": "America/New_York"
        }
      }
      """;
    Files.writeString(
      testFeedDir.resolve("system_information.json"),
      systemInfo
    );

    // Create gbfs.json with file:// URL pointing to system_information.json
    String gbfsJson = String.format(
      """
      {
        "last_updated": 1609459200,
        "ttl": 0,
        "version": "2.2",
        "data": {
          "en": {
            "feeds": [
              {
                "name": "system_information",
                "url": "file://%s"
              }
            ]
          }
        }
      }
      """,
      testFeedDir.resolve("system_information.json").toAbsolutePath()
    );
    Files.writeString(testFeedDir.resolve("gbfs.json"), gbfsJson);
  }

  @Test
  void testCli_ValidLocalFeed_Success() {
    CliResult result = execute(
      "-u",
      "file://" + testFeedDir.resolve("gbfs.json").toAbsolutePath()
    );

    assertTrue(
      result.exitCode == 0 || result.exitCode == 1,
      "Expected success or validation error, got: " +
      result.exitCode +
      "\nOutput: " +
      result.output
    );
    assertTrue(result.output.contains("GBFS Validation Report"));
  }

  @Test
  void testCli_ValidLocalFeed_JsonFormat() {
    CliResult result = execute(
      "-u",
      "file://" + testFeedDir.resolve("gbfs.json").toAbsolutePath(),
      "--format",
      "json"
    );

    assertTrue(
      result.exitCode == 0 || result.exitCode == 1,
      "Expected success or validation error, got: " + result.exitCode
    );
    assertTrue(result.output.contains("\"summary\""));
    assertTrue(result.output.contains("\"files\""));
  }

  @Test
  void testCli_SaveReport_CreatesFile(@TempDir Path tempDir) {
    Path reportFile = tempDir.resolve("report.txt");

    CliResult result = execute(
      "-u",
      "file://" + testFeedDir.resolve("gbfs.json").toAbsolutePath(),
      "-s",
      reportFile.toString(),
      "-pr",
      "no"
    );

    assertTrue(
      result.exitCode == 0 || result.exitCode == 1,
      "Expected success or validation error, got: " + result.exitCode
    );
    assertTrue(Files.exists(reportFile), "Report file should exist");
    assertTrue(
      reportFile.toFile().length() > 0,
      "Report file should not be empty"
    );
    assertTrue(result.output.contains("Report saved to:"));
  }

  @Test
  void testCli_VerboseFlag() {
    CliResult result = execute(
      "-u",
      "file://" + testFeedDir.resolve("gbfs.json").toAbsolutePath(),
      "--verbose"
    );

    assertTrue(
      result.exitCode == 0 || result.exitCode == 1,
      "Expected success or validation error, got: " + result.exitCode
    );
  }

  @Test
  void testCli_MissingUrl_ShowsError() {
    CliResult result = execute();

    assertNotEquals(0, result.exitCode, "Should fail when URL is missing");
    assertTrue(
      result.output.contains("Missing required option") ||
      result.output.contains("--url"),
      "Should mention missing URL option"
    );
  }

  @Test
  void testCli_InvalidUrl_SystemError() {
    CliResult result = execute(
      "-u",
      "https://invalid.example.com/nonexistent.json"
    );

    assertEquals(2, result.exitCode, "Should return system error code");
    assertTrue(
      result.output.contains("ERROR") ||
      result.output.contains("Failed to load"),
      "Should show error message"
    );
  }

  @Test
  void testCli_HelpOption_Success() {
    CliResult result = execute("--help");

    assertEquals(0, result.exitCode, "Help should return success");
    assertTrue(result.output.contains("Usage:"));
    assertTrue(result.output.contains("--url"));
  }

  @Test
  void testCli_VersionOption_Success() {
    CliResult result = execute("--version");

    assertEquals(0, result.exitCode, "Version should return success");
    // Just verify it outputs something (version format may vary)
    assertFalse(result.output.trim().isEmpty(), "Should output version info");
    assertTrue(
      result.output.contains(".") || result.output.matches(".*\\d+.*"),
      "Should contain version-like content"
    );
  }
}
