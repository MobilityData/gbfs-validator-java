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

import java.util.Objects;

/**
 * A summary of the validation of a set of GBFS files
 * @param version The version the files were validated against
 * @param timestamp The time when validation was performed
 * @param errorsCount The total amount of errors encountered during validation
 */
public record ValidationSummary(
         String version,
         long timestamp,
         int errorsCount
) implements ValidationResultComponentIdentity<ValidationSummary> {

    @Override
    public String toString() {
        return "ValidationSummary{" +
                "version='" + version + '\'' +
                ", timestamp=" + timestamp +
                ", errorsCount=" + errorsCount +
                '}';
    }

    @Override
    public boolean sameAs(ValidationSummary other) {
        if (other == null) return false;
        if (errorsCount != other.errorsCount) return false;
        return Objects.equals(version, other.version);
    }
}
