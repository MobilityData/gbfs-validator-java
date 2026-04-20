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
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mobilitydata.gbfs.validation.validator.SchemaJsonPath;

/**
 * References to regions in station_information must exist in the system's system_regions file
 */
public class NoInvalidReferenceToRegionInStationInformation
  implements CustomRuleSchemaPatcher {

  public static final String REGION_IDS_SCHEMA_PATH =
    "$.properties.data.properties.stations.items.properties.region_id";

  /**
   * Adds an enum to the region_id schema of stations.region_id with the region ids from system_regions.json
   */
  @Override
  public DocumentContext addRule(
    DocumentContext rawSchemaDocumentContext,
    Map<String, JSONObject> feeds
  ) {
    JSONObject systemRegionsFeed = feeds.get("system_regions");
    JSONObject regionIdSchema = rawSchemaDocumentContext.read(
      REGION_IDS_SCHEMA_PATH
    );

    JSONArray regionIds = systemRegionsFeed != null
      ? SchemaJsonPath
        .parse(systemRegionsFeed)
        .read("$.data.regions[*].region_id")
      : new JSONArray();

    regionIdSchema.put("enum", regionIds);

    return rawSchemaDocumentContext.set(REGION_IDS_SCHEMA_PATH, regionIdSchema);
  }
}
