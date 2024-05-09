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
 * It is required to provide ios and android store uris in system_information if vehicle_status
 * or station_information has ios and android rental uris respectively
 */
public class NoMissingStoreUriInSystemInformation implements CustomRuleSchemaPatcher {

    private static final String DATA_REQUIRED_SCHEMA_PATH = "$.properties.data.required";
    private static final String RENTAL_APPS_SCHEMA_PATH = "$.properties.data.properties.rental_apps";


    private final String vehicleStatusFileName;


    public NoMissingStoreUriInSystemInformation(String vehicleStatusFileName) {
        this.vehicleStatusFileName = vehicleStatusFileName;
    }

    @Override
    public DocumentContext addRule(DocumentContext rawSchemaDocumentContext, Map<String, JSONObject> feeds) {
        boolean hasIosRentalUris = false;
        boolean hasAndroidRentalUris = false;

        JSONObject vehicleStatusFeed = feeds.get(vehicleStatusFileName);

        if (vehicleStatusFeed != null) {
            String vehiclesKey = vehicleStatusFileName.equals("vehicle_status") ? "vehicles" : "bikes";

             if (!((JSONArray) JsonPath.parse(vehicleStatusFeed)
                    .read("$.data." + vehiclesKey + "[:1].rental_uris.ios")).isEmpty()) {
                 hasIosRentalUris = true;
             }

             if (!((JSONArray) JsonPath.parse(vehicleStatusFeed)
                    .read("$.data." + vehiclesKey + "[:1].rental_uris.android")).isEmpty()) {
                 hasAndroidRentalUris = true;
             }
        }

        JSONObject stationInformationFeed = feeds.get("station_information");

        if (stationInformationFeed != null) {
            if (!((JSONArray) JsonPath.parse(stationInformationFeed)
                    .read("$.data.stations[:1].rental_uris.ios")).isEmpty()) {
                hasIosRentalUris = true;
            }

            if (!((JSONArray) JsonPath.parse(stationInformationFeed)
                    .read("$.data.stations[:1].rental_uris.android")).isEmpty()) {
                hasAndroidRentalUris = true;
            }
        }

        if (hasIosRentalUris || hasAndroidRentalUris) {

            JSONArray systemInformationDataRequiredSchema = rawSchemaDocumentContext.read(DATA_REQUIRED_SCHEMA_PATH);
            systemInformationDataRequiredSchema.put("rental_apps");

            JSONObject rentalAppsSchema = rawSchemaDocumentContext.read(RENTAL_APPS_SCHEMA_PATH);
            JSONArray rentalAppRequired = new JSONArray();


            if (hasIosRentalUris) {
                rentalAppRequired.put("ios");
            }

            if (hasAndroidRentalUris) {
                rentalAppRequired.put("android");
            }

            rentalAppsSchema.put("required", rentalAppRequired);
        }

        return rawSchemaDocumentContext;
    }
}
