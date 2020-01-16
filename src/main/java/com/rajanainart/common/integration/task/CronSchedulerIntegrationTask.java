package com.rajanainart.common.integration.task;

import com.rajanainart.common.concurrency.CronScheduler;
import com.rajanainart.common.helper.MiscHelper;
import com.rajanainart.common.integration.IntegrationContext;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component("integration-task-cron_scheduler")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CronSchedulerIntegrationTask implements IntegrationTask, Runnable {
    private IntegrationContext context = null;
    private Status             current = Status.PROCESSING;

    private CronScheduler     scheduler;
    private DelegateTransform transform;

    @Override
    public void setup(IntegrationContext context) {
        this.context = context;
    }

    @Override
    public Status currentStatus() {
        return current;
    }

    @Override
    public Status process(IntegrationTask.DelegateTransform transform) {
        current = Status.PROCESSING;
        String msg = String.format("CRON: Processing exec %s/%s", context.getConfig().getId(), context.getTaskConfig().getId());
        context.getLogger().log(msg);

        CronScheduler.Frequency frequency;
        CronScheduler.WeekDay   weekDay = CronScheduler.WeekDay.SUN;
        int day   ;
        int hour  ;
        int minute;

        List<Map<String, Object>> cron = context.getSourceDb().selectAsMapList("SELECT * FROM CMN_INTEGRATION_CRON WHERE config_name = ?name",
                                                            context.getSourceDb().new Parameter("name", context.getConfig().getId()));
        if (cron.size() == 0) {
            msg = String.format("Cron Scheduler config does not exist for the integration config: %s", context.getConfig().getId());
            context.getLogger().log(msg);
            context.getLogger().log("Exiting messaging process");
            current = Status.FAILURE_COMPLETE;
            return current;
        }

        this.transform = transform;
        frequency = Enum.valueOf(CronScheduler.Frequency.class, String.valueOf(cron.get(0).get("FREQUENCY")));

        if (frequency == CronScheduler.Frequency.WEEKLY && (cron.get(0).get("WEEK_DAY") == null || String.valueOf(cron.get(0).get("WEEK_DAY")).isEmpty())) {
            context.getLogger().log("Weekday is mandatory for the Weekly frequency");
            current = Status.FAILURE_COMPLETE;
            return current;
        }
        if (frequency == CronScheduler.Frequency.WEEKLY)
            weekDay = Enum.valueOf(CronScheduler.WeekDay.class, String.valueOf(cron.get(0).get("WEEK_DAY" )));
        day    = MiscHelper.convertObjectToInt(cron.get(0).get("MONTH_DAY"));
        hour   = MiscHelper.convertObjectToInt(cron.get(0).get("HOUR"     ));
        minute = MiscHelper.convertObjectToInt(cron.get(0).get("MINUTE"   ));

        if (frequency == CronScheduler.Frequency.DAILY)
            scheduler = new CronScheduler(hour, minute);
        else if (frequency == CronScheduler.Frequency.WEEKLY)
            scheduler = new CronScheduler(weekDay, hour, minute);
        else if (frequency == CronScheduler.Frequency.MONTHLY)
            scheduler = new CronScheduler(day, hour, minute);

        initRuntimeHook(context, String.format("Shutting down Cron Scheduler:%s", context.getConfig().getId()));
        scheduler.submit(this);
        context.getLogger().log("CRON scheduler has been submitted");
        return current;
    }

    @Override
    public void run() {
        context.getLogger().log("CRON: Started scheduled task: %s/%s", context.getConfig().getId(), context.getTaskConfig().getExecValue());
        executeDependentTask(context, transform);
    }
}
