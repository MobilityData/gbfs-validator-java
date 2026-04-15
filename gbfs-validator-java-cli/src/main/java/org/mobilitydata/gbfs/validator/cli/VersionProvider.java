package org.mobilitydata.gbfs.validator.cli;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import picocli.CommandLine;

public class VersionProvider implements CommandLine.IVersionProvider {

  @Override
  public String[] getVersion() throws Exception {
    Properties properties = new Properties();
    try (
      InputStream input = getClass()
        .getClassLoader()
        .getResourceAsStream("version.properties")
    ) {
      if (input == null) {
        return new String[] { "Unknown version" };
      }
      properties.load(input);
      String version = properties.getProperty("version", "Unknown version");
      return new String[] { version };
    } catch (IOException e) {
      return new String[] { "Unknown version" };
    }
  }
}
