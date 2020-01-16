package com.rajanainart.common.integration;

import com.rajanainart.common.config.BaseConfig;
import com.rajanainart.common.config.XmlConfig;
import com.rajanainart.common.helper.XmlNodeHelper;
import com.rajanainart.common.integration.iaas.IaaSRequest;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component("process-config")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class IntegrationConfig implements XmlConfig {
    public enum TaskType      { IMPORT, PROCEDURE, JAVA, MESSAGING, SPARK, NAS, NOSQL_IMPORT, MAIL, CRON_SCHEDULER }
    public enum ExecLevel     { ROOT, DEPENDENT }
    public enum TransformType { NONE, FOREIGN_KEY, CUSTOM }

    private boolean active = true ;
    private boolean auto   = false;
    private String id, name;
    private List<TaskConfig> tasks = null;

    public String  getId       () { return id    ; }
    public String  getName     () { return name  ; }
    public boolean getActive   () { return active; }
    public boolean getAutoStart() { return auto  ; }

    public List<TaskConfig> getTasks() { return tasks; }

    public static IntegrationConfig getInstance(IaaSRequest iaaSRequest) {
        IntegrationConfig instance = new IntegrationConfig();
        instance.id     = iaaSRequest.getIntegrationName();
        instance.name   = iaaSRequest.getName();
        instance.tasks  = new ArrayList<>();

        TaskConfig task = instance.new TaskConfig(iaaSRequest);
        instance.tasks.add(task);
        return instance;
    }

    @Override
    public synchronized void configure(Node node) {
        synchronized (this) {
            id    = XmlNodeHelper.getAttributeValue(node, "id"  );
            name  = XmlNodeHelper.getAttributeValue(node, "name");

            if (id.isEmpty())
                throw new NullPointerException("Attribute id is mandatory");

            ArrayList<Node> nodes = XmlNodeHelper.getChildNodes(node, "task");
            tasks = new ArrayList<>();
            for (int idx=0; idx<nodes.size(); idx++)
                tasks.add(new TaskConfig(nodes.get(idx)));

            String value = XmlNodeHelper.getAttributeValue(node, "active");
            if (!value.isEmpty())
                active = XmlNodeHelper.getAttributeValueAsBoolean(node, "active");

            value = XmlNodeHelper.getAttributeValue(node, "auto");
            if (!value.isEmpty())
                auto = XmlNodeHelper.getAttributeValueAsBoolean(node, "auto");
        }
    }

    public class TaskConfig extends BaseConfig {
        private String       source, target, evalue, prevalue, postvalue;
        private TaskType     type   = TaskType.IMPORT;
        private ExecLevel    level  = ExecLevel.ROOT;
        private List<TransformConfig> transforms = null;
        private int volume = 100;

        public int          getBulkCount () { return volume   ; }
        public String       getSource    () { return source   ; }
        public String       getTarget    () { return target   ; }
        public String       getExecValue () { return evalue   ; }
        public TaskType     getType      () { return type     ; }
        public ExecLevel    getLevel     () { return level    ; }
        public List<TransformConfig> getTransforms() { return transforms; }

        public String[] getPreExecValue () { return prevalue .isEmpty() ? new String[] {} : prevalue .split(","); }
        public String[] getPostExecValue() { return postvalue.isEmpty() ? new String[] {} : postvalue.split(","); }
        public String[] getExecValues   () { return evalue   .isEmpty() ? new String[] {} : evalue   .split(","); }

        TaskConfig(IaaSRequest iaaSRequest) {
            String key = iaaSRequest.getTaskName();
            id = name  = key;
            type       = iaaSRequest.getTaskType();
            source     = iaaSRequest.getSource();
            target     = type == TaskType.NOSQL_IMPORT ? iaaSRequest.getNoSqlConfigName() : iaaSRequest.getTarget();
            evalue     = iaaSRequest.getRESTQueryName();
            prevalue   = postvalue = "";
            level      = ExecLevel.ROOT;
            transforms = new ArrayList<>();
        }

        public TaskConfig(Node node) {
            super(node);

            source = XmlNodeHelper.getAttributeValue(node, "source");
            target = XmlNodeHelper.getAttributeValue(node, "target");
            if (source.isEmpty() || target.isEmpty())
                throw new NullPointerException("Attributes source/target are mandatory");

            evalue    = XmlNodeHelper.getAttributeValue(node, "exec-value"     );
            prevalue  = XmlNodeHelper.getAttributeValue(node, "pre-exec-value" );
            postvalue = XmlNodeHelper.getAttributeValue(node, "post-exec-value");
            volume    = XmlNodeHelper.getAttributeValueAsInteger(node, "bulk-count");

            String value = XmlNodeHelper.getAttributeValue(node, "type");
            if (!value.isEmpty())
                type = XmlNodeHelper.getNodeAttributeValueAsEnum(TaskType.class, node, "type");

            value = XmlNodeHelper.getAttributeValue(node, "exec-level");
            if (!value.isEmpty())
                level = XmlNodeHelper.getNodeAttributeValueAsEnum(ExecLevel.class, node, "exec-level");

            ArrayList<Node> nodes = XmlNodeHelper.getChildNodes(node, "transform");
            transforms = new ArrayList<>();
            for (int idx=0; idx<nodes.size(); idx++)
                transforms.add(new TransformConfig(nodes.get(idx)));
        }
    }

    public class TransformConfig {
        private TransformType type = TransformType.NONE;
        private String field;
        private Map<String, String> params = null;

        public TransformType getTransformType() { return type  ; }
        public String        getField        () { return field ; }
        public Map<String, String> getParams() { return params; }

        public TransformConfig(Node node) {
            field = XmlNodeHelper.getAttributeValue(node, "field");

            String value = XmlNodeHelper.getAttributeValue(node, "type");
            if (!value.isEmpty())
                type = XmlNodeHelper.getNodeAttributeValueAsEnum(TransformType.class, node, "type");

            params = new HashMap<>();
            for (int index=0; index<node.getAttributes().getLength(); index++) {
                Node attribute = node.getAttributes().item(index);
                if (attribute.getNodeName().startsWith("param-"))
                    params.put(attribute.getNodeName(), attribute.getNodeValue());
            }
        }
    }
}
