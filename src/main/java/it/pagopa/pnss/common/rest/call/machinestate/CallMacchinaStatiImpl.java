package it.pagopa.pnss.common.rest.call.machinestate;

import it.pagopa.pnss.common.configurationproperties.endpoint.internal.statemachine.StateMachineEndpointProperties;
import it.pagopa.pnss.common.exception.InvalidNextStatusException;
import it.pagopa.pnss.common.exception.StateMachineServiceException;
import it.pagopa.pnss.common.model.dto.MacchinaStatiValidateStatoResponseDto;
import it.pagopa.pnss.common.model.pojo.DocumentStatusChange;
import lombok.CustomLog;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

@Component
@CustomLog
public class CallMacchinaStatiImpl implements CallMacchinaStati {

    private final WebClient stateMachineWebClient;
    private final StateMachineEndpointProperties stateMachineEndpointProperties;
    private final RetryBackoffSpec smRetryStrategy;
    private static final String CLIENT_ID_QUERY_PARAM = "clientId";

    public CallMacchinaStatiImpl(WebClient stateMachineWebClient, StateMachineEndpointProperties stateMachineEndpointProperties, RetryBackoffSpec smRetryStrategy) {
        this.stateMachineWebClient = stateMachineWebClient;
        this.stateMachineEndpointProperties = stateMachineEndpointProperties;
        this.smRetryStrategy = smRetryStrategy;
    }

    @Override
    public Mono<MacchinaStatiValidateStatoResponseDto> statusValidation(DocumentStatusChange documentStatusChange) throws InvalidNextStatusException {
        log.logInvokingExternalService("pn-statemachinemanager", "statusValidation()");
        return stateMachineWebClient.get()
                .uri(uriBuilder -> uriBuilder.path(stateMachineEndpointProperties.validate())
                        .queryParam(CLIENT_ID_QUERY_PARAM,
                                documentStatusChange.getXPagopaExtchCxId())
                        .queryParam("nextStatus", documentStatusChange.getNextStatus())
                        .build(documentStatusChange.getProcessId(),
                                documentStatusChange.getCurrentStatus()))
                .retrieve()
                .onStatus(HttpStatus::is5xxServerError, clientResponse -> clientResponse.createException().onErrorMap(WebClientResponseException.class, throwable -> new StateMachineServiceException(throwable.getMessage(), throwable)))
                .bodyToMono(MacchinaStatiValidateStatoResponseDto.class)
                .onErrorMap(WebClientRequestException.class, throwable -> new StateMachineServiceException(throwable.getMessage(), throwable))
                .flatMap(macchinaStatiValidateStatoResponseDto -> {
                    if (!macchinaStatiValidateStatoResponseDto.isAllowed()) {
                        return Mono.error(new InvalidNextStatusException(documentStatusChange));
                    } else {
                        return Mono.just(macchinaStatiValidateStatoResponseDto);
                    }
                })
                .retryWhen(smRetryStrategy);
    }
}
