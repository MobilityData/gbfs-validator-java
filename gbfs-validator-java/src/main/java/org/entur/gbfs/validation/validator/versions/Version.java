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

package org.entur.gbfs.validation.validator.versions;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * Representation of a version of GBFS to be validated
 */
public interface Version {

    /**
     * The version represented as a string
     * @return
     */
    String getVersionString();

    /**
     * The names of all the files in this version of GBFS
     * @return
     */
    List<String> getFileNames();

    /**
     * Whether a file with the given name is required in this version of GBFS
     * @param fileName
     * @return
     */
    boolean isFileRequired(String fileName);

    /**
     * Get the json schema for the file with the given name in this version of GBFS. Apply custom rules by using
     * the provided map of files
     * @param fileName
     * @param feedMap
     * @return
     */
    Schema getSchema(String fileName, Map<String, JSONObject> feedMap);

    /**
     * Get the json schema for the file with the given name in this version of GBFS, without any custom rules
     * applied
     * @param fileName
     * @return
     */
    Schema getSchema(String fileName);

    /**
     * Validate the file with the given name according to this version of GBFS from the provided map of files
     * @param fileName
     * @param feedMap
     * @throws ValidationException Thrown if the file did not validate. Contains all the validation errors
     */
    void validate(String fileName, Map<String, JSONObject> feedMap) throws ValidationException;
}
