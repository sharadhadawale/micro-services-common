package com.rajanainart.common.rest;

import java.util.*;

import com.rajanainart.common.data.QueryFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

public class RestQueryRequest {
    public static final String HTTP_CURRENT_PAGE_KEY = "currentPage";
    public static final String HTTP_PAGE_SIZE_KEY    = "pageSize";

    @JsonIgnore  private int dummy;
    @JsonInclude private ArrayList<QueryFilter> filter  = new ArrayList<>();
    @JsonInclude private Map<String, String>    params  = new HashMap  <>();
    @JsonInclude private List<Map<String, String>> data = new ArrayList<>();
    @JsonInclude private Map<String, Object> paramsWithObject = new HashMap<>();
    @JsonInclude private ArrayList<Map<String, Object>> listParams = new ArrayList<>();

    public ArrayList<QueryFilter> getFilter () { return filter; }
    public Map<String, String> 	   getParams () { return params; }
    public List<Map<String, String>> getData() { return data; }

    public ArrayList<Map<String, Object>> getListParams() {
        return listParams;
    }

    public void setFilter(ArrayList<QueryFilter> f) { filter = f; }
    public void setParams(Map<String, String> p   ) { params = p; }
    public void setData(List<Map<String, String>> data) { this.data = data; }

    public void setListParams(ArrayList<Map<String, Object>> lParams) {
        listParams = lParams;
    }

    public int getCurrentPageNumber() {
        if (!params.containsKey(HTTP_CURRENT_PAGE_KEY))
            return -1;
        return Integer.parseInt(params.get(HTTP_CURRENT_PAGE_KEY));
    }

    public Optional<Integer> getPageSize() {
        if (!params.containsKey(HTTP_PAGE_SIZE_KEY))
            return Optional.empty();
        return Optional.of(Integer.parseInt(params.get(HTTP_PAGE_SIZE_KEY)));
    }

    public Map<String, Object> getParamsWithObject() {
        return paramsWithObject;
    }

    public void setParamsWithObject(Map<String, Object> paramsWithObject) {
        this.paramsWithObject = paramsWithObject;
    }
}