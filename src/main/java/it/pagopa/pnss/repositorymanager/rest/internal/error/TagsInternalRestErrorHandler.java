package it.pagopa.pnss.repositorymanager.rest.internal.error;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.Error;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsRelationsResponse;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsResponse;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.exception.MissingTagException;
import it.pagopa.pnss.common.exception.IndexingLimitException;
import it.pagopa.pnss.repositorymanager.exception.ItemDoesNotExist;
import it.pagopa.pnss.repositorymanager.rest.internal.TagsInternalApiController;
import lombok.CustomLog;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ControllerAdvice(basePackages = "it.pagopa.pnss.repositorymanager.rest.internal", basePackageClasses = TagsInternalApiController.class)
@CustomLog
public class TagsInternalRestErrorHandler {

    @ExceptionHandler(ItemDoesNotExist.class)
    public final ResponseEntity<TagsRelationsResponse> handleItemDoesNotExistException(ItemDoesNotExist exception) {
        var response = new TagsRelationsResponse();
        Error error = new Error();
        error.setCode("404");
        error.setDescription(exception.getMessage());
        response.setError(error);
        return new ResponseEntity<>(response, NOT_FOUND);
    }

    @ExceptionHandler(IndexingLimitException.class)
    public final ResponseEntity<TagsResponse> handleIndexingLimit(IndexingLimitException exception) {
        var response = new TagsResponse();
        Error error = new Error();
        error.setCode("400");
        error.setDescription(exception.getMessage());
        response.setError(error);
        return new ResponseEntity<>(response, BAD_REQUEST);
    }

    @ExceptionHandler(DocumentKeyNotPresentException.class)
    public final ResponseEntity<TagsResponse> handleDocumentKeyNotPresent(DocumentKeyNotPresentException exception) {
        var response = new TagsResponse();
        Error error = new Error();
        error.setCode("404");
        error.setDescription(exception.getMessage());
        response.setError(error);
        return new ResponseEntity<>(response, NOT_FOUND);
    }

    @ExceptionHandler(MissingTagException.class)
    public final ResponseEntity<TagsResponse> handleMissingTagException(MissingTagException exception) {
        var response = new TagsResponse();
        Error error = new Error();
        error.setCode("400");
        error.setDescription(exception.getMessage());
        response.setError(error);
        return new ResponseEntity<>(response, BAD_REQUEST);
    }

}
