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

package org.mobilitydata.gbfs.validator.loader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public record LoadedFile(
  String fileName,
  String url,
  InputStream fileContents,
  String language,
  List<LoaderError> loaderErrors
) {
  public LoadedFile(
    String fileName,
    String url,
    InputStream fileContents,
    String language
  ) {
    this(fileName, url, fileContents, language, new ArrayList<>());
  }

  public LoadedFile(String fileName, String url, InputStream fileContents) {
    this(fileName, url, fileContents, null, new ArrayList<>());
  }
}
