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

package org.entur.gbfs.validation.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class ValidationResultTest {

    @Test
    void testSameAsSucceeds() {
        Assertions.assertTrue(generateValidationResult(
                "message-1",
                "message-2"
        ).sameAs(generateValidationResult(
                "message-1",
                "message-2"
        )));
    }

    @Test
    void testSameAsFails() {
        Assertions.assertFalse(generateValidationResult(
                "message-1",
                "message-2"
        ).sameAs(generateValidationResult(
                "message-1",
                "message-3"
        )));
    }

    private ValidationResult generateValidationResult(String message1, String message2) {
        return new ValidationResult(
                new ValidationSummary(null, 0, 0),
                Map.of(
                        "gbfs", generateFileValidationResult(message1, message2)
                )
        );
    }

    private ValidationSummary generateValidationSummary() {
        return new ValidationSummary(
                "2.2",
                System.currentTimeMillis(),
                2
        );
    }

    private FileValidationResult generateFileValidationResult(String message1, String message2) {

        return new FileValidationResult(
                "gbfs",
                true,
                true,
                2,
                null,
                null,
                null,
                List.of(
                        generateFileValidationError(message1),
                        generateFileValidationError(message2)
                )
        );
    }

    private FileValidationError generateFileValidationError(String message) {
        return new FileValidationError(
                "schema/path",
                "violation/path",
                message
        );
    }
}
