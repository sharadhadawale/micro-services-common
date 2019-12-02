package com.rajanainart.common.upload;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.rajanainart.common.upload.task.UploadTask;
import com.rajanainart.common.upload.validator.DataValidator;
import org.springframework.web.multipart.MultipartFile;

import com.rajanainart.common.config.AppConfig;
import com.rajanainart.common.config.AppContext;
import com.rajanainart.common.data.Database;
import com.rajanainart.common.helper.MiscHelper;

public class Upload {
    public final static String SUCCESS = "OK";

    public final String DEFAULT_UPLOAD_TASK = "default-upload-task";

    public final static Map<String, DataValidator> VALIDATORS    = AppContext.getBeansOfType(DataValidator.class);
    public final static Map<String, UploadTask>    TASKS         = AppContext.getBeansOfType(UploadTask.class   );
    public final static Map<String, UploadConfig> UPLOAD_CONFIGS =
            AppConfig.getBeansFromConfig("/upload-framework/upload-config", "upload-config", "id");

    private MultipartFile mFile ;
    private ExcelDocument excel ;
    private UploadConfig  config;
    private List<ValidationError>     errors ;
    private List<Map<String, Object>> records;
    private Map<String, String>       request;
    private UploadContext             context;
    private String id;

    public ExcelDocument         getExcel () { return excel ; }
    public UploadConfig          getConfig() { return config; }
    public List<ValidationError> getErrors() { return errors; }
    public String              getUploadId() { return id    ; }

    public Upload(Database db, MultipartFile file, String uploadName, Map<String, String> requestParams) throws IOException {
        if (!UPLOAD_CONFIGS.containsKey(uploadName))
            throw new NullPointerException(String.format("Upload config %s does not exist", uploadName));
        mFile  = file;
        config = UPLOAD_CONFIGS.get(uploadName);
        excel  = new ExcelDocument(file);
        errors = new ArrayList<>();
        request= requestParams;
        context= new UploadContext(db, config, request);
    }

    public boolean validate() {
        records     = excel.getAllRecords(config);
        int current = 0;

        if (records.size() <= 1) {
            ValidationError error = new ValidationError();
            error.LineNo          = 1;
            error.ErrorDetails    = new ArrayList<>();
            error.UploadedDetails = new HashMap<>();
            error.ErrorDetails.add("No records found");
            errors.add(error);
            return false;
        }
        Map<String, Object> r = records.get(0);
        for (UploadConfig.ColumnConfig c : config.getUploadColumns()) {
            if (!c.getIsVisible()) continue;
            if (!r.containsKey(c.getId()) || !String.valueOf(r.getOrDefault(c.getId(), "")).equalsIgnoreCase(c.getId()) ) {
                ValidationError error = new ValidationError();
                error.LineNo          = 1;
                error.ErrorDetails    = new ArrayList<>();
                error.UploadedDetails = r;
                error.ErrorDetails.add(String.format("Column %s is missing", c.getId()));
                errors.add(error);
            }
        }
        List<Integer> indexToRemove = new ArrayList<>();
        int index = 0;
        for (Map<String, Object> record : records) {
            if (current++ == 0) continue;
            index++;
            if (MiscHelper.isEmptyMap(record)) { 
            	indexToRemove.add(index);
            	continue;
            }
            for (UploadConfig.ColumnConfig c : config.getUploadColumns()) {
                Object          value = record.get(c.getId());
                List<String>    msgs  = new ArrayList<>();
                ValidationError error = null;
                for (UploadConfig.ValidatorConfig v : c.getValidators()) {
                    DataValidator validator = VALIDATORS.containsKey(v.getType()) ? VALIDATORS.get(v.getType()) : null;
                    if (validator != null) {
                        String result = validator.validate(context, c.getName(), value, v.getParameters());
                        if (!result.equalsIgnoreCase(SUCCESS)) {
                            msgs.add(result);
                            error                 = new ValidationError();
                            error.LineNo          = current;
                            error.ErrorDetails    = msgs;
                            error.UploadedDetails = record;
                        }
                    }
                }
                if (error != null) errors.add(error);
            }
        }
        if(!indexToRemove.isEmpty()) {
        	Collections.sort(indexToRemove,Collections.reverseOrder()); 
        	for(int key : indexToRemove) {
        		records.remove(key);
        	}
        }
        return errors.size() == 0;
    }

    private final String userId         = "USER_ID";
    private final String excelUploadId  = "EXCEL_UPLOAD_ID";
    private final String userIdUploadBy = "USER_ID_UPLOAD_BY";

