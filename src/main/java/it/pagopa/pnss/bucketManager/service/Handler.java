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
import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pnss.bucketManager.exception.EventNameNotFoundException;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.impl.DocumentClientCallImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
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


public class Handler implements RequestHandler<S3Event, String> {



    DocumentClientCallImpl client = new DocumentClientCallImpl();

    @Override
    public String handleRequest(S3Event s3Event, Context context) {
        String result = "";
        try {
            for(S3EventNotificationRecord s3record : s3Event.getRecords()){
                String eventName = s3record.getEventName();
                S3EventNotification.S3Entity eventEntity = s3record.getS3();
                if (eventName.equals("ObjectCreated:*") || eventName.equals("ObjectRestore:Completed")) {
                    result = availableObject(eventEntity);
                } else if (eventName.equals("LifecycleTransition") || eventName.equals("ObjectRestore:Delete")) {
                    result = freezedObject(eventEntity);
                } else if (eventName.equals("LifecycleExpiration:Delete") || eventName.equals("ObjectRemoved:Delete")) {
                    result = deletedObject(eventEntity);
                }
            }

        } catch (Exception e) {

            throw new EventNameNotFoundException();

        }
        return result;
    }



    public String availableObject(S3EventNotification.S3Entity eventEntity){
        Document document = new Document();
        document.setDocumentKey(eventEntity.getObject().getKey());
        document.setDocumentState(Document.DocumentStateEnum.AVAILABLE);
        ResponseEntity<Document> response = client.patchdocument(eventEntity.getObject().getKey(), document);
        return String.valueOf(response.getStatusCodeValue());
    }

    public String freezedObject(S3EventNotification.S3Entity eventEntity) {
        Document document = new Document();
        document.setDocumentKey(eventEntity.getObject().getKey());
        document.setDocumentState(Document.DocumentStateEnum.FREEZED);
        ResponseEntity<Document> response = client.patchdocument(eventEntity.getObject().getKey(), document);
        return String.valueOf(response.getStatusCodeValue());
    }

    private String deletedObject(S3EventNotification.S3Entity eventEntity) {
        Document document = new Document();
        document.setDocumentKey(eventEntity.getObject().getKey());
        document.setDocumentState(Document.DocumentStateEnum.DELETED);
        ResponseEntity<Document> response = client.patchdocument(eventEntity.getObject().getKey(), document);
        return String.valueOf(response.getStatusCodeValue());
    }

}