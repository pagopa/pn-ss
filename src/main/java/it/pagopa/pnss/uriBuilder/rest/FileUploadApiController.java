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
import software.amazon.awssdk.services.mq.model.BadRequestException;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class FileUploadApiController implements FileUploadApi {

    @Autowired
    UriBuilderService uriBuilderService;
    List<String> listaTipologieDoc = Arrays.asList("PN_NOTIFICATION_ATTACHMENTS","PN_AAR","PN_LEGAL_FACTS","PN_EXTERNAL_LEGAL_FACTS","PN_DOWNTIME_LEGAL_FACTS");
    List<String> listaTipoDocumenti =  Arrays.asList("PDF","ZIP","TIFF");
    List<String> listaStatus =  Arrays.asList("PRELOADED","ATTACHED");
    @Override
    public Mono<ResponseEntity<FileCreationResponse>> createFile(String xPagopaSafestorageCxId, Mono<FileCreationRequest> fileCreationRequest,  final ServerWebExchange exchange){

        return fileCreationRequest.map(request ->{
            String contentType = request.getContentType();
            String documentType = request.getDocumentType();
            String status = request.getStatus();
            validationField(contentType,documentType,status);
            FileCreationResponse creationResp = uriBuilderService.createUriForUploadFile(contentType, documentType, status);
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
