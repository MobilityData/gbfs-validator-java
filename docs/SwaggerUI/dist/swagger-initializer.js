window.onload = function() {
  // The spec is the one the API is generated from, read from master so the docs follow the code.
  // GitHub Pages serves only docs/, so it is loaded from raw.githubusercontent.com (CORS enabled).
  const SPEC_URL = "https://raw.githubusercontent.com/MobilityData/gbfs-validator-java/master/gbfs-validator-java-api/src/main/resources/public/openapi.yaml";

  // MobilityData's hosted environments, for "Try it out". They are added here rather than in
  // openapi.yaml, so the spec stays neutral for anyone running the API on their own infrastructure.
  const SERVERS = [
    { url: "https://gbfs.api.mobilitydatabase.org", description: "Production" },
    { url: "https://qa.gbfs.api.mobilitydatabase.org", description: "Pre-production" },
    { url: "https://dev.gbfs.api.mobilitydatabase.org", description: "Development" }
  ];

  let serversAdded = false;
  window.ui = SwaggerUIBundle({
    url: SPEC_URL,
    dom_id: '#swagger-ui',
    deepLinking: true,
    presets: [
      SwaggerUIBundle.presets.apis,
      SwaggerUIStandalonePreset
    ],
    plugins: [
      SwaggerUIBundle.plugins.DownloadUrl
    ],
    layout: "StandaloneLayout",
    onComplete: function() {
      if (serversAdded) return;
      serversAdded = true;
      const spec = window.ui.specSelectors.specJson().toJS();
      window.ui.specActions.updateJsonSpec(Object.assign({}, spec, { servers: SERVERS }));
    }
  });
};
