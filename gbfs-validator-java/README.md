[![CircleCI](https://circleci.com/gh/entur/gbfs-validator-java/tree/master.svg?style=svg)](https://circleci.com/gh/entur/gbfs-validator-java/tree/master)

# gbfs-validator-java

Validate GBFS feeds. Intended as Java native alternative to https://github.com/MobilityData/gbfs-validator.

Uses the official json schema to validate files.

## Usage

Create an instance of `GbfsValidator`:

    GbfsValidator gbfsValidator = GbfsValidatorFactory.getGbfsJsonValidator();

The `GbfsValidator` interface has two methods:

### Validate a set of GBFS Files

Validate a set of GBFS files by providing a map of InputStreams, keyed by filename. 
The input streams maybe come from an HTTP response or from files. This validation
method will apply custom rules (see below), by dynamically patching the static JSON
schemas using data from the files themselves.

    gbfsValidator.validate(
      Map.of(
        "gbfs", gbfsInputStream,
        "system_information", systemInformationInputStream
        ...
      )
    );


### Validate a single GBFS file

Validate a single GBFS file by providing a filename and an InputStream. This validation
method will not apply any custom rules, but will validate only using the static JSON
schemas.


    gbfsValidator.validate(
      "system_information", systemInformationInputStream
    ); 

### Using the validation results

The validation methods above will return the `ValidationResult` record. This will contain a summary of the
validation process, as well as a map of validation results per file. See javadocs in `model` for details.


## Additional validation rules

The interface `CustomRuleSchemaPatcher` enables adding additional rules dynamically by schema patching:

    JSONObject addRule(JSONObject rawSchema, Map<String, JSONObject> feeds);

The raw schema along with a map of the data feeds is passed to this method. The patched schema should be returned.

List of additional rules:

* `NoInvalidReferenceToPricingPlansInVehicleTypes`
* `NoInvalidReferenceToVehicleTypesInStationStatus`
* `NoMissingVehicleTypesAvailableWhenVehicleTypesExists`
* `NoMissingVehicleTypeIdInVehicleStatusWhenVehicleTypesExist`
* `NoMissingCurrentRangeMetersInVehicleStatusForMotorizedVehicles`
* `NoMissingStoreUriInSystemInformation`

## Non-schema rules:

Some rules can't be validated with json schema:

Existing rules: 

* All version of gbfs require the system_information endpoint.
* In addition, gbfs endpoint is required as of v2.0.

Planned rules:

* Either station_information or station_status is required if the other is present
  * Can this be checked by cross-checking ids between files - using schema patching?
* vehicle_types is required if vehicle types are referenced in other files (already covered?)
* system_pricing_plans is required if pricing plans are referenced in other files

