package it.pagopa.pnss.common.constant;

import java.util.List;

public final class QueueNameConstant {

    private QueueNameConstant() {
        throw new IllegalStateException("QueueNameConstant is a constant class");
    }

    public static final String SIGN_QUEUE_NAME = "dgs-bing-ss-PnSsQueueStagingBucket-Pja8ntKQxYrs";
    public static final List<String> ALL_QUEUE_NAME_LIST = List.of(SIGN_QUEUE_NAME);

}
