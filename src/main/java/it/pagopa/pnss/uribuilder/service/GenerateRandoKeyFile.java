package it.pagopa.pnss.uribuilder.service;

import java.util.UUID;

public  class GenerateRandoKeyFile {

    private static GenerateRandoKeyFile generateRandoKeyFile = null;

    public static GenerateRandoKeyFile getInstance(){
        if (generateRandoKeyFile == null)
            generateRandoKeyFile = new GenerateRandoKeyFile();

        return generateRandoKeyFile;
    }

    public synchronized  String createKeyName(String documentType) {
        UUID temp = UUID.randomUUID();
        String uuidString = Long.toHexString(temp.getMostSignificantBits())
                + Long.toHexString(temp.getLeastSignificantBits());
        return documentType+"-"+uuidString;
    }


}
