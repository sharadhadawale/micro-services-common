package com.rajanainart.common.nas;

import com.rajanainart.common.concurrency.ConcurrencyManager;
import com.rajanainart.common.helper.FileHelper;
import com.rajanainart.common.integration.IntegrationLog;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;
import org.apache.poi.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public final class NasFile {
    public  static final String HTTP_SUB_FOLDER_NAME_KEY = "subFolderName";
    private static final Logger logger = LoggerFactory.getLogger(NasFile.class);

    private NasConfig.NasInfo   nas  ;
    private SmbFile             file ;
    private MultipartFile       mfile;
    private Map<String, MultipartFile> mfiles;
    private boolean upload = false;
    private String  subFolderName;
    private IntegrationLog integrationLog;

    public String getSubFolderName() {
        return subFolderName != null && !subFolderName.isEmpty() ? subFolderName : "";
    }

    public NasConfig.NasInfo getNasInfo() { return nas ; }
    public SmbFile           getSmbFile() { return file; }
    public void setIntegrationLog(IntegrationLog log) { integrationLog = log; }

    public NasFile(NasConfig.NasInfo source, String subFolderName, String fileName) throws IOException {
        nas = source;
        this.subFolderName = subFolderName;

        String path = FileHelper.combinePaths("/", source.getPath(), subFolderName, fileName);
        file = buildSmbFile(source, path);

        if (file.exists() && !file.isFile())
            throw new IOException(String.format("%s is not a file", source.getPath()));
    }

    public NasFile(NasConfig.NasInfo source, String subFolderName, SmbFile file) {
        nas = source;
        this.file = file;
        this.subFolderName = subFolderName;
    }

    public NasFile(NasConfig.NasInfo target, String subFolderName, MultipartFile file) throws IOException {
        upload = true  ;
        nas    = target;
        mfile  = file  ;
        this.subFolderName = subFolderName;

        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        String path     = FileHelper.combinePaths("/", target.getPath(), subFolderName, fileName);
        this.file       = buildSmbFile(target, path);
    }

    public NasFile(NasConfig.NasInfo target, String subFolderName, Map<String, MultipartFile> files) {
        upload = true  ;
        nas    = target;
        mfiles = files ;
        this.subFolderName = subFolderName;
    }

    public static SmbFile buildSmbFile(NasConfig.NasInfo target, String path) throws IOException {
        return new SmbFile(path, new NtlmPasswordAuthentication(null, target.getUserName(), target.getPassword()));
    }

    public void copy(NasFile target) throws IOException {
        if (!upload)
            file.copyTo(target.getSmbFile());
        else if (mfile != null) {
            try (SmbFileOutputStream stream = new SmbFileOutputStream(file)) {
                stream.write(mfile.getBytes());
            }
        }
        else if (mfiles != null) {
            try (ConcurrencyManager concurrency = new ConcurrencyManager("nas-copy")) {
                NasFolder folder = new NasFolder(nas, getSubFolderName());
                folder.createFolder();

                for (Map.Entry<String, MultipartFile> f : mfiles.entrySet()) {
                    log(String.format("Thread initiated for copying file:%s", f.getKey()));
                    ProcessFile process = new ProcessFile(nas, getSubFolderName(), f);
                    concurrency.submit(process);
                }
                concurrency.awaitTermination((message) -> log(message), 10);
            }
        }
    }

    public void writeTo(OutputStream outputStream) throws IOException {
        if (!upload) {
            try (InputStream inputStream = file.getInputStream()) {
                byte[] rawData = IOUtils.toByteArray(inputStream);
                outputStream.write(rawData);
                outputStream.close();
            }
        }
    }

    private void log(String msg) {
        if (integrationLog != null)
            integrationLog.log(msg);
        else
            logger.info(msg);
    }

    public void delete() throws SmbException {
        if (!upload)
            file.delete();
    }

    public class ProcessFile implements ConcurrencyManager.BaseConcurrencyThread {
        private Map.Entry<String, MultipartFile> file;
        private String  name;
        private boolean complete = false;
        private String subFolderName = "";
        private NasConfig.NasInfo target;

        public boolean getIsComplete() { return complete; }
        public String  getThreadName() { return name    ; }

        public ProcessFile(NasConfig.NasInfo target, String subFolderName, Map.Entry<String, MultipartFile> file) {
            this.target = target;
            this.file   = file;
            this.subFolderName = subFolderName;
            name = String.format("NasProcessFile-%s", file.getKey());
        }

        @Override
        public void run() {
            try {
                String  fileName = StringUtils.cleanPath(file.getValue().getOriginalFilename());
                String  path     = FileHelper.combinePaths("/", target.getPath(), subFolderName, fileName);
                SmbFile smbFile  = NasFile.buildSmbFile(target, path);
                try (SmbFileOutputStream stream = new SmbFileOutputStream(smbFile)) {
                    stream.write(file.getValue().getBytes());
                }
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
            complete = true;
        }
    }
}
