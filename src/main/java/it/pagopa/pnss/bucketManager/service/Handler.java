package it.pagopa.pnss.bucketManager.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.S3Client;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Handler value: example.Handler
public class Handler implements RequestHandler<S3Event, String> {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger logger = LoggerFactory.getLogger(Handler.class);

    @Override
    public String handleRequest(S3Event s3Event, Context context) {

        S3EventNotificationRecord record = s3Event.getRecords().get(0);
        String eventName = record.getEventName();
        S3EventNotification.S3Entity eventEntity = record.getS3();
        if(eventName.equals("ObjectCreated:*") || eventName.equals("ObjectRestore:Completed")){
            availableObject(eventEntity);
        } else if (eventName.equals("LifecycleTransition") || eventName.equals("ObjectRestore:Completed")){
            freezedObject(eventEntity);
        }

      return "";
    }

    public String availableObject(S3EventNotification.S3Entity eventEntity){

        return "";
    }

    public void freezedObject(S3EventNotification.S3Entity eventEntity) {
    }


}