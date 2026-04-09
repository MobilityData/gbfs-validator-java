package org.mobilitydata.gbfs.validator.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class ReportWriter {

  public static void writeReport(File file, String content) throws IOException {
    Files.writeString(
      file.toPath(),
      content,
      StandardOpenOption.CREATE,
      StandardOpenOption.TRUNCATE_EXISTING
    );
  }
}
