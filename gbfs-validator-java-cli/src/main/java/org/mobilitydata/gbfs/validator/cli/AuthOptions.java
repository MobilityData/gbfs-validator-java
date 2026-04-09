package org.mobilitydata.gbfs.validator.cli;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public class AuthOptions {

  @Option(
    names = { "--auth-type" },
    description = "Authentication type: basic, bearer, or oauth"
  )
  AuthType authType;

  @ArgGroup(exclusive = false)
  BasicAuthOptions basicAuth;

  @ArgGroup(exclusive = false)
  BearerAuthOptions bearerAuth;

  @ArgGroup(exclusive = false)
  OAuthOptions oauthOptions;

  public static class BasicAuthOptions {

    @Option(
      names = { "--username" },
      description = "Username for Basic Authentication",
      required = true
    )
    String username;

    @Option(
      names = { "--password" },
      description = "Password for Basic Authentication",
      required = true
    )
    String password;
  }

  public static class BearerAuthOptions {

    @Option(
      names = { "--token" },
      description = "Bearer token",
      required = true
    )
    String token;
  }

  public static class OAuthOptions {

    @Option(
      names = { "--client-id" },
      description = "OAuth client ID",
      required = true
    )
    String clientId;

    @Option(
      names = { "--client-secret" },
      description = "OAuth client secret",
      required = true
    )
    String clientSecret;

    @Option(
      names = { "--token-url" },
      description = "OAuth token endpoint URL",
      required = true
    )
    String tokenUrl;
  }
}
