package com.rajanainart.common.helper;

import java.io.File;
import java.util.ArrayList;

import com.rajanainart.common.config.AppContext;
import org.apache.commons.io.FilenameUtils;

public final class FileHelper {
    File file = null;
    String[] endsWithMatching = {};
    ArrayList<String> files = new ArrayList<String>();

    public File getRootDirectory() { return file; }
    public String[] getEndsWithFilters() { return endsWithMatching; }

    public FileHelper(File directory, String ... endsWithMatching) {
        file = directory;
        this.endsWithMatching = endsWithMatching;
    }

    public FileHelper(String directory, String ... endsWithMatching) {
        this(new File(FilenameUtils.normalize(directory)), endsWithMatching);
    }

    public ArrayList<String> getAvailableFiles(boolean includeSubFolder) {
        if (!file.isDirectory()) {
            files.add(file.toString());
            return files;
        }
        for (File f : file.listFiles()) {
            if (f.isDirectory() && includeSubFolder)
                getAvailableFiles(f);
            else if (contains(endsWithMatching, f.toString()))
                files.add(f.getAbsolutePath());
        }
        return files;
    }

    public static String combinePaths(String separator, String ... paths) {
        StringBuilder result = new StringBuilder();
        for (String path : paths)
            result.append(result.length() == 0 || result.toString().endsWith(separator) || path.startsWith(separator) ? path : separator+path);
        return result.toString();
    }

    public static boolean contains(String[] endsWithMatching, String name) {
        if (endsWithMatching.length == 0) return true;

        for (String n : endsWithMatching)
            if (name.endsWith(n))
                return true;
        return false;
    }

    public static String getAppBasePath(String resource) {
        File path = new File(AppContext.class.getClassLoader().getResource(resource).getPath()).getParentFile();
        return path.getAbsolutePath();
    }

    private void getAvailableFiles(File directory) {
        for (File f : directory.listFiles()) {
            if (f.isDirectory())
                getAvailableFiles(f);
            else if (contains(endsWithMatching, f.toString()))
                files.add(f.getAbsolutePath());
        }
    }
}
