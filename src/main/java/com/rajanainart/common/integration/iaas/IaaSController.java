package com.rajanainart.common.integration.iaas;

import com.rajanainart.common.data.BaseEntity;
import com.rajanainart.common.integration.IntegrationManager;
import com.rajanainart.common.rest.BaseRestController;
import com.rajanainart.common.rest.RestMessageEntity;
import com.rajanainart.common.rest.RestQueryConfig;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@RequestMapping("/integration/iaas")
public class IaaSController extends BaseRestController {
    @RequestMapping(value = "/{name:[a-zA-Z0-9]*}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<BaseEntity>> executeAdhocRestQuery(@PathVariable("name") String name,
                                                                  @RequestBody IaaSRequest body,
                                                                  HttpServletRequest request) {
        String      escaped = HtmlUtils.htmlEscape(name);
        HttpHeaders headers = buildHttpHeaders(RestQueryConfig.RestQueryContentType.JSON.toString());

        String msg = body.validate();
        if (!msg.equalsIgnoreCase(BaseRestController.SUCCESS))
            return new ResponseEntity<>(RestMessageEntity.getInstanceList("", msg), headers, HttpStatus.OK);

        IntegrationManager manager = new IntegrationManager(request, body);
        Thread             thread  = new Thread(manager);
        thread.setDaemon(true);
        thread.start();

        long id = manager.getLogger().getProcessId();
        return new ResponseEntity<>(
                RestMessageEntity.getInstanceList(String.valueOf(id),
                        String.format("Integration process started, please use /integration/status/%s for getting the current status", escaped)),
                headers, HttpStatus.OK);
    }
}
