package com.rajanainart.nas;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.fileinformation.FileAllInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.io.InputStreamByteChunkProvider;
import com.hierynomus.smbj.share.File;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.EnumSet;

public final class NasFile extends BaseNas {
    public  static final String HTTP_SUB_FOLDER_NAME_KEY        = "subFolderName";
    public  static final String HTTP_TARGET_SUB_FOLDER_NAME_KEY = "targetSubFolderName";
    private static final Logger logger = LoggerFactory.getLogger(NasFile.class);

    private MultipartFile mfile ;
    private InputStream   stream;
    private boolean upload = false;

    public NasFile(NasSession sourceSession, String subFolderName, String fileName) {
        super(sourceSession, subFolderName, fileName);
    }

    public NasFile(NasSession targetSession, String subFolderName, MultipartFile file) {
        super(targetSession, subFolderName, file.getOriginalFilename());

        upload = true;
        mfile  = file;
    }

    public NasFile(NasSession targetSession, String subFolderName, String fileName, InputStream stream) {
        super(targetSession, subFolderName, fileName);

        upload      = true;
        this.stream = stream;
    }

    public ByteArrayInputStream readInputStream() throws IOException {
        ByteArrayInputStream byteStream;
        try (File file = getSession().getDiskShare().openFile(getFullPath(), EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null)) {
            byteStream = new ByteArrayInputStream(IOUtils.toByteArray(file.getInputStream()));
        }
        return byteStream;
    }

    public byte[] readBytes() throws IOException {
        byte[] bytes;
        try (File file = getSession().getDiskShare().openFile(getFullPath(), EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null)) {
            bytes = IOUtils.toByteArray(file.getInputStream());
        }
        return bytes;
    }

    public void copy(NasFile target) throws IOException {
        if (!upload) {
            try (File file = target.getSession().getDiskShare().openFile(target.getFullPath(), EnumSet.of(AccessMask.GENERIC_WRITE), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OVERWRITE_IF, null)) {
                file.write(new InputStreamByteChunkProvider(readInputStream()));
            }
        }
        else if (upload && mfile != null) {
            try (File file = getSession().getDiskShare().openFile(target.getFullPath(), EnumSet.of(AccessMask.GENERIC_WRITE), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OVERWRITE_IF, null)) {
                file.write(new InputStreamByteChunkProvider(mfile.getInputStream()));
            }
        }
        else if (upload && stream != null) {
            try (File file = getSession().getDiskShare().openFile(target.getFullPath(), EnumSet.of(AccessMask.GENERIC_WRITE), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OVERWRITE_IF, null)) {
                file.write(new InputStreamByteChunkProvider(stream));
            }
        }
    }

    public void writeTo(OutputStream outputStream) throws IOException {
        if (!upload) {
            outputStream.write(readBytes());
            outputStream.close();
        }
    }

    public void delete() {
        if (!upload) getSession().getDiskShare().rm(getFullPath());
    }

    public static String getFileName(FileAllInformation info) {
        String[] splits = info.getNameInformation().split("\\\\");
        return splits.length > 0 ? splits[splits.length-1] : "";
    }
}
