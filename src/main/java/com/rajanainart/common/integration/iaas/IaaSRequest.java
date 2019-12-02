package com.rajanainart.common.integration.iaas;

import com.rajanainart.common.integration.IntegrationConfig;
import com.rajanainart.common.rest.BaseRestController;

import java.util.Locale;

public class IaaSRequest {
    private String type   = "";
    private String source = "";
    private String target = "";
    private String query  = "";
    private String pkCol  = "";
    private String targetTable = "";
    private NoSql  nosql  = null;

    public void setSource(String value) { this.source = value; }
    public void setTarget(String value) { this.target = value; }
    public void setQuery (String value) { this.query  = value; }
    public void setType  (String value) { this.type   = value; }
    public void setPkCol (String value) { this.pkCol  = value; }
    public void setTargetTable(String value) { this.targetTable = value; }
    public void setNoSqlConfig(NoSql  value) { this.nosql       = value; }

    public String getType  () { return type  ; }
    public String getSource() { return source; }
    public String getTarget() { return target; }
    public String getQuery () { return query ; }
    public String getPkCol () { return pkCol ; }

    public String getName       () { return targetTable; }
    public String getTargetTable() { return targetTable; }
    public NoSql  getNoSqlConfig() { return nosql      ; }

    public String getRESTQueryName  () { return String.format("Qry%s", getName()); }
    public String getIntegrationName() { return String.format("Int%s", getName()); }
    public String getTaskName       () { return String.format("Tsk%s", getName()); }
    public String getNoSqlConfigName() { return String.format("Cfg%s", getName()); }

    public String validate() {
        if (type.isEmpty() || source.isEmpty() || target.isEmpty() || targetTable.isEmpty() ||
            pkCol.isEmpty() || query.isEmpty())
            return "Parameters type, source, target, targetTable, pkCol and query are mandatory";

        if (getTaskType() == IntegrationConfig.TaskType.NOSQL_IMPORT) {
            if (nosql == null || (nosql.type.isEmpty() || nosql.url.isEmpty()))
                return "Parameters noSqlConfig.type and noSqlConfig.url are mandatory";
        }

        return BaseRestController.SUCCESS;
    }

    public IntegrationConfig.TaskType getTaskType() {
        try {
            return Enum.valueOf(IntegrationConfig.TaskType.class, type.toUpperCase(Locale.ENGLISH));
        }
        catch (Exception ex) {
            return IntegrationConfig.TaskType.IMPORT;
        }
    }

    public class NoSql {
        private String type = "";
        private String url  = "";
        private String userName;
        private String password;

        public void setType(String value) { type = value; }
        public void setUrl (String value) { url  = value; }
        public void setUserName(String value) { userName = value; }
        public void setPassword(String value) { password = value; }

        public String getType() { return type; }
        public String getUrl () { return url ; }
        public String getUserName() { return userName; }
        public String getPassword() { return password; }
    }
}
