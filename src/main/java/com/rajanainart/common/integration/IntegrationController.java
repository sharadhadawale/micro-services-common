package com.rajanainart.common.integration;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rajanainart.common.helper.MiscHelper;
import com.rajanainart.common.config.AppContext;
import com.rajanainart.common.rest.BaseRestController;
import com.rajanainart.common.rest.RestMessageEntity;
import com.rajanainart.common.rest.RestQueryConfig;
import com.rajanainart.common.rest.RestQueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.rajanainart.common.data.BaseEntity;
import com.rajanainart.common.data.Database;
import com.rajanainart.common.integration.task.BaseJavaIntegrationTask;
import com.rajanainart.common.integration.task.IntegrationTask;
import com.rajanainart.common.integration.task.JavaIntegrationTask;
import org.springframework.web.util.HtmlUtils;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/integration")
public class IntegrationController extends BaseRestController {
    private static final Logger logger = LoggerFactory.getLogger(IntegrationController.class);

    public void startAutoIntegrations() {
        String env = System.getenv("INSTANCE_INDEX");
        int    idx = MiscHelper.fixFortifyLogForging(env);
        String log = String.format("Container Index:%s", idx);
        logger.info(log);
        if (idx == 0) {
            logger.info("Start auto-configured integrations");
            int count = 0;
            try (Database db = new Database()) {
                for (String key : IntegrationManager.INTEGRATION_CONFIGS.keySet()) {
                    if (!IntegrationManager.INTEGRATION_CONFIGS.get(key).getActive() ||
                        !IntegrationManager.INTEGRATION_CONFIGS.get(key).getAutoStart())
                        continue;

                    //log = String.format("Starting integration %s", IntegrationManager.INTEGRATION_CONFIGS.get(key).getId());
                    //logger.info(log);
                    Long processId = db.selectScalar("SELECT process_id AS status FROM CMN_INTEGRATION_PROCESS WHERE status = 0 AND config_name = ?p_name1",
                                            db.new Parameter("p_name1", key));
                    if (processId != null) {
                        db.executeQueryWithJdbc("INSERT INTO CMN_INTEGRATION_PROCESS_LOG (process_id, log, as_on) VALUES (?p_id, 'Forced restart', CURRENT_TIMESTAMP)",
                                db.new Parameter("p_id"  , processId));
                        db.executeQueryWithJdbc("UPDATE CMN_INTEGRATION_PROCESS SET status = 1 WHERE process_id = ?p_id AND config_name = ?p_name",
                                db.new Parameter("p_id"  , processId),
                                db.new Parameter("p_name", key));
                        db.commit();
                    }
                    executeRestQuery(key, new RestQueryRequest());
                    count++;
                }
            }
            log = String.format("Successfully started %s auto-configured integrations", count);
            logger.info(log);
        }
    }

    @RequestMapping(value = "/{name:[a-zA-Z0-9]*}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<BaseEntity>> executeRestQuery(@PathVariable("name") String name,
                                                             @RequestBody RestQueryRequest body) {
        String            escaped = HtmlUtils.htmlEscape(name);
        IntegrationConfig config  = IntegrationManager.INTEGRATION_CONFIGS.getOrDefault(escaped, null);
        HttpHeaders headers       = buildHttpHeaders(RestQueryConfig.RestQueryContentType.JSON.toString());
        if (config == null)
            return new ResponseEntity<>(
                    RestMessageEntity.getInstanceList("", String.format("Invalid Request. No Integration config is available '%s'", escaped)),
                    headers, HttpStatus.BAD_REQUEST);
        if (!config.getActive())
            return new ResponseEntity<>(
                    RestMessageEntity.getInstanceList("", String.format("Integration config '%s' is disabled", escaped)),
                    headers, HttpStatus.BAD_REQUEST);

        awaitForTasksCompletion();

        ResponseEntity<List<BaseEntity>> result = validate(escaped, headers);
        if (result != null) return result;

        IntegrationManager manager = new IntegrationManager(null, escaped, body);
        Thread             thread  = new Thread(manager);
        thread.setDaemon(true);
        thread.start();

        long id = manager.getLogger().getProcessId();
        return new ResponseEntity<>(
                RestMessageEntity.getInstanceList(String.valueOf(id),
                        String.format("Integration process started, please use /integration/status/%s for getting the current status", escaped)),
                headers, HttpStatus.OK);
    }

