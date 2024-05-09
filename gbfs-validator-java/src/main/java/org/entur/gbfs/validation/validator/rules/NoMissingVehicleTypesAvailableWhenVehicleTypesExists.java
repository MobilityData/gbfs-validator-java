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
 * It is required to list available vehicle types in station_status when vehicle_types file exists
 */
public class NoMissingVehicleTypesAvailableWhenVehicleTypesExists implements CustomRuleSchemaPatcher {

    public static final String STATION_ITEMS_REQUIRED_SCHEMA_PATH = "$.properties.data.properties.stations.items.required";

    /**
     * Adds vehicle_types_available to list of required properties on stations in station_status
     */
    @Override
    public DocumentContext addRule(DocumentContext rawSchemaDocumentContext, Map<String, JSONObject> feeds) {
        JSONObject vehicleTypesFeed = feeds.get("vehicle_types");
        JSONArray stationItemsRequiredSchema = rawSchemaDocumentContext.read(STATION_ITEMS_REQUIRED_SCHEMA_PATH);
        if (vehicleTypesFeed != null) {
            stationItemsRequiredSchema.put("vehicle_types_available");
        }
        return rawSchemaDocumentContext.set(STATION_ITEMS_REQUIRED_SCHEMA_PATH, stationItemsRequiredSchema);
    }
}
