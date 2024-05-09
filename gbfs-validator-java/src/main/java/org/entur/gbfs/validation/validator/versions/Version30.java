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

import org.entur.gbfs.validation.validator.rules.NoMissingCurrentRangeMetersInVehicleStatusForMotorizedVehicles;
import org.entur.gbfs.validation.validator.rules.CustomRuleSchemaPatcher;
import org.entur.gbfs.validation.validator.rules.NoInvalidReferenceToPricingPlansInVehicleTypes;
import org.entur.gbfs.validation.validator.rules.NoMissingStoreUriInSystemInformation;
import org.entur.gbfs.validation.validator.rules.NoMissingVehicleTypeIdInVehicleStatusWhenVehicleTypesExist;
import org.entur.gbfs.validation.validator.rules.NoInvalidReferenceToVehicleTypesInStationStatus;
import org.entur.gbfs.validation.validator.rules.NoMissingVehicleTypesAvailableWhenVehicleTypesExists;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Version30 extends AbstractVersion {
    public static final String VERSION = "3.0";

    private static final List<String> feeds = Arrays.asList(
            "gbfs",
            "gbfs_versions",
            "system_information",
            "vehicle_types",
            "station_information",
            "station_status",
            "vehicle_status",
            "manifest",
            "system_regions",
            "system_pricing_plans",
            "system_alerts",
            "geofencing_zones"
    );

    private static final Map<String, List<CustomRuleSchemaPatcher>> customRules = Map.of(
            "vehicle_types", List.of(
                    new NoInvalidReferenceToPricingPlansInVehicleTypes()
            ),
            "station_status", List.of(
                    new NoInvalidReferenceToVehicleTypesInStationStatus(),
                    new NoMissingVehicleTypesAvailableWhenVehicleTypesExists()
            ),
            "vehicle_status", List.of(
                    new NoMissingVehicleTypeIdInVehicleStatusWhenVehicleTypesExist("vehicle_status"),
                    new NoMissingCurrentRangeMetersInVehicleStatusForMotorizedVehicles("vehicle_status")
            ),
            "system_information", List.of(
                    new NoMissingStoreUriInSystemInformation("vehicle_status")
            )
    );

    protected Version30() {
        super(VERSION, feeds, customRules);
    }

    @Override
    public boolean isFileRequired(String file) {
        return super.isFileRequired(file) || "gbfs".equals(file);
    }
}
