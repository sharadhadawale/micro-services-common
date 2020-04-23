package com.rajanainart.upload.validator;

import com.rajanainart.upload.Upload;
import com.rajanainart.upload.UploadContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("mandatory")
public class MandatoryValidator implements DataValidator {
    public String validate(UploadContext context, String name, Object value, List<String> paramters) {
        if (String.valueOf(value).trim().isEmpty())
            return String.format("Column %s is mandatory", name);
        return Upload.SUCCESS;
    }
}
