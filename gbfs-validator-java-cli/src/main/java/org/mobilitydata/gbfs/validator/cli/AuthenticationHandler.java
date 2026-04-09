package org.mobilitydata.gbfs.validator.cli;

import org.mobilitydata.gbfs.validator.loader.auth.Authentication;
import org.mobilitydata.gbfs.validator.loader.auth.BasicAuth;
import org.mobilitydata.gbfs.validator.loader.auth.BearerTokenAuth;
import org.mobilitydata.gbfs.validator.loader.auth.OAuthClientCredentialsGrantAuth;

public class AuthenticationHandler {

  public static Authentication buildAuthentication(AuthOptions authOptions) {
    if (authOptions == null || authOptions.authType == null) {
      return null;
    }

    return switch (authOptions.authType) {
      case BASIC -> {
        if (authOptions.basicAuth == null) {
          throw new IllegalArgumentException(
            "Basic auth selected but --username and --password not provided"
          );
        }
        yield new BasicAuth(
          authOptions.basicAuth.username,
          authOptions.basicAuth.password
        );
      }
      case BEARER -> {
        if (authOptions.bearerAuth == null) {
          throw new IllegalArgumentException(
            "Bearer auth selected but --token not provided"
          );
        }
        yield new BearerTokenAuth(authOptions.bearerAuth.token);
      }
      case OAUTH -> {
        if (authOptions.oauthOptions == null) {
          throw new IllegalArgumentException(
            "OAuth auth selected but credentials not provided"
          );
        }
        yield new OAuthClientCredentialsGrantAuth(
          authOptions.oauthOptions.clientId,
          authOptions.oauthOptions.clientSecret,
          authOptions.oauthOptions.tokenUrl
        );
      }
    };
  }
}
