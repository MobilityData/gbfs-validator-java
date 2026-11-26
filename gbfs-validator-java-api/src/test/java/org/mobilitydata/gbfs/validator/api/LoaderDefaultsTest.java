package org.mobilitydata.gbfs.validator.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mobilitydata.gbfs.validator.api.handler.LoaderProperties;
import org.mobilitydata.gbfs.validator.api.handler.OpenApiGeneratorApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** API callers are untrusted: the loader must refuse local files and private networks unless configured otherwise. */
@SpringBootTest(classes = OpenApiGeneratorApplication.class)
public class LoaderDefaultsTest {

  @Autowired
  private LoaderProperties loaderProperties;

  @Test
  void apiLoadsOnlyPublicUrlsByDefault() {
    assertTrue(loaderProperties.isPublicNetworkOnly());
  }
}
