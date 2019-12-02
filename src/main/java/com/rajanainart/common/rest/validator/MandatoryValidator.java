package com.rajanainart.common.rest.validator;

import java.util.Map;

import com.rajanainart.common.rest.RestQueryConfig;
import org.springframework.stereotype.Component;

import com.rajanainart.common.rest.BaseRestController;

@Component("mandatory-validator")
public class MandatoryValidator implements BaseRestValidator {

    public static final String VALIDATOR_KEY = "mandatory-validator";

    public String validate(RestQueryConfig config, RestQueryConfig.ValidationExecutionType type, Map<String, String> params) {
        StringBuilder message = new StringBuilder();

        for (RestQueryConfig.MandatoryValidator v : config.getMandatoryValidators()) {
            if (v.getExecutionType() != type) continue;

            boolean success = false;
            paramsLoop:
            for (String p : v.getParamNames()) {
                success = params.containsKey(p) && !params.get(p).isEmpty();
                if ( success && v.getOperatorType() == RestQueryConfig.ValidationOperatorType.OR ) break paramsLoop;
                if (!success && v.getOperatorType() == RestQueryConfig.ValidationOperatorType.AND) break paramsLoop;
            }
            if (!success)
                message.append(String.format("%s:%s %n ", (v.getOperatorType() == RestQueryConfig.ValidationOperatorType.AND ?
                                "All the input parameters are mandatory" :
                                "Atleast one input parameter is required"),
                        String.join(",", v.getParamNames())));
        }

        return message.length() != 0 ? message.toString() : BaseRestController.SUCCESS;
    }

}
