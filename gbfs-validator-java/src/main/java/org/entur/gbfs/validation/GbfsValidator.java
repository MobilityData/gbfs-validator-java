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

package org.entur.gbfs.validation;

import org.entur.gbfs.validation.model.FileValidationResult;
import org.entur.gbfs.validation.model.ValidationResult;

import java.io.InputStream;
import java.util.Map;

/**
 * Represents a validator of GBFS files
 */
public interface GbfsValidator {

    /**
     * Validate all files in the map of GBFS files, keyed by the name of the file. Will validate using
     * custom rules in addition to the static schema
     * @param fileMap
     * @return
     */
    ValidationResult validate(Map<String, InputStream> fileMap);

    /**
     * Validate the GBFS file with the given name and the file itself as an InputStream. Will not apply
     * custom rules, but only validate using the static schema
     * @param fileName
     * @param file
     * @return
     */
    FileValidationResult validateFile(String fileName, InputStream file);
}
