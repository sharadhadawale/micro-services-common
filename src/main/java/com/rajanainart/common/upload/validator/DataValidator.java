package com.rajanainart.common.upload.validator;

import com.rajanainart.common.upload.UploadContext;

import java.util.List;

public interface DataValidator {
    String validate(UploadContext context, String name, Object value, List<String> paramters);
}
