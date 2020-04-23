package com.rajanainart.integration.task;

import com.rajanainart.concurrency.CronScheduler;
import com.rajanainart.helper.MiscHelper;
import com.rajanainart.integration.IntegrationContext;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@Component("integration-task-cron_scheduler")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CronSchedulerIntegrationTask implements IntegrationTask {
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

        String scheduleFor = context.getRestQueryRequest().getParams().getOrDefault("ScheduleFor", "");
        if (!MiscHelper.isNumeric(scheduleFor)) {
            msg = String.format("Invalid 'ScheduleFor' request parameter", context.getConfig().getId());
            context.getLogger().log(msg);
            context.getLogger().log("Exiting messaging process");
            current = Status.FAILURE_COMPLETE;
            return current;
        }

        CronScheduler.Frequency frequency;
        CronScheduler.WeekDay   weekDay = CronScheduler.WeekDay.SUN;
        int day   ;
        int hour  ;
        int minute;
        TimeZone timeZone;

        StringBuilder builder = new StringBuilder();
        builder.append("SELECT c.INTEGRATION_CRON_ID, c.NAME, c.DESCRIPTION, UCASE(f.description) AS FREQUENCY,\r\n")
                .append("c.MONTH_DAY, UCASE(d.description) AS WEEK_DAY, c.HOUR, c.MINUTE, c.TIME_ZONE, c.IS_RUNNING\r\n")
                .append("FROM CMN_INTEGRATION_CRON c\r\n")
                .append("INNER JOIN CMN_SYSTEM_ENUMS f ON f.enum_name = 'CRON_FREQUENCY' AND f.value = c.frequency\r\n")
                .append("LEFT OUTER JOIN CMN_SYSTEM_ENUMS d ON d.enum_name = 'CRON_WEEK_DAY' AND d.value = c.week_day\r\n")
                .append("WHERE c.schedule_for = ?p_for");

        List<Map<String, Object>> cron = context.getSourceDb().selectAsMapList(builder.toString(),
                                                            context.getSourceDb().new Parameter("p_for", scheduleFor));
        if (cron.size() == 0) {
            msg = String.format("No Cron Schedulers for the given 'ScheduleFor'");
            context.getLogger().log(msg);
            context.getLogger().log("Exiting messaging process");
            current = Status.FAILURE_COMPLETE;
            return current;
        }

        String runOnDbStatus = context.getRestQueryRequest().getParams().getOrDefault("RunOnDbStatus", "0");
        for (Map<String, Object> job : cron) {
            if (runOnDbStatus.equalsIgnoreCase("1") &&
                String.valueOf(job.get("IS_RUNNING")).equalsIgnoreCase( "0")) continue;

            context.getLogger().log("Starting the CRON scheduler: %s", job.get("NAME"));
            this.transform = transform;
            frequency = Enum.valueOf(CronScheduler.Frequency.class, String.valueOf(job.get("FREQUENCY")));

            if (frequency == CronScheduler.Frequency.WEEKLY && (job.get("WEEK_DAY") == null || String.valueOf(job.get("WEEK_DAY")).isEmpty())) {
                context.getLogger().log("Weekday is mandatory for the Weekly frequency");
                current = Status.FAILURE_COMPLETE;
                return current;
            }
            if (frequency == CronScheduler.Frequency.WEEKLY)
                weekDay = Enum.valueOf(CronScheduler.WeekDay.class, String.valueOf(job.get("WEEK_DAY" )));
            day      = MiscHelper.convertObjectToInt(job.get("MONTH_DAY"));
            hour     = MiscHelper.convertObjectToInt(job.get("HOUR"     ));
            minute   = MiscHelper.convertObjectToInt(job.get("MINUTE"   ));
            timeZone = TimeZone.getTimeZone(String.valueOf(job.get("TIME_ZONE")));

            if (frequency == CronScheduler.Frequency.DAILY)
                scheduler = new CronScheduler(timeZone, hour, minute);
            else if (frequency == CronScheduler.Frequency.WEEKLY)
                scheduler = new CronScheduler(timeZone, weekDay, hour, minute);
            else if (frequency == CronScheduler.Frequency.MONTHLY)
                scheduler = new CronScheduler(timeZone, day, hour, minute);

            scheduler.submit(new CronSchedulerTask(MiscHelper.convertObjectToLong(job.get("INTEGRATION_CRON_ID"))));
            context.getSourceDb().executeQueryWithJdbc("UPDATE CMN_INTEGRATION_CRON SET IS_RUNNING = 1 WHERE INTEGRATION_CRON_ID = ?p_id",
                    context.getSourceDb().new Parameter("p_id", job.get("INTEGRATION_CRON_ID")));
            context.getLogger().log("CRON scheduler '%s' has been submitted", job.get("NAME"));
        }
        context.getSourceDb().commit();
        initRuntimeHook(context, String.format("Shutting down Cron Scheduler:%s", context.getConfig().getId()));
        return current;
    }

    public class CronSchedulerTask extends Thread {
        private long id;

        public long getId() { return id; }

        public CronSchedulerTask(long id) {
            this.id = id;
        }
        @Override
        public void run() {
            synchronized (this) {
                context.getLogger().log("CRON: Started scheduled task: %s/%s", context.getConfig().getId(), context.getTaskConfig().getExecValue());
                context.getRestQueryRequest().getParams().put("ID", String.valueOf(id));
                executeDependentTask(context, transform);
            }
        }
    }
}
