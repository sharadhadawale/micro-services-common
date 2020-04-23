package com.rajanainart.nas;

import com.rajanainart.config.XmlConfig;
import com.rajanainart.helper.XmlNodeHelper;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;

@Component("nas-config")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class NasConfig implements XmlConfig {
    public enum OperationType { NAS_COPY, UPLOAD, FILE_COPY }
    public enum TargetExists  { SKIP, REPLACE }

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
        private String server  ;
        private String domain  ;
        private String userName;
        private String password;
        private String path  = "";
        private String share = "";

        public String getServer  () { return server  ; }
        public String getDomain  () { return domain  ; }
        public String getUserName() { return userName; }
        public String getPassword() { return password; }
        public String getPath    () { return path    ; }
        public String getShare   () { return share   ; }

        protected NasInfo(Node node) {
            Node n = XmlNodeHelper.getChildNode(node, "server");
            if (n != null) server = XmlNodeHelper.getNodeValue(n);

            n = XmlNodeHelper.getChildNode(node, "domain");
            if (n != null) domain = XmlNodeHelper.getNodeValue(n);

            n = XmlNodeHelper.getChildNode(node, "username");
            if (n != null) userName = XmlNodeHelper.getNodeValue(n);

            n = XmlNodeHelper.getChildNode(node, "password");
            if (n != null) password = XmlNodeHelper.getNodeValue(n);

            n = XmlNodeHelper.getChildNode(node, "path");
            if (n != null) path = XmlNodeHelper.getNodeValue(n);

            n = XmlNodeHelper.getChildNode(node, "share");
            if (n != null) share = XmlNodeHelper.getNodeValue(n);
        }
    }

    @Override
    public String toString() {
        return String.format("%s:%s:%s", id, name, type.toString());
    }
}
