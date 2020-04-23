package com.rajanainart.helper;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TimeZoneHelper {
    public enum OffsetBase { UTC, GMT }

    private OffsetBase offsetBase = OffsetBase.UTC;

    public TimeZoneHelper(OffsetBase offsetBase) {
        this.offsetBase = offsetBase;
    }

    public List<TimeZoneDetail> getTimeZones(ZoneId locale) {
        LocalDateTime now = LocalDateTime.now();

        return ZoneId.getAvailableZoneIds().stream()
                .map(ZoneId::of)
                .sorted(new ZoneComparator())
                .map(id -> {
                    TimeZoneDetail detail  = new TimeZoneDetail();
                    detail.TimeZoneKey     = id.getId();
                    detail.TimeZoneDisplay = String.format("(%s%s) %s",  offsetBase, getOffset(now, id), id.getId());
                    if (locale != null)
                        detail.IsLocale    = id.getId().equalsIgnoreCase(locale.getId());
                    return detail;
                }).collect(Collectors.toList());
    }

    public static String getOffset(LocalDateTime dateTime, ZoneId id) {
        return dateTime
                .atZone(id)
                .getOffset()
                .getId()
                .replace("Z", "+00:00");
    }

    public class ZoneComparator implements Comparator<ZoneId> {
        @Override
        public int compare(ZoneId zoneId1, ZoneId zoneId2) {
            LocalDateTime now = LocalDateTime.now();
            ZoneOffset offset1 = now.atZone(zoneId1).getOffset();
            ZoneOffset offset2 = now.atZone(zoneId2).getOffset();

            return offset2.compareTo(offset1);
        }
    }

    public class TimeZoneDetail {
        public String  TimeZoneKey;
        public String  TimeZoneDisplay;
        public boolean IsLocale = false;
    }
}
