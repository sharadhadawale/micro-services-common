package com.rajanainart.upload;

import com.rajanainart.data.Database;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/upload")
public class UploadController {
    public enum UploadStatus { SUCCESS, PARTIAL_SUCCESS, VALIDATION_ERROR, FATAL }

    @RequestMapping(value = "/excel/set-submit", method = RequestMethod.POST)
    @ResponseBody
    public UploadResult handleUpload(@RequestBody Map<String, String> requestParams) {
        UploadResult result    = new UploadResult();
        result.RecordsAffected = 0;
        result.Message         = "";
        result.ValidationErrors= new ArrayList<>();

        if (!requestParams.containsKey("EXCEL_UPLOAD_ID")) {
            result.Message = "Parameter EXCEL_UPLOAD_ID is mandatory";
            result.Result  = UploadStatus.VALIDATION_ERROR;
        }
        else {
            int affected = 0;
            try (Database db = new Database()) {
                affected = db.executeQueryWithJdbc("UPDATE CMN_EXCEL_UPLOAD SET status = 1, as_on = CURRENT_TIMESTAMP, user_id_upload_by = ?p_user1 WHERE excel_upload_id = ?p_id",
                                    db.new Parameter("p_user1", requestParams.getOrDefault("USER_ID_UPLOAD_BY", "")),
                                    db.new Parameter("p_id"   , requestParams.get("EXCEL_UPLOAD_ID")));
                db.commit();
            }
            if (affected != 0) {
                result.Message = "Excel Upload Status has been set as Submitted";
                result.Result = UploadStatus.SUCCESS;
            }
            else {
                result.Message = "No records affected";
                result.Result = UploadStatus.SUCCESS;
            }
        }
        return result;
    }

    @RequestMapping(value = "/excel/{name:[a-zA-Z0-9]*}", method = RequestMethod.POST)
    @ResponseBody
    public UploadResult handleUpload(@RequestParam("file") MultipartFile file,
                                     @PathVariable("name") String name,
                                     @RequestParam Map<String, String> requestParams) {
        UploadResult result    = new UploadResult();
        result.RecordsAffected = 0;
        result.Message         = "";
        result.ValidationErrors= new ArrayList<>();
        try {
            try (Database db = new Database()) {
                Upload upload   = new Upload(db, file, name, requestParams);
                boolean success = upload.validate();
                if (!success) {
                    result.Result           = UploadStatus.VALIDATION_ERROR;
                    result.ValidationErrors = upload.getErrors();
                }
                else {
                    upload.save();
                    result.ExcelUploadId   = upload.getUploadId();
                    result.Result          = UploadStatus.SUCCESS;
                    result.RecordsAffected = upload.getExcel().getSheet().getLastRowNum();
                    result.Message         = "Successfully uploaded";
                }
                upload.getExcel().close();
            }

        }
        catch(Exception ex) {
            result.Result  = UploadStatus.FATAL;
            result.Message = ex.getMessage();
            ex.printStackTrace();
        }
        return result;
    }

    @RequestMapping(value = "/excel/format-download/{name:[a-zA-Z0-9]*}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Resource> handleDownloadFormat(@PathVariable("name") String name, HttpServletResponse response) {
        try {
            String       fileName = "UploadFormat.xlsx";
            UploadConfig config   = Upload.UPLOAD_CONFIGS.getOrDefault(name, null);
            if (config  != null)
                fileName = String.format("\"%s.xlsx\"", config.getName());

            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Content-Disposition", "attachment; filename="+fileName);

            if (config != null)
                Upload.downloadFormat(name, response.getOutputStream());
            else {
                ExcelDocument document = new ExcelDocument(response.getOutputStream(), fileName);
                document.writeString(String.format("Upload config %s does not exist", name), 0, 0);
                document.close();
            }
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @RequestMapping(value = "/excel/error-download/{name:[a-zA-Z0-9]*}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Resource> handleErrorUpload(@RequestParam("file") MultipartFile file,
                                                      @PathVariable("name") String name,
                                                      @RequestParam Map<String, String> requestParams,
                                                      HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Content-Disposition", "attachment; filename=UploadErrors.xlsx");
            try (Database db  = new Database()) {
                Upload upload = new Upload(db, file, name, requestParams);
                upload.validate();
                upload.getExcel().writeErrors(response.getOutputStream(), upload.getConfig(), upload.getErrors());
                upload.getExcel().close();
            }
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public class UploadResult {
        public UploadStatus Result;
        public String       Message;
        public int          RecordsAffected;
        public String       ExcelUploadId;
        public List<Upload.ValidationError> ValidationErrors;
    }
}
