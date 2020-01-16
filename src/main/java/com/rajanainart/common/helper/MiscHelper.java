package com.rajanainart.common.helper;

import org.springframework.web.util.HtmlUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class MiscHelper {
    private MiscHelper() {}

    //for fortify fix
    public static boolean isValidName(String value) {
        return value.matches("[a-zA-Z0-9-_ ]*");
    }

    //for fortify fix
    public static Map<String, String> paramsValidated(Map<String, String> params) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key   = HtmlUtils.htmlEscape(entry.getKey  ());
            String value = HtmlUtils.htmlEscape(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static List<String> parseAsList(String value, String regex) {
        return Arrays.asList(value.split(regex));
    }

    public static int fixFortifyLogForging(String value) {
        int actual = -1;
        try {
            actual = Integer.parseInt(value);
        }
        catch (NumberFormatException ex) {
            ex.printStackTrace();
        }
        return actual;
    }

    public static String getSystemDate() {
        DateFormat df = new SimpleDateFormat("MMddyyyy-HHmmss");
        Date now = getSystemDateTime();
        return df.format(now);
    }

    public static String buildArrayToString(ArrayList input) {
        List<String> results = new ArrayList<>();
        results.add("");
        input.forEach(x -> {
            String temp = results.get(0).isEmpty() ? String.format("'%s'", x) : String.format("%s, '%s'", results.get(0), x);
            results.set(0, temp);
        });
        return results.get(0);
    }

    public static boolean isEmptyMap(Map<String, Object> map) {
        boolean empty = true;
        for (Map.Entry<String, Object> key : map.entrySet()) {
            if (key.getValue() != null && !String.valueOf(key.getValue()).isEmpty()) {
                empty = false;
                break;
            }
        }
        return empty;
    }

    public static Date getSystemDateTime() {
        return Calendar.getInstance().getTime();
    }

    public static String convertDateToString(Date date, String format) {
        DateFormat df = new SimpleDateFormat(format);
        return df.format(date);
    }

    public static String getMapAsString(Map<String, String> map) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> key : map.entrySet())
            builder.append(String.format("%s %s=%s", (builder.length() != 0 ? "," : ""), key.getKey(), key.getValue()));
        return builder.toString();
    }

    public static Date convertStringToDate(String value, String format) {
        try {
            DateFormat f = new SimpleDateFormat(format, Locale.ENGLISH);
            return f.parse(value);
        }
        catch(Exception ex) {
            return getSystemDateTime();
        }
    }

    public static long convertStringToLong(String value) {
        try {
            Double result = Double.parseDouble(value);
            return result.longValue();
        }
        catch(Exception e) { return 0; }
    }

    public static long convertObjectToLong(Object value) {
        try {
            if (value != null) {
                Double result = Double.parseDouble(String.valueOf(value));
                return result.longValue();
            }
            else
                return 0;
        }
        catch(Exception e) { return 0; }
    }

    public static int convertStringToInt(String value) {
        try {
            Double result = Double.parseDouble(value);
            return result.intValue();
        }
        catch(Exception e) { return 0; }
    }

    public static int convertObjectToInt(Object value) {
        try {
            if (value != null) {
                Double result = Double.parseDouble(String.valueOf(value));
                return result.intValue();
            }
            else
                return 0;
        }
        catch(Exception e) { return 0; }
    }

    public static double convertStringToDouble(String value) {
        try {
            return Double.parseDouble(value);
        }
        catch(Exception e) { return 0; }
    }

    public static double convertObjectToDouble(Object value) {
        try {
            if (value != null)
                return Double.parseDouble(value.toString());
            else
                return 0;
        }
        catch(Exception e) { return 0; }
    }

    public static ArrayList<String> convertStringToArray(String value, int length) {
        ArrayList<String> result = new ArrayList<String>();
        int current = 0;
        int counter = 1;
        do {
            if ((current+length) > value.length()){
                result.add(value.substring(current, value.length()));
                break;
            }
            else {
                result.add(value.substring(current, (counter*length)));
                current+=length;
                counter++;
            }
        }
        while (true);

        return result;
    }

    public static String getOracleVarcharFilter(String input, String delimiter) {
        StringTokenizer tokenizer = new StringTokenizer(input, delimiter);
        String result = "";
        while (tokenizer.hasMoreTokens())
            result += String.format("%s'%s'", result.length()==0?"":",", tokenizer.nextToken());
        return result;
    }

    public static String getOracleVarchar(ArrayList<String> input) {
        String result = "";
        for (int index=0; index<input.size(); index++)
            result += String.format("%s'%s'", (index==0?"":"||"), input.get(index));
        return result;
    }

    public static boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        }
        catch(Exception e) {
            return false;
        }
    }

    public static boolean isNumeric(String value) {
        try {
            Double.parseDouble(value);
            return true;
        }
        catch(Exception e) {
            return false;
        }
    }

    public static boolean isDate(String value, String format) {
        try {
            DateFormat f = new SimpleDateFormat(format, Locale.ENGLISH);
            f.parse(value);
            return true;
        }
        catch(Exception ex) {
            return false;
        }
    }
}
