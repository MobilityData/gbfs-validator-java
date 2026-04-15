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
 * A vehicle's pricing_plan_id must exist in the system's system_pricing_plan file
 */
public class NoInvalidReferenceToPricingPlansInVehicleStatus
  implements CustomRuleSchemaPatcher {

  public static final String VEHICLE_PRICING_PLAN_ID_SCHEMA_PATH =
    "$.properties.data.properties.vehicles.items.properties.pricing_plan_id";
  public static final String BIKE_PRICING_PLAN_ID_SCHEMA_PATH =
    "$.properties.data.properties.bikes.items.properties.pricing_plan_id";

  private final String fileName;

  public NoInvalidReferenceToPricingPlansInVehicleStatus(String fileName) {
    this.fileName = fileName;
  }

  /**
   * Adds an enum to vehicle_status's pricing_plan_id schema with the plan ids from the system_pricing_plan feed
   */
  @Override
  public DocumentContext addRule(
    DocumentContext rawSchemaDocumentContext,
    Map<String, JSONObject> feeds
  ) {
    JSONObject pricingPlansFeed = feeds.get("system_pricing_plans");

    String requiredPath = VEHICLE_PRICING_PLAN_ID_SCHEMA_PATH;
    // backwards compatibility
    if (fileName.equals("free_bike_status")) {
      requiredPath = BIKE_PRICING_PLAN_ID_SCHEMA_PATH;
    }
    JSONObject pricingPlanIdSchema = rawSchemaDocumentContext.read(
      requiredPath
    );

    JSONArray pricingPlanIds = pricingPlansFeed != null
      ? JsonPath.parse(pricingPlansFeed).read("$.data.plans[*].plan_id")
      : new JSONArray();
    pricingPlanIdSchema.put("enum", pricingPlanIds);

    return rawSchemaDocumentContext.set(requiredPath, pricingPlanIdSchema);
  }
}
