package com.rajanainart.common.data.provider;

import java.util.List;

public interface DbSpecificProvider {
    String       getParameterRegex();
    String       getParameterKey();
    String       getParameterizedQuery(String query);
    List<String> getQueryParameters(String query);

    String selectCurrentSequenceString(String sequenceName);
}