    @RequestMapping(value = "/upload/{name:[a-zA-Z0-9]*}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<List<BaseEntity>> executeRestQueryUpload(@PathVariable("name") String name,
                                                                   HttpServletRequest request) {
        String            escaped = HtmlUtils.htmlEscape(name);
        IntegrationConfig config  = IntegrationManager.INTEGRATION_CONFIGS.getOrDefault(escaped, null);
        HttpHeaders headers       = buildHttpHeaders(RestQueryConfig.RestQueryContentType.JSON.toString());
        if (config == null)
            return new ResponseEntity<>(
                    RestMessageEntity.getInstanceList("", String.format("Invalid Request. No Integration config is available '%s'", escaped)),
                    headers, HttpStatus.BAD_REQUEST);
        if (!config.getActive())
            return new ResponseEntity<>(
                    RestMessageEntity.getInstanceList("", String.format("Integration config '%s' is disabled", escaped)),
                    headers, HttpStatus.BAD_REQUEST);

        awaitForTasksCompletion();

        ResponseEntity<List<BaseEntity>> result = validate(escaped, headers);
        if (result != null) return result;

        IntegrationManager manager = new IntegrationManager(request, escaped, new RestQueryRequest());
        manager.run();
        //Thread             thread  = new Thread(manager);
        //thread.setDaemon(true);
        //thread.start();

        long id = manager.getLogger().getProcessId();
        return new ResponseEntity<>(
                RestMessageEntity.getInstanceList(String.valueOf(id),
                        String.format("Integration process started, please use /integration/status/%s for getting the current status", escaped)),
                headers, HttpStatus.OK);
    }

    private void awaitForTasksCompletion() {
        try {
            ThreadPoolTaskExecutor executor = AppContext.getApplicationContext().getBean(ThreadPoolTaskExecutor.class);
            executor.setWaitForTasksToCompleteOnShutdown(true);
        }
        catch(Exception ex) {
            if (ex.getClass() != NoSuchBeanDefinitionException.class) {
                logger.info("Exception while setting setWaitForTasksToCompleteOnShutdown");
                ex.printStackTrace();
            }
        }
    }

