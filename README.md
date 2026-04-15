![Build](https://github.com/entur/gbfs-validator-java/actions/workflows/build.yml/badge.svg?branch=master)
![Deploy Maven Central](https://github.com/entur/gbfs-validator-java/actions/workflows/deploy.yml/badge.svg)
[![codecov](https://codecov.io/gh/entur/gbfs-validator-java/graph/badge.svg?token=WP0DSLPSVR)](https://codecov.io/gh/entur/gbfs-validator-java)

# gbfs-validator-java

Validate GBFS feeds. Intended as Java native alternative to https://github.com/MobilityData/gbfs-validator.

Uses the official json schema to validate files.

## Example

See the [`example/`](example/) directory for a working Maven project that depends on this library as a published artifact.

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

* `NoInvalidReferenceToPricingPlansInVehicleStatus`
* `NoInvalidReferenceToPricingPlansInVehicleTypes`
* `NoInvalidReferenceToRegionInStationInformation`
* `NoInvalidReferenceToStation`
* `NoInvalidReferenceToVehicleTypesInStationStatus`
* `NoMissingVehicleTypesAvailableWhenVehicleTypesExists`
* `NoMissingOrInvalidVehicleTypeIdInVehicleStatusWhenVehicleTypesExist`
* `NoMissingCurrentRangeMetersInVehicleStatusForMotorizedVehicles`
* `NoMissingStoreUriInSystemInformation`

