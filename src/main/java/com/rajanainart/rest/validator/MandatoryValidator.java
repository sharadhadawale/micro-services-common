package com.rajanainart.rest.validator;

import java.util.Map;

import com.rajanainart.rest.RestQueryConfig;
import org.springframework.stereotype.Component;

import com.rajanainart.rest.BaseRestController;

@Component("mandatory-validator")
public class MandatoryValidator implements BaseRestValidator {

    public static final String VALIDATOR_KEY = "mandatory-validator";

    public String validate(RestQueryConfig config, RestQueryConfig.ValidationExecutionType type, Map<String, String> params, Map<String, Object> objectParams) {
        StringBuilder message = new StringBuilder();

        for (RestQueryConfig.MandatoryValidator v : config.getMandatoryValidators()) {
            if (v.getExecutionType() != type) continue;

            boolean success = false;
            paramsLoop:
            for (String p : v.getParamNames()) {
                success = (params.containsKey(p) && !params.get(p).isEmpty()) ||
                          (objectParams != null && objectParams.containsKey(p) && objectParams.get(p) != null);
                if (!success) success = hasDefaultValue(config, p);
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

    public boolean hasDefaultValue(RestQueryConfig config, String paramName) {
        for (RestQueryConfig.ParameterConfig p : config.getParameters()) {
            if (!p.getId().equalsIgnoreCase(paramName)) continue;

            return !p.getDefaultValue().isEmpty();
        }
        return false;
    }
}
