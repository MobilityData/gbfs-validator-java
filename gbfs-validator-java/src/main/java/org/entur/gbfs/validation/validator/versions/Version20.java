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

import java.util.Arrays;
import java.util.List;

public class Version20 extends AbstractVersion {
    public static final String VERSION = "2.0";

    private static final List<String> feeds = Arrays.asList(
            "gbfs",
            "gbfs_versions",
            "system_information",
            "station_information",
            "station_status",
            "free_bike_status",
            "system_hours",
            "system_calendar",
            "system_regions",
            "system_pricing_plans",
            "system_alerts"
    );

    protected Version20() {
        super(VERSION, feeds);
    }

    @Override
    public boolean isFileRequired(String file) {
        return super.isFileRequired(file) || "gbfs".equals(file);
    }
}
