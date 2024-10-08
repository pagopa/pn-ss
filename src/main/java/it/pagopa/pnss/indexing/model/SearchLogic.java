package it.pagopa.pnss.indexing.model;

import it.pagopa.pnss.common.exception.InvalidSearchLogicException;

public enum SearchLogic {


    AND("and"),
    OR("or");

    private final String logic;

    SearchLogic(String logic) {
        this.logic = logic;
    }

    public String getLogic() {
        return logic;
    }

    public static SearchLogic getEnumLogic(String logic) {
        for (SearchLogic searchLogic : SearchLogic.values()) {
            if (searchLogic.getLogic().equals(logic)) {
                return searchLogic;
            }
        }
        throw new InvalidSearchLogicException(logic);
    }
}
