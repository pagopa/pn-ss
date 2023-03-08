package it.pagopa.pnss.common;

import software.amazon.awssdk.regions.Region;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public class Constant {

    public static final String APPLICATION_PDF = "application/pdf";
    public static final String APPLICATION_ZIP = "application/zip";
    public static final String IMAGE_TIFF = "image/tiff";
    public static final String PN_NOTIFICATION_ATTACHMENTS = "PN_NOTIFICATION_ATTACHMENTS";
    public static final String PN_AAR = "PN_AAR";
    public static final String PN_LEGAL_FACTS = "PN_LEGAL_FACTS";
    public static final String PN_EXTERNAL_LEGAL_FACTS = "PN_EXTERNAL_LEGAL_FACTS";
    public static final String PN_DOWNTIME_LEGAL_FACTS = "PN_DOWNTIME_LEGAL_FACTS";
    public static final BigDecimal MAX_RECOVER_COLD = new BigDecimal(259200);

    public static final String PRELOADED = "PRELOADED";
    public static final String ATTACHED = "ATTACHED";
    public static final String SAVED ="SAVED";
    public static final String BOOKED ="BOOKED";
    public static final String FREEZED = "FREEZED";
    public static final String AVAILABLE = "AVAILABLE";
    public static final String STAGED = "STAGED";
    
    public static final String STORAGETYPE = "storageType";

    public static final List<String> listaTipologieDoc = Arrays.asList(PN_NOTIFICATION_ATTACHMENTS, PN_AAR, PN_LEGAL_FACTS, PN_EXTERNAL_LEGAL_FACTS, PN_DOWNTIME_LEGAL_FACTS);
    public static final List<String> listaTipoDocumenti =  Arrays.asList(APPLICATION_PDF, APPLICATION_ZIP, IMAGE_TIFF);
    public static final List<String> listaStatus =  Arrays.asList(PRELOADED, ATTACHED);

    public static final Region EU_CENTRAL_1 = Region.EU_CENTRAL_1;

    public static final String technicalStatus_booked="BOOKED";
    public static final String technicalStatus_attached="ATTACHED";
    public static final String technicalStatus_available="AVAILABLE";

    public static final String EVEN_BUS_SOURCE_AVAILABLE_DOCUMENT = "AVAILABLE DOCUMENT";
    public static final String GESTORE_DISPONIBILITA_EVENT_NAME = "GESTORE DISPONIBILITA";
}
