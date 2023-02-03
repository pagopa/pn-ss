package it.pagopa.pnss.bucketManager.service;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@Slf4j
  class HandlerTest {

    public S3Event event = new S3Event();
    @Autowired
    private Handler handler;

    Context context = new Context() {
        @Override
        public String getAwsRequestId() {
            return null;
        }

        @Override
        public String getLogGroupName() {
            return null;
        }

        @Override
        public String getLogStreamName() {
            return null;
        }

        @Override
        public String getFunctionName() {
            return null;
        }

        @Override
        public String getFunctionVersion() {
            return null;
        }

        @Override
        public String getInvokedFunctionArn() {
            return null;
        }

        @Override
        public CognitoIdentity getIdentity() {
            return null;
        }

        @Override
        public ClientContext getClientContext() {
            return null;
        }

        @Override
        public int getRemainingTimeInMillis() {
            return 0;
        }

        @Override
        public int getMemoryLimitInMB() {
            return 0;
        }

        @Override
        public LambdaLogger getLogger() {
            return null;
        }
    };

    String eventName = "ObjectCreated:*";
    S3EventNotification.S3EventNotificationRecord recordCreate = new S3EventNotification.S3EventNotificationRecord(
            "eu-central-1",
            eventName,
            "aws:s3",
            "2023-01-08T00:30:12.456Z",
            "2.1",
            new S3EventNotification.RequestParametersEntity("174.255.255.156"),
            new S3EventNotification.ResponseElementsEntity("nBbLJPAHhdvxmplPvtCgTrWCqf/KtonyV93l9rcoMLeIWJxpS9x9P8u01+Tj0OdbAoGs+VGvEvWl/Sg1NW5uEsVO25Laq7L", "AF2D7AB6002E898D"),
            new S3EventNotification.S3Entity("682bbb7a-xmpl-48ca-94b1-7f77c4d6dbf0",
                    new S3EventNotification.S3BucketEntity("Cold",
                            new S3EventNotification.UserIdentityEntity("A3XMPLFAF2AI3E"),
                            "arn:aws:s3:::" + "Cold"),
                    new S3EventNotification.S3ObjectEntity("keyTest",
                            new Long(21476),
                            "d132690b6c65b6d1629721dcfb49b883",
                            "",
                            "005E64A65DF093B26D"),
                    "1.0"),
            new S3EventNotification.UserIdentityEntity("AWS:AIDAINPONIXMPLT3IKHL2")
    );
    public List<S3EventNotification.S3EventNotificationRecord> s3Records = new ArrayList<>();
    public S3EventNotification s3notification = new S3EventNotification(s3Records);
    public HandlerTest() throws IOException {
    }

    @Test
     void handlerTest() {
        String result = "";
        this.event.getRecords().add(0, recordCreate);

        result = handler.handleRequest(event, context);

        /*if(this.event.getRecords().get(0).getEventName().equals("ObjectCreated:*")){
            result = createdObjectTestOK(this.event.getRecords().get(0).getS3());
        }
        if (this.event.getRecords().get(1).getEventName().equals("ObjectRemoved:*")){
            result = removedObjectTestKO(this.event.getRecords().get(1).getS3());
        }
        if (this.event.getRecords().get(2).getEventName().equals("ObjectRemoved:Delete")){
            result = removedObjectTestOK(this.event.getRecords().get(2).getS3());
        }*/
        Assertions.assertNotEquals("", result);
    }

    public Boolean createdObjectTestOK(S3EventNotification.S3Entity entity) {
        boolean result = false;

        Assertions.assertTrue(result);
        return result;
    }


    public Boolean removedObjectTestKO(S3EventNotification.S3Entity entity){
        boolean result = false;
        if(entity.getObject().getKey().equals("keyTest")){
            result = true;
        }
        Assertions.assertTrue(result);
        return result;
    }


    public Boolean removedObjectTestOK(S3EventNotification.S3Entity entity){
        boolean result = false;
        if(entity.getObject().getKey().equals("keyTest")){
            result = true;
        }
        Assertions.assertTrue(result);
        return result;
    }


}