    private void saveUploadDetails() {
        if (request.getOrDefault(userId, "").trim().isEmpty())
            throw new NullPointerException("Request parameter USER_ID is mandatory");

        StringBuilder query = new StringBuilder();
        String id1 = request.getOrDefault(excelUploadId, "");
        long count = context.getUnderlyingDb().selectScalar("SELECT COUNT(*) FROM CMN_EXCEL_UPLOAD WHERE excel_upload_id = ?p_id",
                                                            context.getUnderlyingDb().new Parameter("p_id", id1));
        if (count == 0 || id1.trim().isEmpty()) {
            query.append("INSERT INTO CMN_EXCEL_UPLOAD (\r\n")
                 .append("file_name, record_count, status, user_id, as_on, user_id_upload_by\r\n")
                 .append(") VALUES (\r\n")
                 .append("?p_name1, ?p_count, 0, ?p_user, CURRENT_TIMESTAMP, ?p_user1\r\n")
                 .append(")");
            context.getUnderlyingDb().executeQueryWithJdbc(query.toString(),
                    context.getUnderlyingDb().new Parameter("p_name1", mFile.getOriginalFilename()),
                    context.getUnderlyingDb().new Parameter("p_count", getUniqueRows()),
                    context.getUnderlyingDb().new Parameter("p_user" , request.get("USER_ID")),
                    context.getUnderlyingDb().new Parameter("p_user1", request.getOrDefault(userIdUploadBy, "")));

            id1 = String.valueOf(context.getUnderlyingDb().selectCurrentSequenceValue(""));
            request.put("EXCEL_UPLOAD_ID", id1);
        }
        else {
            query.append("UPDATE CMN_EXCEL_UPLOAD SET file_name = ?p_name, record_count = ?p_count, status = 0, as_on = CURRENT_TIMESTAMP, user_id_upload_by = ?p_user1 WHERE excel_upload_id = ?p_id");
            context.getUnderlyingDb().executeQueryWithJdbc(query.toString(),
                    context.getUnderlyingDb().new Parameter("p_user1", request.getOrDefault(userIdUploadBy, "")),
                    context.getUnderlyingDb().new Parameter("p_name" , mFile.getOriginalFilename()),
                    context.getUnderlyingDb().new Parameter("p_count", getUniqueRows()),
                    context.getUnderlyingDb().new Parameter("p_id"   , id1));
            query.delete(0, query.length());

            query.append("DELETE FROM CMN_EXCEL_UPLOAD_DETAIL WHERE excel_upload_id = ?p_id");
            context.getUnderlyingDb().executeQueryWithJdbc(query.toString(),
                    context.getUnderlyingDb().new Parameter("p_id", id1));
        }
        for (Map.Entry<String, String> entry : request.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(userId) || entry.getKey().equalsIgnoreCase(excelUploadId) ||
                entry.getKey().equalsIgnoreCase(userIdUploadBy)) continue;

            query.delete(0, query.length());
            query.append("INSERT INTO CMN_EXCEL_UPLOAD_DETAIL (\r\n")
                    .append("excel_upload_id, parameter_name, parameter_value\r\n")
                    .append(") VALUES (")
                    .append("?p_id, ?p_name, ?p_value\r\n")
                    .append(")");
            context.getUnderlyingDb().executeQueryWithJdbc(query.toString(),
                    context.getUnderlyingDb().new Parameter("p_id"   , id1),
                    context.getUnderlyingDb().new Parameter("p_name" , entry.getKey()),
                    context.getUnderlyingDb().new Parameter("p_value", entry.getValue()));
        }
        context.getUnderlyingDb().commit();

        this.id = id1;
    }

	private int getUniqueRows() {
        if (config.getUniqueCols().size() == 0) return records.size()-1;

		Set<Object> uniques = new HashSet<>();
		for(Map<String, Object> child : records) {
		    String unique = "";
		    for (String key : config.getUniqueCols())
                unique = String.format("%s_%s", unique, child.get(key));
		    uniques.add(unique);
        }
		return uniques.size()-1;
	}

	public static long copy(Database db, long sourceExcelUploadId, int quarterId) {
        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO CMN_EXCEL_UPLOAD (\r\n")
             .append("file_name, record_count, status, user_id, as_on, user_id_upload_by\r\n")
             .append(")\r\n")
             .append("SELECT file_name, record_count, 0 AS status, user_id, as_on, user_id_upload_by\r\n")
             .append("FROM CMN_EXCEL_UPLOAD\r\n")
             .append("WHERE excel_upload_id = ?p_id");
        db.executeQueryWithJdbc(query.toString(),
                db.new Parameter("p_id", sourceExcelUploadId));

        long id = db.selectCurrentSequenceValue("CMN_EXCEL_UPLOAD");

        query.delete(0, query.length());
        query.append("INSERT INTO CMN_EXCEL_UPLOAD_DETAIL (\r\n")
             .append("excel_upload_id, parameter_name, parameter_value\r\n")
             .append(")\r\n")
             .append("SELECT ?p_nid, parameter_name, \r\n")
             .append("CASE WHEN parameter_name = 'QUARTER_ID' THEN ?p_qid ELSE parameter_value END AS parameter_value\r\n")
             .append("FROM CMN_EXCEL_UPLOAD_DETAIL\r\n")
             .append("WHERE excel_upload_id = ?p_id");
        db.executeQueryWithJdbc(query.toString(),
                db.new Parameter("p_nid", id),
                db.new Parameter("p_qid", quarterId),
                db.new Parameter("p_id" , sourceExcelUploadId));

        return id;
    }

    public boolean save() {
        String     key  = String.format("%s-upload-task", config.getId());
        UploadTask task = TASKS.containsKey(key) ? TASKS.get(key) : TASKS.get(DEFAULT_UPLOAD_TASK);

        if (records == null) {
            boolean success = validate();
            if (!success) return false;
        }

        task.executePre(context);
        saveUploadDetails();

        int count = 1;
        List<Map<String, Object>> processRecords = new ArrayList<>();
        for (Map<String, Object> record : records) {
            if (count++ == 1) continue;
            Map<String, String> req = MiscHelper.paramsValidated(request);
            for (Map.Entry<String, String> entry : req.entrySet())
                record.put(entry.getKey(), entry.getValue());
            processRecords.add(record);
            if (count % config.getBulkCount() == 0) {
                context.setUploadRecords(processRecords);
                task.executePreRecord (context);
                task.executeRecord    (context);
                task.executePostRecord(context);

                processRecords.clear();
            }
        }
        if (processRecords.size() > 0) {
            context.setUploadRecords(processRecords);
            task.executePreRecord (context);
            task.executeRecord    (context);
            task.executePostRecord(context);
        }
        task.executePost(context);
        context.getUnderlyingDb().commit();

        return true;
    }

    public static class ValidationError {
        public int                 LineNo;
        public List<String>        ErrorDetails;
        public Map<String, Object> UploadedDetails;
    }
}
