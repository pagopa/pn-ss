package it.pagopa.pnss.common;

import java.util.List;

public class QueueNameConstant {

    public static final String SIGN_QUEUE_NAME = "dgs-bing-ss-PnSsQueueStagingBucket-Pja8ntKQxYrs";

    public static final String BUCKET_STAGE_NAME = "PnSsStagingBucketName";
    public static final String BUCKET_HOT_NAME = "PnSsBucketName";

    public static final  int MAXIMUM_LISTENING_TIME = 25;
    public static final List<String> ALL_QUEUE_NAME_LIST = List.of(SIGN_QUEUE_NAME);

    public static final List<String> ALL_BUCKET_NAME_LIST = List.of(BUCKET_STAGE_NAME,BUCKET_HOT_NAME);

}
