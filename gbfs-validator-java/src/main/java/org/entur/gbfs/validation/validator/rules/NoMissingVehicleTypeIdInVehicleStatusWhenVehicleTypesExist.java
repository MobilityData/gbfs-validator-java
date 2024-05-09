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
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

/**
 * Bikes / vehicles must refer to a vehicle type when vehicle_types exists
 */
public class NoMissingVehicleTypeIdInVehicleStatusWhenVehicleTypesExist implements CustomRuleSchemaPatcher {

    private final String fileName;

    public NoMissingVehicleTypeIdInVehicleStatusWhenVehicleTypesExist(String fileName) {
        this.fileName = fileName;
    }

    public static final String BIKE_ITEMS_REQUIRED = "$.properties.data.properties.bikes.items.required";
    public static final String VEHICLE_ITEMS_REQUIRED = "$.properties.data.properties.vehicles.items.required";
    @Override
    public DocumentContext addRule(DocumentContext rawSchemaDocumentContext, Map<String, JSONObject> feeds) {
        JSONObject vehicleTypesFeed = feeds.get("vehicle_types");

        String requiredPath = VEHICLE_ITEMS_REQUIRED;

        // backwards compatibility
        if (fileName.equals("free_bike_status")) {
            requiredPath = BIKE_ITEMS_REQUIRED;
        }

        JSONArray vehicleItemsRequiredSchema = rawSchemaDocumentContext.read(requiredPath);
        if (vehicleTypesFeed != null) {
            vehicleItemsRequiredSchema.put("vehicle_type_id");
        }
        return rawSchemaDocumentContext.set(requiredPath, vehicleItemsRequiredSchema);
    }
}
