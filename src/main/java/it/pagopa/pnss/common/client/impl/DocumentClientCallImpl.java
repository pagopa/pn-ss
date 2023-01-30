package it.pagopa.pnss.common.client.impl;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.repositoryManager.dto.DocumentInput;
import it.pagopa.pnss.repositoryManager.dto.DocumentOutput;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class DocumentClientCallImpl extends CommonBaseClient implements DocumentClientCall {
    private final WebClient.Builder ecInternalWebClient= WebClient.builder();

    @Value("${gestore.repository.anagrafica.docClient}")
    String anagraficaDocumentiClientEndpoint;



    @Override
    public ResponseEntity<DocumentOutput> getdocument(String keyFile) throws IdClientNotFoundException {
        return getWebClient().get()
                .uri(String.format(anagraficaDocumentiClientEndpoint, keyFile))
                .retrieve()
                .bodyToMono(ResponseEntity.class).block();
    }

    @Override
    public ResponseEntity<DocumentOutput> postdocument(DocumentInput documentInput) throws IdClientNotFoundException {
        return getWebClient().post()
                .uri(String.format(anagraficaDocumentiClientEndpoint))
                .bodyValue(documentInput)
                .retrieve()
                .bodyToMono(ResponseEntity.class).block();
    }

    @Override
    public ResponseEntity<DocumentOutput> updatedocument(DocumentInput documentInput) throws IdClientNotFoundException {
        return getWebClient().put()
                .uri(String.format(anagraficaDocumentiClientEndpoint))
                .bodyValue(documentInput)
                .retrieve()
                .bodyToMono(ResponseEntity.class).block();
    }

    @Override
    public ResponseEntity<DocumentOutput> deletedocument(String keyFile) throws IdClientNotFoundException {
        return null;
    }
    public WebClient getWebClient(){
        WebClient.Builder builder = enrichBuilder(ecInternalWebClient);
        return builder.build();
    }

}
