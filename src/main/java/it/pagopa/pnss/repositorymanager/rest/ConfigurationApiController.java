package it.pagopa.pnss.repositorymanager.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pn.template.rest.v1.api.CfgApi;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypesConfigurations;
import it.pagopa.pn.template.rest.v1.dto.UserConfiguration;
import it.pagopa.pnss.common.client.exception.DocumentTypeNotPresentException;
import it.pagopa.pnss.common.client.exception.HeaderCheckException;
import it.pagopa.pnss.common.client.exception.HeaderNotFoundException;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.common.client.exception.IdentityCheckFailException;
import it.pagopa.pnss.common.service.HeadersChecker;
import it.pagopa.pnss.repositorymanager.exception.BucketException;
import it.pagopa.pnss.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositorymanager.service.DocumentsConfigsService;
import it.pagopa.pnss.repositorymanager.service.UserConfigurationService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;


@RestController
@Slf4j
public class ConfigurationApiController implements CfgApi {

    private final UserConfigurationService userConfigurationService;
    private final DocumentsConfigsService documentsConfigsService;
    private final ObjectMapper objectMapper;
    
    private final HeadersChecker headersChecker;

    public ConfigurationApiController(UserConfigurationService userConfigurationService, DocumentsConfigsService documentsConfigsService,
                                      ObjectMapper objectMapper, HeadersChecker headersChecker) {
        this.userConfigurationService = userConfigurationService;
        this.documentsConfigsService = documentsConfigsService;
        this.objectMapper = objectMapper;
        this.headersChecker = headersChecker;
    }
    
    private Mono<ResponseEntity<DocumentTypesConfigurations>> getDocumentTypesConfigurationsErrorResponse(Throwable throwable) {
    	log.error("errore",throwable);
    	
        if (throwable instanceof DocumentTypeNotPresentException 
        		|| throwable instanceof IdClientNotFoundException) {
            return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
        } else if (throwable instanceof BucketException
        		|| throwable instanceof HeaderNotFoundException
        		|| throwable instanceof HeaderCheckException) {
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null));
        } else if (throwable instanceof IdentityCheckFailException) {
        	return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(null));
        }
    	log.info("getErrorResponse() : other");
    	log.info("getErrorResponse() : {}", throwable.getClass());
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null));
    }

    private Mono<ResponseEntity<UserConfiguration>> getUserConfigurationErrorResponse(String clientId, Throwable throwable) {
    	log.error("errore",throwable);
    	
        if (throwable instanceof IdClientNotFoundException) {
            return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
        } else if (throwable instanceof RepositoryManagerException 
        		|| throwable instanceof HeaderNotFoundException
        		|| throwable instanceof HeaderCheckException) {
        	return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null));
        } else if (throwable instanceof IdentityCheckFailException) {
        	return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(null));
        }
       	log.info("getErrorResponse() : other");
    	log.info("getErrorResponse() : {}", throwable.getClass());
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null));
    }

    @Override
    public Mono<ResponseEntity<DocumentTypesConfigurations>> getDocumentsConfigs(final ServerWebExchange exchange) {
    	
    	return headersChecker.checkIdentity(exchange)
    			.flatMap(unused -> documentsConfigsService.getDocumentsConfigs())
    			.map(ResponseEntity::ok)
    			.onErrorResume(this::getDocumentTypesConfigurationsErrorResponse);
    }

    /**
     * Ogni microservizio che vuole utilizzare SafeStorage deve essere censito, avere almeno una api-key assegnata,
     * e avere alcune configurazioni:
     * <ul>
     * <li>name: il nome del sistema upstream (client).</li>
     * <li>canCreate: tipi di documento che il client può caricare.</li>
     * <li>canRead: tipi di documento che il client può leggere.</li>
     * <li>canModifyStatus: tipi di documento che il client può modificare.</li>
     * <li>signatureInfo: informazioni necessarie per firmare digitalmente, per conto del client, i file caricati.</li>
     * <li>destination: informazioni necessarie a notificare eventi al client.</li>
     * </ul>
     */
    @Override
    public Mono<ResponseEntity<UserConfiguration>> getCurrentClientConfig(String clientId, final ServerWebExchange exchange) {

        return headersChecker.checkIdentity(exchange)
        		.flatMap(unused -> userConfigurationService.getUserConfiguration(clientId))
        		.map(userConfigurationInternal -> ResponseEntity.ok(objectMapper.convertValue(
                                               userConfigurationInternal,
                                               UserConfiguration.class)))
                .onErrorResume(throwable -> getUserConfigurationErrorResponse(clientId, throwable));
    }

}
