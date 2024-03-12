package it.pagopa.pn.library.sign.service.impl;

import com.sun.xml.ws.encoding.xml.XMLMessage;
import it.pagopa.pn.library.sign.exception.PnSpapiTemporaryErrorException;
import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import it.pagopa.pn.library.sign.exception.aruba.ArubaSignException;
import it.pagopa.pn.library.sign.service.PnSignService;
import it.pagopa.pnss.transformation.wsdl.*;
import javax.activation.DataHandler;
import javax.xml.ws.Response;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static it.pagopa.pnss.common.utils.LogUtils.*;
import static it.pagopa.pnss.transformation.wsdl.XmlSignatureType.XMLENVELOPED;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

@Service("arubaProviderService")
@DependsOn("pnSignCredentialConf")
@CustomLog
public class ArubaSignProviderService implements PnSignService {

    private final ArubaSignService arubaSignService;

    @Value("${aruba.cert_id}")
    public String certificationID;

    @Value("${PnSsTimemarkUrl:#{null}}")
    public String timeMarkUrl;

    @Value("${PnSsTsaIdentity:#{true}}")
    public boolean tsaIdentity;

    @Value("${aruba.sign.delegated.domain}")
    private String delegatedDomain;

    @Value("${aruba.sign.delegated.user}")
    private String delegatedPassword;

    @Value("${aruba.sign.delegated.password}")
    private String delegatedUser;

    @Value("${aruba.sign.otp.pwd}")
    private String otpPwd;

    @Value("${aruba.sign.type.otp.auth}")
    private String typeOtpAuth;

    @Value("${aruba.sign.user}")
    private String user;

    @Value("${aruba.timemark.user}")
    private String timemarkUser;

    @Value("${aruba.timemark.password}")
    private String timemarkPassword;

    private final Long arubaSignTimeout;

    private static final UnaryOperator<Mono<SignReturnV2>> CHECK_IF_RESPONSE_IS_OK = f -> f.handle((signRequestV2, sink) -> {
        if (signRequestV2.getStatus().equals("KO")) {
            sink.error(new ArubaSignException());
        } else {
            sink.next(signRequestV2);
        }
    });

    public ArubaSignProviderService(ArubaSignService arubaSignService, @Value("${aruba.sign.timeout}") String arubaSignTimeout) {
        this.arubaSignService = arubaSignService;
        this.arubaSignTimeout = Long.valueOf(arubaSignTimeout);
        }

    private <T> void createMonoFromSoapRequest(MonoSink<Object> sink, Response<T> response) {
        try {
            sink.success(response.get());
        } catch (Exception throwable) {
            endSoapRequest(sink, throwable);
        }
    }

    private void endSoapRequest(MonoSink<Object> sink, Throwable throwable) {
        log.error(throwable.getMessage());
        sink.error(new ArubaSignException(throwable.getMessage()));
        Thread.currentThread().interrupt();
    }

