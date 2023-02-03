package it.pagopa.pnss.uriBuilder.rest;

import it.pagopa.pnss.uriBuilder.service.UriBuilderService;
import it.pagopa.pn.template.rest.v1.api.FileUploadApi;
import it.pagopa.pn.template.rest.v1.dto.FileCreationRequest;
import it.pagopa.pn.template.rest.v1.dto.FileCreationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static it.pagopa.pnss.common.Constant.*;

@RestController
@RequiredArgsConstructor
public class FileUploadApiController implements FileUploadApi {




    @Autowired
    UriBuilderService uriBuilderService;



    @Override
    public Mono<ResponseEntity<FileCreationResponse>> createFile(String xPagopaSafestorageCxId, Mono<FileCreationRequest> fileCreationRequest,  final ServerWebExchange exchange){

        return fileCreationRequest.map(request ->{
            String contentType = request.getContentType();
            String documentType = request.getDocumentType();
            String status = request.getStatus();
            validationField(contentType,documentType,status);
            FileCreationResponse creationResp = null;
            try {
                creationResp = uriBuilderService.createUriForUploadFile(xPagopaSafestorageCxId,contentType, documentType, status);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return ResponseEntity.ok().body(creationResp);
        });



    }

    private void validationField(String contentType, String documentType, String status) {

        if(!listaTipoDocumenti.contains(contentType)){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "ContentType :"+contentType+" - Not valid");
        }
        if(!listaTipologieDoc.contains(documentType)){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "DocumentType :"+documentType+" - Not valid");
        }
        if (!status.equals("")){
            if (!listaStatus.contains(status)){
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "status :"+status+" - Not valid ");
            }else{
                if (!(documentType.equals("PN_NOTIFICATION_ATTACHMENTS")&& status.equals("PRELOADED"))){
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "status :"+status+" - Not valid for documentType");
                }
            }
        }

    }

}
