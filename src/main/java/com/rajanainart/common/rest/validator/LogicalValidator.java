package com.rajanainart.common.rest.validator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.rajanainart.common.helper.MiscHelper;
import com.rajanainart.common.rest.BaseRestController;
import com.rajanainart.common.rest.RestQueryConfig;
import org.springframework.stereotype.Component;

@Component("logical-validator")
public class LogicalValidator implements BaseRestValidator {

    public static final String VALIDATOR_KEY = "logical-validator";

    public String validate(RestQueryConfig config, RestQueryConfig.ValidationExecutionType type, Map<String, String> params, Map<String, Object> objectParams) {
        StringBuilder message = new StringBuilder();
        for (RestQueryConfig.LogicalValidator v : config.getLogicalValidators()) {
            if (v.getExecutionType() != type) continue;

            int index = 0;
            List<Double> methodValues = Arrays.asList(0d, 0d, 0d);
            paramsLoop:
            for (String p : v.getParamNames()) {
                boolean numeric = MiscHelper.isNumeric(p);
                if (!numeric && !params.containsKey(p)) continue paramsLoop;

                methodValues.set(index++, numeric ? MiscHelper.convertStringToDouble(p) : MiscHelper.convertStringToDouble(params.get(p)));
            }
            switch(v.getLogicalType()) {
                case EQUAL_TO:
                    if (!methodValues.get(0).equals(methodValues.get(1)))
                        message.append(String.format("Parameters %s and %s are not equal %n ", v.getParamNames().get(0), v.getParamNames().get((1))));
                    break;
                case NOT_EQUAL_TO:
                    if (methodValues.get(0).equals(methodValues.get(1)))
                        message.append(String.format("Parameters %s and %s are equal %n ", v.getParamNames().get(0), v.getParamNames().get((1))));
                    break;
                case LESSER_THAN:
                    if (!(methodValues.get(0) < methodValues.get(1)))
                        message.append(String.format("Parameter %s is not lesser than parameter %s %n ", v.getParamNames().get(0), v.getParamNames().get((1))));
                    break;
                case LESSER_THAN_EQUAL_TO:
                    if (!(methodValues.get(0) <= methodValues.get(1)))
                        message.append(String.format("Parameter %s is not lesser than/equal to parameter %s %n ", v.getParamNames().get(0), v.getParamNames().get((1))));
                    break;
                case GREATER_THAN:
                    if (!(methodValues.get(0) > methodValues.get(1)))
                        message.append(String.format("Parameter %s is not greater than parameter %s %n ", v.getParamNames().get(0), v.getParamNames().get((1))));
                    break;
                case GREATER_THAN_EQUAL_TO:
                    if (!(methodValues.get(0) >= methodValues.get(1)))
                        message.append(String.format("Parameter %s is not greater than/equal to parameter %s %n ", v.getParamNames().get(0), v.getParamNames().get((1))));
                    break;
                case BETWEEN:
                    if (v.getParamNames().size() < 3) break;

                    if (!(methodValues.get(0) >= methodValues.get(1) && methodValues.get(0) <= methodValues.get(1)))
                        message.append(String.format("Parameter %s is not between parameter %s and %s %n ", v.getParamNames().get(0), v.getParamNames().get(1), v.getParamNames().get(2)));
                    break;
                case NOT_BETWEEN:
                    if (v.getParamNames().size() < 3) break;

                    if (methodValues.get(0) >= methodValues.get(1) && methodValues.get(0) <= methodValues.get(1))
                        message.append(String.format("Parameter %s is between parameter %s and %s %n ", v.getParamNames().get(0), v.getParamNames().get(1), v.getParamNames().get(2)));
                    break;
            }
        }
        return message.length() != 0 ? message.toString() : BaseRestController.SUCCESS;
    }
}