    private Auth createIdentity() {
        var auth = new Auth();
        auth.setDelegatedDomain(delegatedDomain);
        auth.setDelegatedPassword(delegatedPassword);
        auth.setDelegatedUser(delegatedUser);
        auth.setOtpPwd(otpPwd);
        auth.setTypeOtpAuth(typeOtpAuth);
        auth.setUser(user);
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
            tsaAuth.setUser(timemarkUser);
            tsaAuth.setPassword(timemarkPassword);
            tsaAuth.setTsaurl(timeMarkUrl != null ? timeMarkUrl : null);
            signRequestV2.setTsaIdentity(tsaAuth);
        }
    }

    @Override
    public Mono<PnSignDocumentResponse> signPdfDocument(byte[] fileBytes, Boolean timestamping) {
        log.debug(CLIENT_METHOD_INVOCATION, ARUBA_SIGN_PDF_DOCUMENT, timestamping);
        return Mono.fromCallable(() -> {
                    var signRequestV2 = createAuthenticatedSignRequestV2();
                    signRequestV2.setRequiredmark(timestamping);
                    signRequestV2.setBinaryinput(fileBytes);
                    signRequestV2.setTransport(TypeTransport.BYNARYNET);
                    setSignRequestV2WithTsaAuth(timestamping, signRequestV2);
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
                .transform(CHECK_IF_RESPONSE_IS_OK)
                .timeout(Duration.ofSeconds(arubaSignTimeout), Mono.error(new ArubaSignException("Request timeout.")))
                .doOnNext(result -> log.info(CLIENT_METHOD_RETURN, ARUBA_SIGN_PDF_DOCUMENT, Stream.of(result.getStatus(), result.getReturnCode(), result.getDescription()).toList()))
                .map(signReturnV2 -> {
                    PnSignDocumentResponse pnSignDocumentResponse = new PnSignDocumentResponse();
                    pnSignDocumentResponse.setSignedDocument(signReturnV2.getBinaryoutput());
                    return pnSignDocumentResponse;
                })
                .onErrorResume(throwable -> Mono.error(new PnSpapiTemporaryErrorException(throwable.getMessage(), throwable)));
    }

    @Override
    public Mono<PnSignDocumentResponse> signXmlDocument(byte[] fileBytes, Boolean timestamping) {
        log.debug(CLIENT_METHOD_INVOCATION, ARUBA_SIGN_XML_DOCUMENT, timestamping);
        return Mono.fromCallable(() -> {
                    var signRequestV2 = createAuthenticatedSignRequestV2();
                    signRequestV2.setStream(new DataHandler(XMLMessage.createDataSource(APPLICATION_XML_VALUE,
                            new ByteArrayInputStream(fileBytes))));
                    signRequestV2.setTransport(TypeTransport.STREAM);
                    signRequestV2.setRequiredmark(timestamping);
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
                .transform(CHECK_IF_RESPONSE_IS_OK)
                .timeout(Duration.ofSeconds(arubaSignTimeout), Mono.error(new ArubaSignException("Request timeout.")))
                .doOnNext(result -> log.info(CLIENT_METHOD_RETURN, ARUBA_SIGN_XML_DOCUMENT, Stream.of(result.getStatus(), result.getReturnCode(), result.getDescription()).toList()))
                .map(signReturnV2 -> {
                    PnSignDocumentResponse pnSignDocumentResponse = new PnSignDocumentResponse();
                    pnSignDocumentResponse.setSignedDocument(signReturnV2.getBinaryoutput());
                    return pnSignDocumentResponse;
                })
                .onErrorResume(throwable -> Mono.error(new PnSpapiTemporaryErrorException(throwable.getMessage(), throwable)));
    }

    @Override
    public Mono<PnSignDocumentResponse> pkcs7Signature(byte[] fileBytes, Boolean timestamping) {
        log.debug(CLIENT_METHOD_INVOCATION, ARUBA_PKCS_7_SIGNATURE, timestamping);
        return Mono.fromCallable(() -> {
                    var signRequestV2 = createAuthenticatedSignRequestV2();
                    signRequestV2.setRequiredmark(timestamping);
                    signRequestV2.setBinaryinput(fileBytes);
                    signRequestV2.setTransport(TypeTransport.BYNARYNET);
                    setSignRequestV2WithTsaAuth(timestamping, signRequestV2);
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
                .transform(CHECK_IF_RESPONSE_IS_OK)
                .timeout(Duration.ofSeconds(arubaSignTimeout), Mono.error(new ArubaSignException("Request timeout.")))
                .doOnNext(result -> log.info(CLIENT_METHOD_RETURN, ARUBA_PKCS_7_SIGNATURE, Stream.of(result.getStatus(), result.getReturnCode(), result.getDescription()).toList()))
                .map(signReturnV2 -> {
                    PnSignDocumentResponse pnSignDocumentResponse = new PnSignDocumentResponse();
                    pnSignDocumentResponse.setSignedDocument(signReturnV2.getBinaryoutput());
                    return pnSignDocumentResponse;
                })
                .onErrorResume(throwable -> Mono.error(new PnSpapiTemporaryErrorException(throwable.getMessage(), throwable)));
    }

}
