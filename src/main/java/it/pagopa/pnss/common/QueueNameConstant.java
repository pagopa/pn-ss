package it.pagopa.pnss.common;

import java.util.List;

public class QueueNameConstant {

    public static final String SIGN_QUEUE_NAME = "PnSsQueueNameStagingBucket";
    public static final  int MAXIMUM_LISTENING_TIME = 25;
    public static final List<String> ALL_QUEUE_NAME_LIST = List.of(SIGN_QUEUE_NAME);
}
