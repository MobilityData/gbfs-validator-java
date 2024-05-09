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
import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.JsonPath;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

import static com.jayway.jsonpath.Criteria.where;

/**
 * It is required to provide the current_range_meters property in vehicle_status for motorized vehicles
 */
public class NoMissingCurrentRangeMetersInVehicleStatusForMotorizedVehicles implements CustomRuleSchemaPatcher {

    private final String fileName;

    public NoMissingCurrentRangeMetersInVehicleStatusForMotorizedVehicles(String fileName) {
        this.fileName = fileName;
    }

    private static final Filter motorizedVehicleTypesFilter = Filter.filter(
            where("propulsion_type").in(
                    List.of(
                        "electric_assist", "electric", "combustion"
                )
            )
    );
    private static final String BIKE_ITEMS_SCHEMA_PATH = "$.properties.data.properties.bikes.items";
    private static final String VEHICLE_ITEMS_SCHEMA_PATH = "$.properties.data.properties.vehicles.items";

    @Override
    public DocumentContext addRule(DocumentContext rawSchemaDocumentContext, Map<String, JSONObject> feeds) {
        JSONObject vehicleTypesFeed = feeds.get("vehicle_types");

        JSONArray motorizedVehicleTypeIds = null;

        if (vehicleTypesFeed != null) {
            motorizedVehicleTypeIds = JsonPath.parse(vehicleTypesFeed)
                    .read("$.data.vehicle_types[?].vehicle_type_id", motorizedVehicleTypesFilter);
        }

        String schemaPath = VEHICLE_ITEMS_SCHEMA_PATH;

        if (fileName.equals("free_bike_status")) {
            schemaPath = BIKE_ITEMS_SCHEMA_PATH;
        }

        JSONObject bikeItemsSchema = rawSchemaDocumentContext.read(schemaPath);

        if (motorizedVehicleTypeIds != null && !motorizedVehicleTypeIds.isEmpty()) {
            bikeItemsSchema.put("errorMessage", new JSONObject().put("required", new JSONObject().put("vehicle_type_id", "'vehicle_type_id' is required for this vehicle type")));
            bikeItemsSchema
                    .put("if",
                            new JSONObject()
                                .put("properties", new JSONObject().put("vehicle_type_id", new JSONObject().put("enum", motorizedVehicleTypeIds)))

                                // "required" so it only trigger "then" when "vehicle_type_id" is present.
                                .put("required", new JSONArray().put("vehicle_type_id"))
                    )
                    .put("then", new JSONObject().put("required", new JSONArray().put("current_range_meters")));
        }

        return rawSchemaDocumentContext.set(schemaPath, bikeItemsSchema);
    }
}
