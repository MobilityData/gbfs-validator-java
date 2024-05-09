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

package org.entur.gbfs.validation.model;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * The result of validating a single GBFS file
 * @param file The name of the file that was validated
 * @param required Whether the file is required in the given version of GBFS
 * @param exists Whether the file existed in the validation input
 * @param errorsCount The number of errors found while validating the file
 * @param schema The schema used to validate the file
 * @param fileContents The contents of the file
 * @param version The version of the file
 * @param errors A list of errors encountered while validating the file
 */
public record FileValidationResult(
        String file,
         boolean required,
         boolean exists,
         int errorsCount,
         String schema,
         String fileContents,
         String version,
         List<FileValidationError> errors
) implements ValidationResultComponentIdentity<FileValidationResult> {

    @Override
    public String toString() {
        return "FileValidationResult{" +
                "file='" + file + '\'' +
                ", required=" + required +
                ", exists=" + exists +
                ", errorsCount=" + errorsCount +
                ", schema='" + schema + '\'' +
                ", fileContents='" + fileContents + '\'' +
                ", version='" + version + '\'' +
                ", errors=" + errors +
                '}';
    }

    @Override
    public boolean sameAs(FileValidationResult other) {
        if (other == null) return false;
        if (required != other.required) return false;
        if (exists != other.exists) return false;
        if (errorsCount != other.errorsCount) return false;
        if (!Objects.equals(file, other.file)) return false;
        if (!Objects.equals(version, other.version)) return false;
        return IntStream
                .range(0, errors.size())
                .allMatch(i -> errors.get(i).sameAs(other.errors.get(i)));
    }
}
