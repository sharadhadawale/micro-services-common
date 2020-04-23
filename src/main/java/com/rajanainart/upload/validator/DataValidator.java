package com.rajanainart.upload.validator;

import com.rajanainart.upload.UploadContext;

import java.util.List;

public interface DataValidator {
    String validate(UploadContext context, String name, Object value, List<String> paramters);
}
