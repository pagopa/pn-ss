package it.pagopa.pnss.repositoryManager.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pn.template.rest.v1.api.CfgApi;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypesConfigurations;
import it.pagopa.pn.template.rest.v1.dto.UserConfiguration;
import it.pagopa.pnss.repositoryManager.service.DocTypesService;
import it.pagopa.pnss.repositoryManager.service.impl.UserConfigurationServiceImpl;
import reactor.core.publisher.Mono;


@RestController
public class ConfigurationApiController implements CfgApi {
	
	@Autowired
	private DocTypesService docTypesService;
	@Autowired
	private UserConfigurationServiceImpl userConfigurationService;
	
	
    public Mono<ResponseEntity<UserConfiguration>> getCurrentClientConfig(String clientId,  final ServerWebExchange exchange) {
//        Mono<Void> result = Mono.empty();
//        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
//        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
//            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
//                String exampleString = "{ \"signatureInfo\" : \"{}\", \"canRead\" : [ \"canRead\", \"canRead\" ], \"name\" : \"pn-delivery-push\", \"destination\" : { \"sqsUrl\" : \"sqsUrl\" }, \"canCreate\" : [ \"canCreate\", \"canCreate\" ] }";
//                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
//                break;
//            }
//        }
//        return result.then(Mono.empty());
    	
    	return null;
    }
    
    public  Mono<ResponseEntity<DocumentTypesConfigurations>> getDocumentsConfigs( final ServerWebExchange exchange) {
//        Mono<Void> result = Mono.empty();
//        exchange.getResponse().setStatusCode(HttpStatus.NOT_IMPLEMENTED);
//        for (MediaType mediaType : exchange.getRequest().getHeaders().getAccept()) {
//            if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
//                String exampleString = "{ \"storageConfigurations\" : [ { \"hotPeriod\" : \"1y10d\", \"name\" : \"name\", \"retentionPeriod\" : \"1y10d\" }, { \"hotPeriod\" : \"1y10d\", \"name\" : \"name\", \"retentionPeriod\" : \"1y10d\" } ], \"documentsTypes\" : [ { \"digitalSignature\" : true, \"timestamped\" : \"NONE\", \"name\" : \"name\", \"checksum\" : \"MD5\", \"initialStatus\" : \"initialStatus\", \"statuses\" : { \"key\" : { \"storage\" : \"storage\", \"allowedStatusTransitions\" : [ \"allowedStatusTransitions\", \"allowedStatusTransitions\" ] } } }, { \"digitalSignature\" : true, \"timestamped\" : \"NONE\", \"name\" : \"name\", \"checksum\" : \"MD5\", \"initialStatus\" : \"initialStatus\", \"statuses\" : { \"key\" : { \"storage\" : \"storage\", \"allowedStatusTransitions\" : [ \"allowedStatusTransitions\", \"allowedStatusTransitions\" ] } } } ] }";
//                result = ApiUtil.getExampleResponse(exchange, mediaType, exampleString);
//                break;
//            }
//        }
//        return result.then(Mono.empty());

    	
    	return null;
    }

}
