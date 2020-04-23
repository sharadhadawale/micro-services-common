package com.rajanainart.resource;

import com.rajanainart.config.XmlConfig;
import com.rajanainart.helper.XmlNodeHelper;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;

import java.util.Locale;

@Component("file-config")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class FileConfig implements XmlConfig {
    public enum StorageTarget { RESPONSE_STREAM, PERSIST_INTERNALLY }
    public enum FileType { CSV, TSV, TXT, PDF, XLS, XLSX }

    private String   id, name;
    private String   fileName;
    private String   delimiter = "|";
    private FileType fileType  = FileType.TXT;
    private boolean  header    = true;
    private StorageTarget target = StorageTarget.RESPONSE_STREAM;

    public String   getId       () { return id       ; }
    public String   getName     () { return name     ; }
    public String   getFileName () { return fileName ; }
    public FileType getFileType () { return fileType ; }
    public boolean  hasHeader   () { return header   ; }
    public StorageTarget getTarget() { return target ; }

    public String getDelimiter() {
        switch (getFileType()) {
            case CSV:
                return ",";
            case TSV:
                return "\t";
            default:
                return delimiter;
        }
    }

    public static FileConfig getInstance(String fileName, StorageTarget target) {
        String name = fileName.replace("\"", "");
        int    idx  = name.lastIndexOf(".");
        String ext  = name.substring(idx+1).toUpperCase(Locale.ENGLISH);

        FileConfig config = new FileConfig();
        config.id = config.name = "CfgFileConfig";
        config.fileName  = fileName;
        config.fileType  = Enum.valueOf(FileType.class, ext);
        config.delimiter = config.getDelimiter();
        config.header    = true;
        config.target    = target;
        return config;
    }

    @Override
    public synchronized void configure(Node node) {
        synchronized (this) {
            id    = XmlNodeHelper.getAttributeValue(node, "id"  );
            name  = XmlNodeHelper.getAttributeValue(node, "name");

            Node n = XmlNodeHelper.getChildNode(node, "file-name");
            if (n != null) fileName = XmlNodeHelper.getNodeValue(n);

            n = XmlNodeHelper.getChildNode(node, "file-type");
            if (n != null) fileType = XmlNodeHelper.getNodeValueAsEnum(FileType.class, n);

            n = XmlNodeHelper.getChildNode(node, "delimiter");
            if (n != null) delimiter = XmlNodeHelper.getNodeValue(n);

            n = XmlNodeHelper.getChildNode(node, "header");
            if (n != null) header = XmlNodeHelper.getNodeValueAsBoolean(n);

            n = XmlNodeHelper.getChildNode(node, "target");
            if (n != null) target = XmlNodeHelper.getNodeValueAsEnum(StorageTarget.class, n);
        }
    }

    @Override
    public String toString() {
        return String.format("%s:%s:%s", id, name, fileName);
    }
}
