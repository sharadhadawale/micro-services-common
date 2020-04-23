package com.rajanainart.concurrency;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.util.Locale;
import java.util.TimeZone;

public class CronScheduler {
    public enum Frequency { DAILY, WEEKLY, MONTHLY }
    public enum WeekDay   { SUN, MON, TUE, WED, THU, FRI, SAT }

    private static ThreadPoolTaskScheduler executor = null;

    private int day   ;
    private int hour  ;
    private int minute;
    private TimeZone timeZone;

    private Frequency frequency = Frequency.DAILY;
    private WeekDay   weekDay   = WeekDay.SUN;

    private static void init() {
        if (executor == null) {
            executor = new ThreadPoolTaskScheduler();
            executor.setPoolSize(50);
            executor.initialize();
        }
    }

    public Frequency getFrequency() { return frequency; }

    public CronScheduler(TimeZone timeZone, int hour, int minute) {
        frequency     = Frequency.DAILY;
        this.hour     = hour;
        this.minute   = minute;
        this.timeZone = timeZone;

        if (hour < 0 || hour > 23)
            throw new IllegalArgumentException("Hour should be between 0 to 23");

        if (minute < 0 || minute > 59)
            throw new IllegalArgumentException("Minute should be between 0 to 59");

        init();
    }

    public CronScheduler(TimeZone timeZone, WeekDay weekDay, int hour, int minute) {
        this(timeZone, hour, minute);

        frequency    = Frequency.WEEKLY;
        this.weekDay = weekDay;
    }

    public CronScheduler(TimeZone timeZone, int day, int hour, int minute) {
        this(timeZone, hour, minute);

        frequency = Frequency.MONTHLY;
        this.day  = day;

        if (day < 0 || day > 31)
            throw new IllegalArgumentException("Day should be between 0 to 31");
    }

    public String getCronExpression() {
        String expression = "";
        switch (frequency) {
            case DAILY:
                expression = String.format("0 %s %s * * ?", minute, hour);
                break;
            case WEEKLY:
                expression = String.format("0 %s %s ? * %s", minute, hour, weekDay.toString().toUpperCase(Locale.ENGLISH));
                break;
            case MONTHLY:
                expression = String.format("0 %s %s %s * ?", minute, hour, day);
                break;
        }

        return expression;
    }

    public void submit(Runnable thread) {
        executor.schedule(thread, new CronTrigger(getCronExpression(), timeZone));
    }

    public static void shutdown() {
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }
}
