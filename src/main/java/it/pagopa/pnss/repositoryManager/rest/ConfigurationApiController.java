package it.pagopa.pnss.repositoryManager.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pn.template.rest.v1.api.CfgApi;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypesConfigurations;
import it.pagopa.pn.template.rest.v1.dto.UserConfiguration;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.repositoryManager.exception.BucketException;
import it.pagopa.pnss.repositoryManager.exception.RepositoryManagerException;
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
	
	private  Mono<ResponseEntity<DocumentTypesConfigurations>> getDocumentTypesConfigurationsErrorResponse(Throwable throwable) 
	{
		DocumentTypesConfigurations response = new DocumentTypesConfigurations();
		if (throwable instanceof BucketException) {
			return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));			
		}
		return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
	}
	
	private  Mono<ResponseEntity<UserConfiguration>> getUserConfigurationErrorResponse(String clientId, Throwable throwable) 
	{
		UserConfiguration response = new UserConfiguration();
		response.setName(clientId);

		if (throwable instanceof IdClientNotFoundException) {
			return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(response));
		}
		else if (throwable instanceof RepositoryManagerException) {
			return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));			
		}
		return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
	}
	
    @Override
    public  Mono<ResponseEntity<DocumentTypesConfigurations>> getDocumentsConfigs( final ServerWebExchange exchange) {
    	
		return documentsConfigsService.getDocumentsConfigs()
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
	 * <li>signatureInfo: informazioni necessarie per firmare digitalmente, per conto del client, i file caricati.</li>
	 * <li>destination: informazioni necessarie a notificare eventi al client.</li>
	 * </ul>
	 */
	@Override
    public Mono<ResponseEntity<UserConfiguration>> getCurrentClientConfig(String clientId, final ServerWebExchange exchange) {
		
		return userConfigurationService.getUserConfiguration(clientId)
			.map(userConfigurationInternal -> ResponseEntity.ok(objectMapper.convertValue(userConfigurationInternal, UserConfiguration.class)))
			.onErrorResume(throwable -> getUserConfigurationErrorResponse(clientId, throwable));
    }

}
