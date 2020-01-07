package com.rajanainart.common.rest.hierarchy;

import lombok.Data;

@Data
public class TableDetails {
    private String tableName    ;
    private String pkColName    ;
    private String nameColName  ;
    private String descColName  ;
    private String levelColName ;
    private String parentColName;
}
