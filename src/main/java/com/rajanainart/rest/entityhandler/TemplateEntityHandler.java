package com.rajanainart.rest.entityhandler;

import java.util.HashMap;
import java.util.Map;

import com.rajanainart.integration.IntegrationController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rajanainart.rest.BaseRestController;
import com.rajanainart.rest.DefaultEntityHandler;
import com.rajanainart.rest.RestQueryRequest;

@Component("restentityhandler-Template")
public class TemplateEntityHandler extends DefaultEntityHandler {

	@Autowired

    IntegrationController process;

	@Override
	public String preValidateRestEntity() {
		String success = super.preValidateRestEntity();
		if (!getRestQueryConfig().getActionName().equalsIgnoreCase("UpdateTemplate"))
			return success;

		if (success.equalsIgnoreCase(BaseRestController.SUCCESS)) {

			Long cronId = getUnderlyingDb().selectScalar(
					"SELECT INTEGRATION_CRON_ID FROM CMN_TEMPLATE WHERE TEMPLATE_ID= ?p_id",
					getUnderlyingDb().new Parameter("p_id", getRestQueryRequest().getParams().get("ID")));
			Long count = getUnderlyingDb().selectScalar(
					"SELECT COUNT(*) FROM CMN_TEMPLATE WHERE INTEGRATION_CRON_ID= ?p_id",
					getUnderlyingDb().new Parameter("p_id", cronId.toString()));
			if (count == 1) {
				RestQueryRequest body = new RestQueryRequest();
				Map<String, String> params = new HashMap<>();
				params.put("ID", cronId.toString());
				body.setParams(params);
				process.controlScheduler(body, 0);
			}

		}
		return success;
	}
}
