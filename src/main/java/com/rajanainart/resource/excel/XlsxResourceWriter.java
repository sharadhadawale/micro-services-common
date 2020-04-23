package com.rajanainart.resource.excel;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("resource-writer-xlsx")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class XlsxResourceWriter extends XlsResourceWriter {
}
