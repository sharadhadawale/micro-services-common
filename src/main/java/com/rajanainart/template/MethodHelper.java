package com.rajanainart.template;

import com.rajanainart.data.BaseEntity;
import com.rajanainart.helper.MiscHelper;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

@ExtensionMethod
@Component("DateHelper")
public class MethodHelper extends BaseEntity {
    public Date AddDaysToCurrentDate(String timeZone, int days) {
        TimeZone tz       = TimeZone.getTimeZone(timeZone);
        Calendar calendar = Calendar.getInstance(tz);
        calendar.add(Calendar.DATE, days);
        Date newDate = calendar.getTime();
        return MiscHelper.truncateTime(newDate);
    }

    public Date GetCurrentDate(String timeZone) {
        TimeZone tz       = TimeZone.getTimeZone(timeZone);
        Calendar calendar = Calendar.getInstance(tz);
        return calendar.getTime();
    }

    public Date AddDaysToCSTCurrentDate(int days) {
        TimeZone tz       = TimeZone.getTimeZone("CST");
        Calendar calendar = Calendar.getInstance(tz);
        calendar.add(Calendar.DATE, days);
        return calendar.getTime();
    }

    public Date GetCSTCurrentDate() {
        TimeZone tz       = TimeZone.getTimeZone("CST");
        Calendar calendar = Calendar.getInstance(tz);
        return calendar.getTime();
    }
}
