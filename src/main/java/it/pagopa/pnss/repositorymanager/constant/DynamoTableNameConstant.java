package it.pagopa.pnss.repositorymanager.constant;

public final class DynamoTableNameConstant {

    private DynamoTableNameConstant() {
        throw new IllegalStateException("DynamoTableNameConstant is a class of constant");
    }

    public static final String ANAGRAFICA_CLIENT_TABLE_NAME = "pn-ss-anagrafica-client";
//    public static final String ANAGRAFICA_CLIENT_TABLE_NAME = "UserConfiguration";
    public static final String DOC_TYPES_TABLE_NAME = "pn-ss-tipologie-documenti";
//    public static final String DOC_TYPES_TABLE_NAME = "DocTypes";
    public static final String DOCUMENT_TABLE_NAME = "pn-ss-documenti";
//    public static final String DOCUMENT_TABLE_NAME = "Document";
    
    public static final String USER_CONFIGURATION_TABLE_NAME = "UserConfiguration";
}