    private ResponseEntity<List<BaseEntity>> validate(String name, HttpHeaders headers) {
        List<Integer> processes = new ArrayList<>();
        try (Database db = new Database()) {
            db.selectWithCallback("SELECT NVL(MAX(process_id),0) AS id FROM CMN_INTEGRATION_PROCESS WHERE config_name = ?p_name AND status = 0",
                    (record, index) -> {
                        try {
                            processes.add(record.getInt("id"));
                        }
                        catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }, db.new Parameter("p_name", name));
        }
        if (processes.get(0) > 0)
            return new ResponseEntity<>(
                    RestMessageEntity.getInstanceList("", String.format("Integration process %s is already running, process id: %s", name, processes.get(0))),
                    headers, HttpStatus.INTERNAL_SERVER_ERROR);
        return null;
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<List<IntegrationProcessMessage>> listProcesses() {
        HttpHeaders headers = buildHttpHeaders(RestQueryConfig.RestQueryContentType.JSON.toString());
        List<IntegrationProcessMessage> msg = new ArrayList<>();
        try (Database db = new Database()) {
            StringBuilder query = new StringBuilder();
            query.append("WITH processes AS (\r\n")
                 .append("SELECT p.config_name, MAX(p.process_id) AS process_id,\r\n")
                 .append("TO_CHAR(MAX(l.as_on), 'DD-MON-YYYY HH24:MI:SS') AS as_on\r\n")
                 .append("FROM CMN_INTEGRATION_PROCESS p \r\n")
                 .append("INNER JOIN CMN_INTEGRATION_PROCESS_LOG l ON l.process_id = p.process_id\r\n")
                 .append("GROUP BY p.config_name\r\n")
                 .append(")\r\n")
                 .append("SELECT p.process_id, MAX(p.config_name) AS config_name, NULL AS config_desc, \r\n")
                 .append("MAX(p.status) AS status, p1.as_on\r\n")
                 .append("FROM CMN_INTEGRATION_PROCESS p\r\n")
                 .append("INNER JOIN processes p1 ON p1.process_id = p.process_id\r\n")
                 .append("GROUP BY p.process_id");
            db.selectWithCallback(query.toString(),
                (record, index) -> msg.add(new IntegrationProcessMessage(record)));
        }
        for (String key : IntegrationManager.INTEGRATION_CONFIGS.keySet()) {
            if (!IntegrationManager.INTEGRATION_CONFIGS.get(key).getActive()) continue;

            boolean found = false;
            for (IntegrationProcessMessage m : msg) {
                if (key.equalsIgnoreCase(m.getName())) {
                    m.setDesc(IntegrationManager.INTEGRATION_CONFIGS.get(key).getName());
                    found = true;
                    break;
                }
            }
            if (!found)
                msg.add(new IntegrationProcessMessage(key, IntegrationManager.INTEGRATION_CONFIGS.get(key).getName()));
        }
        msg.removeIf(m -> m.getDesc() == null || m.getDesc().isEmpty());

        for (IntegrationProcessMessage m : msg) {
            for (IntegrationConfig.TaskConfig task : IntegrationManager.INTEGRATION_CONFIGS.get(m.getName()).getTasks()) {
                if (task.getType() == IntegrationConfig.TaskType.IMPORT || task.getType() == IntegrationConfig.TaskType.PROCEDURE) {
                    RestQueryConfig config = BaseRestController.REST_QUERY_CONFIGS.getOrDefault(task.getExecValue(), null);
                    if (config != null) {
                        for (RestQueryConfig.ParameterConfig p : config.getParameters())
                            m.getParams().put(p.getId(), p.getName());
                    }
                }
                else if (task.getType() == IntegrationConfig.TaskType.JAVA) {
                    BaseJavaIntegrationTask t = JavaIntegrationTask.JAVA_INTEGRATION_TASKS.getOrDefault(task.getExecValue(), null);
                    if (t != null) {
                        for (String key : t.getParams().keySet())
                            m.getParams().put(key, t.getParams().get(key));
                    }
                }
            }
        }
        return new ResponseEntity<>(msg, headers, HttpStatus.OK);
    }

    @RequestMapping(value = "/status/{name:[a-zA-Z0-9]*}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IntegrationProcessMessage> executeStatusQuery(@PathVariable("name") String name,
                                                                        @RequestBody RestQueryRequest body) {
        List<IntegrationProcessMessage> msg = new ArrayList<>();
        String id = body.getParams().getOrDefault("id", "");
        if (id.isEmpty())
            return null;

        try (Database db = new Database()) {
            StringBuilder builder = new StringBuilder();
            builder.append("SELECT MAX(p.process_id) AS process_id, MAX(p.config_name) AS config_name,\r\n")
                    .append("MAX(p.status) AS status, NULL AS config_desc, TO_CHAR(MAX(l.as_on), 'DD-MON-YYYY HH24:MI:SS') AS as_on\r\n")
                    .append("FROM CMN_INTEGRATION_PROCESS p\r\n")
                    .append("INNER JOIN CMN_INTEGRATION_PROCESS_LOG l ON l.process_id = p.process_id\r\n")
                    .append("WHERE p.process_id = ?p_id");
            db.selectWithCallback(builder.toString(),
                (record, index) -> msg.add(new IntegrationProcessMessage(record)),
              db.new Parameter("p_id", id));

            builder.delete(0, builder.length());
            builder.append("SELECT l.log, TO_CHAR(l.as_on, 'DD-MON-YYYY HH24:MI:SS') AS as_on\r\n")
                   .append("FROM CMN_INTEGRATION_PROCESS_LOG l\r\n")
                   .append("WHERE l.process_id = ?p_id\r\n")
                   .append("ORDER BY l.as_on");

            if (msg.size() > 0)
                db.selectWithCallback(builder.toString(),
                        (record, index) -> msg.get(0).getLogs().add(new IntegrationProcessLog(record)),
                    db.new Parameter("p_id", id));
        }
        HttpHeaders               headers = buildHttpHeaders(RestQueryConfig.RestQueryContentType.JSON.toString());
        IntegrationProcessMessage result  = msg.size() > 0 ? msg.get(0) : null;
        IntegrationConfig         config  = result != null ? IntegrationManager.INTEGRATION_CONFIGS.getOrDefault(result.getName(), null) : null;
        if (result != null && config != null) {
            result.setDesc(config.getName());
            List<IntegrationProcessMessage> validated = validateForFortify(msg);
            if (validated.size() > 0)
                return new ResponseEntity<>(validated.get(0), headers, HttpStatus.OK);
        }
        return new ResponseEntity<>(null, headers, HttpStatus.OK);
    }

    private List<IntegrationProcessMessage> validateForFortify(List<IntegrationProcessMessage> input) {
        List<IntegrationProcessMessage> output = new ArrayList<>();
        for (IntegrationProcessMessage m : input) {
            if (m.getId() != -1 && MiscHelper.isValidName(m.getName()))
                output.add(m);
        }
        return output;
    }

    public class IntegrationProcessLog {
        private String log, asOn;

        public String getLog () { return log ; }
        public String getAsOn() { return asOn; }

        public IntegrationProcessLog(ResultSet record) {
            try {
                this.log  = record.getString("log");
                this.asOn = record.getString("as_on");
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public class IntegrationProcessMessage {
        private long   id;
        private String name = "";
        private String desc = "";
        private int    status;
        private String asOn = "";
        private Map<String, String>         params;
        private List<IntegrationProcessLog> logs  ;

        public long   getId  () { return id  ; }
        public String getName() { return name; }
        public String getDesc() { return desc; }
        public String getAsOn() { return asOn; }

        public Map<String, String>         getParams() { return params; }
        public List<IntegrationProcessLog> getLogs  () { return logs  ; }

        public IntegrationTask.Status getStatus() {
            if (id == 0)
                return IntegrationTask.Status.NO_RUN;
            else
                return IntegrationTask.Status.values()[status];
        }

        public void setDesc(String name) { this.desc = name; }

        public IntegrationProcessMessage(String name, String desc) {
            this.name = name;
            this.desc = desc;
            logs      = new ArrayList<>();
            params    = new HashMap<>();
        }

        public IntegrationProcessMessage(ResultSet record) {
            logs   = new ArrayList<>();
            params = new HashMap<>();
            try {
                id     = record.getLong  ("process_id" );
                name   = record.getString("config_name");
                desc   = record.getString("config_desc");
                status = record.getInt   ("status");
                asOn   = record.getString("as_on" );
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
