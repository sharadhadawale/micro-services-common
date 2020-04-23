package com.rajanainart.rest.entityhandler;

import com.rajanainart.integration.IntegrationController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rajanainart.rest.BaseRestController;
import com.rajanainart.rest.DefaultEntityHandler;

@Component("restentityhandler-Scheduler")
public class SchedulerEntityHandler extends DefaultEntityHandler {

	@Autowired

    IntegrationController process;

	@Override
	public String preValidateRestEntity() {
		String success = super.preValidateRestEntity();
		if (!getRestQueryConfig().getActionName().equalsIgnoreCase("DeleteScheduler"))
			return success;

		if (success.equalsIgnoreCase(BaseRestController.SUCCESS)) {
			String key = String.format("cron-scheduler-%s", getRestQueryRequest().getParams().get("ID"));
			Integer running = getUnderlyingDb().selectScalar(
					"SELECT IS_RUNNING FROM CMN_INTEGRATION_CRON WHERE INTEGRATION_CRON_ID = ?p_id",
					getUnderlyingDb().new Parameter("p_id", getRestQueryRequest().getParams().get("ID")));
			if (running == null)
				success = String.format("Scheduler does not exist: %s", getRestQueryRequest().getParams().get("ID"));
			else if (running != null && running == 1)
				success = String.format("Scheduler '%s' is already running, can't be deleted", key);
		}
		return success;

	}
}
