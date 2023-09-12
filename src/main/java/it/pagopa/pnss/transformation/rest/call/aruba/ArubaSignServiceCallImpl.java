package it.pagopa.pnss.transformation.rest.call.aruba;

import com.sun.xml.ws.encoding.xml.XMLMessage;
import it.pagopa.pnss.common.client.exception.ArubaSignException;
import it.pagopa.pnss.transformation.model.pojo.ArubaSecretValue;
import it.pagopa.pnss.transformation.model.pojo.IdentitySecretTimeMark;
import it.pagopa.pnss.transformation.wsdl.*;
import jakarta.activation.DataHandler;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.xml.ws.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

import static it.pagopa.pnss.transformation.wsdl.XmlSignatureType.XMLENVELOPED;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

@Service
@Slf4j
public class ArubaSignServiceCallImpl implements ArubaSignServiceCall {

    private final ArubaSignService arubaSignService;
    private final IdentitySecretTimeMark identitySecretTimemark;
    private final ArubaSecretValue arubaSecretValue;

    @Value("${aruba.cert_id}")
    public String certificationID;

    @Value("${PnSsTimemarkUrl:#{null}}")
    public String timeMarkUrl;

    @Value("${PnSsTsaIdentity:#{true}}")
    public boolean tsaIdentity;

    private final Long arubaSignTimeout;

    private static final UnaryOperator<Mono<SignReturnV2>> CHECK_IF_RESPONSE_IS_OK = f -> f.handle((signRequestV2, sink) -> {
        if (signRequestV2.getStatus().equals("KO")) {
            sink.error(new ArubaSignException());
        } else {
            sink.next(signRequestV2);
        }
    });

    public ArubaSignServiceCallImpl(ArubaSignService arubaSignService, IdentitySecretTimeMark identitySecretTimemark,
                                    ArubaSecretValue arubaSecretValue, @Value("${aruba.sign.timeout}") String arubaSignTimeout) {
        this.arubaSignService = arubaSignService;
        this.identitySecretTimemark = identitySecretTimemark;
        this.arubaSecretValue = arubaSecretValue;
        this.arubaSignTimeout = Long.valueOf(arubaSignTimeout);
    }

    private <T> void createMonoFromSoapRequest(MonoSink<Object> sink, Response<T> response) {
        try {
            sink.success(response.get(arubaSignTimeout, TimeUnit.SECONDS));
        } catch (Exception throwable) {
            endSoapRequest(sink, throwable);
        }
    }

    private void endSoapRequest(MonoSink<Object> sink, Throwable throwable) {
        log.error(throwable.getMessage());
        sink.error(new ArubaSignException());
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
            tsaAuth.setUser(identitySecretTimemark.getUserTimeMark());
            tsaAuth.setPassword(identitySecretTimemark.getPasswordTimeMark());
            tsaAuth.setTsaurl(timeMarkUrl != null ? timeMarkUrl : null);
            signRequestV2.setTsaIdentity(tsaAuth);
        }
    }

    /*
    The AsyncHandler callback is both asynchronous and non-blocking. We can ignore its return value and
    rely exclusively on the AsyncHandler. This callback handler is invoked on a JAX-WS worker thread when the response has come in.
    */

    @Override
    public Mono<SignReturnV2> signPdfDocument(byte[] pdfFile, Boolean marcatura) {
        return Mono.fromCallable(() -> {
                       var signRequestV2 = createAuthenticatedSignRequestV2();
                       signRequestV2.setRequiredmark(marcatura);
                       signRequestV2.setBinaryinput(pdfFile);
                       signRequestV2.setTransport(TypeTransport.BYNARYNET);
                       setSignRequestV2WithTsaAuth(marcatura, signRequestV2);
                       return signRequestV2;
                   })
                   .map(signRequestV2 -> {
                       var pdfsignatureV2 = new PdfsignatureV2();
                       pdfsignatureV2.setSignRequestV2(signRequestV2);
                       return pdfsignatureV2;
                   })
                   .flatMap(pdfsignatureV2 -> Mono.create(sink -> arubaSignService.pdfsignatureV2Async(pdfsignatureV2,
                                                                                                       res -> createMonoFromSoapRequest(sink,
                                                                                                                                        res))))
                   .cast(PdfsignatureV2Response.class)
                   .map(PdfsignatureV2Response::getReturn)
                   .transform(CHECK_IF_RESPONSE_IS_OK);
    }

    @Override
    public Mono<SignReturnV2> pkcs7signV2(byte[] buf, Boolean marcatura) {
        return Mono.fromCallable(() -> {
                       var signRequestV2 = createAuthenticatedSignRequestV2();
                       signRequestV2.setRequiredmark(marcatura);
                       signRequestV2.setBinaryinput(buf);
                       signRequestV2.setTransport(TypeTransport.BYNARYNET);
                       setSignRequestV2WithTsaAuth(marcatura, signRequestV2);
                       return signRequestV2;
                   })
                   .map(signRequestV2 -> {
                       var pkcs7SignV2 = new Pkcs7SignV2();
                       pkcs7SignV2.setSignRequestV2(signRequestV2);
                       return pkcs7SignV2;
                   })
                   .flatMap(pkcs7SignV2 -> Mono.create(sink -> arubaSignService.pkcs7SignV2Async(pkcs7SignV2,
                                                                                                 res -> createMonoFromSoapRequest(sink,
                                                                                                                                  res))))
                   .cast(Pkcs7SignV2Response.class)
                   .map(Pkcs7SignV2Response::getReturn)
                   .transform(CHECK_IF_RESPONSE_IS_OK);
    }

    @Override
    public Mono<SignReturnV2> xmlSignature(byte[] xmlBytes, Boolean marcatura) {
        return Mono.fromCallable(() -> {
                       var signRequestV2 = createAuthenticatedSignRequestV2();
                       signRequestV2.setStream(new DataHandler(XMLMessage.createDataSource(APPLICATION_XML_VALUE,
                                                                                           new ByteArrayInputStream(xmlBytes))));
                       signRequestV2.setTransport(TypeTransport.STREAM);
                       signRequestV2.setRequiredmark(marcatura);
                       return signRequestV2;
                   })
                   .map(signRequestV2 -> {
                       var xmlsignature = new Xmlsignature();
                       var xmlSignatureParameter = new XmlSignatureParameter();
                       xmlSignatureParameter.setType(XMLENVELOPED);
                       xmlsignature.setSignRequestV2(signRequestV2);
                       xmlsignature.setParameter(xmlSignatureParameter);
                       return xmlsignature;
                   })
                   .flatMap(xmlsignature -> Mono.create(sink -> arubaSignService.xmlsignatureAsync(xmlsignature,
                                                                                                   res -> createMonoFromSoapRequest(sink,
                                                                                                                                    res))))
                   .cast(XmlsignatureResponse.class)
                   .map(XmlsignatureResponse::getReturn)
                   .transform(CHECK_IF_RESPONSE_IS_OK);
    }
}
