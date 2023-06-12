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

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JsonOrgJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JsonOrgMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import org.entur.gbfs.validation.validator.FileValidator;
import org.entur.gbfs.validation.validator.URIFormatValidator;
import org.entur.gbfs.validation.validator.rules.CustomRuleSchemaPatcher;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractVersion implements Version {
    private static final Logger logger = LoggerFactory.getLogger(AbstractVersion.class);
    private final String versionString;
    private final List<String> feeds;
    private Map<String, JSONObject> schemas = new ConcurrentHashMap<>();
    private final Map<String, List<CustomRuleSchemaPatcher>> customRules;

    protected AbstractVersion(String versionString, List<String> feeds, Map<String, List<CustomRuleSchemaPatcher>> customRules) {
        this.versionString = versionString;
        this.feeds = feeds;
        this.customRules = customRules;
        configureJsonPath();
    }

    protected AbstractVersion(String versionString, List<String> feeds) {
        this.versionString = versionString;
        this.feeds = feeds;
        this.customRules = Map.of();
        configureJsonPath();
    }

    @Override
    public String getVersionString() {
        return versionString;
    }

    @Override
    public List<String> getFileNames() {
        return feeds;
    }

    @Override
    public boolean isFileRequired(String file) {
        return "system_information".equals(file);
    }

    @Override
    public void validate(String fileName, Map<String, JSONObject> feedMap) throws ValidationException {
        getSchema(fileName, feedMap).validate(feedMap.get(fileName));
    }

    public Schema getSchema(String feedName, Map<String, JSONObject> feedMap) {
        return loadSchema(applyCustomRules(feedName, getRawSchema(feedName), feedMap));
    }
    public Schema getSchema(String feedName) {
        return loadSchema(getRawSchema(feedName));
    }

    private JSONObject getRawSchema(String feedName) {
        schemas.computeIfAbsent(feedName, _ -> loadRawSchema(feedName));
        return schemas.get(feedName);
    }

    private JSONObject applyCustomRules(String feedName, JSONObject rawSchema, Map<String, JSONObject> feedMap) {

        // Risky use of reduce?
        return getCustomRules(feedName).stream().reduce(rawSchema, (schema, patcher) -> applyRule(schema, patcher, feedMap), (a, b) -> a);
    }

    protected JSONObject applyRule(JSONObject schema, CustomRuleSchemaPatcher patcher, Map<String, JSONObject> feedMap) {
        return patcher.addRule(schema, feedMap);
    }

    private void configureJsonPath() {
        Configuration.setDefaults(new Configuration.Defaults() {
            JsonProvider jsonProvider = new JsonOrgJsonProvider();
            MappingProvider mappingProvider = new JsonOrgMappingProvider();

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public Set<Option> options() {
                return EnumSet.noneOf(Option.class);
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }
        });
    }

    private List<CustomRuleSchemaPatcher> getCustomRules(String fileName) {
        return Optional.ofNullable(customRules.get(fileName)).orElse(Collections.emptyList());
    }

    protected JSONObject loadRawSchema(String feedName) {
        InputStream inputStream = FileValidator.class.getClassLoader().getResourceAsStream("schema/v"+ versionString +"/"+feedName+".json");

        if (inputStream == null) {
            logger.warn("Unable to load schema version={} feedName={}", versionString, feedName);
            return null;
        }

        return new JSONObject(new JSONTokener(inputStream));
    }

    protected org.everit.json.schema.Schema loadSchema(JSONObject rawSchema) {
        SchemaLoader schemaLoader = SchemaLoader.builder()
                .enableOverrideOfBuiltInFormatValidators()
                .addFormatValidator(new URIFormatValidator())
                .schemaJson(rawSchema)
                .build();

        return schemaLoader.load().build();
    }
}
