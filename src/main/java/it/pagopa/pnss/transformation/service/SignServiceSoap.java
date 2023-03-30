package it.pagopa.pnss.transformation.service;

import com.sun.xml.ws.encoding.xml.XMLMessage;
import it.pagopa.pnss.transformation.model.pojo.ArubaSecretValue;
import it.pagopa.pnss.transformation.model.pojo.IdentitySecretTimemark;
import it.pagopa.pnss.transformation.wsdl.*;
import jakarta.activation.DataHandler;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.io.InputStream;

import static it.pagopa.pnss.transformation.wsdl.XmlSignatureType.XMLENVELOPED;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

@Service
@Slf4j
public class SignServiceSoap {

    private final ArubaSignService arubaSignService;
    private final IdentitySecretTimemark identitySecretTimemark;
    private final ArubaSecretValue arubaSecretValue;

    @Value("${aruba.cert_id}")
    public String certificationID;

    @Value("${PnSsTimemarkUrl:#{null}}")
    public String timeMarkUrl;

    @Value("${PnSsTsaIdentity:#{true}}")
    public boolean tsaIdentity;

    public SignServiceSoap(ArubaSignService arubaSignService, IdentitySecretTimemark identitySecretTimemark,
                           ArubaSecretValue arubaSecretValue) {
        this.arubaSignService = arubaSignService;
        this.identitySecretTimemark = identitySecretTimemark;
        this.arubaSecretValue = arubaSecretValue;
    }

    private void endSoapRequest(MonoSink<Object> sink, Throwable throwable) {
        log.error(throwable.getMessage());
        sink.error(throwable);
        Thread.currentThread().interrupt();
    }

    private Auth createIdentity() {
        var auth = new Auth();
        auth.setDelegatedDomain(arubaSecretValue.getDelegatedDomain());
        auth.setDelegatedPassword(arubaSecretValue.getDelegatedPassword());
        auth.setDelegatedUser(arubaSecretValue.getDelegatedUser());
        auth.setOtpPwd(arubaSecretValue.getOtpPwd());
        auth.setTypeOtpAuth(arubaSecretValue.getTypeOtpAuth());
        auth.setUser(arubaSecretValue.getUser());
        return auth;
    }

    private SignRequestV2 createAuthenticatedSignRequestV2() {
        var signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID(certificationID);
        signRequestV2.setIdentity(createIdentity());
        return signRequestV2;
    }

    private void setSignRequestV2WithTsaAuth(Boolean marcatura, SignRequestV2 signRequestV2) {
        if (Boolean.TRUE.equals(marcatura) && tsaIdentity) {
            var tsaAuth = new TsaAuth();
            tsaAuth.setUser(identitySecretTimemark.getUserTimemark());
            tsaAuth.setPassword(identitySecretTimemark.getPasswordTimemark());
            tsaAuth.setTsaurl(timeMarkUrl);
            signRequestV2.setTsaIdentity(tsaAuth);
        }
    }

    public Mono<SignReturnV2> signPdfDocument(byte[] pdfFile, Boolean marcatura) {
        return Mono.fromCallable(() -> {
            var signRequestV2 = createAuthenticatedSignRequestV2();
            signRequestV2.setRequiredmark(marcatura);
            signRequestV2.setBinaryinput(pdfFile);
            signRequestV2.setTransport(TypeTransport.BYNARYNET);
            setSignRequestV2WithTsaAuth(marcatura, signRequestV2);
            return signRequestV2;
        }).map(signRequestV2 -> {
            var pdfsignatureV2 = new PdfsignatureV2();
            pdfsignatureV2.setSignRequestV2(signRequestV2);
            return pdfsignatureV2;
        }).flatMap(pdfsignatureV2 -> Mono.create(sink -> arubaSignService.pdfsignatureV2Async(pdfsignatureV2, outputFuture -> {
            try {
                sink.success(outputFuture.get());
            } catch (Exception throwable) {
                endSoapRequest(sink, throwable);
            }
        }))).cast(PdfsignatureV2Response.class).map(PdfsignatureV2Response::getReturn);
    }

    public Mono<SignReturnV2> pkcs7signV2(byte[] buf, Boolean marcatura) {
        return Mono.fromCallable(() -> {
            var signRequestV2 = createAuthenticatedSignRequestV2();
            signRequestV2.setStream(new DataHandler(new ByteArrayDataSource(buf, APPLICATION_OCTET_STREAM_VALUE)));
            signRequestV2.setTransport(TypeTransport.STREAM);
            signRequestV2.setRequiredmark(marcatura);
            setSignRequestV2WithTsaAuth(marcatura, signRequestV2);
            return signRequestV2;
        }).map(signRequestV2 -> {
            var pkcs7SignV2 = new Pkcs7SignV2();
            pkcs7SignV2.setSignRequestV2(signRequestV2);
            return pkcs7SignV2;
        }).flatMap(pkcs7SignV2 -> Mono.create(sink -> arubaSignService.pkcs7SignV2Async(pkcs7SignV2, outputFuture -> {
            try {
                sink.success(outputFuture.get());
            } catch (Exception throwable) {
                endSoapRequest(sink, throwable);
            }
        }))).cast(Pkcs7SignV2Response.class).map(Pkcs7SignV2Response::getReturn);
    }

    public Mono<SignReturnV2> xmlSignature(String contentType, InputStream xml, Boolean marcatura) {
        return Mono.fromCallable(() -> {
            var signRequestV2 = createAuthenticatedSignRequestV2();
            signRequestV2.setStream(new DataHandler(XMLMessage.createDataSource(contentType, xml)));
            signRequestV2.setTransport(TypeTransport.STREAM);
            signRequestV2.setRequiredmark(marcatura);
            return signRequestV2;
        }).map(signRequestV2 -> {
            var xmlsignature = new Xmlsignature();
            var xmlSignatureParameter = new XmlSignatureParameter();
            xmlSignatureParameter.setType(XMLENVELOPED);
            xmlsignature.setSignRequestV2(signRequestV2);
            xmlsignature.setParameter(xmlSignatureParameter);
            return xmlsignature;
        }).flatMap(xmlsignature -> Mono.create(sink -> arubaSignService.xmlsignatureAsync(xmlsignature, outputFuture -> {
            try {
                sink.success(outputFuture.get());
            } catch (Exception throwable) {
                endSoapRequest(sink, throwable);
            }
        }))).cast(XmlsignatureResponse.class).map(XmlsignatureResponse::getReturn);
    }
}
