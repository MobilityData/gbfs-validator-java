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
 * References to stations in station_information must exist in station_status file and vice versa.
 */
public class NoInvalidReferenceToStation implements CustomRuleSchemaPatcher {

  public static final String STATION_IDS_SCHEMA_PATH =
    "$.properties.data.properties.stations.items.properties.station_id";

  private final String stationReferenceFileName;

  public NoInvalidReferenceToStation(String stationReferenceFileName) {
    this.stationReferenceFileName = stationReferenceFileName;
  }

  /**
   * Adds an enum to the station_id schema of stations.station_id with the station ids from station_status.json
   */
  @Override
  public DocumentContext addRule(
    DocumentContext rawSchemaDocumentContext,
    Map<String, JSONObject> feeds
  ) {
    JSONObject stationReferenceFeed = feeds.get(stationReferenceFileName);

    JSONObject stationIdSchema = rawSchemaDocumentContext.read(
      STATION_IDS_SCHEMA_PATH
    );

    JSONArray stationIds = stationReferenceFeed != null
      ? SchemaJsonPath
        .parse(stationReferenceFeed)
        .read("$.data.stations[*].station_id")
      : new JSONArray();

    stationIdSchema.put("enum", stationIds);

    return rawSchemaDocumentContext.set(
      STATION_IDS_SCHEMA_PATH,
      stationIdSchema
    );
  }
}
