package org.entur.gbfs.validator.api.handler;

import org.entur.gbfs.validation.GbfsValidator;
import org.entur.gbfs.validation.GbfsValidatorFactory;
import org.entur.gbfs.validator.Loader;
import org.entur.gbfs.validator.api.gen.ValidateOption1Api;
import org.entur.gbfs.validator.api.gen.ValidateOption1ApiDelegate;
import org.entur.gbfs.validator.api.model.ValidateOption1PostRequest;
import org.entur.gbfs.validator.api.model.ValidationResultOption1;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Map;

@Service
public class ValidateApiDelegateHandler implements ValidateOption1ApiDelegate {

    @Override
    public ResponseEntity<ValidationResultOption1> validateOption1Post(ValidateOption1PostRequest validateOption1PostRequest) {
        Loader loader = new Loader();
        Map<String, InputStream> fileMap = loader.load(validateOption1PostRequest.getFeedUrl());
        GbfsValidator validator = GbfsValidatorFactory.getGbfsJsonValidator();
//        return validator.validate(fileMap);
        return null;
    }
}
