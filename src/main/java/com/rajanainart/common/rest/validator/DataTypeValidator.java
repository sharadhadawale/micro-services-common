package com.rajanainart.common.rest.validator;

import java.util.Map;

import com.rajanainart.common.data.BaseEntity;
import com.rajanainart.common.helper.MiscHelper;
import com.rajanainart.common.rest.BaseRestController;
import com.rajanainart.common.rest.RestQueryConfig;
import org.springframework.stereotype.Component;

@Component("type-validator")
public class DataTypeValidator implements BaseRestValidator {

    public static final String VALIDATOR_KEY = "type-validator";

    public String validate(RestQueryConfig config, RestQueryConfig.ValidationExecutionType type, Map<String, String> params, Map<String, Object> objectParams) {
        StringBuilder message = new StringBuilder();

        for (RestQueryConfig.TypeValidator v : config.getTypeValidators()) {
            if (v.getExecutionType() != type) continue;

            boolean success = false;
            paramsLoop:
            for (String p : v.getParamNames()) {
                if (!params.containsKey	  (p)) continue paramsLoop;
                if ( params.get(p).isEmpty())  continue paramsLoop;

                switch (v.getDataType()) {
                    case INTEGER:
                        success = MiscHelper.isInteger(params.get(p));
                        break;
                    case NUMERIC:
                        success = MiscHelper.isNumeric(params.get(p));
                        break;
                    case DATE:
                        success = MiscHelper.isDate(params.get(p), BaseEntity.DAFAULT_DATE_OUTPUT_FORMAT);
                        break;
                    case TEXT:
                        success = true;
                        break;
                }
                if (!success)
                    message.append(String.format("The input parameter %s does not contain valid %s value %n ", p, v.getDataType().toString()));
            }
        }

        return message.length() != 0 ? message.toString() : BaseRestController.SUCCESS;
    }
}
