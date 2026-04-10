# GBFS Validator Java Example

A minimal example project demonstrating how to use the [gbfs-validator-java](https://github.com/MobilityData/gbfs-validator-java) library to validate GBFS feeds programmatically.

## What it does

- Fetches a public GBFS feed (BIXI Montreal by default)
- Validates a single file using `GbfsValidator.validateFile()`
- Validates a full multi-file feed using `GbfsValidator.validate()`
- Prints validation results including errors, version, and schema info

## Prerequisites

- Java 17+
- Maven 3.6+

## Running

```bash
mvn compile exec:java -Dexec.mainClass=org.mobilitydata.gbfs.validator.example.GbfsValidatorExample
```

## Using a different GBFS feed

Edit the `GBFS_FEED_URL` constant in `GbfsValidatorExample.java` to point to any public GBFS feed.

## Using a release version

To use a stable release instead of a snapshot, change `gbfs-validator.version` in `pom.xml` to a published release version, e.g. `3.0.1`, and remove the snapshot repository configuration.

## Key classes

| Class | Description |
|---|---|
| `GbfsValidatorFactory` | Entry point — creates a `GbfsValidator` instance |
| `GbfsValidator` | Validates a single file or a full set of GBFS files |
| `ValidationResult` | Result of validating a full feed (map of file results + summary) |
| `FileValidationResult` | Result for a single file (errors count, version, schema, errors list) |
| `FileValidationError` | A single validation error (message, path in schema, path in file) |
