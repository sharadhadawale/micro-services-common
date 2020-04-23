package com.rajanainart.nas;

import com.rajanainart.data.BaseEntity;
import com.rajanainart.helper.FileHelper;
import com.rajanainart.integration.IntegrationManager;
import com.rajanainart.rest.BaseRestController;
import com.rajanainart.rest.RestMessageEntity;
import com.rajanainart.rest.RestQueryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/nas")
public class NasController extends BaseRestController {
    private static final Logger logger = LoggerFactory.getLogger(NasController.class);

    @RequestMapping(value = "/download/{name:[a-zA-Z0-9]*}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Resource> downloadNasFile(@PathVariable("name") String name,
                                                    @RequestBody Map<String, String> requestParams,
                                                    HttpServletResponse response) {
        String encodedName   = HtmlUtils.htmlEscape(name);
        String subFolderName = HtmlUtils.htmlEscape(requestParams.getOrDefault(NasFile.HTTP_SUB_FOLDER_NAME_KEY, ""));
        String fileName      = HtmlUtils.htmlEscape(requestParams.getOrDefault("fileName", ""));

        if (fileName.isEmpty()) {
            logger.error("No filename provided");
            return null;
        }

        NasConfig config = IntegrationManager.NAS_CONFIGS.getOrDefault(encodedName, null);
        if (config == null) {
            logger.error("NasConfig does not exist");
            return null;
        }

        try {
            String ext = FileHelper.getFileExtension(fileName);
            if (!ext.isEmpty()) {
                String type = HttpContentType.getMimeValueOf(ext.toLowerCase(Locale.ENGLISH));
                String disp = String.format(HttpContentDisposition.getDisposition(), fileName);
                response.setContentType(type);
                response.setHeader("Content-Disposition", disp);
            }
            NasConfig.NasInfo info = !config.getSource().getPath().isEmpty() ? config.getSource() : config.getTarget();
            try (NasSession session = new NasSession(info)) {
                NasFile file = new NasFile(session, subFolderName, fileName);
                file.writeTo(response.getOutputStream());
            }
        }
        catch (Exception ex) {
            logger.error(ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        return null;
    }

    @RequestMapping(value = "/copy/{name:[a-zA-Z0-9]*}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<List<BaseEntity>> copyNasFolder(@PathVariable("name") String name,
                                                          @RequestBody Map<String, String> requestParams) {
        String      encodedName    = HtmlUtils.htmlEscape(name);
        String      subFolderName  = HtmlUtils.htmlEscape(requestParams.getOrDefault(NasFile.HTTP_SUB_FOLDER_NAME_KEY, ""));
        String targetSubFolderName = HtmlUtils.htmlEscape(requestParams.getOrDefault(NasFile.HTTP_TARGET_SUB_FOLDER_NAME_KEY, ""));
        HttpHeaders headers        = buildHttpHeaders(RestQueryConfig.RestQueryContentType.JSON.toString());

        if (subFolderName.isEmpty() || targetSubFolderName.isEmpty())
            return new ResponseEntity<>(
                    RestMessageEntity.getInstanceList("", "Parameters subFolderName & targetSubFolderName are mandatory", RestMessageEntity.MessageStatus.FAILURE),
                    headers, HttpStatus.BAD_REQUEST);

        NasConfig config = IntegrationManager.NAS_CONFIGS.getOrDefault(encodedName, null);
        if (config == null)
            return new ResponseEntity<>(
                    RestMessageEntity.getInstanceList("", "NasConfig does not exist",
                            RestMessageEntity.MessageStatus.FAILURE), headers, HttpStatus.BAD_REQUEST);

        try {
            int count = 0;
            NasConfig.NasInfo info  = !config.getSource().getPath().isEmpty() ? config.getSource() : config.getTarget();
            try (NasSession session = new NasSession(info)) {
                NasFolder   folder  = new NasFolder(session, FileHelper.combinePaths(BaseNas.PATH_SPLITTER, subFolderName, BaseNas.PATH_SPLITTER));
                count = folder.copyAllFilesParallel(session, targetSubFolderName, false);
            }
            return new ResponseEntity<>(
                    RestMessageEntity.getInstanceList("", String.format("%s NAS file(s) have been copied", count),
                            RestMessageEntity.MessageStatus.SUCCESS), headers, HttpStatus.OK);
        }
        catch (Exception ex) {
            logger.error(ex.getLocalizedMessage());
            ex.printStackTrace();
            return new ResponseEntity<>(
                    RestMessageEntity.getInstanceList("", ex.getLocalizedMessage(),
                            RestMessageEntity.MessageStatus.FAILURE), headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/delete/{name:[a-zA-Z0-9]*}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<List<BaseEntity>> deleteNasFiles(@PathVariable("name") String name,
                                                           @RequestBody Map<String, String> requestParams) {
        String      encodedName   = HtmlUtils.htmlEscape(name);
        String      subFolderName = HtmlUtils.htmlEscape(requestParams.getOrDefault(NasFile.HTTP_SUB_FOLDER_NAME_KEY, ""));
        String      fileNames     = HtmlUtils.htmlEscape(requestParams.getOrDefault("fileNames", ""));
        HttpHeaders headers       = buildHttpHeaders(RestQueryConfig.RestQueryContentType.JSON.toString());

        if (fileNames.isEmpty())
            return new ResponseEntity<>(
                    RestMessageEntity.getInstanceList("", "Parameter fileNames is mandatory", RestMessageEntity.MessageStatus.FAILURE),
                    headers, HttpStatus.BAD_REQUEST);

        NasConfig config = IntegrationManager.NAS_CONFIGS.getOrDefault(encodedName, null);
        if (config == null)
            return new ResponseEntity<>(
                    RestMessageEntity.getInstanceList("", "NasConfig does not exist",
                            RestMessageEntity.MessageStatus.FAILURE), headers, HttpStatus.BAD_REQUEST);

        List<BaseEntity> result = new ArrayList<>();
        for (String fileName : fileNames.split(",")) {
            try {
                NasConfig.NasInfo info = !config.getSource().getPath().isEmpty() ? config.getSource() : config.getTarget();
                try (NasSession session = new NasSession(info)) {
                    NasFile file = new NasFile(session, subFolderName, fileName);
                    file.delete();
                }
                BaseEntity msg  = RestMessageEntity.getInstance("", String.format("File %s is deleted", fileName), RestMessageEntity.MessageStatus.SUCCESS);
                result.add(msg);
            }
            catch (Exception ex) {
                logger.error(ex.getLocalizedMessage());
                BaseEntity msg = RestMessageEntity.getInstance("", String.format("File %s: %s", fileName, ex.getLocalizedMessage()), RestMessageEntity.MessageStatus.SUCCESS);
                result.add(msg);
                ex.printStackTrace();
            }
        }
        return new ResponseEntity<>(result, headers, HttpStatus.OK);
    }

    @RequestMapping(value = "/list/{name:[a-zA-Z0-9]*}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<List<String>> listNasFolder(@PathVariable("name") String name,
                                                      @RequestBody Map<String, String> requestParams,
                                                      HttpServletResponse response) {
        String encodedName   = HtmlUtils.htmlEscape(name);
        String subFolderName = HtmlUtils.htmlEscape(requestParams.getOrDefault(NasFile.HTTP_SUB_FOLDER_NAME_KEY, ""));
        HttpHeaders headers  = buildHttpHeaders(RestQueryConfig.RestQueryContentType.JSON.toString());
        List<String> result  = new ArrayList<>();

        NasConfig config = IntegrationManager.NAS_CONFIGS.getOrDefault(encodedName, null);
        if (config == null) {
            String msg = "NasConfig does not exist";
            logger.error(msg);
            result.add(msg);
            return new ResponseEntity<>(result, headers, HttpStatus.BAD_REQUEST);
        }

        try {
            List<String> files;
            NasConfig.NasInfo info  = !config.getSource().getPath().isEmpty() ? config.getSource() : config.getTarget();
            try (NasSession session = new NasSession(info)) {
                NasFolder   folder  = new NasFolder(session, FileHelper.combinePaths(BaseNas.PATH_SPLITTER, subFolderName, BaseNas.PATH_SPLITTER));
                files = folder.getAllFiles();
            }
            return new ResponseEntity<>(files, headers, HttpStatus.OK);
        }
        catch (IOException ex) {
            logger.error(ex.getLocalizedMessage());
            ex.printStackTrace();
            result.add(ex.getLocalizedMessage());
            return new ResponseEntity<>(result, headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
