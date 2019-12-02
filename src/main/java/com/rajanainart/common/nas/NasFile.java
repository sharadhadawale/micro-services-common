package com.rajanainart.common.nas;

import com.rajanainart.common.helper.FileHelper;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public final class NasFile {
    private NasConfig.NasInfo nas ;
    private SmbFile           file;
    private MultipartFile    mfile;
    private boolean upload = false;

    public NasConfig.NasInfo getNasInfo() { return nas ; }
    public SmbFile           getSmbFile() { return file; }

    public NasFile(NasConfig.NasInfo source, String path) throws IOException {
        nas  = source;
        file = buildSmbFile(source, path);

        if (file.exists() && !file.isFile())
            throw new IOException(String.format("%s is not a file", source.getPath()));
    }

    public NasFile(NasConfig.NasInfo source, SmbFile file) {
        nas  = source;
        this.file = file;
    }

    public NasFile(NasConfig.NasInfo target, MultipartFile file) throws IOException {
        upload = true  ;
        nas    = target;
        mfile  = file  ;

        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        String path     = FileHelper.combinePaths("/", target.getPath(), fileName);
        this.file       = buildSmbFile(target, path);
    }

    public static SmbFile buildSmbFile(NasConfig.NasInfo target, String path) throws IOException {
        return new SmbFile(path, new NtlmPasswordAuthentication(null, target.getUserName(), target.getPassword()));
    }

    public void copy(NasFile target) throws IOException {
        if (!upload)
            file.copyTo(target.getSmbFile());
        else {
            try (SmbFileOutputStream stream = new SmbFileOutputStream(file)) {
                stream.write(mfile.getBytes());
            }
        }
    }

    public void delete() throws SmbException {
        if (!upload)
            file.delete();
    }
}
