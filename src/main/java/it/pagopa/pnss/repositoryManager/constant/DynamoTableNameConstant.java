package it.pagopa.pnss.repositoryManager.constant;

public final class DynamoTableNameConstant {

    private DynamoTableNameConstant() {
        throw new IllegalStateException("DynamoTableNameConstant is a class of constant");
    }

    public static final String ANAGRAFICA_CLIENT_TABLE_NAME = "dgs-bing-ss-PnSsTableAnagraficaClient-U1NE0FT6QBLE";
//    public static final String ANAGRAFICA_CLIENT_TABLE_NAME = "UserConfiguration";
    
    public static final String DOC_TYPES_TABLE_NAME = "dgs-bing-ss-PnSsTableTipologieDocumenti-146LR9HHJBTPD";
//    public static final String DOC_TYPES_TABLE_NAME = "DocTypes";
    
    public static final String DOCUMENT_TABLE_NAME = "dgs-bing-ss-PnSsTableDocumenti-1TTOSMTT2OCLC";
//    public static final String DOCUMENT_TABLE_NAME = "Document";
    
}
