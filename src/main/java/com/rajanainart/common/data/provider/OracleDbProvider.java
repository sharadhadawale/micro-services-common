package com.rajanainart.common.data.provider;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("org.hibernate.dialect.Oracle10gDialect")
public class OracleDbProvider implements DbSpecificProvider {
    public String getParameterRegex() {
        return "[\\:][a-zA-Z0-9\\_]*";
    }

    public String getParameterKey() {
        return "\\:";
    }

    public String getParameterizedQuery(String query) {
        return query;
    }

    public List<String> getQueryParameters(String query) {
        List<String> params = new ArrayList<>();
        Pattern pattern = Pattern.compile(getParameterRegex());
        Matcher matcher = pattern.matcher(query);
        while (matcher.find())
            params.add(matcher.group().replaceAll("\\:", ""));
        return params;
    }

    public String selectCurrentSequenceString(String sequenceName) {
        return String.format("SELECT %s.CURRVAL FROM dual", sequenceName);
    }
}
