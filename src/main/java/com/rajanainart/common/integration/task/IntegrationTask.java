package com.rajanainart.common.integration.task;

import com.rajanainart.common.integration.IntegrationContext;

import java.util.Map;

public interface IntegrationTask {
    enum Status {
        PROCESSING(0),
        SUCCESS_COMPLETE(1),
        FAILURE_COMPLETE(2),
        WARNING_COMPLETE(3),
        NO_RUN(4);

        private final int value;
        private Status(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    void   setup  (IntegrationContext context );
    Status process(DelegateTransform transform);
    Status currentStatus();

    @FunctionalInterface
    interface DelegateTransform {
        void process(IntegrationContext context, Map<String, Object> result);
    }
}
