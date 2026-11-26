# GBFS Validator API documentation

Static [Swagger UI](https://github.com/swagger-api/swagger-ui) for the GBFS Validator API, published with GitHub Pages from `master:/docs` at https://mobilitydata.github.io/gbfs-validator-java/SwaggerUI/index.html. Same setup as the [Mobility Database API docs](https://mobilitydata.github.io/mobility-feed-api/SwaggerUI/index.html).

- `index.html` and `dist/` are the Swagger UI static bundle, version in `swagger-ui.version`.
- `dist/swagger-initializer.js` loads `gbfs-validator-java-api/src/main/resources/public/openapi.yaml` from `master`, and adds MobilityData's hosted environments as servers for "Try it out".

The API itself still ships Swagger UI (springdoc) at `/swagger-ui/index.html` for anyone running it. MobilityData's deployment turns it off with the environment variables `SPRINGDOC_SWAGGERUI_ENABLED=false` and `SPRINGDOC_APIDOCS_ENABLED=false`.
