package com.rajanainart.nas;

import com.rajanainart.concurrency.ConcurrencyManager;
import com.rajanainart.helper.FileHelper;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.msfscc.fileinformation.FileAllInformation;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class NasFolder extends BaseNas {
    private static final Logger logger = LoggerFactory.getLogger(NasFolder.class);

    private boolean upload = false;
    private Map<String, MultipartFile> multipartFiles;

    public NasFolder(NasSession sourceSession, String subFolderName) {
        super(sourceSession, subFolderName);
    }

    public NasFolder(NasSession targetSession, String subFolderName, Map<String, MultipartFile> multipartFiles) {
        super(targetSession, subFolderName);

        this.multipartFiles = multipartFiles;
        upload = true;
    }

    public List<String> getAllFiles() {
        List<String> result = new ArrayList<>();
        List<FileIdBothDirectoryInformation> files = getSession().getDiskShare().list(getFullPath());

        files.forEach(x -> {
            if (x.getFileAttributes() == FileAttributes.FILE_ATTRIBUTE_ARCHIVE.getValue())
                result.add(x.getFileName());
        });
        return result;
    }

    public List<NasFile> getAllNasFiles() {
        List<NasFile> result = new ArrayList<>();
        List<FileIdBothDirectoryInformation> files = getSession().getDiskShare().list(getFullPath());

        files.forEach(x -> buildFiles(x, result));
        return result;
    }

    public List<NasFile> getAllNasFiles(String filterRegex) {
        List<NasFile> result = new ArrayList<>();
        List<FileIdBothDirectoryInformation> files = getSession().getDiskShare().list(getFullPath(), filterRegex);

        files.forEach(x -> buildFiles(x, result));
        return result;
    }

    private void buildFiles(FileIdBothDirectoryInformation fileId, List<NasFile> result) {
        String             path     = FileHelper.combinePaths(PATH_SPLITTER, getFullPath(), fileId.getFileName());
        FileAllInformation fileInfo = getSession().getDiskShare().getFileInformation(path);
        if (fileInfo.getBasicInformation().getFileAttributes() == FileAttributes.FILE_ATTRIBUTE_ARCHIVE.getValue())
            result.add(new NasFile(getSession(), getSubFolderName(), NasFile.getFileName(fileInfo)));
    }

    public int copyAllFilesParallel(NasSession targetSession, String targetSubFolderName, boolean replace) {
        if (!upload) {
            List<NasFile> files = getAllNasFiles();
            return copyAllFilesParallel(targetSession, targetSubFolderName, files, replace);
        }
        else
            return copyAllFilesParallel(replace);
    }

    public int copyAllFilesParallel(NasSession targetSession, String targetSubFolderName, String filterRegex, boolean replace) {
        if (!upload) {
            List<NasFile> files = !filterRegex.isEmpty() ? getAllNasFiles(filterRegex) : getAllNasFiles();
            return copyAllFilesParallel(targetSession, targetSubFolderName, files, replace);
        }
        else
            return copyAllFilesParallel(replace);
    }

    private int copyAllFilesParallel(NasSession targetSession, String targetSubFolderName, List<NasFile> files, boolean replace) {
        if (files.size() == 0) return 0;

        try (ConcurrencyManager concurrency = new ConcurrencyManager("nas-copy")) {
            for (NasFile f : files) {
                log(logger, String.format("Thread initiated for copying file:%s", f.getFileName()));
                ProcessFile process = new ProcessFile(targetSession, f, targetSubFolderName, replace);
                concurrency.submit(process);
            }
            concurrency.awaitTermination((message) -> log(logger, message), 10);
        }
        return files.size();
    }

    private int copyAllFilesParallel(boolean replace) {
        createFolder();
        try (ConcurrencyManager concurrency = new ConcurrencyManager("nas-copy")) {
            for (Map.Entry<String, MultipartFile> f : multipartFiles.entrySet()) {
                log(logger, String.format("Thread initiated for copying file:%s", f.getValue().getOriginalFilename()));
                NasFile   file  = new NasFile(getSession(), getSubFolderName(), f.getValue());
                NasFolder.ProcessFile process = this.new ProcessFile(getSession(), file, getSubFolderName(), replace);
                concurrency.submit(process);
            }
            concurrency.awaitTermination((message) -> log(logger, message), 1);
        }
        return multipartFiles.size();
    }

    public void createFolder() {
        if (!getSession().getDiskShare().folderExists(getFullPath()))
            getSession().getDiskShare().mkdir(getFullPath());
    }

    public class ProcessFile implements ConcurrencyManager.BaseConcurrencyThread {
        private NasSession session;
        private NasFile    file;
        private boolean    replace;
        private boolean    complete = false;
        private String     name;
        private String     targetSubFolderName;

        public ProcessFile(NasSession session, NasFile file, String targetSubFolderName, boolean replace) {
            this.session = session;
            this.file    = file;
            this.replace = replace;
            this.targetSubFolderName = targetSubFolderName;

            name = String.format("NasProcessFile-%s-%s", file.getFileName(), new SecureRandom().nextLong());
        }

        @Override
        public void run() {
            try {
                NasFile nTarget = new NasFile(session, targetSubFolderName, file.getFileName());
                if (nTarget.getSession().getDiskShare().fileExists(nTarget.getFullPath()) && replace)
                    nTarget.delete();
                file.copy(nTarget);
                logger.info(String.format("Copying file to NAS is completed: %s", name));
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            complete = true;
        }

        public boolean getIsComplete() { return complete; }
        public String  getThreadName() { return name    ; }
    }
}
