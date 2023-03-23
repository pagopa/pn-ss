package it.pagopa.pnss.common.rest.call.machinestate;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import it.pagopa.pnss.common.configurationproperties.endpoint.internal.statemachine.StateMachineEndpointProperties;
import it.pagopa.pnss.common.exception.InvalidNextStatusException;
import it.pagopa.pnss.common.model.dto.MacchinaStatiValidateStatoResponseDto;
import it.pagopa.pnss.common.model.pojo.DocumentStatusChange;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class CallMacchinaStatiImpl implements CallMacchinaStati {

    private final WebClient stateMachineWebClient;
    private final StateMachineEndpointProperties stateMachineEndpointProperties;
    private static final String CLIENT_ID_QUERY_PARAM = "clientId";

    public CallMacchinaStatiImpl(WebClient stateMachineWebClient, StateMachineEndpointProperties stateMachineEndpointProperties) {
        this.stateMachineWebClient = stateMachineWebClient;
        this.stateMachineEndpointProperties = stateMachineEndpointProperties;
    }

    @Override
    public Mono<MacchinaStatiValidateStatoResponseDto> statusValidation(DocumentStatusChange documentStatusChange) throws InvalidNextStatusException {
    	log.info("CallMacchinaStatiImpl.statusValidation() : START");
        return stateMachineWebClient.get()
                .uri(uriBuilder -> uriBuilder.path(stateMachineEndpointProperties.validate())
                        .queryParam(CLIENT_ID_QUERY_PARAM,
                                documentStatusChange.getXPagopaExtchCxId())
                        .queryParam("nextStatus", documentStatusChange.getNextStatus())
                        .build(documentStatusChange.getProcessId(),
                                documentStatusChange.getCurrentStatus()))
                .retrieve()
                .bodyToMono(MacchinaStatiValidateStatoResponseDto.class)
                .flatMap(macchinaStatiValidateStatoResponseDto -> {
                    if (!macchinaStatiValidateStatoResponseDto.isAllowed()) {
                        return Mono.error(new InvalidNextStatusException(documentStatusChange));
                    } else {
                        return Mono.just(macchinaStatiValidateStatoResponseDto);
                    }
                });
    }
}
