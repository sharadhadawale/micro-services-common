package com.rajanainart.common.nas;

import com.rajanainart.common.concurrency.ConcurrencyManager;
import com.rajanainart.common.helper.FileHelper;
import com.rajanainart.common.integration.IntegrationLog;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFilenameFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class NasFolder {
    private static final Logger logger = LoggerFactory.getLogger(NasFolder.class);

    private NasConfig.NasInfo nas ;
    private SmbFile           file;
    private IntegrationLog integrationLog;
    private String            subFolderName;

    public String getSubFolderName() {
        return subFolderName != null && !subFolderName.isEmpty() ? subFolderName : "";
    }

    public NasConfig.NasInfo getNasInfo() { return nas ; }
    public SmbFile           getSmbFile() { return file; }

    public NasFolder(NasConfig.NasInfo info, String subFolderName) throws IOException {
        this.subFolderName = subFolderName;
        nas  = info;
        file = NasFile.buildSmbFile(info, FileHelper.combinePaths("/", info.getPath(), getSubFolderName()));

        if (file.exists() && !file.isDirectory())
            throw new IOException(String.format("%s is not a directory", info.getPath()));
    }

    public void setIntegrationLog(IntegrationLog log) { integrationLog = log; }

    public List<String> getAllFiles() {
        List<String> result = new ArrayList<>();
        try {
            String[] files = file.list();
            result = getAllFiles(files);
        }
        catch (SmbException ex) {
            logger.warn(String.format("Error while SMB listing the files: %s", ex.getMessage()));
            ex.printStackTrace();
        }
        return result;
    }

    public List<String> getAllFiles(String filterRegex) {
        List<String> result = new ArrayList<>();
        try {
            String[] files = file.list(new NasFileFilter(filterRegex));
            result = getAllFiles(files);
        }
        catch (SmbException ex) {
            logger.warn(String.format("Error while smb listing the files: %s", ex.getMessage()));
            ex.printStackTrace();
        }
        return result;
    }

    private List<String> getAllFiles(String[] files) {
        List<String> result = new ArrayList<>();
        for (String f : files)
            result.add(f);
        return result;
    }

    public List<NasFile> getAllNasFiles() {
        List<String> files = getAllFiles();
        return getAllNasFiles(files);
    }

    public List<NasFile> getAllNasFiles(String filterRegex) {
        List<String> files = getAllFiles(filterRegex);
        return getAllNasFiles(files);
    }

    private List<NasFile> getAllNasFiles(List<String> files) {
        List<NasFile> list  = new ArrayList<>();
        for (String f : files) {
            try {
                SmbFile smb = NasFile.buildSmbFile(nas, FileHelper.combinePaths("/", nas.getPath(), f));
                if (smb.isFile()) list.add(new NasFile(nas, "", smb));
            }
            catch (IOException ex) {
                logger.warn(String.format("Error while smb listing the files: %s", ex.getMessage()));
                ex.printStackTrace();
            }
        }
        return list;
    }

    public void copyAllFiles(NasConfig.NasInfo target, boolean replace) {
        List<NasFile> files = getAllNasFiles();
        copyAllFiles(target, files, replace);
    }

    public void copyAllFiles(NasConfig.NasInfo target, String filterRegex, boolean replace) {
        List<NasFile> files = getAllNasFiles(filterRegex);
        copyAllFiles(target, files, replace);
    }

    private void copyAllFiles(NasConfig.NasInfo target, List<NasFile> files, boolean replace) {
        for (NasFile f : files) {
            try {
                log(String.format("Copying file:%s", f.getSmbFile().getName()));
                NasFile nTarget = new NasFile(target, "", f.getSmbFile().getName());
                if (nTarget.getSmbFile().exists() && replace) nTarget.delete();
                f.copy(nTarget);
                log(String.format("Copying file:%s is complete", f.getSmbFile().getName()));
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void createFolder() throws SmbException {
        if (!file.exists()) file.mkdir();
    }

    private void log(String msg) {
        if (integrationLog != null)
            integrationLog.log(msg);
        else
            logger.info(msg);
    }

    public void copyAllFilesParallel(NasConfig.NasInfo target, boolean replace) {
        List<NasFile> files = getAllNasFiles();
        copyAllFilesParallel(target, files, replace);
    }

    public void copyAllFilesParallel(NasConfig.NasInfo target, String filterRegex, boolean replace) {
        List<NasFile> files = getAllNasFiles(filterRegex);
        copyAllFilesParallel(target, files, replace);
    }

    private void copyAllFilesParallel(NasConfig.NasInfo target, List<NasFile> files, boolean replace) {
        if (files.size() == 0) return;

        try (ConcurrencyManager concurrency = new ConcurrencyManager("nas-copy")) {
            for (NasFile f : files) {
                log(String.format("Thread initiated for copying file:%s", f.getSmbFile().getName()));
                ProcessFile process = new ProcessFile(target, f, replace);
                concurrency.submit(process);
            }
            concurrency.awaitTermination((message) -> log(message), 10);
        }
    }

    public class ProcessFile implements ConcurrencyManager.BaseConcurrencyThread {
        private NasConfig.NasInfo info;
        private NasFile           file;
        private boolean           replace;
        private boolean           complete = false;
        private String            name;

        public ProcessFile(NasConfig.NasInfo info, NasFile file, boolean replace) {
            this.info    = info;
            this.file    = file;
            this.replace = replace;

            name = String.format("NasProcessFile-%s", file.getSmbFile().getName());
        }

        @Override
        public void run() {
            try {
                NasFile nTarget = new NasFile(info, "", file.getSmbFile().getName());
                if (nTarget.getSmbFile().exists() && replace) nTarget.delete();
                file.copy(nTarget);
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
            complete = true;
        }

        public boolean getIsComplete() { return complete; }
        public String  getThreadName() { return name    ; }
    }

    public class NasFileFilter implements SmbFilenameFilter {
        private String regex = "";

        public NasFileFilter(String filter) {
            regex = filter;
        }

        @Override
        public boolean accept(SmbFile parent, String name) {
            if (regex.isEmpty()) return true;

            return name.matches(regex);
        }
    }
}
