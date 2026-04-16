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

package org.mobilitydata.gbfs.validation.validator;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JsonOrgJsonProvider;
import com.jayway.jsonpath.spi.mapper.JsonOrgMappingProvider;

/**
 * Thin facade over {@link JsonPath} that always uses {@link JsonOrgJsonProvider}
 * and {@link JsonOrgMappingProvider}. All schema-patching code in this library
 * operates on {@code org.json.JSONObject} / {@code JSONArray}, which the default
 * JsonPath provider does not recognise.
 *
 * <p>Use {@link #parse(Object)} instead of {@code JsonPath.parse(...)} so that
 * we never depend on {@link Configuration#setDefaults(Configuration.Defaults)} —
 * mutating that JVM-wide default would silently break any other JsonPath user
 * in the same process (e.g. Spring's GraphQL test tooling).
 */
public final class SchemaJsonPath {

  private static final Configuration CONFIGURATION = Configuration
    .builder()
    .jsonProvider(new JsonOrgJsonProvider())
    .mappingProvider(new JsonOrgMappingProvider())
    .build();

  private SchemaJsonPath() {}

  /**
   * Parse an {@code org.json.JSONObject}, {@code JSONArray} or JSON string into a
   * {@link DocumentContext} configured to read and write {@code org.json} types.
   */
  public static DocumentContext parse(Object json) {
    return JsonPath.using(CONFIGURATION).parse(json);
  }
}
