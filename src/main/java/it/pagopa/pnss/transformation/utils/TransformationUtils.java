package it.pagopa.pnss.transformation.utils;

import it.pagopa.pn.library.exceptions.PnSpapiPermanentErrorException;
import it.pagopa.pn.library.exceptions.PnSpapiTemporaryErrorException;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.CurrentStatus;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentResponse;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentType;
import it.pagopa.pnss.repositorymanager.entity.CurrentStatusEntity;
import it.pagopa.pnss.repositorymanager.entity.DocTypeEntity;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import lombok.CustomLog;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;
import software.amazon.awssdk.utils.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static it.pagopa.pnss.configurationproperties.TransformationProperties.TRANSFORMATION_TAG_PREFIX;

@CustomLog
public class TransformationUtils {
    public static final String OBJECT_CREATED_PUT_EVENT = "Object Created";
    public static final String OBJECT_TAGGING_PUT_EVENT = "Object Tags Added";
    public static final String PUT_OBJECT_REASON = "PutObject";
    public static final String TRANSFORMATION_IN_PROGRESS = "inProgress";
    public static final int TRANSFORMATION_MAX_RETRY = 10;
    public static final List<Class<? extends Throwable>> PERMANENT_TRASNFORMATION_EXCEPTIONS = List.of(PnSpapiPermanentErrorException.class);
    public static final Predicate<Throwable> isPermanentException = e -> PERMANENT_TRASNFORMATION_EXCEPTIONS.contains(e.getClass());
    public static final List<Class<? extends Throwable>> TEMPORARY_TRANSFORMATION_EXCEPTIONS = List.of(PnSpapiTemporaryErrorException.class);
    public static final Predicate<Throwable> isPapiTemporaryException = e ->
            ExceptionUtils.getThrowableList(e).stream()
                    .anyMatch(t -> TEMPORARY_TRANSFORMATION_EXCEPTIONS.contains(t.getClass()));

    private TransformationUtils() {
        throw new IllegalStateException("TransformationUtils is a utility class");
    }

    public static boolean isValidEventType(String detailType, String reason) {
        return isObjectTaggingPutEvent(detailType) || isObjectCreatedPutEvent(detailType, reason);
    }

    // Il campo reason non è sempre valorizzato: https://docs.aws.amazon.com/AmazonS3/latest/userguide/ev-events.html
    public static boolean isObjectCreatedPutEvent(String detailType, String reason) {
        return OBJECT_CREATED_PUT_EVENT.equals(detailType) && StringUtils.equals(reason, PUT_OBJECT_REASON);
    }

    public static boolean isObjectTaggingPutEvent(String detailType) {
        return OBJECT_TAGGING_PUT_EVENT.equals(detailType);
    }

    public static Mono<DocumentEntity> mapToDocumentEntity(DocumentResponse documentResponse) {
        return Mono.fromSupplier(() -> {

            DocTypeEntity docTypeEntity = documentTypeToDocEntity(documentResponse.getDocument().getDocumentType());

            DocumentEntity documentEntity = new DocumentEntity();

            documentEntity.setDocumentType(docTypeEntity);
            documentEntity.setDocumentLogicalState(documentResponse.getDocument().getDocumentLogicalState());
            documentEntity.setDocumentKey(documentResponse.getDocument().getDocumentKey());
            documentEntity.setDocumentState(documentResponse.getDocument().getDocumentState());
            documentEntity.setContentType(documentResponse.getDocument().getContentType());
            documentEntity.setClientShortCode(documentResponse.getDocument().getClientShortCode());
            documentEntity.setCheckSum(documentResponse.getDocument().getCheckSum());
            documentEntity.setDocumentType(docTypeEntity);
            documentEntity.setContentLenght(documentResponse.getDocument().getContentLenght());
            documentEntity.setLastStatusChangeTimestamp(documentResponse.getDocument().getLastStatusChangeTimestamp());

            return documentEntity;
        });
    }

   public static DocTypeEntity documentTypeToDocEntity(DocumentType documentType) {
       DocTypeEntity docTypeEntity = new DocTypeEntity();
       Map<String, CurrentStatus> statuses = documentType.getStatuses();
       Map<String, CurrentStatusEntity> statusesEntity = statusDtoMapToEntity(statuses);

       docTypeEntity.setTransformations(documentType.getTransformations());
       docTypeEntity.setChecksum(documentType.getChecksum().getValue());
       docTypeEntity.setStatuses(statusesEntity);
       docTypeEntity.setInformationClassification(documentType.getInformationClassification());
       docTypeEntity.setInitialStatus(documentType.getInitialStatus());
       docTypeEntity.setTimeStamped(documentType.getTimeStamped());
       docTypeEntity.setTipoDocumento(documentType.getTipoDocumento());



       return docTypeEntity;
   }

    public static @NotNull Map<String, CurrentStatusEntity> statusDtoMapToEntity(Map<String, CurrentStatus> statuses) {
        Map<String,CurrentStatusEntity> statusesEntity = new HashMap<>();

        for (Map.Entry<String, CurrentStatus> entry : statuses.entrySet()) {
            statusesEntity.put(entry.getKey(), currentStatusDtoToEntity(entry.getValue()));
        }
        return statusesEntity;
    }

    public static Tagging buildTransformationTagging(String transformation, String value) {
        return Tagging.builder().tagSet(Tag.builder().key(TRANSFORMATION_TAG_PREFIX + transformation).value(value).build()).build();
    }
    

    public static CurrentStatusEntity currentStatusDtoToEntity (CurrentStatus currentStatus) {
        CurrentStatusEntity currentStatusEntity = new CurrentStatusEntity();
        currentStatusEntity.setAllowedStatusTransitions(currentStatus.getAllowedStatusTransitions());
        currentStatusEntity.setStorage(currentStatus.getStorage());
        currentStatusEntity.setTechnicalState(currentStatus.getTechnicalState());
        return currentStatusEntity;
    }


}
