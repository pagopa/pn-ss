package it.pagopa.pnss.common.exception;

import it.pagopa.pnss.repositorymanager.exception.ItemAlreadyPresent;

import java.io.Serializable;

public class IdemPotentElementException extends ItemAlreadyPresent implements Serializable {


    public IdemPotentElementException(String fileKey) {
        super(String.format("The insertion od the element '%s' would have no effect: same values or already present.",fileKey));
    }


}
