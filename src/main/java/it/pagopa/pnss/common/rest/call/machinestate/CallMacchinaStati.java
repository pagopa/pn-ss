package it.pagopa.pnss.common.rest.call.machinestate;

import it.pagopa.pnss.common.exception.InvalidNextStatusException;
import it.pagopa.pnss.common.model.dto.MacchinaStatiValidateStatoResponseDto;
import it.pagopa.pnss.common.model.pojo.DocumentStatusChange;
import reactor.core.publisher.Mono;

public interface CallMacchinaStati {

    /**
     * Set xPagopaExtchCxId, processId, currentStatus and nextStatus in the object argument
     */
    Mono<MacchinaStatiValidateStatoResponseDto> statusValidation(DocumentStatusChange documentStatusChange) throws InvalidNextStatusException;
}
