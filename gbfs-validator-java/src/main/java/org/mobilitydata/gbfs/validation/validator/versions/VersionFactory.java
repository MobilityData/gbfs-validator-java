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

package org.mobilitydata.gbfs.validation.validator.versions;

import java.util.List;

public class VersionFactory {

  private static final List<String> SUPPORTED_VERSIONS_DESC = List.of(
    "3.1-RC3",
    "3.0",
    "2.3",
    "2.2",
    "2.1",
    "2.0",
    "1.1",
    "1.0"
  );

  private VersionFactory() {}

  public static Version createVersion(String version) {
    switch (version) {
      case "1.0":
        return new Version10();
      case "1.1":
        return new Version11();
      case "2.0":
        return new Version20();
      case "2.1":
        return new Version21();
      case "2.2":
        return new Version22();
      case "2.3":
        return new Version23();
      case "3.0":
        return new Version30();
      case "3.1-RC3":
        return new Version31RC3();
      default:
        throw new UnsupportedOperationException("Version not implemented");
    }
  }

  public static Version createLatestVersionSupportingFeed(
    String feedName,
    String fallbackVersion
  ) {
    return SUPPORTED_VERSIONS_DESC
      .stream()
      .map(VersionFactory::createVersion)
      .filter(version -> version.getFileNames().contains(feedName))
      .findFirst()
      .orElse(createVersion(fallbackVersion));
  }
}
