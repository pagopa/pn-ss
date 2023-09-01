package it.pagopa.pnss.uribuilder.utils;

import com.amazonaws.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

public  class GenerateRandoKeyFile {

    private static GenerateRandoKeyFile generateRandoKeyFile = null;

    public static GenerateRandoKeyFile getInstance(){
        if (generateRandoKeyFile == null)
            generateRandoKeyFile = new GenerateRandoKeyFile();

        return generateRandoKeyFile;
    }

    public synchronized String createEncodedKeyName(String documentType, String extension) {
        OffsetDateTime todayDate = OffsetDateTime.now();

        var year = String.valueOf(todayDate.getYear());

        var month = twoDigitFormat(String.valueOf(todayDate.getMonth().getValue()));

        var day = twoDigitFormat(String.valueOf(todayDate.getDayOfMonth()));

        var hour = twoDigitFormat(String.valueOf(todayDate.getHour()));

        UUID temp = UUID.randomUUID();
        String uuidString = Long.toHexString(temp.getMostSignificantBits())
                + Long.toHexString(temp.getLeastSignificantBits());
        var documentName = documentType + "-" + uuidString + extension;

        return StringUtils.join("/", year, month, day, hour, documentName);
    }

    private String twoDigitFormat(String value)
    {
        return value.length() < 2 ? '0' + value : value;
    }

}
