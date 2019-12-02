package com.rajanainart.common.upload;

import com.rajanainart.common.config.BaseConfig;
import com.rajanainart.common.config.XmlConfig;
import com.rajanainart.common.data.BaseMessageColumn;
import com.rajanainart.common.data.BaseMessageTable;
import com.rajanainart.common.helper.XmlNodeHelper;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

@Component("upload-config")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class UploadConfig implements XmlConfig, BaseMessageTable {
    private String id, name, table;
    private int count;
    private List<ColumnConfig> columns    = null;
    private List<String     >  uniqueCols = null;

    public String getId   () { return id  ; }
    public String getName () { return name; }
    public int          getBulkCount () { return count     ; }
    public String       getTarget    () { return table     ; }
    public List<String> getUniqueCols() { return uniqueCols; }

    public List<UploadConfig.ColumnConfig> getUploadColumns() { return columns; }

    public ArrayList<BaseMessageColumn> getColumns() {
        ArrayList<BaseMessageColumn> list = new ArrayList<>();
        for (ColumnConfig c : columns)
            list.add(c);
        return list;
    }

    public synchronized void configure(Node node) {
        synchronized (this) {
            id    = XmlNodeHelper.getAttributeValue(node, "id"   );
            name  = XmlNodeHelper.getAttributeValue(node, "name" );
            table = XmlNodeHelper.getAttributeValue(node, "target-table");
            count = XmlNodeHelper.getAttributeValueAsInteger(node, "bulk-count");

            if (id.isEmpty() || table.isEmpty())
                throw new NullPointerException("Attributes id & target-table are mandatory");

            String unique = XmlNodeHelper.getAttributeValue(node, "unique-columns");
            if (unique.isEmpty())
                throw new NullPointerException("Attribute unique-columns is mandatory");
            uniqueCols = new ArrayList<>();
            for (String col : unique.split(","))
                if (!col.isEmpty())
                    uniqueCols.add(col);

            ArrayList<Node> nodes = XmlNodeHelper.getChildNodes(node, "column");
            columns = new ArrayList<>();
            for (int idx=0; idx<nodes.size(); idx++)
                columns.add(new UploadConfig.ColumnConfig(nodes.get(idx)));
        }
    }

    public class ColumnConfig extends BaseConfig implements BaseMessageColumn {
        private String column = "";
        private int index = 0;
        private boolean visible = true;
        private List<UploadConfig.ValidatorConfig> validators = null;

        public List<UploadConfig.ValidatorConfig> getValidators() { return validators; }
        public int     getIndex      () { return index  ; }
        public String  getTargetField() { return column ; }
        public String  getFormat     () { return ""     ; }
        public String  getSelectQuery() { return ""     ; }
        public boolean getIsMandatory() { return false  ; }
        public boolean getIsEditable () { return false  ; }
        public boolean getAutoIncr   () { return false  ; }
        public boolean getIsVisible  () { return visible; }
        public boolean getIsPk       () { return false  ; }
        public BaseMessageColumn.ColumnType getType() { return ColumnType.TEXT; }

        public ColumnConfig(Node node) {
            super(node);

            column = XmlNodeHelper.getAttributeValue(node, "target-column");
            if (column.isEmpty())
                throw new NullPointerException("Attributes target-column is mandatory");

            visible = XmlNodeHelper.getAttributeValueAsBoolean(node, "visible");
            index   = XmlNodeHelper.getAttributeValueAsInteger(node, "index");

            ArrayList<Node> nodes = XmlNodeHelper.getChildNodes(node, "validator");
            validators = new ArrayList<>();
            for (int idx=0; idx<nodes.size(); idx++)
                validators.add(new UploadConfig.ValidatorConfig(nodes.get(idx)));
        }
    }

    public class ValidatorConfig {
        private String type = "";
        private String message = "";
        private List<String> parameters;

        public String getType() { return type; }
        public String getErrorMessage() { return message; }
        public List<String> getParameters() { return parameters; }

        protected ValidatorConfig(Node node) {
            type = XmlNodeHelper.getAttributeValue(node, "type");
            if (type.isEmpty())
                throw new NullPointerException("Attributes validator/type is mandatory");
            message = XmlNodeHelper.getAttributeValue(node, "error-message");

            String value = "";
            parameters = new ArrayList<>();
            for (int index=0;;index++) {
                value = XmlNodeHelper.getAttributeValue(node, "param"+index);
                if (value.isEmpty()) break;

                parameters.add(value);
            }
        }
    }
}
