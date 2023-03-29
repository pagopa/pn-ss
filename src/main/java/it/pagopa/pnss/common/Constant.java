package it.pagopa.pnss.common;

import java.util.List;

public final class Constant {

    private Constant() {
        throw new IllegalStateException("Constant is a constant class");
    }

    public static final String APPLICATION_PDF = "application/pdf";
    public static final String APPLICATION_ZIP = "application/zip";
    public static final String IMAGE_TIFF = "image/tiff";
    public static final String FILE_EXTENSION_PDF = ".pdf";
    public static final String FILE_EXTENSION_ZIP = ".zip";
    public static final String FILE_EXTENSION_TIFF = ".tiff";
    public static final String PN_NOTIFICATION_ATTACHMENTS = "PN_NOTIFICATION_ATTACHMENTS";
    public static final String PN_AAR = "PN_AAR";
    public static final String PN_LEGAL_FACTS = "PN_LEGAL_FACTS";
    public static final String PN_EXTERNAL_LEGAL_FACTS = "PN_EXTERNAL_LEGAL_FACTS";
    public static final String PN_DOWNTIME_LEGAL_FACTS = "PN_DOWNTIME_LEGAL_FACTS";
    public static final String PRELOADED = "PRELOADED";
    public static final String ATTACHED = "ATTACHED";
    public static final String SAVED = "SAVED";
    public static final String BOOKED = "BOOKED";
    public static final String FREEZED = "FREEZED";
    public static final String AVAILABLE = "AVAILABLE";
    public static final String STAGED = "STAGED";
    public static final String STORAGE_TYPE = "storageType";
    public static final List<String> LISTA_TIPOLOGIE_DOC =
            List.of(PN_NOTIFICATION_ATTACHMENTS, PN_AAR, PN_LEGAL_FACTS, PN_EXTERNAL_LEGAL_FACTS, PN_DOWNTIME_LEGAL_FACTS);
    public static final List<String> LISTA_TIPO_DOCUMENTI = List.of(APPLICATION_PDF, APPLICATION_ZIP, IMAGE_TIFF);
    public static final String TECHNICAL_STATUS_BOOKED = "BOOKED";
    public static final String TECHNICAL_STATUS_ATTACHED = "ATTACHED";
    public static final String TECHNICAL_STATUS_AVAILABLE = "AVAILABLE";
    public static final String TECHNICAL_STATUS_FREEZED = "FREEZED";
    public static final String EVENT_BUS_SOURCE_AVAILABLE_DOCUMENT = "SafeStorageOutcomeEvent";
    public static final String GESTORE_DISPONIBILITA_EVENT_NAME = "GESTORE DISPONIBILITA";
}
