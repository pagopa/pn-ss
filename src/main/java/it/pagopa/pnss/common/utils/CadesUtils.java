package it.pagopa.pnss.common.utils;

import it.pagopa.pnss.common.exception.CadesContentMismatchException;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class CadesUtils {

    private CadesUtils() {
        throw new IllegalStateException("CadesUtils is a utility class");
    }

    public static void verifyP7mContentHash(byte[] originalBytes, byte[] p7mBytes, String fileKey) throws CMSException {
        CMSSignedData signedData = new CMSSignedData(p7mBytes);
        byte[] extracted = (byte[]) signedData.getSignedContent().getContent();

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }

        if (!Arrays.equals(digest.digest(originalBytes), digest.digest(extracted))) {
            throw new CadesContentMismatchException(fileKey);
        }
    }
}
