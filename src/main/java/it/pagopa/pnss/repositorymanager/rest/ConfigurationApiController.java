package it.pagopa.pnss.repositorymanager.rest;

import it.pagopa.pnss.common.constant.Constant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pn.template.rest.v1.api.CfgApi;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypesConfigurations;
import it.pagopa.pn.template.rest.v1.dto.UserConfiguration;
import it.pagopa.pnss.common.client.exception.DocumentTypeNotPresentException;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
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

    public ConfigurationApiController(UserConfigurationService userConfigurationService, DocumentsConfigsService documentsConfigsService,
                                      ObjectMapper objectMapper) {
        this.userConfigurationService = userConfigurationService;
        this.documentsConfigsService = documentsConfigsService;
        this.objectMapper = objectMapper;
    }

    private Mono<ResponseEntity<DocumentTypesConfigurations>> getDocumentTypesConfigurationsErrorResponse(Throwable throwable) {

        if (throwable instanceof DocumentTypeNotPresentException) {
            return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
        } else if (throwable instanceof BucketException) {
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null));
        } else if (throwable instanceof IdClientNotFoundException) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(null));
        }
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null));
    }

    private Mono<ResponseEntity<UserConfiguration>> getUserConfigurationErrorResponse(Throwable throwable) {

        if (throwable instanceof RepositoryManagerException) {
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null));
        } else if (throwable instanceof IdClientNotFoundException) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(null));
        }
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null));
    }

    @Override
    public Mono<ResponseEntity<DocumentTypesConfigurations>> getDocumentsConfigs(final ServerWebExchange exchange) {
        final String GET_DOCUMENTS_CONFIGS = "getDocumentsConfigs";

        log.info(Constant.STARTING_PROCESS, GET_DOCUMENTS_CONFIGS);

        log.debug(Constant.INVOKING_METHOD, GET_DOCUMENTS_CONFIGS, "");
        return documentsConfigsService.getDocumentsConfigs()
                                      .map(documentTypesConfigurations -> {
                                          log.info(Constant.ENDING_PROCESS, GET_DOCUMENTS_CONFIGS);
                                          return ResponseEntity.ok(documentTypesConfigurations);
                                      })
                                      .onErrorResume(throwable -> {
                                          log.info(Constant.ENDING_PROCESS_WITH_ERROR, GET_DOCUMENTS_CONFIGS, throwable, throwable.getMessage());
                                          return this.getDocumentTypesConfigurationsErrorResponse(throwable);
                                      });
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
        final String GET_CURRENT_CLIENT_CONFIGS = "getCurrentClientConfig";

        log.info(Constant.STARTING_PROCESS_ON, GET_CURRENT_CLIENT_CONFIGS, clientId);

        log.debug(Constant.INVOKING_METHOD, GET_CURRENT_CLIENT_CONFIGS, clientId);
        return userConfigurationService.getUserConfiguration(clientId)
                                       .map(userConfigurationInternal -> {
                                           log.info(Constant.ENDING_PROCESS_ON, GET_CURRENT_CLIENT_CONFIGS, clientId);
                                           return ResponseEntity.ok(objectMapper.convertValue(
                                                   userConfigurationInternal,
                                                   UserConfiguration.class));
                                       })
                                       .onErrorResume(throwable -> {
                                           log.info(Constant.ENDING_PROCESS_WITH_ERROR, GET_CURRENT_CLIENT_CONFIGS, throwable, throwable.getMessage());
                                           return getUserConfigurationErrorResponse(throwable);
                                       });
    }
}
