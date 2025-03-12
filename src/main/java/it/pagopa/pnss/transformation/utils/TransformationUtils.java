package it.pagopa.pnss.transformation.utils;

import it.pagopa.pn.library.exceptions.PnSpapiPermanentErrorException;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentResponse;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import lombok.CustomLog;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;
import software.amazon.awssdk.utils.StringUtils;

import java.util.List;
import java.util.function.Predicate;

import static it.pagopa.pnss.configurationproperties.TransformationProperties.TRANSFORMATION_TAG_PREFIX;

@CustomLog
public class TransformationUtils {
    public static final String OBJECT_CREATED_PUT_EVENT = "Object Created";
    public static final String OBJECT_TAGGING_PUT_EVENT = "Object Tags Added";
    public static final String PUT_OBJECT_REASON = "PutObject";
    public static final List<Class<? extends Throwable>> PERMANENT_TRASNFORMATION_EXCEPTIONS = List.of(PnSpapiPermanentErrorException.class);
    public static final Predicate<Throwable> isPermanentException = e -> PERMANENT_TRASNFORMATION_EXCEPTIONS.contains(e.getClass());

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
            DocumentEntity documentEntity = new DocumentEntity();
            documentEntity.setDocumentKey(documentResponse.getDocument().getDocumentKey());
            documentEntity.setDocumentState(documentResponse.getDocument().getDocumentState());
            documentEntity.setContentType(documentResponse.getDocument().getContentType());
            documentEntity.setClientShortCode(documentResponse.getDocument().getClientShortCode());
            documentEntity.setCheckSum(documentResponse.getDocument().getCheckSum());
            return documentEntity;
        });
    }
    public static Tagging buildTransformationTagging(String transformation, String value) {
        return Tagging.builder().tagSet(Tag.builder().key(TRANSFORMATION_TAG_PREFIX + transformation).value(value).build()).build();
    }


}
