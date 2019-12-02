package com.rajanainart.common.nas;

import com.rajanainart.common.config.XmlConfig;
import com.rajanainart.common.helper.XmlNodeHelper;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;

@Component("nas-config")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class NasConfig implements XmlConfig {
    public enum OperationType { NAS_COPY, UPLOAD }
    public enum TargetExists  { SKIP, REPLACE    }

    private String id  ;
    private String name;
    private String filter = "[\\s\\S\\d]+";

    private NasInfo source;
    private NasInfo target;

    private OperationType type   = OperationType.NAS_COPY;
    private TargetExists  exists = TargetExists.SKIP     ;

    public String  getId    () { return id    ; }
    public String  getName  () { return name  ; }
    public NasInfo getSource() { return source; }
    public NasInfo getTarget() { return target; }

    public String        getFilterRegex  () { return filter; }
    public OperationType getOperationType() { return type  ; }
    public TargetExists  getTargetExists () { return exists; }

    @Override
    public synchronized void configure(Node node) {
        synchronized (this) {
            id     = XmlNodeHelper.getAttributeValue(node, "id"  );
            name   = XmlNodeHelper.getAttributeValue(node, "name");

            Node n = XmlNodeHelper.getChildNode(node, "filter-regex");
            if (n != null) filter = XmlNodeHelper.getNodeValue(n);

            n = XmlNodeHelper.getChildNode(node, "op-type");
            if (n != null) {
                String value = XmlNodeHelper.getNodeValue(n);
                if (!value.isEmpty())
                    type = XmlNodeHelper.getNodeValueAsEnum(OperationType.class, n);
            }

            n = XmlNodeHelper.getChildNode(node, "exists");
            if (n != null) {
                String value = XmlNodeHelper.getNodeValue(n);
                if (!value.isEmpty())
                    exists = XmlNodeHelper.getNodeValueAsEnum(TargetExists.class, n);
            }

            n = XmlNodeHelper.getChildNode(node, "source");
            if (n != null) source = new NasInfo(n);

            n = XmlNodeHelper.getChildNode(node, "target");
            if (n != null) target = new NasInfo(n);
        }
    }

    public class NasInfo {
        private String userName;
        private String password;
        private String path;

        public String getUserName() { return userName; }
        public String getPassword() { return password; }
        public String getPath    () { return path    ; }

        protected NasInfo(Node node) {
            Node n = XmlNodeHelper.getChildNode(node, "username");
            if (n != null) userName = XmlNodeHelper.getNodeValue(n);
            n = XmlNodeHelper.getChildNode(node, "password");
            if (n != null) password = XmlNodeHelper.getNodeValue(n);
            n = XmlNodeHelper.getChildNode(node, "path");
            if (n != null) path = XmlNodeHelper.getNodeValue(n);
        }
    }
}
