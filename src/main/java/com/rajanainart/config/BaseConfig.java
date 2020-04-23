package com.rajanainart.config;

import com.rajanainart.helper.XmlNodeHelper;
import org.w3c.dom.Node;

public class BaseConfig {
    protected String id   = "";
    protected String name = "";

    public String getId  () { return id;   }
    public String getName() { return name; }

    protected BaseConfig() {}

    public BaseConfig(Node node) {
        id   = XmlNodeHelper.getAttributeValue(node, "id"  );
        name = XmlNodeHelper.getAttributeValue(node, "name");

        if (id.isEmpty() || name.isEmpty())
            throw new NullPointerException("Attributes 'id/name' are mandatory");
    }
}
