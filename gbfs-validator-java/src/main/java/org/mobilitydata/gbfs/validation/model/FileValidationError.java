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

package org.mobilitydata.gbfs.validation.model;

import java.util.Objects;

/**
 * Representing a single validation error in a GBFS file
 * @param schemaPath The path in the schema that was violated
 * @param violationPath The path in the file containing the error
 * @param message An error message
 */
public record FileValidationError(
  String schemaPath,
  String violationPath,
  String message,
  String keyword
)
  implements ValidationResultComponentIdentity<FileValidationError> {
  @Override
  public boolean sameAs(FileValidationError other) {
    if (other == null) return false;
    if (!Objects.equals(schemaPath, other.schemaPath)) return false;
    if (!Objects.equals(violationPath, other.violationPath)) return false;
    if (!Objects.equals(message, other.message)) return false;
    return Objects.equals(keyword, other.keyword);
  }
}
