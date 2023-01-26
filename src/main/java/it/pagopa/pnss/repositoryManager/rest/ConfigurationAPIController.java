package it.pagopa.pnss.repositoryManager.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pn.template.rest.v1.api.CfgApi;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypesConfigurations;
import it.pagopa.pn.template.rest.v1.dto.UserConfiguration;
import it.pagopa.pnss.repositoryManager.service.DocTypesService;
import it.pagopa.pnss.repositoryManager.service.UserConfigurationService;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/configuration-api")
public class ConfigurationAPIController implements CfgApi{


	@Override
	public Mono<ResponseEntity<UserConfiguration>> getCurrentClientConfig(String clientId,
			ServerWebExchange exchange) {
		
		return CfgApi.super.getCurrentClientConfig(clientId, exchange);
	}

	@Override
	public Mono<ResponseEntity<DocumentTypesConfigurations>> getDocumentsConfigs(ServerWebExchange exchange) {
		// TODO Auto-generated method stub
		return CfgApi.super.getDocumentsConfigs(exchange);
	}
}
