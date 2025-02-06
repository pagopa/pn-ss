package it.pagopa.pnss.utils;

import java.util.List;

public final class QueueNameConstant {

    private QueueNameConstant() {
        throw new IllegalStateException("QueueNameConstant is a constant class");
    }

    public static final String TRANSFORMATION_QUEUE_NAME = "dgs-bing-ss-PnSsQueueStagingBucket-Pja8ntKQxYrs";
    public static final String AVAILABILITY_QUEUE_NAME = "Pn-Ss-Availability-Queue";
    public static final String SIGN_AND_TIMEMARK_QUEUE_NAME = "pn-ss-transformation-sign-and-timemark-queue";
    public static final String SIGN_QUEUE_NAME = "pn-ss-transformation-sign-queue";
    public static final String DUMMY_QUEUE_NAME = "pn-ss-transformation-dummy-queue";

    public static final List<String> ALL_QUEUE_NAME_LIST = List.of(TRANSFORMATION_QUEUE_NAME, AVAILABILITY_QUEUE_NAME, SIGN_AND_TIMEMARK_QUEUE_NAME, SIGN_QUEUE_NAME, DUMMY_QUEUE_NAME);

}
