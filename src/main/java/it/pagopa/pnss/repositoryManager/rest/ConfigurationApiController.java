package it.pagopa.pnss.repositoryManager.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pn.template.rest.v1.api.CfgApi;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypesConfigurations;
import it.pagopa.pn.template.rest.v1.dto.UserConfiguration;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.repositoryManager.service.DocumentsConfigsService;
import it.pagopa.pnss.repositoryManager.service.UserConfigurationService;
import reactor.core.publisher.Mono;


@RestController
public class ConfigurationApiController implements CfgApi {
	
	@Autowired
	private UserConfigurationService userConfigurationService;
	@Autowired
	private DocumentsConfigsService documentsConfigsService;
	@Autowired
	private ObjectMapper objectMapper;
	
    @Override
    public  Mono<ResponseEntity<DocumentTypesConfigurations>> getDocumentsConfigs( final ServerWebExchange exchange) {
    	
       	DocumentTypesConfigurations configurations = documentsConfigsService.getAllDocumentType();
    	return Mono.just(ResponseEntity.ok().body(configurations));
    	
    } //RepositoryManagerException
	
	/**
	 * Ogni microservizio che vuole utilizzare SafeStorage deve essere censito, avere almeno una api-key assegnata, 
	 * e avere alcune configurazioni:
	 * <ul>
	 * <li>name: il nome del sistema upstream (client).</li>
	 * <li>canCreate: tipi di documento che il client può caricare.</li>
	 * <li>canRead: tipi di documento che il client può leggere.</li>
	 * <li>signatureInfo: informazioni necessarie per firmare digitalmente, per conto del client, i file caricati.</li>
	 * <li>destination: informazioni necessarie a notificare eventi al client.</li>
	 * </ul>
	 */
	@Override
    public Mono<ResponseEntity<UserConfiguration>> getCurrentClientConfig(String clientId,  final ServerWebExchange exchange) {
		
		return userConfigurationService.getUserConfiguration(clientId)
			.map(userConfigurationInternal -> 
				ResponseEntity.ok(objectMapper.convertValue(userConfigurationInternal, it.pagopa.pn.template.rest.v1.dto.UserConfiguration.class))
			)
			.onErrorResume(error -> {
				if (error instanceof IdClientNotFoundException) {
					return Mono.just(ResponseEntity.notFound().build());
				}
				return Mono.just(ResponseEntity.badRequest().build());
			});
		
    }

}
