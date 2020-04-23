package com.rajanainart.mail;

import com.rajanainart.config.XmlConfig;
import com.rajanainart.helper.XmlNodeHelper;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;

import java.util.Arrays;
import java.util.List;

@Component("mail-config")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MailConfig  implements XmlConfig {
    private String id  ;
    private String name;

    private String from = "";
    private String to   = "";
    private String body = "";
    private String subject;
    private boolean html = false;

    public String getId     () { return id     ; }
    public String getName   () { return name   ; }
    public String getFrom   () { return from   ; }
    public String getTo     () { return to     ; }
    public String getBody   () { return body   ; }
    public String getSubject() { return subject; }
    public boolean isHtml   () { return html   ; }

    public List<String> getToList() { return Arrays.asList(to.split(",")); }

    @Override
    public synchronized void configure(Node node) {
        synchronized (this) {
            id     = XmlNodeHelper.getAttributeValue(node, "id"  );
            name   = XmlNodeHelper.getAttributeValue(node, "name");

            Node n = XmlNodeHelper.getChildNode(node, "from");
            if (n != null) from = XmlNodeHelper.getNodeValue(n);

            n = XmlNodeHelper.getChildNode(node, "to");
            if (n != null) to = XmlNodeHelper.getNodeValue(n);

            n = XmlNodeHelper.getChildNode(node, "subject");
            if (n != null) subject = XmlNodeHelper.getNodeValue(n);

            n = XmlNodeHelper.getChildNode(node, "body");
            if (n != null) body = XmlNodeHelper.getNodeValue(n);

            n = XmlNodeHelper.getChildNode(node, "html");
            if (n != null) html = XmlNodeHelper.getNodeValueAsBoolean(n);

            if (id.isEmpty() || from.isEmpty())
                throw new NullPointerException("Configuration values for 'id' attribute and 'from' node value are mandatory");
        }
    }

    @Override
    public String toString() {
        return String.format("%s:%s", id, name);
    }
}
