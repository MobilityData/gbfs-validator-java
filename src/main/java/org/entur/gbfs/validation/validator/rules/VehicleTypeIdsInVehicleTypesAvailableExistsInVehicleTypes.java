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

package org.entur.gbfs.validation.validator.rules;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

/**
 * Available vehicle types in station_status must exist in the system's vehicle_types file
 */
public class VehicleTypeIdsInVehicleTypesAvailableExistsInVehicleTypes implements CustomRuleSchemaPatcher {

    public static final String VEHICLE_TYPE_ID_SCHEMA_PATH = "$.properties.data.properties.stations.items.properties.vehicle_types_available.items.properties.vehicle_type_id";

    /**
     * Adds an enum to the vehicle_type_id schema of vehicle_types_available with the vehilce type ids from vehicle_types.json
     */
    @Override
    public DocumentContext addRule(DocumentContext rawSchemaDocumentContext, Map<String, JSONObject> feeds) {
        JSONObject vehicleTypesFeed = feeds.get("vehicle_types");
        JSONObject vehicleTypeIdSchema = rawSchemaDocumentContext.read(VEHICLE_TYPE_ID_SCHEMA_PATH);

        if (vehicleTypesFeed != null) {
            JSONArray vehicleTypeIds = JsonPath.parse(vehicleTypesFeed).read("$.data.vehicle_types[*].vehicle_type_id");
            vehicleTypeIdSchema.put("enum", vehicleTypeIds);
        }

        return rawSchemaDocumentContext.set(VEHICLE_TYPE_ID_SCHEMA_PATH, vehicleTypeIdSchema);
    }
}
