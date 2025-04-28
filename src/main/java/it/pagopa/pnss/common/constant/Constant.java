package it.pagopa.pnss.common.constant;

import java.util.List;
import java.util.Map;
import static java.util.Map.entry;
import static org.springframework.http.MediaType.*;

public final class Constant {

    private Constant() {
        throw new IllegalStateException("Constant is a constant class");
    }

    public static final String APPLICATION_ZIP_VALUE = "application/zip";
    public static final String IMAGE_TIFF_VALUE = "image/tiff";
    public static final String APPLICATION_PKCS7_MIME = "application/pkcs7-mime";
    public static final Map<String, String> MEDIA_TYPE_WITH_EXTENSION_MAP = Map.ofEntries(entry(APPLICATION_PDF_VALUE, ".pdf"),
                                                                                          entry(APPLICATION_ZIP_VALUE, ".zip"),
                                                                                          entry(APPLICATION_OCTET_STREAM_VALUE, ".bin"),
                                                                                          entry(APPLICATION_XML_VALUE, ".xml"),
                                                                                          entry(IMAGE_TIFF_VALUE, ".tiff"),
                                                                                          entry(APPLICATION_PKCS7_MIME, ".p7m"));
    public static final String PRELOADED = "preloaded";
    public static final String ATTACHED = "attached";
    public static final String SAVED = "saved";
    public static final String BOOKED = "booked";
    public static final String FREEZED = "freezed";
    public static final String AVAILABLE = "available";
    public static final String STAGED = "staged";
    public static final String DELETED = "deleted";

    public static final String STORAGE_TYPE = "storageType";
    public static final String STORAGE_EXPIRY = "storage_expiry";
    public static final String STORAGE_FREEZE = "storage_freeze";
    public static final List<String> LISTA_TIPO_DOCUMENTI =
            List.of(APPLICATION_PDF_VALUE, APPLICATION_ZIP_VALUE, APPLICATION_OCTET_STREAM_VALUE, APPLICATION_XML_VALUE, IMAGE_TIFF_VALUE, APPLICATION_PKCS7_MIME);
    public static final String TECHNICAL_STATUS_BOOKED = "BOOKED";
    public static final String TECHNICAL_STATUS_ATTACHED = "ATTACHED";
    public static final String TECHNICAL_STATUS_AVAILABLE = "AVAILABLE";
    public static final String TECHNICAL_STATUS_FREEZED = "FREEZED";
    public static final String EVENT_BUS_SOURCE_AVAILABLE_DOCUMENT = "SafeStorageOutcomeEvent";
    public static final String EVENT_BUS_SOURCE_TRANSFORMATION_DOCUMENT = "SafeStorageTransformEvent";
    public static final String EVENT_BUS_SOURCE_GLACIER_DOCUMENTS = "SafeStorageGlacierRestoreEvent";
    public static final String GESTORE_DISPONIBILITA_EVENT_NAME = "GESTORE DISPONIBILITA";
    //providers
    public static final String ARUBA ="ARUBA";
    public static final String NAMIRIAL ="NAMIRIAL";
    public static final String DUMMY ="DUMMY";
}
