package it.pagopa.pnss.uriBuilder.client;


import it.pagopa.pnss.uriBuilder.model.DocumentRepositoryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient (decode404 = true, name = "GetRepositoryClient", url = "http://localhost:8078/")
public interface GetRepositoryClient {

    @GetMapping(value = "/repository-manager/retrieveDocument")
    ResponseEntity <DocumentRepositoryDto> retrieveDocument(
            @RequestParam(name = "idDcoument", required = true) String filekey);


    @GetMapping(value = "/repository-manager/uploadDocument")
    ResponseEntity <Boolean> upLoadDocument(
            @RequestBody( required= true) DocumentRepositoryDto documentRepositoryDto);


}
