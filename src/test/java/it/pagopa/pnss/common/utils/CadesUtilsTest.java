package it.pagopa.pnss.common.utils;

import it.pagopa.pnss.common.exception.CadesContentMismatchException;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.junit.jupiter.api.Test;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CadesUtilsTest {

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private byte[] buildCadesP7m(byte[] content) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        long now = System.currentTimeMillis();
        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(
                    new JcaX509v3CertificateBuilder(
                        new X500Principal("CN=test"),
                        BigInteger.ONE,
                        new Date(now - 1000),
                        new Date(now + 86400000),
                        new X500Principal("CN=test"),
                        kp.getPublic()
                    ).build(new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(kp.getPrivate()))
                );

        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(kp.getPrivate());
        gen.addSignerInfoGenerator(
            new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().setProvider("BC").build())
                .build(signer, cert)
        );
        gen.addCertificates(new JcaCertStore(List.of(cert)));
        return gen.generate(new CMSProcessableByteArray(content), true).getEncoded();
    }

    @Test
    void verifyP7mContentHash_validSignature_doesNotThrow() throws Exception {
        byte[] original = "test zip content".getBytes();
        byte[] p7m = buildCadesP7m(original);
        assertDoesNotThrow(() -> CadesUtils.verifyP7mContentHash(original, p7m, "test-file.zip"));
    }

    @Test
    void verifyP7mContentHash_corruptedContent_throwsMismatch() throws Exception {
        byte[] original = "original content".getBytes();
        byte[] different = "different content".getBytes();
        byte[] p7m = buildCadesP7m(original);
        assertThrows(CadesContentMismatchException.class,
                () -> CadesUtils.verifyP7mContentHash(different, p7m, "test-file.zip"));
    }

    @Test
    void verifyP7mContentHash_invalidBytes_throwsCmsException() {
        byte[] original = "content".getBytes();
        byte[] invalidP7m = new byte[]{1, 2, 3, 4, 5};
        assertThrows(CMSException.class,
                () -> CadesUtils.verifyP7mContentHash(original, invalidP7m, "test-file.zip"));
    }
}
