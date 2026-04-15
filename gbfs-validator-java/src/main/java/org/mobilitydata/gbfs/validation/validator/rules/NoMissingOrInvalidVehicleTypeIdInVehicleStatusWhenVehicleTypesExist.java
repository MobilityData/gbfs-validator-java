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

package org.mobilitydata.gbfs.validation.validator.rules;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Bikes / vehicles must refer to a vehicle type when vehicle_types exists
 */
public class NoMissingOrInvalidVehicleTypeIdInVehicleStatusWhenVehicleTypesExist
  implements CustomRuleSchemaPatcher {

  private final String fileName;

  public NoMissingOrInvalidVehicleTypeIdInVehicleStatusWhenVehicleTypesExist(
    String fileName
  ) {
    this.fileName = fileName;
  }

  private static final String BIKE_ITEMS_SCHEMA_PATH =
    "$.properties.data.properties.bikes.items";
  private static final String VEHICLE_ITEMS_SCHEMA_PATH =
    "$.properties.data.properties.vehicles.items";

  @Override
  public DocumentContext addRule(
    DocumentContext rawSchemaDocumentContext,
    Map<String, JSONObject> feeds
  ) {
    JSONObject vehicleTypesFeed = feeds.get("vehicle_types");

    String requiredPath = VEHICLE_ITEMS_SCHEMA_PATH;

    // backwards compatibility
    if (fileName.equals("free_bike_status")) {
      requiredPath = BIKE_ITEMS_SCHEMA_PATH;
    }

    JSONObject vehicleItemsSchema = rawSchemaDocumentContext.read(requiredPath);
    if (vehicleTypesFeed != null) {
      vehicleItemsSchema.append("required", "vehicle_type_id");
    }
    JSONArray vehicleTypeIds = vehicleTypesFeed != null
      ? JsonPath
        .parse(vehicleTypesFeed)
        .read("$.data.vehicle_types[*].vehicle_type_id")
      : new JSONArray();
    vehicleItemsSchema
      .getJSONObject("properties")
      .getJSONObject("vehicle_type_id")
      .put("enum", vehicleTypeIds);
    return rawSchemaDocumentContext.set(requiredPath, vehicleItemsSchema);
  }
}
