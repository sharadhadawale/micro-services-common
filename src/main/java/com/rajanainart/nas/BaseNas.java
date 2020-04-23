package com.rajanainart.nas;

import com.rajanainart.helper.FileHelper;
import com.rajanainart.integration.IntegrationLog;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.msfscc.fileinformation.FileAllInformation;
import org.slf4j.Logger;

public abstract class BaseNas {
    public static final String PATH_SPLITTER = "/";

    private IntegrationLog integrationLog;
    private NasSession        session;
    private String            subFolderName;
    private String            fileName;

    public void setIntegrationLog(IntegrationLog log) { integrationLog = log; }

    public NasSession         getSession () { return session; }
    public FileAllInformation getFileInfo() { return session.getDiskShare().getFileInformation(getFullPath()); }

    public String getSubFolderName() { return subFolderName != null ? subFolderName : ""; }
    public String getFileName     () { return fileName      != null ? fileName      : ""; }
    public String getFullPath     () { return FileHelper.combinePaths(PATH_SPLITTER, session.getNasInfo().getPath(), getSubFolderName(), getFileName()); }

    public boolean isDirectory() {
        FileAllInformation info = session.getDiskShare().getFileInformation(getFullPath());
        return info.getBasicInformation().getFileAttributes() == FileAttributes.FILE_ATTRIBUTE_DIRECTORY.getValue();
    }

    public boolean isFile() {
        FileAllInformation info = session.getDiskShare().getFileInformation(getFullPath());
        return info.getBasicInformation().getFileAttributes() == FileAttributes.FILE_ATTRIBUTE_ARCHIVE.getValue();
    }

    protected BaseNas(NasSession session, String subFolderName) {
        this.session       = session;
        this.subFolderName = subFolderName;
    }

    protected BaseNas(NasSession session, String subFolderName, String fileName) {
        this(session, subFolderName);

        this.fileName = fileName;
    }

    protected void log(Logger logger, String msg) {
        logger.info(msg);
        if (integrationLog != null)
            integrationLog.log(msg);
    }
}
